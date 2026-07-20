# Contrato de API — Revisión exhaustiva (2.ª pasada) para la colección Postman

> **Objetivo:** documento base, verificado **leyendo el código fuente real**, para construir
> una colección Postman/newman que se ejecute de principio a fin sin fallos y cubra
> happy‑path, errores técnicos (400/401/403/404) y reglas de negocio (409/502/503).
> Todo lo de aquí está confirmado contra controladores, DTOs, enums, `SecurityConfig`,
> `GlobalExceptionHandler`, `application.yaml` y los `service impl`.

Última verificación: 2.ª revisión completa de los 6 microservicios.

---

## 0.0 Correcciones de código aplicadas y verificadas en runtime (esta sesión)

| # | Servicio | Problema detectado | Corrección aplicada | Estado |
|---|----------|--------------------|---------------------|--------|
| A | Infra JWT | `keys/jwt_public.pem` **no era pareja** de `keys/jwt_private.pem`: login daba 200 pero **todos** los endpoints autenticados de los 4 servicios Spring devolvían 401 (Nest sí validaba). | Regenerada la pública desde la privada (`openssl rsa -in jwt_private.pem -pubout`) + reinicio de los 4 servicios Spring. Respaldo en `keys/jwt_public.pem.bak-*`. | ✅ Verificado E2E |
| B | zonas | `POST /api/v1/espacios` con `tipo` de enum inexistente (p.ej. `HELICOPTERO`) devolvía **500** (`HttpMessageNotReadableException` no manejada) en vez de 400. | Añadido `@ExceptionHandler(HttpMessageNotReadableException.class)` en `GlobalExceptionHandler` que detecta `InvalidFormatException` sobre enums y responde **400** con mensaje `"Valor invalido '<x>' ... valores permitidos: [...]"`. | ✅ 400 confirmado |

> Tras estas correcciones, la colección `postman/QA_Parqueaderos.postman_collection.json`
> corre **117/117 aserciones verdes** (40 requests, 0 fallos).

---

## 0. Correcciones críticas encontradas en esta 2.ª revisión

Estas son diferencias respecto al inventario inicial que **habrían roto** la colección:

| # | Tema | Inventario viejo (incorrecto) | Realidad verificada en código |
|---|------|-------------------------------|-------------------------------|
| 1 | **DTO de vehículos** | payload plano `{placa, marca, tipo...}` | **anidado** `{ "tipo": "Auto", "datos": { ...campos } }` (polimórfico) |
| 2 | **Puerto de vehículos que consumen tickets/asignaciones** | `http://vehiculos:8083` | `http://localhost:3000` (prefijo `/api`) — ver `application.yaml` |
| 3 | **Tarifa CAMIONETA_BUSETA** | `3.00` | **`2.00`** |
| 4 | **Factores por rol** | INVITADO/CLIENTE/VIP | `INVITADO=1.00`, `CLIENTE=0.60`, `RECAUDADOR=0.50`, `ADMIN=0.50`, **`ROOT=0.00`** |
| 5 | **`categoriaTarifa` del ticket** | fija | = **rol de autorización** de la asignación (o `assignmentType` si null) |
| 6 | **TipoZona** | PUBLICA/PRIVADA/RESTRINGIDA | **`VIP, REGULAR, INTERNA, EXTERNA, PREFERENCIAL`** |
| 7 | **PATCH activar/desactivar zona y espacio** | 200 | **204 No Content** (sin body) |
| 8 | **Cambiar estado espacio** | body JSON | **query param** `?estado=OCUPADO` |
| 9 | **`tickets/listar`** | array | **`Page`** → `{ content:[...], totalElements, ... }` |
| 10 | **ms-audit** | público | requiere **JWT** en todas las rutas (guard global) |

> ⚠️ **Trampa de cobro:** si el vehículo del ticket se asigna a `root`, `categoriaTarifa`
> resuelve a `ROOT` → factor `0.00` → **`valorRecaudado = 0.00`**. Para que el cobro sea
> positivo, el vehículo debe pertenecer a un usuario **CLIENTE** (factor `0.60`).

---

## 1. Infraestructura, puertos, credenciales y JWT

| Servicio | Stack | Puerto local | Prefijo de rutas |
|----------|-------|--------------|------------------|
| usuarios | Spring Boot | `8081` | `/api/v1/...` |
| zonas | Spring Boot | `8080` | `/api/v1/...` |
| asignaciones | Spring Boot | `8082` | `/api/v1/...` |
| tickets | Spring Boot | `8083` | `/api/v1/...` |
| vehiculos | NestJS | `3000` | `/api/vehiculos` (prefijo global `/api`) |
| ms-audit | NestJS | `3002` | `/api/v1/audit` (prefijo global `/api/v1`) |

