# Documento de Frontend — Pantallas requeridas (Cliente y Administrador)

> **Propósito:** lista mínima y completa de pantallas para diseñar el frontend en Claude Design.
> Cada pantalla indica **qué muestra**, **qué acciones tiene**, **qué endpoints consume** y **qué validaciones/estados** necesita.
> Todo está alineado con el contrato real de la API (`docs/contrato-api-microservicios.md`).
> **No incluye diseño visual** — solo el "qué necesita".

---

## 0. Contexto técnico que el frontend debe conocer

**Base URLs (local):**
| Recurso | URL base |
|---------|----------|
| usuarios / auth | `http://localhost:8081/api/v1` |
| zonas / espacios | `http://localhost:8080/api/v1` |
| asignaciones-vehículos | `http://localhost:8082/api/v1` |
| tickets | `http://localhost:8083/api/v1` |
| vehículos | `http://localhost:3000/api/vehiculos` |
| auditoría | `http://localhost:3002/api/v1/audit` |

> En producción todo entra por **Kong** (`http://localhost:8000`) con las mismas rutas.

**Autenticación:** JWT RS256. Header en toda petición autenticada: `Authorization: Bearer <token>`.
- El login devuelve: `token`, `refreshToken`, `expiresIn` (7200 s), `refreshExpiresIn` (604800 s), `idUsuario`, `username`, `roles[]`.
- El frontend debe: guardar token, **renovar** con `/auth/refresh` antes de expirar, y **enrutar por `roles[]`**.

**Roles del sistema:** `ROOT`, `ADMIN`, `RECAUDADOR`, `CLIENTE`, `INVITADO`.
- **Cliente** → app de autoservicio (portal cliente).
- **Administrador** → panel de gestión (ADMIN/ROOT) + operación de tickets (RECAUDADOR).

**Formato de error a manejar en UI:**
- Spring: `{ "status", "error", "mensaje", "errores": { campo: msg } }` (el `errores` solo en 400 de validación).
- Nest (vehículos, auditoría): `{ "statusCode", "message", "error" }` (`message` puede ser **array**).

**Estados transversales que TODA pantalla de datos necesita:** `cargando`, `vacío`, `error`, `éxito` (toast).

---

## 1. Realidades de permisos que condicionan el diseño (leer antes de diseñar)

Como ingeniero, estos límites de la API deben reflejarse en el frontend para no diseñar pantallas imposibles:

1. **El registro de cliente NO es 100% autoservicio.** `POST /auth/register` exige un `idPersona` **ya existente**, y crear personas (`POST /personas`) es solo **ADMIN/ROOT**. → El alta de la persona la hace el administrador; el cliente solo crea su **usuario** sobre una persona existente. (Diséñalo como "onboarding asistido").
2. **El cliente NO puede operar tickets.** Los endpoints de tickets (ingreso, pago, anulación, listado) son solo **RECAUDADOR/ADMIN/ROOT**. → El portal del cliente es **informativo/consulta**, no transaccional sobre tickets.
3. **El cliente solo ve SUS vehículos**, vía `GET /propietarios/{idUsuario}/vehiculos` (`idUsuario == su propio id`).
4. **El cliente puede ver disponibilidad** de zonas/espacios (cualquier token autenticado).
5. **Crear/editar vehículos** es **ADMIN/ROOT**; el cliente solo consulta.

---

## 2. Shell / componentes globales (compartidos por ambas apps)

Pantallas y componentes base que ambos roles usan:

| Componente | Necesita |
|------------|----------|
| **Login** | form `username` + `password`; botón entrar; manejo 401 ("usuario o contraseña incorrectos"); redirección por rol. |
| **Guard de sesión** | token válido; refresco automático; logout; expulsar a login en 401. |
| **Layout autenticado** | barra superior con nombre de usuario + rol + logout; menú lateral según rol. |
| **Toast / notificaciones** | éxito y error (leer `mensaje`/`message`). |
| **Confirmación (modal)** | para activar/desactivar, anular, pagar. |
| **Tabla reutilizable** | paginación, búsqueda, estados vacío/carga/error, badges de estado (activo/inactivo). |

---

## 3. PORTAL CLIENTE (rol `CLIENTE`)

### C1. Login
- **Muestra:** form usuario/contraseña.
- **Acción:** iniciar sesión.
- **Endpoint:** `POST /auth/login`.
- **Tras éxito:** guardar token, ir a "Mi panel".

