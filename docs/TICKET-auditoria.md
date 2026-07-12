# Ticket: Sistema de Auditoría Centralizado (ms-audit + RabbitMQ)

**Tipo:** Feature / Infraestructura
**Componentes afectados:** `ms-audit` (nuevo), `vehiculos`, `zonas`, `usuarios`, `asignaciones`, `tickets`, `docker-compose.yml`, `init-db/`, `gateway/kong.yml`, `postman/`

---

## 1. Objetivo

Dotar al sistema de un **trail de auditoría centralizado**: cada microservicio publica un evento por cada operación de escritura relevante (alta, edición, activar/desactivar, login) hacia un bus de mensajería (RabbitMQ), y un microservicio dedicado (`ms-audit`) los consume, valida y persiste, exponiéndolos después por una API REST protegida.

La configuración base (nombres de exchange/cola, forma del DTO, reglas de validación) se tomó como referencia de un proyecto anterior (`parking-App/ms-audit`) y se adaptó/corrigió para este repositorio.

---

## 2. Arquitectura general

```
                         ┌──────────────────────────────┐
 usuarios (8081) ───┐    │                              │
 zonas (8080)   ────┤    │   RabbitMQ (topic exchange)  │
 asignaciones(8082)─┼───►│   audit_exchange              │──► ms-audit (3002)
 tickets (8083) ────┤    │   routing key: audit.event    │      │
 vehiculos (3000)───┘    │   binding: audit.#            │      ▼
                         └──────────────────────────────┘   PostgreSQL
                                                              audit_db
                                                          tabla evento_auditoria
```

- Todos los microservicios publican con la routing key **`audit.event`**.
- `ms-audit` liga su cola (`audit_queue`) con el patrón **`audit.#`** (wildcard), así que capturaría cualquier variante futura tipo `audit.<algo>` sin cambiar el binding.
- El exchange es `topic`, `durable: true` (sobrevive reinicios de RabbitMQ).
- Todo pasa (opcionalmente) por **Kong** como gateway único de entrada; cada servicio también valida JWT a nivel de aplicación (defensa en profundidad).

---

## 3. `ms-audit`: cómo se construyó

### 3.1 Stack y librerías

| Librería | Uso |
|---|---|
| **NestJS 11** (`@nestjs/common`, `@nestjs/core`, `@nestjs/platform-express`) | Framework base, DI, HTTP |
| **@nestjs/config** | Carga de `.env` |
| **@nestjs/typeorm** + **typeorm** + **pg** | Persistencia en PostgreSQL |
| **@nestjs/throttler** | Rate limiting de los endpoints REST de consulta |
| **amqplib** | Cliente AMQP **de bajo nivel** (no `@nestjs/microservices`) para conectar, declarar exchange/cola/binding y consumir mensajes a mano — mismo patrón que traía el proyecto de referencia |
| **class-validator** + **class-transformer** | Validación estricta del payload de cada evento antes de guardarlo |
| **jsonwebtoken** | Verificación del JWT compartido (mismo secreto que el resto del sistema) |

### 3.2 Estructura

```
ms-audit/
├── src/
│   ├── main.ts                      # bootstrap, ValidationPipe global, prefijo /api/v1
│   ├── app.module.ts                # ConfigModule, TypeOrmModule, ThrottlerModule, JwtAuthGuard global
│   ├── auth/
│   │   ├── jwt-auth.guard.ts        # valida Authorization: Bearer <jwt> contra el secreto compartido
│   │   └── express.d.ts             # tipado de Request.user
│   └── audit/
│       ├── audit.module.ts
│       ├── audit.controller.ts      # GET /audit, GET /audit/:id, POST /audit (fallback manual)
│       ├── audit.service.ts         # TypeORM: create/findAll/findOne
│       ├── audit.consumer.ts        # conecta a RabbitMQ, declara exchange/cola/binding, consume
│       ├── dto/create-audit.dto.ts  # class-validator: reglas del payload
│       └── entities/evento-auditoria.entity.ts
```

### 3.3 Contrato del evento (`CreateAuditEventDto`)

