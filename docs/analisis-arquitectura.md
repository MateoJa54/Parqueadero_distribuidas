# Análisis Arquitectónico — Sistema de Gestión de Parqueaderos

> Revisión senior (rol: Arquitecto de Software / Tech Lead) del sistema real del workspace.
> Alcance: 6 microservicios + PostgreSQL unificado + RabbitMQ + Kong Gateway + dashboard SSE.
> Basado en el código, no en una descripción teórica.

---

## 1. Panorama del sistema

| Componente | Stack | Puerto | Rol |
|---|---|---|---|
| `usuarios` | Spring Boot 3.5 / Java 25 | 8081 | Identidad, roles, **emisor/validador JWT (RS256)** |
| `zonas` | Spring Boot 3.5 / Java 25 | 8080 | Zonas + espacios + **canal SSE** de disponibilidad |
| `vehiculos` | NestJS 11 + TypeORM | 3000 | Catálogo de vehículos (herencia Single Table) |
| `asignaciones` | Spring Boot 3.5 / Java 25 | 8082 | Relación vehículo ↔ propietario/autorización |
| `tickets` | Spring Boot 3.5 / Java 25 | 8083 | **Núcleo**: ingreso/salida/cobro (orquestador) |
| `ms-audit` | NestJS 11 + TypeORM | 3002 | Trail de auditoría (consume RabbitMQ) |
| PostgreSQL 16 | Docker | 5433 | 6 bases lógicas en **una** instancia |
| RabbitMQ | Docker | 5672 | Bus de eventos de auditoría |
| Kong 3.7 (DB-less) | Docker | 8000 | API Gateway (JWT, rate-limit, CORS, correlation-id) |

**Estilo arquitectónico:** microservicios con base de datos por servicio (database-per-service *lógico*), comunicación **síncrona HTTP** para el flujo de negocio y **asíncrona por eventos** solo para auditoría. `tickets` actúa como **orquestador** que consulta `vehiculos → asignaciones → zonas`.

```
Cliente ─► Kong :8000 ─► usuarios / zonas / vehiculos / asignaciones / tickets / ms-audit
                                                │
   tickets (orquestador) ──HTTP──► vehiculos, asignaciones, zonas
   todos ──evento──► RabbitMQ(audit_exchange) ──► ms-audit ──► audit_db
   dashboard ◄──SSE── zonas (/sse/espacios)
```

---

## 2. Evaluación general

### 2.1 Puntos fuertes

- **Separación de responsabilidades limpia.** Cada dominio (identidad, catálogo, ubicación física, relación de propiedad, transacción de parqueo, auditoría) es un servicio con su propia base. Bajo acoplamiento de datos.
- **El invariante crítico está garantizado en la BD, no en memoria.** Los índices únicos parciales `ux_ticket_vehiculo_activo` y `ux_ticket_espacio_activo` (`WHERE estado_ticket='ACTIVO'`) hacen imposible que existan dos tickets activos para el mismo vehículo o espacio. Esto es *exactamente* la solución correcta al problema de "dos autos reservan el mismo espacio". **Este es el mayor acierto del diseño.**
- **Seguridad con firma asimétrica.** JWT **RS256**: Kong solo tiene la clave pública (verificación); la privada nunca sale de `usuarios`. La clave pública en el repo no es un riesgo. Defensa en profundidad: los servicios validan el token *además* de Kong.
- **Auditoría desacoplada por eventos.** RabbitMQ (exchange topic durable) evita que registrar la auditoría acople o frene las operaciones de negocio.
- **Observabilidad de disponibilidad en tiempo real** vía SSE (push), no polling. Elegante y eficiente para el dashboard.
- **Timeouts explícitos** en las llamadas remotas del orquestador (connect 3s / read 5s): una dependencia colgada no bloquea el hilo indefinidamente.
- **Códigos de ticket con secuencia de BD** (`nextval`), atómica, en vez del inseguro `count()+1`.

### 2.2 Puntos débiles