### C2. Registro de usuario (onboarding asistido)
- **Muestra:** form `username` (3–15, `A-Za-z0-9._-`), `password` (6–30, 1 minús + 1 mayús + 1 dígito), campo/selector de **persona** ya existente (`idPersona`).
- **Acción:** crear cuenta.
- **Endpoint:** `POST /auth/register` (auto-asigna rol CLIENTE, devuelve token).
- **Nota de diseño:** si la persona no existe, mostrar mensaje "solicita tu alta al administrador". No hay auto-alta de persona.
- **Validaciones en vivo:** patrones de username/password; 409 si username duplicado.

### C3. Mi perfil
- **Muestra:** datos del usuario (`username`, `nombreCompleto`, `roles`, `active`, `lastLogin`).
- **Acción:** editar `username` y/o `password` (password vacío = conservar la actual).
- **Endpoints:** `GET /auth/me` (o `GET /usuarios/{idUsuario}`), `PUT /usuarios/{idUsuario}` (dueño).
- **Estados:** éxito (toast), 409 username duplicado.

### C4. Mis vehículos (solo lectura)
- **Muestra:** lista de vehículos del cliente: `placa`, `marca`, `modelo`, `tipo`, `color`, `anio`, `activo`.
- **Acción:** abrir detalle.
- **Endpoint:** `GET /propietarios/{idUsuario}/vehiculos` (asignaciones :8082, su propio id).
- **Estado vacío:** "No tienes vehículos asignados. Contacta al administrador".

### C5. Detalle de vehículo
- **Muestra:** todos los campos según tipo — Auto: `numeroPuertas`, `capacidadMaletero`; Moto: `cilindraje`, `tipoMoto`; Camioneta: `cabina`, `capacidadCarga`; + `clasificacion`, `activo`.
- **Endpoint:** `GET /vehiculos/:id`.

### C6. Disponibilidad de parqueadero (informativo)
- **Muestra:** zonas y espacios disponibles: por zona → `nombre`, `tipoZona`, `capacidad`; espacios `codigo`, `tipo`, `estado`.
- **Acciones:** filtrar por zona y por tipo de espacio.
- **Endpoints:** `GET /zonas`, `GET /espacios/disponibles?idZona=&tipo=`, `GET /espacios/estado/DISPONIBLE`.
- **Nota:** solo consulta; el cliente no reserva ni ocupa (eso lo hace el recaudador al registrar el ingreso).

> **Gap conocido (no diseñar aún):** "Mi historial de tickets" **no** está disponible para el cliente (endpoints de tickets son RECAUDADOR+). Si se requiere, es un cambio de backend (exponer un `GET /tickets/mios` filtrado por dueño).

**Total portal cliente: 6 pantallas (C1–C6).**

---

## 4. PANEL ADMINISTRADOR (roles `ADMIN`/`ROOT`, con módulo operativo `RECAUDADOR`)

### A0. Login
- Igual que C1; tras éxito, ir al Dashboard. Rutas protegidas por rol.

### A1. Dashboard
- **Muestra (KPIs con lo que expone la API):** nº tickets activos, recaudación (suma de `valorRecaudado` de pagados), nº espacios ocupados/disponibles, nº vehículos, nº usuarios activos.
- **Endpoints:** `GET /tickets?estado=ACTIVO` (Page → `totalElements`), `GET /tickets?estado=PAGADO`, `GET /espacios/estado/OCUPADO`, `GET /espacios/estado/DISPONIBLE`, `GET /vehiculos`, `GET /usuarios`.
- **Nota:** los KPIs se calculan en el cliente a partir de `totalElements`/longitud de listas (no hay endpoint de métricas dedicado).

### A2. Personas — Gestión
- **Lista + búsqueda:** `firstName`, `lastName`, `dni`, `email`, `phone`, `active`. Búsqueda por `dni`/`nombre`/`apellido`.
- **Detalle / Crear / Editar:** todos los campos de `PersonaRequestDto` (nombre, apellidos, `dni` **cédula EC válida**, `email`, `phone` 7–10 dígitos, `address`, `nationality`).
- **Activar / Desactivar** (desactivar cascadea a usuario+roles).
- **Endpoints:** `GET /personas`, `GET /personas/buscar?dni=&nombre=&apellido=`, `GET /personas/{id}`, `POST /personas`, `PUT /personas/{id}`, `PATCH /personas/{id}/activar`, `PATCH /personas/{id}/desactivar`.
- **Validaciones UI:** cédula EC (10 díg., provincia 01–24, verificador); email/phone únicos (409).