| Campo | Obligatorio | Regla |
|---|---|---|
| `servicio` | sí | `^(ms-[a-zA-Z]+)$` — ej. `ms-vehiculos` |
| `accion` | sí | uno de `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `SELECT` |
| `entidad` | sí | `^[A-Z-]+$`, 3–20 caracteres — ej. `VEHICULO`, `ZONA` |
| `datos` | no | objeto libre con el registro afectado |
| `usuario` | no | 3–40 caracteres, `^[a-zA-Z0-9._-]+$` (admite username o UUID) |
| `rol` | no | texto libre |
| `ip` | **sí** | IPv4 válida |
| `mac` | **sí** | dirección MAC válida |

Un mensaje que no cumpla el DTO se **descarta** (`channel.nack(msg, false, false)`, sin reencolar) y solo deja un `WARN` en el log de `ms-audit` — ver sección de riesgos.

### 3.4 Seguridad de `ms-audit`

- **Kong**: ruta `/api/v1/audit` con plugins `jwt` + `rate-limiting` (igual que `tickets`).
- **A nivel de aplicación**: `JwtAuthGuard` propio (copiado del patrón de `vehiculos`) aplicado globalmente vía `APP_GUARD`, para que quede protegido aunque alguien acceda directo al puerto 3002 sin pasar por Kong. Verifica firma + emisor + `type: "access"` contra el mismo secreto (`JWT_SECRET`/`JWT_ISSUER`) que usan todos los microservicios.

---

## 4. Infraestructura Docker

**`docker-compose.yml` (raíz)** — se agregó el servicio `rabbitmq`:

```yaml
rabbitmq:
  image: rabbitmq:3-management
  ports:
    - "5672:5672"    # AMQP
    - "15672:15672"  # panel de administracion
  volumes:
    - parqueadero_rabbitmq_data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
```

**`init-db/01-init.sql`** — se agregó la creación de `audit_user`/`audit_db` sobre el mismo Postgres unificado (host 5433), siguiendo el mismo patrón que las demás bases (`zonas`, `usuarios`, `asignaciones`, `tickets`).

> Nota operativa: el script de init solo corre la **primera vez** que se crea el volumen. Si ya tenías el volumen de una instalación previa, `audit_db` hay que crearla a mano una sola vez (documentado en el `README.md`).

---

## 5. El "cliente" de RabbitMQ en cada microservicio

Cada servicio tiene su **propio publisher**, adaptado a su stack, pero todos comparten el mismo contrato de mensaje. No se centralizó en una librería compartida porque son dos ecosistemas distintos (Node/Nest y Java/Spring) sin un mono-repo de paquetes internos.

### 5.1 `vehiculos` (NestJS) — cliente AMQP manual

Archivo: `vehiculos/vehiculos/src/vehiculos/event-publisher.service.ts`

- Librería: **`amqplib`** (igual que `ms-audit`, sin envoltorio de más alto nivel).
- `EventPublisher` (`@Injectable`, `OnModuleInit`/`OnModuleDestroy`):
  - Conecta al arrancar el módulo, declara el exchange (`assertExchange(..., 'topic', { durable: true })`).
  - **Reconexión automática**: si la conexión se cae (`connection.on('close'/'error')`), reintenta cada 5s.
  - `publish(event)` nunca lanza excepción: si RabbitMQ no está disponible, loguea y sigue (la operación de negocio no se ve afectada).
- El **contexto de auditoría** (usuario, rol, ip, mac) se arma en `vehiculos.controller.ts` (función `auditContextDesdeRequest`), leyendo:
  - `usuario`/`rol` del JWT ya decodificado por `JwtAuthGuard` (`request.user`).
  - `ip`: primero `X-Forwarded-For` (para no perder la IP real detrás de Kong), si no existe cae a `req.ip`/`req.socket.remoteAddress`; se normalizan formas IPv6 de loopback (`::1`, `::ffff:...`) a IPv4.
  - `mac`: header `X-Device-Mac` que debe mandar el cliente; si no llega, `00:00:00:00:00:00`.
- `VehiculosService` recibe ese contexto como parámetro explícito en cada método de escritura (`create`, `update`, `activar`, `desactivar`) y llama a `eventPublisher.publish(...)` justo después de guardar en base.

### 5.2 Servicios Spring Boot (`zonas`, `usuarios`, `asignaciones`, `tickets`)

Mismo patrón replicado en los 4, paquete `ec.edu.espe.<servicio>.audit`:

| Librería | Uso |
|---|---|
| `spring-boot-starter-amqp` | Cliente RabbitMQ oficial de Spring (`RabbitTemplate`, `ConnectionFactory`, `TopicExchange`) |
| `jackson-datatype-hibernate6` | Permite serializar entidades JPA con relaciones `LAZY` sin que Jackson falle (ver sección de bugs) |
| `jackson-databind` / `jackson-datatype-jsr310` (ya en el proyecto) | Serialización JSON del evento, incluyendo fechas |

**`AuditConfig.java`**:
- Declara el `TopicExchange` (`audit_exchange`, durable) — así el exchange existe aunque `ms-audit` todavía no haya arrancado.
- Configura el `RabbitTemplate` con un `Jackson2JsonMessageConverter` propio: `ObjectMapper` con `JavaTimeModule` (fechas legibles) + `Hibernate6Module` (relaciones lazy seguras).

**`AuditPublisher.java`** (`@Component`, inyectado en los `*ServicioImpl`):
- Método principal: `publicar(String accion, String entidad, Object datos)` — nunca lanza excepción (try/catch + `log.warn`).
- Overload `publicar(accion, entidad, datos, usuarioExplicito, rolExplicito)` para el caso especial de **LOGIN**, donde todavía no existe una sesión autenticada de la que inferir el actor.
- **`usuario`**: se lee directo del claim `username` del JWT (decodificando el payload del token ya validado por `JwtAuthenticationFilter`, sin volver a verificar la firma), con fallback al UUID (`SecurityContextHolder`) si no se puede leer. Se hizo así — en vez de tocar `JwtAuthenticationFilter` — porque varios controladores (`AuthController.miPerfil`, `TicketController` vía `@AuthenticationPrincipal`) dependen de que el "principal" de Spring Security siga siendo el UUID.
- **`rol`**: primera authority del `Authentication` (se le quita el prefijo `ROLE_`).
- **`ip`**: header `X-Forwarded-For` (primer valor si viene una cadena de proxies) con fallback a `HttpServletRequest.getRemoteAddr()`; misma normalización de loopback IPv6 que en `vehiculos`.
- **`mac`**: header `X-Device-Mac`, con el mismo valor por defecto que `vehiculos`.
- El request/response HTTP en curso se obtiene sin tener que pasarlo por parámetro, vía `RequestContextHolder.currentRequestAttributes()` — así los `*ServicioImpl` no necesitan recibir `HttpServletRequest` en cada método.

**Puntos de enganche** (después de cada `save()` exitoso, nunca antes ni dentro de un bloque revertible):
- `zonas`: `ZonaServicioImpl` (crear/actualizar/activar/desactivar zona), `EspacioServicioImpl` (crear/actualizar/cambiar estado/activar/desactivar espacio).
- `usuarios`: `PersonaServicioImpl`, `UsuarioServicioImpl`, `RolServicioImpl`, `AsignacionServicioImpl` (asignación de roles), `AuthServicioImpl` (LOGIN).
- `asignaciones`: **no se agregó en cada método del servicio** — este microservicio ya tenía un sistema de auditoría local propio (`AssignmentAuditEvent` + `AuditEventListener` + `ApplicationEventPublisher`). Se aprovechó ese único punto de enganche (`AuditEventListener.onAssignmentChanged`) para publicar también hacia RabbitMQ, traduciendo `CREACION→CREATE`, `MODIFICACION→UPDATE`, `ELIMINACION→DELETE`. El log local en la tabla `assignment_audit_events` se mantiene intacto.
- `tickets`: `TicketServiceImpl` (`registrarIngreso`→CREATE, `pagar`→UPDATE, `anular`→UPDATE).

---

## 6. Kong Gateway

Se agregó la entrada `ms-audit` en `gateway/kong.yml`:

```yaml
- name: ms-audit
  url: http://host.docker.internal:3002
  routes:
    - name: ms-audit-routes
      paths: ["/api/v1/audit"]
  plugins:
    - name: rate-limiting
      config: { minute: 100, policy: local }
    - name: jwt
      config: { claims_to_verify: ["exp"] }