- **Consistencia distribuida frágil (dual-write).** El estado de ocupación vive **duplicado**: en `tickets.estado_ticket` y en `zonas.espacio.estado`. Se sincronizan con un PATCH HTTP sin saga/outbox → pueden divergir ante fallos parciales.
- **Un solo servidor PostgreSQL** para las 6 bases: aísla esquemas pero **no** infraestructura → punto único de fallo y de contención.
- **Los microservicios no están contenedorizados.** Solo BD, RabbitMQ y Kong corren en Docker; los servicios se levantan a mano en el host. No es reproducible ni orquestable.
- **Exposición directa de puertos del host** (8081, 8083…): un cliente puede saltarse Kong (y por tanto el rate-limiting y el correlation-id). El JWT in-service mitiga, pero no el resto de plugins.
- **Sin paginación** en ningún listado (`findAll` devuelve todo). No escala con el volumen.
- **Sin resiliencia** (circuit breaker / retry / bulkhead) en la cadena síncrona `tickets → 3 servicios`: riesgo de fallo en cascada.
- **Taxonomía de tipos inconsistente** entre `vehiculos` y `zonas`, parcheada con un mapeador (`CompatibilidadTipos`) → deuda técnica.

**Veredicto:** cada microservicio está bien construido a nivel individual; los riesgos son **sistémicos** (consistencia distribuida, infraestructura, escalabilidad de lecturas).

---

## 3. Análisis por componente

### 3.1 Base de datos / Modelo relacional

| Aspecto | Observación |
|---|---|
| Aislamiento | ✅ Base por servicio (correcto para microservicios). |
| Infraestructura | ⚠️ Las 6 bases en **una** instancia → SPOF y acoplamiento operativo. |
| Denormalización | ✅ `tickets` guarda *snapshots* (`placa`, `tipoVehiculo`, `codigoEspacio`) → autonomía sin joins entre servicios. Bien aplicado. |
| Integridad | ✅ Índices únicos parciales + secuencia + FKs internas coherentes. |
| Migraciones | ⚠️ `ddl-auto=update` + `schema.sql`. `update` no borra columnas viejas ni versiona cambios; peligroso fuera de dev. |
| Doble fuente de verdad | ❌ Ocupación en `tickets` **y** en `zonas.espacio.estado`. |
| Paginación | ❌ Ausente. |

**Recomendación:** migrar a **Flyway/Liquibase** (versionado real), separar al menos `audit_db` a su propia instancia, y definir `zonas.espacio.estado` como **proyección derivada** de tickets (no fuente de verdad independiente).

### 3.2 Backend

| Aspecto | Observación |
|---|---|
| Orquestación | `tickets` encadena 3 llamadas HTTP síncronas antes de insertar. Latencia acumulada + acoplamiento temporal. |
| Manejo de concurrencia | ✅ **Excelente**: `saveAndFlush` + captura de `DataIntegrityViolationException` traducida a error de negocio 409, respaldado por índice único parcial. La verificación en memoria es solo *fail-fast*; la garantía real es la BD. |
| Transacción distribuida | ❌ Tras el INSERT, `catalogo.cambiarEstadoEspacio(OCUPADO)` es un PATCH remoto. Si falla, `@Transactional` revierte el ticket (bien), pero si el PATCH **tiene éxito y el commit local falla**, el espacio queda OCUPADO sin ticket → **estado huérfano**. No hay compensación. |
| Evento vs commit | ❌ `auditPublisher.publicar(...)` se ejecuta **dentro** de `@Transactional`, antes del commit → si el commit falla, ya se publicó un evento fantasma (**dual-write** clásico). |
| Resiliencia | ❌ Sin circuit breaker / retry / fallback (Resilience4j). |
| DTOs/capas | ✅ Mapper + DTOs de request/response; excepciones de dominio con `GlobalExceptionHandler`. |
| Validación | ✅ Reglas de negocio y compatibilidad de tipos bien encapsuladas. |

### 3.3 Frontend (dashboard de monitoreo)

| Aspecto | Observación |
|---|---|
| Tecnología | HTML/CSS/JS plano + `EventSource` (SSE) contra `zonas`. |
| Tiempo real | ✅ Recibe `snapshot` + `espacio-actualizado` por push. Buen patrón. |
| Seguridad | ⚠️ `EventSource` **no envía el token** → se conecta **directo** al puerto 8080 saltando Kong. El canal SSE queda sin autenticación. |
| Alcance | ⚠️ Solo lectura de espacios; no hay consola operativa (cobro/anulación) ni consumo de auditoría. |