Infra Docker: PostgreSQL `5433`, RabbitMQ `5672`/`15672`, Kong `8000` (proxy) / `8001` (admin).

**URLs de orquestación (defaults locales, ya correctos):**
- tickets → `USUARIOS_URL=http://localhost:8081`, `VEHICULOS_URL=http://localhost:3000`, `ZONAS_URL=http://localhost:8080`, `ASIGNACIONES_URL=http://localhost:8082`
- asignaciones → `USUARIOS_URL=http://localhost:8081`, `VEHICULOS_URL=http://localhost:3000`

**Credenciales root (auto‑sembradas al primer arranque de usuarios):**
`username: root` · `password: Root2025` · roles `["ROOT","ADMIN"]`.

**JWT:** RS256, `iss=parqueadero`. Claims: `sub` (idUsuario), `username`, `roles` (array), `type=access`.
Header exacto: `Authorization: Bearer <token>` (case‑sensitive: `Bearer`). Access token 7200 s, refresh 604800 s.

**Roles del sistema:** `ROOT, ADMIN, RECAUDADOR, CLIENTE, INVITADO`. Spring evalúa `ROLE_<nombre>`.

**Cuerpo de error estándar (los 4 servicios Spring):**
```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "..." }
```
En validación 400 se añade `"errores": { "campo": "mensaje" }`.

**Cuerpo de error NestJS (vehiculos, ms-audit):**
```json
{ "statusCode": 400, "message": ["..."], "error": "Bad Request" }
```
(en validación `message` es **array**; en negocio suele ser string).

---

## 2. usuarios (:8081)

### 2.1 AuthController `/api/v1/auth`
| Método | Ruta | Auth | Éxito | Body |
|--------|------|------|-------|------|
| POST | `/login` | público | 200 | `LoginRequest` |
| POST | `/register` | público | 201 | `RegisterRequest` (requiere `idPersona`; solo uso admin) |
| POST | `/registro-cliente` | público | 201 | `RegistroClienteRequest` (auto‑registro seguro; auto‑asigna CLIENTE) |
| POST | `/refresh` | público | 200 | `RefreshRequest` |
| GET | `/me` | autenticado | 200 | — |

**`RegistroClienteRequest`** = `{ dni (10 díg), email, username (3‑15), password (6‑30, may+min+díg) }`. Auto‑registro público para el portal cliente **sin exponer el listado de personas**: el backend resuelve la persona por `dni`, exige que el `email` coincida (segundo factor de identidad), que la persona esté activa y que aún no tenga usuario. En **cualquier** fallo responde **409** con un **mensaje único genérico** ("No pudimos verificar tu identidad con esos datos, o ya existe una cuenta asociada…") para impedir enumeración de cédulas/correos. El cliente nunca recibe ni envía `idPersona`. (Verificado en runtime esta sesión.)

### 2.2 PersonaController `/api/v1/personas` — **toda la clase `hasAnyRole('ADMIN','ROOT')`**
| Método | Ruta | Éxito |
|--------|------|-------|
| GET | `/` | 200 |
| GET | `/{idPersona}` | 200 |
| GET | `/buscar?dni=&nombre=&apellido=` (≥1 criterio) | 200 |
| POST | `/` | 201 |
| PUT | `/{idPersona}` | 200 |
| PATCH | `/{idPersona}/activar` | 200 |
| PATCH | `/{idPersona}/desactivar` | 200 (cascada: desactiva usuario+roles) |

### 2.3 UsuarioController `/api/v1/usuarios`
| Método | Ruta | Auth | Éxito |
|--------|------|------|-------|
| GET | `/` | ADMIN/ROOT | 200 |
| GET | `/{idUsuario}` | ADMIN/ROOT **o** dueño | 200 |
| GET | `/buscar?username=` | ADMIN/ROOT | 200 |
| POST | `/` | ADMIN/ROOT | 201 |
| PUT | `/{idUsuario}` | ADMIN/ROOT **o** dueño | 200 |
| PATCH | `/{idUsuario}/activar` | ADMIN/ROOT | 200 |
| PATCH | `/{idUsuario}/desactivar` | ADMIN/ROOT | 200 |

### 2.4 RolController `/api/v1/roles` — clase `hasAnyRole('ADMIN','ROOT')`
GET `/`, POST `/` (201, normaliza nombre a MAYÚSCULAS), GET `/{idRol}`, PUT `/{idRol}`,
PATCH `/{idRol}/activar`, PATCH `/{idRol}/desactivar` (falla 409 si tiene usuarios activos).

