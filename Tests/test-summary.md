# Test Summary Report — Informe de Cierre (Bloque C)

**Proyecto auditado:** Parking System — Distributed Microservices
**Fecha de corrida:** 2026-07-20
**Responsable:** Persona 4 — Pruebas Dinámicas, Defectos e Informe de Cierre
**Ambiente:** local (Docker: Postgres 16 + RabbitMQ + Kong 3.7) + los 6 microservicios + frontend

> Nota: los criterios de Entry/Exit formales y las métricas oficiales de esta sección los define P2 (Sec. 5.1 del SQAP). Este informe reporta los resultados obtenidos hasta ahora; el cierre formal del ciclo de pruebas queda sujeto a que P2 confirme el % mínimo de aprobación aceptado.

## 1. Resumen ejecutivo

| Suite | Casos | Pasaron | Fallaron | % Pass |
|---|---|---|---|---|
| API funcional (Postman/Newman, `QA_Parqueaderos.postman_collection.json`) | 40 | 40 | 0 | 100% |
| Unitarias frontend (Vitest + RTL) | 36 | 36 | 0 | 100% |
| E2E-01 Login (manual, navegador) | 7 (6 guionizados + 1 exploratorio) | 6 | 1 | 86% |
| E2E-02 Navegación RBAC (manual, navegador) | 8 | 7 | 1 | 87.5% |
| E2E-03 Emisión y cobro de ticket (manual, navegador) | 11 (10 guionizados + 1 bonus) | 10 | 1 | 91% |
| **Total ejecutado** | **102** | **99** | **3** | **97.1%** |

**Defectos encontrados:** 4 — ver `defects.md`.
- DEF-01 (cosmético): mensajes sin tildes en `usuarios`.
- DEF-02 (menor/UX): redirect post-login no valida permiso del `from`.
- DEF-03 (**mayor/alta**): "Mi perfil" del portal cliente sin datos personales por desalineación de contrato API.
- DEF-04 (**crítico/alta**): el modal de anulación de tickets pierde el foco en cada tecla y se cierra solo si el motivo contiene un espacio — bloquea de facto anular tickets desde la UI con un motivo real. Backend verificado correcto vía API directa.

Assertions totales en la suite API: 117/117.

## 2. Resultado por módulo (API)

| Módulo (CI) | Casos | Pass | Fail |
|---|---|---|---|
| Auth / usuarios (CI-01) | 5 | 5 | 0 |
| Personas | 3 | 3 | 0 |
| Usuarios (registro) | 2 | 2 | 0 |
| Zonas (CI-02) | 3 | 3 | 0 |
| Espacios | 5 | 5 | 0 |
| Vehículos (CI-03) | 4 | 4 | 0 |
| Asignaciones-vehículos (CI-04) | 4 | 4 | 0 |
| Tickets (CI-05) | 11 | 11 | 0 |
| Auditoría (CI-06) | 3 | 3 | 0 |

## 3. Resultado por módulo (Frontend, CI-07)

| Área | Casos | Pass | Fail | Tipo |
|---|---|---|---|---|
| RBAC (`rbac.ts`) | 16 | 16 | 0 | Unitaria (Vitest) |
| Formato/validación (`format.ts`) | 18 | 18 | 0 | Unitaria (Vitest) |
| Guards de ruta (`guards.tsx`) | 5 | 5 | 0 | Unitaria/componente (Vitest + RTL) |
| Login / sesión (E2E-01) | 7 | 6 | 1 | Manual (navegador) |
| Navegación RBAC en UI (E2E-02) | 8 | 7 | 1 | Manual (navegador) |
| Emisión/cobro/anulación de ticket en UI (E2E-03) | 11 | 10 | 1 | Manual (navegador) |

## 4. Densidad de defectos