**Recomendación:** proteger el SSE (token por query param o cookie + validación en `zonas`), o exponerlo por Kong. Considerar un front con framework (Angular/React) si el panel crece.

### 3.4 Infraestructura / Gateway

| Aspecto | Observación |
|---|---|
| Kong | ✅ DB-less declarativo, JWT RS256, CORS, correlation-id. Limpio y versionado. |
| Rate limiting | ⚠️ `policy: local` → el conteo es **por nodo**; con varias instancias de Kong el límite real se multiplica. Usar `redis` para límite global. |
| CORS | ⚠️ `origins: "*"`. Aceptable con `credentials:false`, pero conviene restringir a orígenes conocidos. |
| Descubrimiento | ⚠️ URLs *hardcoded* (`host.docker.internal:puerto`). Sin service discovery ni balanceo. |
| Contenedores | ❌ Servicios de negocio fuera de Docker → no reproducible, sin health-checks agregados. |
| Observabilidad | ⚠️ Hay `X-Correlation-ID` pero **no** hay colector de logs/trazas (ELK / OpenTelemetry / Jaeger) que lo aproveche. |
| Secretos | ⚠️ Contraseñas de BD en texto plano en `init-db/01-init.sql` y `docker-compose.yml`. Aceptable en académico; inaceptable en producción. |

---

## 4. Concurrencia — el caso "dos autos, un espacio"

Este es el escenario central y el diseño lo resuelve **bien**. Flujo actual de `registrarIngreso`:

1. Lee vehículo (HTTP) → valida activo.
2. Lee asignación activa (HTTP) → valida vigencia/autorización.
3. `findByIdVehiculo...ACTIVO` → *fail-fast* si el vehículo ya tiene ticket.
4. Lee espacio (HTTP) → valida `DISPONIBLE` + compatibilidad de tipos.
5. `findByIdEspacio...ACTIVO` → *fail-fast* si el espacio ya está ocupado.
6. `saveAndFlush(ticket)` → **la BD decide**: el índice único parcial rechaza al segundo INSERT concurrente.
7. PATCH remoto `espacio → OCUPADO`.

Los pasos 3–5 son lecturas potencialmente *stale* (dos instancias podrían leer `DISPONIBLE` a la vez), pero **el paso 6 cierra la ventana de carrera de forma atómica**. Correcto.

### Debilidad residual

| Riesgo | Detalle | Mitigación propuesta |
|---|---|---|
| Estado huérfano | INSERT ok + PATCH ok + commit local falla → espacio OCUPADO sin ticket | Patrón **Outbox** + proyección de estado dirigida por eventos; o reconciliación periódica. |
| Evento fantasma | Publicar auditoría antes del commit | Publicar en `AFTER_COMMIT` (`@TransactionalEventListener`) o vía outbox. |
| Doble verdad de ocupación | `zonas.espacio.estado` puede divergir de `tickets` | Hacer de `tickets` la única fuente y proyectar a `zonas` por evento. |

---

## 5. Antipatrones y vulnerabilidades detectados

| # | Tipo | Hallazgo | Severidad |
|---|---|---|---|
| 1 | Antipatrón | **Dual-write** BD local + PATCH remoto sin saga/outbox | Alta |
| 2 | Antipatrón | Evento de auditoría publicado antes del commit | Media |
| 3 | Antipatrón | Doble fuente de verdad de ocupación | Media |
| 4 | Seguridad | Puertos de servicio expuestos en el host → *bypass* de Kong (rate-limit/correlation) | Media |
| 5 | Seguridad | Canal SSE sin autenticación (fuera de Kong) | Media |
| 6 | Seguridad | Secretos en texto plano en repo | Baja (académico) / Alta (prod) |
| 7 | Seguridad | Sin revocación de JWT (logout no invalida token vigente) | Baja |
| 8 | Escalabilidad | Sin paginación en listados | Media |
| 9 | Resiliencia | Cadena HTTP síncrona sin circuit breaker → fallo en cascada | Media |
| 10 | Operación | `ddl-auto=update` como estrategia de esquema | Media |
| 11 | Diseño | Taxonomía de tipos inconsistente (parche con mapeador) | Baja |
| 12 | Infra | Rate-limit `policy: local` (no global) | Baja |