### 2.5 AsignacionController `/api/v1/asignaciones` — clase `hasAnyRole('ADMIN','ROOT')`
GET `/`, POST `/` (201), GET `/usuario/{idUsuario}`,
PATCH `/usuario/{idUsuario}/rol/{idRol}/desactivar`, `/activar`.

### 2.6 DTOs de request (campos + validaciones exactas)

**LoginRequest:** `username` (@NotBlank), `password` (@NotBlank).

**RegisterRequest:** `idPersona` UUID (@NotNull), `username` (3–15, `^[a-zA-Z0-9._-]+$`),
`password` (6–30, `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$`).

**RefreshRequest:** `refreshToken` (@NotBlank).

**PersonaRequestDto:**
- `firstName` @NotBlank, ≤30, `^[\p{L} ]+$`
- `middleName` opcional, ≤30, `^[\p{L} ]*$`
- `lastName` @NotBlank, ≤30, `^[\p{L} ]+$`
- `dni` @NotBlank, **cédula ecuatoriana válida** (ver §2.8)
- `email` @NotBlank @Email ≤50 (se guarda lowercase, es UNIQUE)
- `phone` @NotBlank `^\d{7,10}$` (UNIQUE)
- `address` opcional ≤255
- `nationality` @NotBlank ≤30 `^[\p{L} ]+$`

**UsuarioRequestDto:** `idPersona` UUID @NotNull (persona debe estar **activa**), `username` (3–15, patrón), `password` (6–30, patrón).

**UsuarioUpdateDto:** `idPersona` @NotNull (**no se puede cambiar** la persona), `username` (3–15, patrón), `password` opcional: vacío `""` conserva la actual; si viene, `^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,30}$`.

**RolRequestDto:** `name` @NotBlank 3–50 `^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9_ ]+$` (→ MAYÚSCULAS), `description` opcional ≤255.

**AsignarRolRequestDto:** `idUser` UUID @NotNull, `idRole` UUID @NotNull (ambos activos).

### 2.7 DTOs de response (nombres exactos de campos)
- **AuthResponse:** `token, refreshToken, tokenType("Bearer"), expiresIn, refreshExpiresIn, idUsuario, username, roles[]`
- **PerfilResponse:** `idUsuario, username, nombreCompleto, active, roles[]`
- **PersonaResponseDto:** `id, firstName, middleName, lastName, dni, email, phone, address, nationality, active, createdAt, updatedAt`
- **UsuarioResponseDto:** `id, idPersona, username, nombreCompleto, active, lastLogin, createdAt, updatedAt`
- **RolResponseDto:** `id, name, description, active, createdAt, updatedAt`
- **AsignacionResponseDto:** `idUser, username, idRole, rol, active, assignedAt, updatedAt`

### 2.8 Validador de cédula ecuatoriana (para generar cédulas válidas en pre‑request)
1. 10 dígitos exactos. 2. Provincia (2 primeros) `01–24`. 3. Tercer dígito `0–5`.
4. Coeficientes `[2,1,2,1,2,1,2,1,2]`; si producto > 9 restar 9; sumar.
5. Verificador `= (10 - (suma % 10)) % 10` == dígito 10.

```javascript
// Pre-request Postman: cédula válida
function cedulaEc() {
  const prov = String(1 + Math.floor(Math.random()*24)).padStart(2,'0');
  const tercero = Math.floor(Math.random()*6);           // 0..5
  let d = prov + tercero;
  for (let i=0;i<6;i++) d += Math.floor(Math.random()*10);
  let suma=0;
  for (let i=0;i<9;i++){ let m=(+d[i])*(i%2===0?2:1); if(m>9)m-=9; suma+=m; }
  return d + ((10-(suma%10))%10);
}
```

### 2.9 Errores y reglas clave (usuarios)
- 401 sin token: `"Token ausente o invalido: inicie sesion"`; 403 sin rol: `"No tiene permisos para esta operacion"`.
- 401 login malo: `CredencialesInvalidasException` → `"Usuario o contrasena incorrectos"`.
- 409: username/dni/email/phone duplicado; persona inactiva al crear usuario; asignar rol a usuario/rol inactivo; desactivar rol con usuarios activos.
- `GET /buscar` sin criterio → 400 (`IllegalArgumentException`).

---

## 3. zonas (:8080)