| Módulo | Defectos encontrados | Casos ejecutados | Densidad |
|---|---|---|---|
| API (6 microservicios) | 0 | 40 | 0 |
| Frontend — unitarias (RBAC/formato/guards) | 0 | 36 | 0 |
| Frontend — E2E manual (login, navegación RBAC, tickets) | 4 (DEF-01, DEF-02, DEF-03, DEF-04) | 26 | 0.15 defectos/caso |
| **Total** | **4** | **102** | **0.039 defectos/caso** |

Dos defectos son de severidad baja/media (DEF-01, DEF-02); dos son de severidad **mayor/crítica** y prioridad **alta**:
- **DEF-03**: "Mi perfil" del portal cliente no muestra datos personales porque el frontend espera campos (`firstName`, `dni`, `email`) que la API de `usuarios` nunca devuelve al propio cliente — hueco de contrato API, no solo bug de UI.
- **DEF-04**: el modal de anulación de tickets (`Modal.tsx` + `TicketsPage.tsx`) pierde el foco del campo de texto en cada pulsación por un `useEffect` con dependencia mal memoizada, y se cierra solo cuando el motivo contiene un espacio (la tecla Espacio activa por accidente el botón "✕" del header, que queda enfocado). **Bloquea de facto la anulación de tickets desde la UI con cualquier motivo real en español.** Backend verificado correcto vía API directa (curl).

Ninguno bloquea el flujo de login o consulta, pero DEF-04 sí bloquea una operación central de negocio (anular tickets) desde la interfaz. Varias pantallas admin/portal (ver `traceability-matrix.xlsx`, filas `NO_CUBIERTO`) siguen sin ejecutarse — la densidad puede subir al completarlas.

## 5. Cobertura y huecos conocidos (honestidad sobre completitud)

- **Sin cobertura QA formal:** CRUD de Roles (existe en la colección de desarrollo `Gestion_Parqueaderos` pero no está replicado en la colección QA).
- **Sin cobertura de frontend más allá de lo unitario:** pantallas de gestión A2–A9 y A11 (panel admin), C2–C6 (portal cliente) — ver Sec. 4 de `docs/frontend-pantallas.md`. Los 3 guiones manuales en `Tests/manual/` cubren los flujos más críticos (login, RBAC, tickets) pero no el resto.
- Esto es consistente con el riesgo **RP-05** (Sec. 14 del SQAP): frontend agregado tarde al alcance, prioridad alta, cobertura parcial.

## 6. Conclusión de esta corrida

El backend (6 microservicios) auditado vía la colección QA existente no presenta defectos funcionales detectables con los 40 casos diseñados — se verificó además, vía llamadas directas a la API, que las operaciones de negocio de `tickets` (cobrar, anular) funcionan correctamente a ese nivel. La lógica crítica de seguridad del frontend (control de acceso por rol) está cubierta y en verde a nivel unitario, y la verificación manual en navegador (E2E-01, E2E-02) confirma que el enforcement de RBAC funciona correctamente en la UI real, no solo en aislamiento (ningún rol de menor privilegio logró acceder a una sección restringida).

Se encontraron 4 defectos reales durante la ejecución manual. Los dos más relevantes para priorizar antes de la entrega:
- **DEF-04 (crítico)**: el modal de anulación de tickets es inutilizable en la práctica — pierde el foco en cada tecla y se cierra solo al escribir un motivo con espacio, impidiendo anular tickets desde la UI. Es el hallazgo más importante de esta ronda porque afecta directamente a `tickets` (CI-05), el módulo que el propio equipo ya había identificado como de mayor riesgo (RP-01) antes de esta auditoría.
- **DEF-03 (mayor)**: la pantalla "Mi perfil" del portal cliente no muestra ningún dato personal por una desalineación entre lo que expone la API de `usuarios` y lo que el frontend espera recibir.

Los otros dos (DEF-01 cosmético, DEF-02 UX) son de impacto bajo. Con las 3 ejecuciones manuales (E2E-01, E2E-02, E2E-03) completas, queda pendiente únicamente cubrir las pantallas de gestión restantes (ver `traceability-matrix.xlsx`, filas `NO_CUBIERTO`) antes de declarar el cierre completo de Ejecución y Evidencias para CI-07.