---

## 6. Propuestas de mejora — Estado actual vs. propuesto

### 6.1 Consistencia y datos

| Área | Estado actual | Estado propuesto | Patrón |
|---|---|---|---|
| Ocupación de espacio | Dual-write (INSERT + PATCH HTTP) | `tickets` única fuente; `zonas` recibe evento y proyecta | **Transactional Outbox + Event Carried State** |
| Auditoría | `publicar()` dentro de la transacción | Publicar tras commit / desde outbox | **`@TransactionalEventListener(AFTER_COMMIT)`** |
| Estado huérfano | Sin compensación | Reconciliación por evento o job idempotente | **Saga / Reconciliation** |
| Migraciones | `ddl-auto=update` + `schema.sql` | Scripts versionados | **Flyway / Liquibase** |
| Listados | Devuelven todo | `Pageable` + `page/size/sort` | **Paginación** |

### 6.2 Resiliencia y rendimiento

| Área | Estado actual | Estado propuesto | Patrón |
|---|---|---|---|
| Llamadas del orquestador | Timeout, sin más | + retry con backoff, circuit breaker, fallback | **Resilience4j** |
| Cadena síncrona | 3 llamadas en serie | Cachear catálogos poco cambiantes / paralelizar lecturas independientes | **Cache-aside / paralelización** |
| Rate limiting | `policy: local` | `policy: redis` (límite global) | **Rate limiting distribuido** |

### 6.3 Seguridad

| Área | Estado actual | Estado propuesto |
|---|---|---|
| Acceso a servicios | Puertos del host abiertos | Servicios en red interna Docker; **solo** Kong expuesto |
| SSE | Sin auth, directo a `zonas` | Token validado en `zonas` o exposición por Kong |
| Secretos | Texto plano en repo | Variables de entorno / Docker secrets / Vault |
| CORS | `*` | Lista blanca de orígenes |
| Revocación JWT | No existe | Lista de revocación (Redis) o refresh rotativo con `jti` |

### 6.4 Infraestructura y operación

| Área | Estado actual | Estado propuesto |
|---|---|---|
| Despliegue de servicios | Manual en el host | **Contenedorizar** cada servicio + `docker-compose` único (o K8s) |
| Descubrimiento | URLs *hardcoded* | Service discovery / DNS interno de Docker/K8s |
| Observabilidad | Solo `X-Correlation-ID` | **OpenTelemetry** + Jaeger (trazas) + logs centralizados (ELK/Loki) |
| BD | 1 instancia, 6 bases | Separar `audit_db` (carga distinta); backups por servicio |
| Salud | Health-check de BD/RabbitMQ | `/actuator/health` + `depends_on: healthy` para todo |

---

## 7. Priorización recomendada (roadmap)

1. **Cerrar el dual-write** (Outbox + evento de ocupación + auditoría `AFTER_COMMIT`). *Impacto: consistencia — el riesgo #1.*
2. **Contenedorizar los servicios** y cerrar puertos del host (solo Kong hacia afuera). *Impacto: seguridad + reproducibilidad.*
3. **Paginación** en todos los listados. *Impacto: escalabilidad, bajo esfuerzo.*
4. **Resiliencia** (Resilience4j) en el orquestador. *Impacto: disponibilidad.*
5. **Migraciones versionadas** (Flyway) y **rate-limit distribuido** (Redis).
6. **Unificar taxonomía** de tipos y **trazabilidad distribuida** (OpenTelemetry).

---

## 8. Conclusión

El sistema demuestra **madurez de diseño a nivel de microservicio**: la garantía de unicidad de ticket activo por espacio/vehículo mediante índices únicos parciales es la solución correcta y bien implementada al problema de concurrencia, y la firma RS256 con validación en profundidad es sólida. Los riesgos reales no están en los servicios individuales sino en la **coordinación distribuida** (dual-write, estado huérfano, doble fuente de verdad) y en la **operación** (servicios fuera de contenedor, sin migraciones versionadas, sin resiliencia ni trazabilidad). Abordando el patrón **Outbox** y la contenedorización se elimina la mayor parte del riesgo sistémico sin reescribir la lógica de negocio.