### 3.1 ZonaController `/api/v1/zonas`
| Método | Ruta | Auth | Éxito |
|--------|------|------|-------|
| GET | `/` | autenticado | 200 |
| GET | `/{idZona}` | autenticado | 200 |
| POST | `/` | ADMIN/ROOT | **201** |
| PUT | `/{idZona}` | ADMIN/ROOT | 200 |
| PATCH | `/{idZona}/activar` | ADMIN/ROOT | **204** |
| PATCH | `/{idZona}/desactivar` | ADMIN/ROOT | **204** |

### 3.2 EspacioController `/api/v1/espacios`
| Método | Ruta | Auth | Éxito |
|--------|------|------|-------|
| GET | `/` | autenticado | 200 |
| GET | `/{idEspacio}` | autenticado | 200 |
| POST | `/` | ADMIN/ROOT | **201** |
| PUT | `/{idEspacio}` | ADMIN/ROOT | 200 |
| PATCH | `/{idEspacio}/estado?estado=<EstadoEspacio>` | RECAUDADOR/ADMIN/ROOT | 200 |
| GET | `/estado/{estado}` | autenticado | 200 |
| GET | `/disponibles?idZona=&tipo=` | autenticado | 200 |
| GET | `/{idEspacio}/disponibilidad` | autenticado | 200 |
| GET | `/zona/{idZona}/estado/{estado}` | autenticado | 200 |
| PATCH | `/{idEspacio}/activar` | ADMIN/ROOT | **204** |
| PATCH | `/{idEspacio}/desactivar` | ADMIN/ROOT | **204** |

### 3.3 Enums
- **TipoZona:** `VIP, REGULAR, INTERNA, EXTERNA, PREFERENCIAL`
- **TipoEspacio:** `MOTO, AUTO, BUSETA`
- **EstadoEspacio:** `DISPONIBLE, OCUPADO, RESERVADO, MANTENIMIENTO`

### 3.4 DTOs request
**ZonaRequestDto:** `nombre` @NotBlank 1–32 (UNIQUE, case‑insensitive), `descripcion` opcional ≤255, `tipo` `TipoZona` @NotNull, `capacidad` int `1..100`.

**EspacioRequestDto:** `idZona` UUID @NotNull, `descripcion` opcional ≤150, `tipo` `TipoEspacio` @NotNull, `estado` `EstadoEspacio` opcional. **`codigo` NO se envía** (autogenerado `ESP-<TIPO3>-<NN>`, p.ej. `ESP-AUT-01`).

### 3.5 DTOs response (nombres exactos)
- **ZonaRespondeDto:** `idZona, nombre, codigo, descripcion, activo, tipoZona, capacidad, espacios[], fechaCreacion, fechaActualizacion`
- **EspacioRespondeDto:** `id, codigo, descripcion, tipo, activo, idZona, nombreZona, estado`
- **DisponibilidadResponseDto:** `idEspacio, codigo, disponible, activo, estado`

### 3.6 Reglas de negocio
- Crear espacio: zona debe **existir** (404) y estar **activa** (409); respeta `capacidad` (409 al llenarse).
- Actualizar zona: nueva `capacidad` ≥ nº de espacios existentes (409).
- Desactivar zona: falla 409 si hay espacios `OCUPADO`.
- `cambiarEstado`: espacio activo (409 si no), y estado destino ≠ actual (409).
- **Enum/JSON inválido en el body** (p.ej. `tipo` fuera de `MOTO|AUTO|BUSETA`) → **400** (manejado por `GlobalExceptionHandler` vía `HttpMessageNotReadableException`/`InvalidFormatException`; antes devolvía 500).
- Público: `/api/v1/sse/**` (permitAll). Todo lo demás requiere JWT.

---

## 4. asignaciones‑vehículos (:8082)

### 4.1 AssignmentController `/api/v1/asignaciones-vehiculos`
| Método | Ruta | Auth | Éxito |
|--------|------|------|-------|
| POST | `/` | ADMIN/ROOT | **201** |
| PATCH | `/{userId}/{vehicleId}` | ADMIN/ROOT | 200 |
| PATCH | `/{userId}/{vehicleId}/desactivar` | ADMIN/ROOT | 200 |
| PATCH | `/{userId}/{vehicleId}/activar` | ADMIN/ROOT | 200 |
| GET | `/vehiculo/{vehicleId}` | RECAUDADOR/ADMIN/ROOT | 200 |
| GET | `/{userId}/{vehicleId}/trazabilidad` | ADMIN/ROOT | 200 |

### 4.2 FleetController `/api/v1/propietarios`
GET `/{userId}/vehiculos` — `#userId == authentication.name` **o** ADMIN/ROOT → 200.

### 4.3 Enums
- **AssignmentType:** `PROPIETARIO, AUTORIZADO, TEMPORAL`
- **AssignmentStatus:** `ACTIVA, SUSPENDIDA, FINALIZADA`