### A3. Usuarios — Gestión
- **Lista + búsqueda** por `username`; muestra `username`, `nombreCompleto`, `active`, `lastLogin`.
- **Crear:** `idPersona` (persona **activa**), `username`, `password`.
- **Editar:** `username`, `password` (opcional). **Activar/Desactivar.**
- **Endpoints:** `GET /usuarios`, `GET /usuarios/buscar?username=`, `GET /usuarios/{id}`, `POST /usuarios`, `PUT /usuarios/{id}`, `PATCH /usuarios/{id}/activar`, `PATCH /usuarios/{id}/desactivar`.

### A4. Roles — Gestión
- **Lista / Crear / Editar:** `name` (se guarda en MAYÚSCULAS), `description`, `active`.
- **Activar/Desactivar** (desactivar falla 409 si tiene usuarios activos).
- **Endpoints:** `GET /roles`, `POST /roles`, `GET /roles/{id}`, `PUT /roles/{id}`, `PATCH /roles/{id}/activar`, `PATCH /roles/{id}/desactivar`.

### A5. Asignación de roles a usuario
- **Muestra:** roles activos de un usuario (`GET /asignaciones/usuario/{idUsuario}`).
- **Acciones:** asignar rol (`idUser` + `idRole`), activar/desactivar rol del usuario.
- **Endpoints:** `POST /asignaciones`, `PATCH /asignaciones/usuario/{idUsuario}/rol/{idRol}/activar` y `/desactivar`.

### A6. Zonas — Gestión
- **Lista / Detalle:** `nombre`, `codigo`, `tipoZona`, `capacidad`, `activo`, nº espacios.
- **Crear/Editar:** `nombre` (único, ≤32), `descripcion`, `tipo` (`VIP|REGULAR|INTERNA|EXTERNA|PREFERENCIAL`), `capacidad` (1–100).
- **Activar/Desactivar** (desactivar falla 409 si hay espacios OCUPADO).
- **Endpoints:** `GET /zonas`, `GET /zonas/{id}`, `POST /zonas`, `PUT /zonas/{id}`, `PATCH /zonas/{id}/activar|desactivar` (**204 sin body**).

### A7. Espacios — Gestión
- **Lista por zona / por estado:** `codigo` (autogenerado), `tipo`, `estado`, `activo`, `nombreZona`.
- **Crear:** `idZona`, `tipo` (`MOTO|AUTO|BUSETA`), `descripcion` (NO enviar `codigo`).
- **Editar** (descripción/tipo), **cambiar estado** (query `?estado=`), **activar/desactivar**, **disponibilidad**.
- **Endpoints:** `GET /espacios`, `GET /espacios/{id}`, `POST /espacios`, `PUT /espacios/{id}`, `PATCH /espacios/{id}/estado?estado=<EstadoEspacio>` (RECAUDADOR+), `GET /espacios/disponibles?idZona=&tipo=`, `GET /espacios/{id}/disponibilidad`, `PATCH /espacios/{id}/activar|desactivar` (**204**).
- **Selector `tipo`:** solo `MOTO/AUTO/BUSETA` (enum inválido → 400).

### A8. Vehículos — Gestión
- **Lista / Detalle:** `placa`, `marca`, `modelo`, `tipo`, `color`, `anio`, `clasificacion`, `activo` (+ campos por tipo).
- **Crear (form polimórfico):** selector `tipo` (`Auto|Motocicleta|Camioneta`) que cambia los campos:
  - **Auto:** placa `ABC-1234`, `numeroPuertas` 2–5, `capacidadMaletero` 50–1500.
  - **Motocicleta:** placa `AB-123C` (¡regex distinta!), `cilindraje` 50–2500, `tipoMoto` (`Deportiva|Scooter|Motocross`).
  - **Camioneta:** placa `ABC-1234`, `cabina` (2 o 4), `capacidadCarga` (p.ej. `"2.5t"`).
  - Comunes: `marca`, `modelo`, `color`, `anio` (1886–actual+1), `clasificacion` (`Eléctrico|Híbrido|Gasolina|Diésel`).
  - **Payload:** `{ "tipo": "...", "datos": { ... } }` (anidado).
- **Editar** (no cambia placa/anio/tipo), **activar/desactivar**.
- **Endpoints:** `POST /vehiculos`, `GET /vehiculos?incluirInactivos=`, `GET /vehiculos/:id`, `GET /vehiculos/placa/:placa`, `PATCH /vehiculos/:id`, `PATCH /vehiculos/:id/activar|desactivar`.
- **Validaciones UI:** placa según tipo; clasificación/tipoMoto **con tildes** exactas; 409 placa duplicada.