```

Mismo criterio que `tickets`: **todas** sus rutas exigen JWT (a diferencia de `zonas`/`vehiculos`, que dejan las lecturas públicas), porque expone datos sensibles (ip, mac, usuario de cada acción del sistema).

---

## 7. Bugs reales encontrados y corregidos (durante pruebas en vivo, no hipotéticos)

| # | Problema | Causa | Fix |
|---|---|---|---|
| 1 | `ms-audit` nunca guardaba nada, aunque los demás servicios publicaran | `RABBITMQ_ROUTING_KEY=audit.#` sin comillas en `.env`: `dotenv` trata `#` como inicio de comentario y trunca el valor a `audit.`, sin el wildcard | Se puso entre comillas: `RABBITMQ_ROUTING_KEY="audit.#"` |
| 2 | El evento `LOGIN` de `usuarios` se perdía en silencio (solo un `WARN`) | `Usuario.persona` es `@OneToOne(LAZY)`; Jackson no puede serializar el proxy de Hibernate sin ayuda | Se registró `Hibernate6Module` en el `ObjectMapper` del `RabbitTemplate` de los 4 servicios Spring |
| 3 | `usuario` salía como UUID (`770bf30a-...`) en Spring Boot pero como username legible en `vehiculos` | `JwtAuthenticationFilter` solo guarda el UUID como "principal" (a propósito, por compatibilidad con otro código) | `AuditPublisher` decodifica el `username` directo del JWT en vez de depender del principal de Spring Security |
| 4 | El hash de la contraseña (`passwordHash`) viajaba dentro de `datos` en cada evento de `Usuario` | Se pasaba la entidad JPA completa como `datos` sin filtrar campos sensibles | `@JsonIgnore` en `Usuario.passwordHash` |
| 5 (menor) | `ip` siempre salía `127.0.0.1` al pasar por Kong | `getRemoteAddr()`/`req.ip` capturan el último salto TCP (Kong), no el cliente real | Se prioriza el header `X-Forwarded-For` (que Kong agrega por defecto), con fallback al remote address |
| 6 (bug pre-existente, no de este ticket) | `vehiculos.service.ts#create` guardaba el vehículo **dos veces** (`save()` llamado dos veces sobre el mismo objeto) | Código heredado, no relacionado con auditoría | Se corrigió de paso al tocar el método para agregar el evento de auditoría |