### 4.4 DTOs request
**CreateAssignmentRequest:** `userId` UUID @NotNull, `vehicleId` UUID @NotNull, `assignmentType` opcional (default `PROPIETARIO`), `vehicleAlias` opcional ≤80, `observation` opcional ≤500.

**UpdateAssignmentRequest:** `status`, `assignmentType`, `validUntil` (OffsetDateTime ≥ validFrom), `vehicleAlias` ≤80, `entryAuthorized` (Boolean, solo `true` si status ACTIVA), `observation` ≤500, `changeReason` ≤500 (todos opcionales).

### 4.5 DTO response — **AssignmentResponse**
`userId, vehicleId, active, status, assignmentType, authorizationRoleId, authorizationRoleName, validFrom, validUntil, vehicleAlias, entryAuthorized, observation, changeReason, assignedAt, updatedAt`.
Al crear: `active=true, status=ACTIVA, entryAuthorized=true, validFrom=now, validUntil=null`.

### 4.6 Reglas de negocio (crear) — orden exacto
1. **Valida usuario activo** → `GET usuarios:8081/api/v1/usuarios/{userId}` (reenvía `Authorization`). 404 si no existe, 409 si inactivo.
2. **Valida vehículo activo** → `GET vehiculos:3000/api/vehiculos/{vehicleId}`. 404/409.
3. **Valida rol activo** → `GET usuarios:8081/api/v1/asignaciones/usuario/{userId}`. Toma el **primer rol activo** como `authorizationRoleName`. 409 si no tiene rol activo.
4. **Una sola asignación ACTIVA por vehículo** (índice único) → 409 `"El vehiculo ya tiene un propietario activo"`.
5. Si ya existe `(userId,vehicleId)` inactiva → reactiva; si activa → 409.

Timeouts externos: connect 3 s, read 5 s. Fallo externo → **502** `"Error consultando un microservicio externo"`.
`DataIntegrityViolationException` (unique) → 409.

> **Implicación para el flujo QA:** el `userId` que se asigna debe ser un usuario **activo con rol activo**.
> Como `root` cumple, sirve; pero para cobro > 0 conviene un usuario **CLIENTE** (ver §6).

---

## 5. vehiculos (:3000, prefijo `/api`)

### 5.1 VehiculosController `/api/vehiculos` (guards globales: JwtAuthGuard + RolesGuard)
| Método | Ruta | Rol | Éxito |
|--------|------|-----|-------|
| POST | `/` | ADMIN/ROOT | **201** |
| GET | `/?incluirInactivos=` | solo JWT | 200 |
| GET | `/placa/:placa` | solo JWT | 200 |
| GET | `/:id` | solo JWT | 200 |
| PATCH | `/:id` | ADMIN/ROOT | 200 |
| PATCH | `/:id/activar` | ADMIN/ROOT | 200 |
| PATCH | `/:id/desactivar` | ADMIN/ROOT | 200 |

### 5.2 DTO **anidado** `CreateVehiculoDto`
```
{ "tipo": "Auto" | "Motocicleta" | "Camioneta", "datos": { ... } }
```
**BaseVehiculoDto** (común): `placa` `^[A-Z]{3}-\d{4}$`, `marca` 2–50 letras, `modelo` 1–100 (letras/números/`. -`), `color` 3–50 letras, `anio` `1886..(añoActual+1)`, `clasificacion` enum.

**Enums de vehículos** (valor serializado):
- `Clasificacion`: `Eléctrico, Híbrido, Gasolina, Diésel`
- `TipoMoto`: `Deportiva, Scooter, Motocross`

**AutoDto** (+): `numeroPuertas` 2–5, `capacidadMaletero` 50–1500.
**MotocicletaDto** (+): `placa` `^[A-Z]{2}-\d{3}[A-Z]$` (¡distinta!), `cilindraje` 50–2500, `tipoMoto` enum.
**CamionetaDto** (+): `cabina` ∈ `{2,4}`, `capacidadCarga` `^[0-9]+(\.[0-9]+)?\s?(kg|KG|t|T)$` (p.ej. `"2.5t"`).

### 5.3 Response (entidad) — campos
`id, placa, marca, modelo, color, anio, clasificacion, tipo, activo` + según tipo:
Auto → `numeroPuertas, capacidadMaletero`; Moto → `cilindraje, tipoMoto`; Camioneta → `cabina, capacidadCarga`.