### A9. Asignaciones vehículo ↔ propietario
- **Lista por vehículo:** `GET /asignaciones-vehiculos/vehiculo/{vehicleId}`.
- **Crear:** `userId` (usuario activo con rol activo), `vehicleId`, `assignmentType` (`PROPIETARIO|AUTORIZADO|TEMPORAL`), `vehicleAlias`, `observation`.
- **Editar/estado:** `status` (`ACTIVA|SUSPENDIDA|FINALIZADA`), `entryAuthorized`, `validUntil`, etc.; **activar/desactivar**; **trazabilidad**.
- **Endpoints:** `POST /asignaciones-vehiculos`, `PATCH /asignaciones-vehiculos/{userId}/{vehicleId}` (+ `/activar`, `/desactivar`), `GET /asignaciones-vehiculos/{userId}/{vehicleId}/trazabilidad`.
- **Reglas UI:** una sola asignación **ACTIVA** por vehículo (409); usuario debe tener rol activo (define la categoría de tarifa).

### A10. Operación de Tickets (módulo RECAUDADOR)
Pantalla operativa central del parqueadero:
- **A10.1 Registrar ingreso:** input `placa` + selector de **espacio disponible** (`idEspacio`). Muestra resultado (`codigo`, `estadoTicket=ACTIVO`, `categoriaTarifa`, hora ingreso).
  - **Endpoint:** `POST /tickets` con `{ placa, idEspacio }`.
  - **Errores a mostrar:** 404 placa inexistente; 409 vehículo ya tiene ticket activo / espacio ocupado / tipos incompatibles (Auto↔AUTO, Moto↔MOTO, Camioneta↔BUSETA).
- **A10.2 Tickets activos (tablero):** lista paginada de tickets `ACTIVO` con `codigo`, `placa`, `codigoEspacio`, hora ingreso.
  - **Endpoint:** `GET /tickets?estado=ACTIVO&page=&size=` (**Page** → leer `content[]`).
- **A10.3 Cobrar / Salida:** seleccionar ticket → **pagar**. Muestra `valorRecaudado`, `fechaHoraSalida`, `estado=PAGADO`.
  - **Endpoint:** `PATCH /tickets/{id}/pagar`. Error 409 si ya no está ACTIVO.
- **A10.4 Anular ticket:** con `motivo` (obligatorio). `estado=ANULADO`, `valorRecaudado=0`.
  - **Endpoint:** `PATCH /tickets/{id}/anular` con `{ motivo }`. 409 si no está ACTIVO.
- **A10.5 Buscar ticket:** por `id` o `codigo`; ver activo por espacio.
  - **Endpoints:** `GET /tickets/{id}`, `GET /tickets/codigo/{codigo}`, `GET /tickets/activo/espacio/{idEspacio}`.

### A11. Auditoría (log)
- **Muestra:** eventos `servicio`, `accion`, `entidad`, `usuario`, `rol`, `ip`, `mac`, `timestamp` (orden desc).
- **Endpoints:** `GET /audit`, `GET /audit/:id`.
- **Nota:** requiere JWT (cualquier token válido). Solo lectura desde el panel.

**Total panel administrador: 12 áreas (A0–A11), con A10 desglosada en 5 sub-vistas operativas.**

---

## 5. Mapa de navegación (resumen)

**Cliente:** Login → (Registro asistido) → Mi panel → { Mi perfil · Mis vehículos → Detalle · Disponibilidad }.

**Administrador:** Login → Dashboard → menú lateral:
`Personas · Usuarios · Roles · Asignación de roles · Zonas · Espacios · Vehículos · Asignaciones vehículo-propietario · Tickets (Ingreso/Activos/Cobro/Anular/Buscar) · Auditoría`.

---

## 6. Checklist de UX que el diseño debe cubrir

- **Badges de estado**: activo/inactivo; ticket ACTIVO/PAGADO/ANULADO; espacio DISPONIBLE/OCUPADO/RESERVADO/MANTENIMIENTO.
- **Formularios con validación en vivo** (cédula EC, placas por tipo, patrones de username/password, enums en selectores — nunca texto libre).
- **Selectores en vez de inputs libres** para todo campo enum (tipo de zona/espacio/vehículo, clasificación, tipoMoto, assignmentType, estados).
- **Paginación** en listados de tickets (respuesta `Page`).
- **Confirmaciones** en acciones irreversibles/negocio (desactivar, pagar, anular).
- **Manejo diferenciado de errores** 400 (mostrar `errores` por campo), 401 (a login), 403 ("sin permisos"), 404, 409 (regla de negocio, mostrar `mensaje`).
- **Feedback de PATCH 204** (zonas/espacios activar/desactivar no devuelven body: confirmar con toast, refrescar lista).
