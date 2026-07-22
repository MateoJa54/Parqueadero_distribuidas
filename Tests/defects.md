# Registro de defectos — Parking System (Distributed Microservices)

Formato: id, módulo, severidad, prioridad, pasos para reproducir, estado. Herramienta de gestión de incidencias confirmada (Sección 8 del SQAP): **GitHub Issues** del propio repositorio, enlazado 1:1 con este registro — este archivo mantiene el detalle completo (causa raíz, pasos, evidencia) y cada Issue da seguimiento visual (estado abierto/cerrado, labels de severidad/prioridad, y cierre automático al referenciar el commit que corrige el defecto). Se prefirió sobre Jira/Trello por no requerir cuenta externa ni tablero nuevo: ya viven en la misma plataforma que el código.

Severidad: BLOQUEANTE / CRÍTICA / MAYOR / MENOR / COSMÉTICA
Prioridad: ALTA / MEDIA / BAJA
Estado: ABIERTO / EN VERIFICACIÓN / CERRADO / NO ES DEFECTO

---

## Resumen

| Corrida | Defectos encontrados |
|---|---|
| 2026-07-20 — Newman `QA_Parqueaderos.postman_collection.json` (40 requests / 117 assertions) | **0** |
| 2026-07-20 — Vitest frontend (36 tests) | **0** |
| 2026-07-20 — E2E-01 Login (manual, vía automatización de navegador) | **2** (DEF-01, DEF-02) |
| 2026-07-20 — E2E-02 Navegación RBAC (manual, vía automatización de navegador) | **1** (DEF-03) |
| 2026-07-20 — E2E-03 Emisión y cobro de ticket (manual, vía automatización de navegador) | **1** (DEF-04, crítico) |

---

### DEF-01
- **Módulo:** usuarios (CI-01, backend)
- **Severidad:** COSMÉTICA
- **Prioridad:** BAJA
- **Detectado en:** E2E-01 (Login), paso 2
- **Pasos para reproducir:**
  1. Ir a `/login` en el frontend.
  2. Ingresar cualquier usuario válido con contraseña incorrecta.
  3. Enviar el formulario.