### 5.4 Reglas
- `placa` se **normaliza** `trim().toUpperCase()` en el servicio. UNIQUE (case‑insensitive) → 409.
- `update` no cambia `placa/anio/tipo/activo`. `activar/desactivar` repetido → 409.
- Errores Nest: 400 `message` **array**; 404/409 `message` string; 401/403 mensaje del guard.

### 5.5 Payloads válidos
```json
// Auto
{ "tipo": "Auto", "datos": { "placa": "PBA-1234", "marca": "Toyota", "modelo": "Corolla", "color": "Blanco", "anio": 2022, "clasificacion": "Gasolina", "numeroPuertas": 4, "capacidadMaletero": 450 } }
```
```json
// Motocicleta
{ "tipo": "Motocicleta", "datos": { "placa": "AB-123C", "marca": "Honda", "modelo": "CB500F", "color": "Rojo", "anio": 2023, "clasificacion": "Gasolina", "cilindraje": 471, "tipoMoto": "Deportiva" } }
```
```json
// Camioneta
{ "tipo": "Camioneta", "datos": { "placa": "XYZ-9876", "marca": "Ford", "modelo": "F150", "color": "Negro", "anio": 2021, "clasificacion": "Diésel", "cabina": 4, "capacidadCarga": "2.5t" } }
```

---

## 6. tickets (:8083) — el corazón de las reglas de negocio

### 6.1 TicketController `/api/v1/tickets` — clase `PUEDE_OPERAR` = RECAUDADOR/ADMIN/ROOT
| Método | Ruta | Éxito | Body |
|--------|------|-------|------|
| POST | `/` | **201** | `RegistrarIngresoRequest` |
| PATCH | `/{id}/pagar` | 200 | — (sin body) |
| PATCH | `/{id}/anular` | 200 | `AnularTicketRequest` |
| GET | `/{id}` | 200 | — |
| GET | `/codigo/{codigo}` | 200 | — |
| GET | `/?estado=<EstadoTicket>&page=&size=` | 200 (**Page**) | — |
| GET | `/activo/espacio/{idEspacio}` | 200 | — |

`idEmpleado` sale del `sub` del JWT (`@AuthenticationPrincipal`).

### 6.2 DTOs
**RegistrarIngresoRequest:** `placa` @NotBlank, `idEspacio` UUID @NotNull.
**AnularTicketRequest:** `motivo` @NotBlank.
**EstadoTicket:** `ACTIVO, PAGADO, ANULADO`.

**TicketResponse:** `id, codigo, idEspacio, codigoEspacio, tipoEspacio, idUsuario, idVehiculo, placa, tipoVehiculo, categoriaTarifa, fechaHoraIngreso, fechaHoraSalida, estadoTicket, idEmpleado, valorRecaudado, motivoAnulacion, createdAt, updatedAt`.

### 6.3 `registrarIngreso` — **orden exacto de validaciones** (define qué error salta primero)
1. Normaliza `placa`. Obtiene vehículo por placa (`GET vehiculos:3000/api/vehiculos/placa/{placa}`). **404** si no existe; **409** si inactivo.
2. Obtiene asignación activa del vehículo (`GET asignaciones:8082/.../vehiculo/{vehicleId}`). Valida: `active`, `status=ACTIVA`, `entryAuthorized`, ventana `validFrom/validUntil`. **409** si algo falla.
3. **¿Vehículo ya tiene ticket ACTIVO?** → **409** `"El vehiculo ya tiene un ticket activo: <cod>"`.
4. Obtiene espacio (`GET zonas:8080/api/v1/espacios/{idEspacio}`). **404** si no existe; **409** si inactivo o `estado != DISPONIBLE`.
5. **Compatibilidad** vehículo↔espacio (ver §6.6) → **409** si incompatible.
6. **¿Espacio ya tiene ticket ACTIVO?** → **409** `"El espacio ya tiene un ticket activo: <cod>"`.
7. `saveAndFlush` (índice único parcial); conflicto concurrente → **409**.
8. `PATCH zonas .../estado?estado=OCUPADO`. Si el servicio remoto cae → **503**.
9. Audita `CREATE` (AFTER_COMMIT vía RabbitMQ).

> **Edge “parqueadero lleno” / “dos autos al mismo espacio”:** al ocupar el espacio A con el
> primer ticket, un segundo ingreso al **mismo espacio** falla en el paso 4 con **409**
> (`estado actual: OCUPADO`) — antes incluso del paso 6. Ambos caminos devuelven 409.

### 6.4 `pagar(idTicket)`
- 404 si no existe; **409** si estado ≠ ACTIVO (`"Solo se pueden pagar tickets activos..."`).
- Calcula `valorRecaudado`, `fechaHoraSalida=now`, `estado=PAGADO`; libera espacio a DISPONIBLE.