---

## 8. Consideraciones de seguridad / limitaciones conocidas

- **`ip` vía `X-Forwarded-For`**: cualquiera que llame **directo** al microservicio (sin pasar por Kong) puede mandar ese header con el valor que quiera y el sistema lo va a creer sin verificar. Es dato informativo, no prueba forense. Para cerrar ese hueco haría falta una lista de proxies de confianza (solo aceptar el header si la conexión viene de la IP de Kong) — no implementado, fuera de alcance de este ticket.
- **`mac` autoreportada**: no existe forma técnica de que un servidor HTTP detecte la MAC real de un cliente remoto (solo sería posible en la misma LAN física vía ARP). Por diseño, el cliente la manda voluntariamente en `X-Device-Mac`; sin verificación. Pensado para un escenario de kioskos físicos que conocen su propia MAC, no como control de seguridad fuerte.
- **Resiliencia**: publicar un evento de auditoría **nunca** puede tumbar la operación de negocio que lo origina — todos los publishers (Node y Java) envuelven el `publish`/`convertAndSend` en try/catch y solo loguean un `WARN` si falla.
- **Mensajes inválidos**: `ms-audit` valida cada mensaje contra el DTO y descarta (sin reencolar) los que no cumplan. Esto es silencioso salvo por el log — si en el futuro se necesita trazabilidad de estos descartes, se podría agregar una dead-letter queue.

---

## 9. Herramientas de prueba (Postman)

Se actualizó `postman/Gestion_Parqueaderos.postman_collection.json`:
- **Pre-request script a nivel de colección** que agrega `X-Device-Mac` automáticamente a las 88 requests, leyendo la variable `macDispositivo`.
- Variable `macDispositivo` agregada en la colección y en ambos environments (`Parqueadero-Local` y `Parqueadero-Kong`, con valores distintos para poder diferenciar a simple vista si una petición pasó por Kong).
- Verificado end-to-end corriendo la colección real con `newman` (CLI de Postman): los eventos resultantes en `audit_db` mostraron el `mac` inyectado por el script, no el valor por defecto.

---

## 10. Cómo verificar que todo funciona (smoke test)

```bash
# 1) Infraestructura
docker compose up -d                 # postgres + rabbitmq (raiz)
cd gateway && docker compose up -d   # kong

# 2) Servicios (cada uno en su terminal)
cd usuarios && ./mvnw spring-boot:run
cd zonas && ./mvnw spring-boot:run
cd asignaciones && ./mvnw spring-boot:run
cd tickets && ./mvnw spring-boot:run
cd vehiculos/vehiculos && npm run start:dev
cd ms-audit && npm run start:dev

# 3) Panel de RabbitMQ: http://localhost:15672 (guest/guest)
#    Exchanges -> audit_exchange -> publish_in debe subir con cada accion.

# 4) Flujo real (via Kong, puerto 8000)
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"root","password":"Root2025"}'
# usar el token devuelto:
curl http://localhost:8000/api/v1/audit -H "Authorization: Bearer <TOKEN>"

# 5) Base de datos directamente
docker exec parqueadero-postgres psql -U audit_user -d audit_db \
  -c "SELECT servicio, accion, entidad, usuario, ip, mac, timestamp FROM evento_auditoria ORDER BY timestamp DESC LIMIT 10;"
```

---

## 11. Pendientes / posibles mejoras futuras

- Lista de proxies de confianza para `X-Forwarded-For` (evitar spoofing si el puerto del microservicio queda expuesto sin Kong delante).
- Evento `LOGOUT` (hoy solo `LOGIN` está implementado; el DTO ya soporta el valor).
- Dead-letter queue en RabbitMQ para mensajes inválidos descartados por `ms-audit` (hoy se pierden silenciosamente, solo quedan en el log).
- Extender la auditoría a `Persona`/`Rol` con más granularidad si se requiere para cumplimiento académico específico.