- **Resultado esperado:** "Usuario o contraseña incorrectos"
- **Resultado obtenido:** "Usuario o **contrasena** incorrectos" (falta la ñ)
- **Causa raíz:** `usuarios/src/main/java/ec/edu/espe/usuarios/services/Impl/AuthServicioImpl.java`, líneas 70, 73 y 76 — el mensaje está escrito sin tildes/ñ en el código fuente. El mismo patrón se repite en líneas 182, 190 y 196 de la misma clase ("invalido", "sesion", "esta inactiva"). El frontend solo reproduce el texto que recibe de la API; no es un bug del frontend.
- **Evidencia:** [`Tests/evidence/DEF-01-login-sin-tildes.png`](evidence/DEF-01-login-sin-tildes.png) — captura real, ver también `Tests/manual/E2E-01-login.md`.
- **Issue:** [#10](https://github.com/MateoJa54/Parqueadero_distribuidas/issues/10)
- **Estado:** ABIERTO

### DEF-02
- **Módulo:** Frontend (CI-07) — `frontend/src/pages/auth/LoginPage.tsx`
- **Severidad:** MENOR (no es una brecha de seguridad — `RequirePermiso` sigue bloqueando el acceso correctamente; es un problema de experiencia de usuario)
- **Prioridad:** MEDIA
- **Detectado en:** E2E-01 (Login), paso 6b (variante exploratoria)
- **Pasos para reproducir:**
  1. Sin sesión activa, navegar directamente a una ruta protegida por permiso que un rol de menor privilegio no tiene, p. ej. `/app` (requiere permiso `dashboard`).
  2. `RequireAuth` redirige a `/login` guardando `state: { from: '/app' }`.
  3. Loguear con un usuario que **no** tiene permiso sobre `/app` (p. ej. `qa.cliente`, rol CLIENTE).
- **Resultado esperado:** el login debería aterrizar al usuario en su ruta inicial correcta según su rol (`rutaInicial(roles)` → `/portal` para CLIENTE), ignorando un `from` para el que no tiene permiso.
- **Resultado obtenido:** navega ciegamente a `from` (`/app`), y como CLIENTE no tiene permiso, `RequirePermiso` lo rebota a `/403 Acceso denegado` — pantalla confusa en vez de un login normal.
- **Causa raíz:** `LoginPage.tsx:27` — `navigate(from ?? rutaInicial(u.roles), { replace: true })` usa `from` sin verificar que el usuario recién autenticado tenga permiso sobre esa ruta.
- **Sugerencia de corrección:** validar `puede(u.roles, permisoDeRuta(from))` antes de usar `from`; si no aplica, usar `rutaInicial(u.roles)`.
- **Evidencia:** [`Tests/evidence/DEF-02-redirect-403-tras-login.png`](evidence/DEF-02-redirect-403-tras-login.png) — captura real mostrando la pantalla "Acceso denegado" tras el login contaminado; ver también `Tests/manual/E2E-01-login.md`.
- **Issue:** [#11](https://github.com/MateoJa54/Parqueadero_distribuidas/issues/11)
- **Estado:** ABIERTO

### DEF-03
- **Módulo:** Frontend (CI-07) `PerfilPage.tsx` + contrato de API `usuarios` (CI-01)
- **Severidad:** MAYOR (pantalla completa del portal cliente muestra datos vacíos para todo usuario CLIENTE)
- **Prioridad:** ALTA
- **Detectado en:** E2E-02 (Navegación RBAC), durante la revisión de "Mi perfil" (C3) como parte del recorrido del portal cliente
- **Pasos para reproducir:**
  1. Loguear como cualquier usuario CLIENTE (p. ej. `qa.cliente`).
  2. Ir a "Mi perfil" (`/portal`).
- **Resultado esperado:** se muestran Nombre, Cédula y Correo reales del usuario.
- **Resultado obtenido:** Nombre, Cédula y Correo aparecen completamente en blanco (ni siquiera el fallback "—" que sí usan Teléfono/Nacionalidad).
- **Causa raíz (confirmada con curl directo a la API, no solo inspección de código):**
  - `frontend/src/pages/portal/PerfilPage.tsx` tipa la respuesta de `authApi.me()` como `Persona` y lee `persona?.firstName`, `persona?.lastName`, `persona?.dni`, `persona?.email`.
  - Pero `GET /api/v1/auth/me` en runtime devuelve **`{ idUsuario, username, nombreCompleto, active, roles }`** — ninguno de esos campos existe en la respuesta real. El campo `nombreCompleto` que sí trae la respuesta no se usa en ningún lado.
  - Se investigó una alternativa: `GET /api/v1/usuarios/{id}` (accesible al propio dueño) tampoco expone `dni`/`email`/`phone`/`nationality` — esos campos **solo existen en `PersonaController`**, restringido a `ADMIN`/`ROOT` (ver Sec. 2.2 de `docs/contrato-api-microservicios.md`). Es decir: **no existe hoy ningún endpoint que un CLIENTE pueda usar para ver su propia cédula/correo/teléfono** — es un hueco de diseño de API, no solo un bug de tipado del frontend.
- **Sugerencia de corrección:** (a) corto plazo — usar `persona.nombreCompleto` (que sí llega) en vez de `firstName/middleName/lastName`, y ocultar Cédula/Correo/Teléfono/Nacionalidad del portal cliente ya que no hay fuente de datos; (b) correcto — exponer un endpoint tipo `GET /personas/me` o `GET /personas/{id}` accesible al propio dueño (mismo patrón ya usado en `GET /propietarios/{idUsuario}/vehiculos` para "Mis vehículos").
- **Evidencia:** [`Tests/evidence/DEF-03-mi-perfil-vacio.png`](evidence/DEF-03-mi-perfil-vacio.png) — captura real mostrando Nombre/Cédula/Correo en blanco vs. el fallback "—" de Teléfono/Nacionalidad; respuesta cruda de `GET /api/v1/auth/me` y `GET /api/v1/usuarios/{id}` vía curl documentada en `Tests/manual/E2E-02-rbac-navegacion.md`.
- **Issue:** [#12](https://github.com/MateoJa54/Parqueadero_distribuidas/issues/12)
- **Estado:** ABIERTO

### DEF-04
- **Módulo:** Frontend (CI-07) `frontend/src/ui/Modal.tsx` + `frontend/src/pages/admin/TicketsPage.tsx`
- **Severidad:** CRÍTICA (bloquea funcionalmente anular un ticket con un motivo normal desde la UI)
- **Prioridad:** ALTA
- **Detectado en:** E2E-03 (Emisión y cobro de ticket), paso 9 (anular con motivo)
- **Pasos para reproducir:**
  1. Como RECAUDADOR, en Tickets → Listado, hacer clic en "Anular" sobre un ticket ACTIVO.
  2. En el modal "Anular ticket", hacer clic en el campo "Motivo de anulación".
  3. Escribir cualquier texto de más de 1 carácter.
- **Resultado esperado:** el campo acumula todo el texto escrito; si el texto contiene un espacio, el campo simplemente contiene ese espacio.
- **Resultado obtenido:** **solo el primer carácter tecleado llega al campo** (se probó escribiendo 21 caracteres seguidos y el campo quedó con 1 solo carácter: "e"). Si el motivo contiene un espacio en cualquier posición después del primer carácter, **el modal se cierra por completo** sin anular el ticket (probado con "Cliente decidio no ocupar el espacio" → el modal se cerró a mitad de la escritura, sin llamar a la API, dejando el ticket ACTIVO).
- **Causa raíz (confirmada leyendo el código, reproducido con clics por referencia de elemento — no es un artefacto de la automatización):**
  - En `TicketsPage.tsx`, el estado `anular` (`{open, ticket, motivo}`) se actualiza en cada pulsación vía `onChange={(e) => setAnular({ ...anular, motivo: e.target.value })}` (línea 310), lo que re-renderiza `ListadoTab` en cada tecla.
  - La prop `onClose` que se pasa al `<Modal>` es una función **inline nueva en cada render** (`onClose={() => setAnular({ open: false, motivo: '' })}`, línea 290) — no está memoizada con `useCallback`.
  - En `Modal.tsx`, el `useEffect` que maneja el foco (líneas 18-57) tiene `[open, onClose]` como dependencias. Como `onClose` cambia de referencia en cada tecla, el efecto se desmonta y remonta en cada pulsación, y la parte de "foco inicial al primer control" (línea 31: `focusables()[0]?.focus()`) **vuelve a mover el foco al primer elemento focuseable del modal — que es el botón "✕" de cerrar del header, no el campo de texto** (el botón "✕" está antes del `modal-body` en el DOM).
  - Como el foco salta al botón "✕" tras la primera tecla, los caracteres siguientes no llegan al input. Y como un `<button>` HTML se activa con la tecla **Espacio** (comportamiento estándar del navegador), si el motivo contiene un espacio en cualquier punto posterior, esa pulsación **activa el botón "✕" y cierra el modal** vía su `onClick={onClose}`.
  - Se verificó a nivel de API (`PATCH /api/v1/tickets/{id}/anular`, vía curl) que el backend anula correctamente (`estadoTicket: ANULADO`, `valorRecaudado: 0`) — el defecto es exclusivamente del modal del frontend.
- **Sugerencia de corrección:** memoizar `onClose` con `useCallback` en `TicketsPage.tsx`, y/o separar en `Modal.tsx` el efecto de "foco inicial al abrir" (dependencia solo `[open]`, para que se ejecute una única vez al abrir el modal) del efecto que registra el listener de teclado para Escape/Tab.
- **Evidencia:** [`Tests/evidence/DEF-04-modal-anular-foco-perdido.png`](evidence/DEF-04-modal-anular-foco-perdido.png) — captura real: campo "Motivo de anulación" completamente vacío tras escribir "Cliente no llego a tiempo", con el anillo de foco azul visible en el botón "✕" (confirma visualmente la causa raíz). Confirmación cruzada vía `curl` de que el backend sí anula correctamente. Ver también `Tests/manual/E2E-03-emision-ticket.md`.
- **Issue:** [#13](https://github.com/MateoJa54/Parqueadero_distribuidas/issues/13) — bloquea el Criterio de Salida X-04 del SQAP (Sección 6.4); ver Apéndice C.7.
- **Estado:** ABIERTO

## Nota — bug ya documentado fuera de este flujo

El defecto RP-02 del SQAP (Sec. 14, Tabla 14-1) — `RABBITMQ_ROUTING_KEY` sin comillas trunca el binding de la cola de auditoría (dotenv trata `#` como comentario) — fue detectado durante el análisis estático/configuración (P3), no por esta suite dinámica. Se referencia aquí por trazabilidad, pero su origen y evidencia completa viven en el reporte de análisis estático de P3.

---

## Plantilla para nuevas entradas

```
### DEF-XX
- **Módulo:**
- **Severidad:**
- **Prioridad:**
- **Detectado en:** (caso de prueba / guion manual)
- **Pasos para reproducir:**
  1.
  2.
- **Resultado esperado:**
- **Resultado obtenido:**
- **Evidencia:** (captura / log)
- **Estado:**
```