### 6.5 `anular(idTicket, motivo)`
- 404 si no existe; **409** si estado ≠ ACTIVO. Fija `ANULADO`, `valorRecaudado=0`, `motivoAnulacion`, libera espacio.

### 6.6 Compatibilidad de tipos (case‑insensitive, sin tildes)
`AUTO→AUTO`, `MOTOCICLETA→MOTO`, `CAMIONETA→BUSETA`. (Vehículo `Auto` requiere espacio `AUTO`, etc.)

### 6.7 Cálculo de tarifa (verificado en `CalculadoraTarifa` + `application.yaml`)
```
horas = max( ceil(minutos/60), 1 )            // mínimo 1 hora, fracción sube
base  = tarifaHora(tipoVeh_tipoEsp) * horas
total = base * factorRol(categoria)           // setScale(2, HALF_UP)
```
**Matriz tarifa/hora:** `AUTO_AUTO=1.50`, `MOTOCICLETA_MOTO=0.75`, `CAMIONETA_BUSETA=2.00`, por‑defecto `1.00`.
**factor‑rol:** `INVITADO=1.00`, `CLIENTE=0.60`, `RECAUDADOR=0.50`, `ADMIN=0.50`, `ROOT=0.00`, por‑defecto `1.00`.
`categoria` = `authorizationRoleName` de la asignación (si null, `assignmentType`).

**Ejemplos (salida inmediata ⇒ 1 hora):**
- Auto en AUTO, dueño **CLIENTE**: `1.50 × 1 × 0.60 =` **`0.90`**.
- Auto en AUTO, dueño **ROOT**: `1.50 × 1 × 0.00 =` **`0.00`**.
- Camioneta en BUSETA, **CLIENTE**: `2.00 × 1 × 0.60 = 1.20`.

> **Recomendación QA:** para validar “cobro > 0”, asigna el vehículo del flujo a un usuario **CLIENTE**
> (registrado vía `/auth/register`, que auto‑asigna CLIENTE). Con `root` el cobro es `0.00`.

### 6.8 Errores especiales tickets
- Servicio externo caído → **503** `"Un microservicio dependiente no esta disponible"`.
- `listar` devuelve `Page`: **asertar `content` (array)**, `totalElements`, `number`, `size`.

---

## 7. ms-audit (:3002, prefijo `/api/v1`)

### 7.1 AuditController `/api/v1/audit` — **JWT global obligatorio** (guard `JwtAuthGuard`)
| Método | Ruta | Éxito |
|--------|------|-------|
| POST | `/` | 201 |
| GET | `/` | 200 (array, orden `timestamp DESC`) |
| GET | `/:id` | 200 |

Sin `@Roles`: cualquier token válido sirve. (Hay `ThrottlerModule` importado, pero el único `APP_GUARD` es el JWT.)

### 7.2 CreateAuditEventDto (validaciones estrictas)
- `servicio` `^ms-[a-zA-Z]+$` (7–50) — p.ej. `ms-vehiculos`
- `accion` ∈ `CREATE|UPDATE|DELETE|LOGIN|LOGOUT|SELECT` (mayúsculas)
- `entidad` `^[A-Z-]+$` (3–20) — p.ej. `VEHICULO`
- `datos` objeto opcional
- `usuario` opcional `^[a-zA-Z0-9._-]+$` (3–40)
- `rol` opcional
- `ip` **@IsIP('4')** (IPv4 válida, no `::1`)
- `mac` **@IsMACAddress** (`AA:BB:CC:DD:EE:FF`, con `:`)

**Respuesta GET/POST (entidad):** `id, servicio, accion, entidad, datos, usuario, rol, ip, mac, timestamp`.

Payload válido:
```json
{ "servicio": "ms-vehiculos", "accion": "CREATE", "entidad": "VEHICULO",
  "datos": { "placa": "PBA-1234" }, "usuario": "root", "rol": "ADMIN",
  "ip": "192.168.1.100", "mac": "AA:BB:CC:DD:EE:FF" }
```

---

## 8. Catálogo de trampas/bugs (checklist para no fallar en Postman)

1. **Vehículos: payload anidado** `{tipo, datos}` — enviar plano ⇒ 400.
2. **Placa auto** `ABC-1234` vs **moto** `AB-123C` — regex distintas.
3. **Clasificación/TipoMoto con tilde** (`Diésel`, `Eléctrico`, `Híbrido`) — usar el valor exacto.
4. **Zona `tipo`**: solo `VIP|REGULAR|INTERNA|EXTERNA|PREFERENCIAL`.
5. **Espacio: no enviar `codigo`** (lo genera el server); crear solo en zona **activa**.
6. **Cambiar estado espacio = query param** `?estado=OCUPADO`, y requiere rol **RECAUDADOR**+.
7. **PATCH activar/desactivar zona/espacio ⇒ 204 sin body** (no asertar JSON).
8. **Asignación**: `userId` debe ser usuario **activo con rol activo**; 1 sola activa por vehículo.
9. **Reenvío de `Authorization`**: al crear asignación/ticket, el token viaja a los servicios llamados; usar siempre un token válido con rol suficiente.
10. **ticket ingreso**: la asignación debe tener `entryAuthorized=true` y `status=ACTIVA` (una recién creada ya lo cumple).
11. **Cobro con root = 0.00** — usar dueño CLIENTE para cobro positivo.
12. **`tickets/listar` es `Page`** (`content[]`), no array.
13. **ms-audit**: `ip` IPv4 y `mac` con `:`; requiere JWT.
14. **Header** `Authorization: Bearer <t>` — `Bearer` exacto.
15. **Idempotencia de reruns**: cédula/username/placa/nombre de zona son únicos → generar aleatorios en pre‑request para poder re‑ejecutar la colección.

---

## 9. Flujo recomendado para la colección (orden que sí pasa)

Variables de colección: `urlUsuarios, urlZonas, urlAsignaciones, urlTickets, urlVehiculos(/api), urlAudit(/api/v1)`,
y dinámicas: `token, idPersona, idUsuarioCliente, idZona, idEspacioA, idEspacioB, idVehiculo1, placa1, idVehiculo2, placa2, idTicket`.

1. **Auth** — login `root/Root2025` → guarda `token`; login malo → 401; login sin campo → 400; `/me` → 200.
2. **Personas** — crear persona (cédula generada) → guarda `idPersona`; dni inválido → 400; sin token → 401.
3. **Usuarios** — `register` con `idPersona` (auto CLIENTE) → guarda `idUsuarioCliente`; username corto → 400.
4. **Zonas** — crear zona (`tipo=REGULAR`, capacidad ≥ 2) → `idZona`; capacidad 0 → 400; sin rol → 403.
5. **Espacios** — crear A (`AUTO`) → `idEspacioA`; crear B (`AUTO`) → `idEspacioB`; tipo inválido → 400; disponibles → 200.
6. **Vehículos** — crear V1 Auto → `idVehiculo1`,`placa1`; crear V2 Auto → `idVehiculo2`,`placa2`; placa inválida → 400; duplicada → 409.
7. **Asignaciones** — asignar V1→`idUsuarioCliente` → 201; asignar V2→`idUsuarioCliente` → 201; duplicada activa V1 → 409; GET por vehículo → 200.
8. **Tickets (flujo + edges)**
   - ingreso `placa1`→A → 201 `ACTIVO`, guarda `idTicket` (categoria=CLIENTE).
   - ingreso `placa1`→B → **409** (vehículo ya tiene ticket activo).
   - ingreso `placa2`→A → **409** (espacio A ocupado).
   - ingreso placa inexistente → **404**; sin token → 401; sin placa → 400.
   - GET `{idTicket}` → 200; `listar?estado=ACTIVO` → 200 (`content[]`).
   - `pagar {idTicket}` → 200 `PAGADO`, `valorRecaudado = 0.90`.
   - `pagar {idTicket}` otra vez → **409**; `anular {idTicket}` (ya pagado) → **409**.
   - (opcional) ingreso `placa2`→A ahora que A quedó libre → 201; `anular` → 200 `ANULADO`.
9. **Auditoría** — `GET /api/v1/audit` → 200 (array).

**Aserciones por request:** `pm.response.code`, estructura JSON (presencia de campos clave, no el texto en español del error), y `pm.expect(pm.response.responseTime).to.be.below(2000)`. Para el cobro: `Number(json.valorRecaudado)` finito y ≥ 0 (y > 0 en el ticket CLIENTE).

---

## 10. Validaciones/observaciones que el QA debe conocer (no son bugs, son diseño)
- **No existe chequeo de “capacidad de zona” en tickets**: “parqueadero lleno” se materializa como
  espacio `OCUPADO` (409 en paso 4), no como un error de aforo de zona.
- **`root` como dueño ⇒ cobro 0.00** por diseño de factores; es correcto, pero para demostrar cobro usar CLIENTE.
- **`tickets/listar` paginado** (cambio respecto a versiones previas): las aserciones deben leer `content`.
- **Errores en español y con clave `mensaje`** (Spring) vs `message` (Nest): asertar por `status`/estructura, no por texto exacto.
