# Documento de estudio — Sistema de Gestión de Parqueaderos

> **Objetivo de este documento:** entender **a fondo** cómo funciona todo el
> sistema para estudiarlo y presentarlo: la lógica de cada flujo, para qué sirve
> cada endpoint, cómo funcionan los tokens (access y refresh), qué es cada rol y
> para qué se usa, y cómo se conectan los microservicios entre sí.
>
> Está escrito para leerse de arriba a abajo. Empieza por los **conceptos base**
> (secciones 1–4) y luego entra al **detalle de cada microservicio** (secciones
> 5–9). Al final hay una **guía de repaso** con preguntas típicas de defensa.

---

## Índice

1. [Visión general del sistema](#1-visión-general-del-sistema)
2. [Conceptos fundamentales](#2-conceptos-fundamentales)
3. [Autenticación y tokens (JWT) a fondo](#3-autenticación-y-tokens-jwt-a-fondo)
4. [Roles: qué es cada uno y para qué sirve](#4-roles-qué-es-cada-uno-y-para-qué-sirve)
5. [Microservicio `usuarios`](#5-microservicio-usuarios)
6. [Microservicio `zonas`](#6-microservicio-zonas)
7. [Microservicio `vehiculos`](#7-microservicio-vehiculos)
8. [Microservicio `asignaciones`](#8-microservicio-asignaciones)
9. [Microservicio `tickets` (el núcleo)](#9-microservicio-tickets-el-núcleo)
10. [El API Gateway (Kong)](#10-el-api-gateway-kong)
11. [Flujos completos de prueba](#11-flujos-completos-de-prueba)
12. [Códigos HTTP y su significado](#12-códigos-http-y-su-significado)
13. [Guía de repaso para la defensa](#13-guía-de-repaso-para-la-defensa)

---

## 1. Visión general del sistema

El sistema simula la operación de un **parqueadero**. Está construido con una
**arquitectura de microservicios**: en lugar de una sola aplicación gigante, hay
**5 servicios independientes**, cada uno con su propia responsabilidad y su
propia base de datos. Un **API Gateway (Kong)** es la única puerta de entrada
para el cliente.

```mermaid
flowchart TB
    Client([Cliente / Postman])
    Client -->|"http://localhost:8000 + JWT"| Kong

    subgraph GW["API Gateway - Kong :8000"]
        Kong["Valida JWT · rate-limiting · CORS · enruta"]
    end

    Kong --> USU["usuarios :8081<br/>identidad, login, roles"]
    Kong --> ZON["zonas :8080<br/>zonas y espacios"]
    Kong --> VEH["vehiculos :3000<br/>catálogo de autos"]
    Kong --> ASI["asignaciones :8082<br/>vehículo ↔ dueño"]
    Kong --> TIC["tickets :8083<br/>NÚCLEO: ingreso/cobro"]

    TIC -.consulta.-> VEH
    TIC -.consulta.-> ASI
    TIC -.consulta y actualiza.-> ZON

    subgraph DB["PostgreSQL :5433 (una BD por servicio)"]
        D1[(usuarios)] & D2[(zonas)] & D3[(vehiculos_db)] & D4[(asignaciones)] & D5[(tickets)]
    end
    USU-->D1
    ZON-->D2
    VEH-->D3
    ASI-->D4
    TIC-->D5
```

| Servicio | Puerto | Tecnología | BD | Responsabilidad |
|---|---|---|---|---|
| `usuarios` | 8081 | Spring Boot / Java 25 | `usuarios` | Personas, usuarios, roles. **Emite y valida los JWT.** |
| `zonas` | 8080 | Spring Boot / Java 25 | `zonas` | Zonas físicas y sus espacios (plazas). |
| `vehiculos` | 3000 | NestJS + TypeORM | `vehiculos_db` | Catálogo de vehículos (Auto/Moto/Camioneta). |
| `asignaciones` | 8082 | Spring Boot / Java 25 | `asignaciones` | Relación vehículo↔propietario + trazabilidad. |
| `tickets` | 8083 | Spring Boot / Java 25 | `tickets` | **Núcleo**: ingreso, cobro, anulación. Orquesta a los demás. |
| Kong | 8000/8001 | Kong 3.7 DB-less | — | Gateway: seguridad y enrutamiento. |
| PostgreSQL | 5433 | Postgres 16 | — | Persistencia. |

---

## 2. Conceptos fundamentales

### 2.1 ¿Qué es un microservicio y por qué así?
Cada servicio es un programa autónomo que:
- Corre en su **propio puerto**.
- Tiene su **propia base de datos** (no comparten tablas).
- Se puede **desplegar, reiniciar y escalar** de forma independiente.
- Se comunica con los demás por **HTTP/REST**.

Ventaja clave para la demo: si `vehiculos` se cae, `usuarios` sigue funcionando.
El precio a pagar es la **coordinación** (por eso existe `tickets`, que orquesta).

### 2.2 ¿Qué es el API Gateway?
Kong es la **única URL pública** (`http://localhost:8000`). El cliente nunca
llama directo a los puertos 8081, 8080, etc. Kong:
1. **Valida el token JWT** (firma y expiración) antes de dejar pasar.
2. Aplica **rate-limiting** (máx. 100 peticiones/minuto).
3. Aplica **CORS** (para que un frontend pueda consumirlo).
4. **Enruta** cada ruta al microservicio correcto.

### 2.3 Autenticación vs Autorización (¡no confundir!)
- **Autenticación** = *"¿quién eres?"* → se resuelve con el **token JWT**. Lo
  hacen `usuarios` (al emitirlo) y Kong (al validarlo).
- **Autorización** = *"¿qué puedes hacer?"* → se resuelve con los **roles**. Lo
  hace **cada microservicio** con `@PreAuthorize`. Ejemplo: `tickets` solo deja
  registrar ingresos a `RECAUDADOR`, `ADMIN` o `ROOT`.

> Regla mental: **Kong dice "el token es válido"; el microservicio dice "y además
> tiene permiso".**

### 2.4 Persona vs Usuario (concepto central del dominio)
- **Persona** = la **identidad** de un ser humano (nombre, cédula, correo). **No
  tiene login.** Un propietario de vehículo puede ser solo una persona.
- **Usuario** = las **credenciales de acceso** (username + password + roles),
  siempre **ligadas a una persona existente**.

> Relación **1:1**: una persona puede tener a lo sumo un usuario, y todo usuario
> tiene exactamente una persona detrás. En la base de datos comparten la misma
> clave primaria (`users.id = persons.id` vía `@MapsId`).

---

## 3. Autenticación y tokens (JWT) a fondo

### 3.1 ¿Qué es un JWT?
Un **JSON Web Token** es una cadena firmada digitalmente que prueba tu identidad
sin que el servidor tenga que guardar sesión (es **stateless**). Tiene 3 partes
separadas por puntos: `header.payload.firma`.

- **header**: qué algoritmo firma el token (aquí **HS512**).
- **payload**: los *claims* (datos) — quién eres, tus roles, cuándo expira.
- **firma**: un HMAC calculado con un **secreto compartido**. Si alguien altera
  el payload, la firma deja de cuadrar y el token se rechaza.

### 3.2 El secreto y el algoritmo
- **Algoritmo:** **HS512** (HMAC-SHA-512). El código usa `signWith(key)` y la
  librería JJWT elige HS512 automáticamente porque el secreto tiene 520 bits
  (≥512). *(El comentario "HS256" en el código es engañoso: lo que realmente se
  firma es HS512, y por eso Kong está configurado con `algorithm: HS512`.)*
- **Secreto compartido:** `parqueadero-espe-clave-secreta-jwt-cambia-esto-en-produccion-2026`.
  Lo conocen **usuarios** (para firmar), **Kong** (para validar) y **cada
  microservicio** (para validar por su cuenta). Debe ser idéntico en todos.
- **Issuer (`iss`):** `parqueadero`. El token dice quién lo emitió; Kong y los
  servicios exigen que coincida.

### 3.3 Los dos tipos de token: ACCESS y REFRESH
El sistema emite **dos** tokens en cada login/registro:

| | **Access token** | **Refresh token** |
|---|---|---|
| Para qué sirve | Autorizar cada petición | Obtener un nuevo access token |
| ¿Lleva `roles`? | ✅ Sí | ❌ No (solo identidad) |
| Duración | **120 minutos (2 h)** | **10 080 minutos (7 días)** |
| Claim `type` | `"access"` | `"refresh"` |
| Se envía en | Header `Authorization: Bearer` | En el body de `/auth/refresh` |

**Claims del ACCESS token:**
```json
{
  "iss": "parqueadero",
  "sub": "uuid-del-usuario",     // identidad → tickets lo usa como idEmpleado
  "type": "access",
  "username": "root",
  "roles": ["ROOT"],              // autorización
  "iat": 1700000000,             // emitido en
  "exp": 1700007200              // expira (iat + 120 min)
}
```

**Claims del REFRESH token:** iguales pero **sin `roles`** y con `type: "refresh"`.

### 3.4 ¿Para qué sirve el refresh token? (pregunta típica)
El access token dura poco (2 h) **a propósito**: si te lo roban, el atacante solo
tiene una ventana corta. Pero sería molesto pedirle al usuario que escriba su
contraseña cada 2 horas.

La solución: el **refresh token** (dura 7 días) permite **pedir un access token
nuevo sin volver a introducir la contraseña**. El flujo es:
1. El access token expira (pasan 2 h).
2. El cliente llama `POST /auth/refresh` enviando su refresh token.
3. `usuarios` valida el refresh token, consulta los **roles actualizados** en la
   BD, y devuelve un **nuevo access token + un nuevo refresh token** (rotación).

**Detalles importantes de la implementación:**
- `refresh` **NO necesita el access token viejo**; solo el refresh token.
- Verifica que el usuario **siga existiendo y activo**.
- Como los roles se releen de la BD, si a un usuario le quitaron un rol, su
  próximo access token ya no lo tendrá.
- El `type` evita confusión: un refresh token **no** sirve como access token.

### 3.5 Los 4 endpoints de autenticación (`/api/v1/auth`, PÚBLICOS)

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/auth/login` | POST | ❌ pública | Recibe `{username, password}`. Verifica el hash bcrypt, actualiza `lastLogin`, y devuelve `{token, refreshToken, roles, ...}`. |
| `/auth/register` | POST | ❌ pública | Recibe `{idPersona, username, password}`. Crea un **usuario** ligado a una persona existente y le asigna **automáticamente el rol CLIENTE**. Devuelve tokens. |
| `/auth/refresh` | POST | ❌ pública | Recibe `{refreshToken}`. Emite un nuevo par de tokens (ver 3.4). |
| `/auth/me` | GET | ✅ token | Devuelve el perfil del usuario del token: `{idUsuario, username, nombreCompleto, active, roles}`. |

**Respuesta de login/register (`AuthResponse`):**
```json
{
  "token": "eyJ...",            // access token
  "refreshToken": "eyJ...",     // refresh token
  "tokenType": "Bearer",
  "expiresIn": 7200,             // segundos (120 min)
  "refreshExpiresIn": 604800,    // segundos (7 días)
  "idUsuario": "uuid",
  "username": "root",
  "roles": ["ROOT"]
}
```

**Reglas de validación al registrar/crear usuario:**
- `username`: 3–15 caracteres, patrón `^[a-zA-Z0-9._-]+$`.
- `password`: 6–30 caracteres, **al menos 1 minúscula, 1 mayúscula y 1 dígito**.
- La contraseña se guarda **hasheada con bcrypt** (nunca en texto plano).

### 3.6 Cómo viaja el token en Postman
En cada petición protegida se envía el header:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6...
```
La colección de Postman guarda el token automáticamente en la variable `{{token}}`
tras el login, y lo reutiliza en las demás peticiones.

---

## 4. Roles: qué es cada uno y para qué sirve

Los roles se **siembran automáticamente** al arrancar `usuarios` (clase
`RolesSeeder`). Son 5:

| Rol | Descripción | Para qué sirve en la práctica |
|---|---|---|
| **ROOT** | Súper usuario, acceso total | Puede hacer **todo**. Pasa cualquier `@PreAuthorize`. En tarifas de tickets paga **$0.00** (factor 0). |
| **ADMIN** | Administra zonas, espacios, vehículos, roles y asignaciones | Gestión administrativa. Igual que ROOT en la mayoría de endpoints (`hasAnyRole('ADMIN','ROOT')`). Factor tarifa 0.50. |
| **RECAUDADOR** | Cobro de tickets | El **operador del parqueadero**. Es el rol mínimo que puede **registrar ingresos, cobrar y anular** en `tickets`. Factor tarifa 0.50. |
| **CLIENTE** | Usuario final | Rol que se asigna **automáticamente al registrarse**. Ve sus propios datos (`/auth/me`, su usuario). **NO puede operar tickets** (por eso da 403). Factor tarifa 0.60 (40% de descuento como comunidad). |
| **INVITADO** | Acceso público mínimo | Visitante externo. Factor tarifa 1.00 (paga precio completo). |

**Usuario administrador de arranque (bootstrap):**
- Se crea solo si no existe (idempotente), en la clase `BootstrapAdminSeeder`.
- **Usuario:** `root` / **Contraseña:** `Root2025`.
- Persona asociada: DNI `0000000000`, nombre "Root Sistema".
- Rol: `ROOT`.

> **Dónde y cómo se verifica el rol (¡importante y sutil!):**
> - Solo **dos** microservicios comprueban roles por su cuenta (con Spring
>   Security + `@PreAuthorize`):
>   - **`usuarios`**: la gestión de personas, roles y asignaciones exige
>     **ADMIN o ROOT** (`hasAnyRole('ADMIN','ROOT')`).
>   - **`tickets`**: operar exige **RECAUDADOR, ADMIN o ROOT** (constante
>     `RolesTickets.PUEDE_OPERAR`).
> - **`zonas` y `vehiculos` NO comprueban roles** (no tienen Spring Security):
>   cualquier token válido que pase por Kong puede usarlos. La autenticación la
>   garantiza Kong; la autorización fina no existe en esos dos.
> - **`asignaciones` tampoco tiene `@PreAuthorize`**, pero **reenvía tu token**
>   a `usuarios` (a un endpoint que exige ADMIN/ROOT). Por eso, en la práctica,
>   **crear o reactivar una asignación requiere un token con ADMIN o ROOT**: si
>   no lo tienes, `usuarios` responde 403 y la operación falla.
> - El rol también define el **descuento en la tarifa** (sección 9.4).

**Un usuario puede tener varios roles.** La relación usuario–rol es **N:N**
(tabla `user_role`) y cada asignación tiene su propio flag `active`, de modo que
se puede quitar/devolver un rol sin borrar la relación.

---

## 5. Microservicio `usuarios`

**Responsabilidad:** identidad (personas), acceso (usuarios), roles y la relación
usuario–rol. Es la **autoridad de seguridad** de todo el sistema.

### 5.1 Modelo de datos
- **Persona (`persons`)**: `id` (UUID PK), `firstName`, `middleName`, `lastName`,
  `dni` (único, cédula ecuatoriana validada), `email` (único), `phone`,
  `address`, `nationality`, `active`, timestamps.
- **Usuario (`users`)**: `id` (= id de la persona vía `@MapsId`), `id_person`
  (FK+PK), `username` (único), `passwordHash` (bcrypt, 60 chars), `lastLogin`,
  `active`, timestamps.
- **Rol (`roles`)**: `id` (UUID PK), `name` (único), `description`, `active`,
  timestamps.
- **UsuarioRol (`user_role`)**: clave compuesta (`id_user`, `id_role`), `active`,
  `assignedAt`, `updatedAt`. Es la tabla puente N:N.

### 5.2 Endpoints de Personas (`/api/v1/personas`) — requiere ADMIN/ROOT
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/personas` | Listar todas. |
| GET | `/personas/{id}` | Obtener una. |
| GET | `/personas/buscar?dni=&nombre=&apellido=` | Búsqueda flexible. |
| POST | `/personas` | Crear (201). Valida cédula, email, teléfono. |
| PUT | `/personas/{id}` | Actualizar. |
| PATCH | `/personas/{id}/activar` | `active=true`. |
| PATCH | `/personas/{id}/desactivar` | `active=false` (cascada al usuario). |

### 5.3 Endpoints de Usuarios (`/api/v1/usuarios`)
| Método | Ruta | Auth | Qué hace |
|---|---|---|---|
| GET | `/usuarios` | ADMIN/ROOT | Listar. |
| GET | `/usuarios/{id}` | ADMIN/ROOT **o el propio usuario** | Obtener (uno puede ver sus datos). |
| GET | `/usuarios/buscar?username=` | ADMIN/ROOT | Buscar por username. |
| POST | `/usuarios` | ADMIN/ROOT | Crear usuario para una persona existente (201). |
| PUT | `/usuarios/{id}` | ADMIN/ROOT o dueño | Actualizar (password opcional: si va vacío, se conserva). |
| PATCH | `/usuarios/{id}/activar` | ADMIN/ROOT | Requiere que la persona también esté activa. |
| PATCH | `/usuarios/{id}/desactivar` | ADMIN/ROOT | Desactiva y **cascada**: apaga sus roles activos. |

### 5.4 Endpoints de Roles (`/api/v1/roles`) — ADMIN/ROOT
CRUD estándar (GET lista/uno, POST crear 201, PUT actualizar, PATCH activar/
desactivar). Validación de `name`: 3–50 chars, letras/dígitos/espacio/guión bajo
(acepta tildes y ñ).

### 5.5 Endpoints de asignación usuario–rol (`/api/v1/asignaciones`) — ADMIN/ROOT
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/asignaciones` | Listar todas las asignaciones usuario–rol. |
| POST | `/asignaciones` | Asignar un rol a un usuario (201). Evita duplicados. |
| GET | `/asignaciones/usuario/{id}` | Roles de un usuario. |
| PATCH | `/asignaciones/usuario/{id}/rol/{idRol}/desactivar` | Quitar un rol (lógico). |
| PATCH | `/asignaciones/usuario/{id}/rol/{idRol}/activar` | Devolver un rol. |

> Al desactivar una asignación, el usuario **pierde ese rol en su próximo
> login/refresh** (los roles se releen de la BD).

---

## 6. Microservicio `zonas`

**Responsabilidad:** el espacio físico del parqueadero. Una **Zona** (p. ej.
"Zona Norte") contiene varios **Espacios** (plazas individuales).

### 6.1 Estados y tipos de un Espacio
- **Estado (`EstadoEspacio`)**: `DISPONIBLE` (libre), `OCUPADO` (hay un carro),
  `MANTENIMIENTO` (fuera de servicio), `RESERVADO` (reservado).
- **Tipo (`TipoEspacio`)**: `AUTO`, `MOTO`, `BUSETA`. Define qué vehículo cabe.

### 6.2 Endpoints de Zona (`/api/v1/zonas`)
GET (lista/uno con sus espacios anidados), POST (201), PUT, PATCH activar/
desactivar (204). Activar/desactivar una zona **cascada** a sus espacios.

### 6.3 Endpoints de Espacio (`/api/v1/espacios`)
| Método | Ruta | Código | Notas |
|---|---|---|---|
| GET | `/espacios` | 200 | Listar. |
| GET | `/espacios/{id}` | 200 | Obtener. |
| POST | `/espacios` | 201 | Crea con `estado=DISPONIBLE, activo=true`. |
| PUT | `/espacios/{id}` | 200 | Actualiza descripción y tipo. |
| PATCH | `/espacios/{id}/estado?estado=X` | 200 | Cambia estado (por *query param*). Rechaza si ya está en ese estado (409). |
| PATCH | `/espacios/{id}/activar` | 204 | Pone `activo=true, estado=DISPONIBLE`. **Requiere que la zona padre esté activa** (409 si no). |
| PATCH | `/espacios/{id}/desactivar` | 204 | Pone `activo=false, estado=MANTENIMIENTO`. **Rechaza si está OCUPADO** (409). |
| GET | `/espacios/estado/{estado}` | 200 | Filtrar por estado. |
| GET | `/espacios/disponibles?idZona=&tipo=` | 200 | Solo DISPONIBLES y activos. |
| GET | `/espacios/{id}/disponibilidad` | 200 | ¿Usable ahora? (`activo && DISPONIBLE`). |
| GET | `/espacios/zona/{idZona}/estado/{estado}` | 200 | Primer espacio de una zona en cierto estado. |
| DELETE | `/espacios/{id}` | **405** | **No existe borrado físico.** Solo baja lógica (desactivar). |

> **Clave para `tickets`:** al ingresar un vehículo, `tickets` pone el espacio
> en `OCUPADO`; al pagar o anular, lo devuelve a `DISPONIBLE`.

---

## 7. Microservicio `vehiculos`

**Responsabilidad:** catálogo de vehículos. Está en **NestJS** (TypeScript) y usa
**herencia de tabla única (STI)**: una sola tabla `vehiculo` con una columna
discriminadora `tipo` que distingue **Auto**, **Motocicleta** y **Camioneta**.

### 7.1 Campos comunes y por tipo
- **Comunes:** `id` (UUID), `placa` (única, normalizada a mayúsculas), `marca`,
  `modelo`, `color`, `anio`, `clasificacion`, `activo`, y `tipo` (expuesto en las
  respuestas).
- **Auto:** `numeroPuertas` (2–5), `capacidadMaletero` (litros).
- **Motocicleta:** `tipoMoto` (Deportiva/Scooter/Motocross), `cilindraje` (cc).
- **Camioneta:** `cabina` (2 o 4), `capacidadCarga` (p. ej. "750kg").

### 7.2 Formatos y enums
- **Placa Auto/Camioneta:** `ABC-1234` (3 letras, guión, 4 dígitos).
- **Placa Motocicleta:** `AB-123A` (2 letras, guión, 3 dígitos, 1 letra).
- **Clasificación (con tildes):** `Eléctrico`, `Híbrido`, `Gasolina`, `Diésel`.

### 7.3 Endpoints (prefijo global `/api`)
| Método | Ruta | Notas |
|---|---|---|
| POST | `/vehiculos` | Body **anidado**: `{"tipo":"Auto","datos":{...}}`. Crea (201). |
| GET | `/vehiculos?incluirInactivos=true` | Lista (por defecto solo activos). |
| GET | `/vehiculos/{id}` | Obtener por UUID. |
| GET | `/vehiculos/placa/{placa}` | **Lo consume `tickets`.** Devuelve incluyendo `tipo`. |
| PATCH | `/vehiculos/{id}` | Actualización parcial. |
| PATCH | `/vehiculos/{id}/activar` · `/desactivar` | Baja/alta lógica. |

> **Detalle técnico importante:** TypeORM no serializa el discriminador por
> defecto. Se expone `tipo` con hooks `@AfterLoad/@AfterInsert/@AfterUpdate`
> para que `tickets` pueda validar la compatibilidad. **Recuerda `npm run build`
> antes de reiniciar el servicio.**

---

## 8. Microservicio `asignaciones`

**Responsabilidad:** vincular un **vehículo** con su **propietario/usuario** y
llevar la **trazabilidad** (auditoría) de esos cambios. Cumple 3 requisitos
funcionales: **RF1** (asignar), **RF2** (modificar/trazabilidad), **RF3** (flota).

### 8.1 `active` vs `status` (concepto que suele preguntarse)
Son **dos cosas distintas**:

| | `active` | `status` |
|---|---|---|
| Qué significa | Existencia lógica (borrado suave) | Etapa del ciclo de vida |
| Valores | `true` / `false` | `ACTIVA` / `SUSPENDIDA` / `FINALIZADA` |
| `tickets` valida | `active == true` | `status == ACTIVA` **y** `entryAuthorized == true` |

Es decir: una asignación puede existir (`active=true`) pero estar `SUSPENDIDA`;
en ese caso el vehículo **no puede ingresar**.

### 8.2 Endpoints
**Asignaciones (`/api/v1/asignaciones-vehiculos`):**
| Método | Ruta | RF | Qué hace |
|---|---|---|---|
| POST | `/` | RF1 | Asignar vehículo a dueño (201). Valida usuario y vehículo **activos**; **solo 1 asignación activa por vehículo** (409 si ya hay o si el vehículo está inactivo). |
| PATCH | `/{userId}/{vehicleId}` | RF2 | Modificar (status, tipo, validez, alias, autorización…). Registra evento de auditoría. |
| PATCH | `/{userId}/{vehicleId}/desactivar` | RF2 | Baja lógica: `active=false, status=FINALIZADA`. |
| PATCH | `/{userId}/{vehicleId}/activar` | RF2 | Reactivar (revalida usuario/vehículo, exige que no haya otra activa). |
| GET | `/vehiculo/{vehicleId}` | — | **Lo consume `tickets`.** Devuelve la asignación activa: `userId, active, status, entryAuthorized, validFrom/Until, rolAutorizacion`. |
| GET | `/{userId}/{vehicleId}/trazabilidad` | RF2 | Historial de eventos (CREACION/MODIFICACION/ELIMINACION). |

**Flota (`/api/v1/propietarios`):**
| Método | Ruta | RF | Qué hace |
|---|---|---|---|
| GET | `/{userId}/vehiculos` | RF3 | Lista los vehículos activos de un propietario, enriquecidos con datos del vehículo. |

### 8.3 Auditoría (trazabilidad)
Cada cambio genera un `AssignmentAuditEvent` con: `action` (CREACION/MODIFICACION/
ELIMINACION), `timestamp`, `oldPayload` y `newPayload` (JSON del antes/después).
Así se puede reconstruir toda la historia de una asignación.

---

## 9. Microservicio `tickets` (el núcleo)

**Responsabilidad:** el motor del parqueadero. Gestiona el **ingreso**, el
**cobro** y la **anulación**. Es el único que **orquesta** a los demás: para
hacer un ingreso consulta a `vehiculos`, `asignaciones` y `zonas`.

### 9.1 La entidad Ticket
Campos clave: `id`, `codigo` (`TKT-000001`), `idEspacio`+`codigoEspacio`+
`tipoEspacio` (snapshot), `idUsuario` (dueño), `idVehiculo`+`placa`+`tipoVehiculo`
(snapshot), `categoriaTarifa` (rol snapshot para el descuento), `fechaHoraIngreso`,
`fechaHoraSalida`, `estadoTicket` (`ACTIVO`/`PAGADO`/`ANULADO`), `idEmpleado`
(= `sub` del JWT, quién operó), `valorRecaudado`, `motivoAnulacion`.

> **¿Por qué "snapshot"?** El ticket **copia** los datos (placa, tipo, tarifa) al
> momento del ingreso. Así, aunque después cambie el vehículo o el rol, el ticket
> conserva la información con la que se creó (reproducibilidad e integridad).

### 9.2 Máquina de estados del ticket
```mermaid
stateDiagram-v2
    [*] --> ACTIVO: Ingreso (POST)
    ACTIVO --> PAGADO: Pagar (calcula tarifa, libera espacio)
    ACTIVO --> ANULADO: Anular (sin cobro, libera espacio)
    PAGADO --> [*]
    ANULADO --> [*]
    PAGADO --> PAGADO: Pagar de nuevo ❌ 409
    PAGADO --> ANULADO: Anular pagado ❌ 409
    ANULADO --> ANULADO: Anular de nuevo ❌ 409
```
Reglas: **no** se paga dos veces, **no** se anula un pagado, **no** se anula dos
veces. Solo se **anula** desde `ACTIVO`.

### 9.3 El flujo de ingreso paso a paso
```mermaid
sequenceDiagram
    actor R as Recaudador
    participant T as tickets
    participant V as vehiculos
    participant A as asignaciones
    participant Z as zonas
    R->>T: POST /api/v1/tickets {placa, idEspacio}
    T->>V: GET /vehiculos/placa/{placa}
    V-->>T: {id, tipo, activo}
    Note over T: valida vehiculo.activo == true
    T->>A: GET /asignaciones-vehiculos/vehiculo/{id}
    A-->>T: {userId, status, entryAuthorized, validez, rol}
    Note over T: valida active, status=ACTIVA,<br/>entryAuthorized, ventana de validez
    T->>T: ¿ya hay ticket ACTIVO para este vehiculo? → 409
    T->>Z: GET /espacios/{idEspacio}
    Z-->>T: {estado, tipo, activo}
    Note over T: valida activo && estado=DISPONIBLE
    T->>T: ¿ya hay ticket ACTIVO en este espacio? → 409
    T->>T: valida compatibilidad tipo (Auto→AUTO...)
    T->>T: INSERT ticket (ACTIVO, valor 0) [saveAndFlush]
    T->>Z: PATCH /espacios/{id}/estado?estado=OCUPADO
    Z-->>T: 204
    T-->>R: 201 {codigo TKT-000001, ...}
```
**Validaciones (en orden), cada fallo con su código:**
1. Vehículo existe (404) y está activo (409 si inactivo).
2. Asignación activa: `active`, `status=ACTIVA`, `entryAuthorized`, dentro de la
   ventana de validez (409 si algo falla, 404 si no hay asignación).
3. No hay otro ticket ACTIVO para ese vehículo (409).
4. Espacio existe (404), activo y `DISPONIBLE` (409 si no).
5. No hay otro ticket ACTIVO en ese espacio (409).
6. **Compatibilidad de tipo** (409 si no cuadra).
7. Se inserta el ticket y **como último paso** se ocupa el espacio. Si el PATCH
   falla, la transacción **revierte** el ticket (consistencia).

### 9.4 Cálculo de la tarifa (cobro)
La tarifa es **por hora**, según la combinación (tipo de vehículo _ tipo de
espacio), multiplicada por un **factor según el rol** del propietario.

**Tarifa base por hora (desde `.env`):**
| Combinación | Tarifa/hora |
|---|---|
| `AUTO_AUTO` | $1.50 |
| `MOTOCICLETA_MOTO` | $0.75 |
| `CAMIONETA_BUSETA` | $2.00 |
| (por defecto) | $1.00 |

**Factor por rol (descuento comunidad):**
| Rol | Factor | Efecto |
|---|---|---|
| INVITADO | 1.00 | Precio completo |
| CLIENTE | 0.60 | 40% de descuento |
| RECAUDADOR | 0.50 | 50% |
| ADMIN | 0.50 | 50% |
| ROOT | 0.00 | Gratis |

**Fórmula:**
```
horas  = techo( minutos_estacionado / 60 )   // fracción cuenta como hora completa
horas  = máximo( horas, 1 )                  // mínimo 1 hora
base   = tarifaPorHora(tipoVehiculo, tipoEspacio) × horas
total  = base × factorRol(categoria)         // redondeo a 2 decimales, HALF_UP
```
Ejemplo: un Auto en espacio AUTO, dueño CLIENTE, 70 minutos →
`ceil(70/60)=2 h` → `1.50 × 2 = 3.00` → `× 0.60 = $1.80`.

### 9.5 Compatibilidad de tipos (estricta)
```
Auto        → AUTO
Motocicleta → MOTO
Camioneta   → BUSETA
```
La comparación normaliza (mayúsculas, sin tildes). No se permiten sustituciones:
un Auto **no** entra en un espacio MOTO.

### 9.6 Endpoints (requieren RECAUDADOR/ADMIN/ROOT)
| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/v1/tickets` | Ingreso por placa (201). |
| PATCH | `/api/v1/tickets/{id}/pagar` | Cobra, marca `PAGADO`, libera espacio. |
| PATCH | `/api/v1/tickets/{id}/anular` | Body `{motivo}`. Solo ACTIVO → `ANULADO`, sin cobro, libera espacio. |
| GET | `/api/v1/tickets/{id}` | Por UUID. |
| GET | `/api/v1/tickets/codigo/{codigo}` | Por código `TKT-000001`. |
| GET | `/api/v1/tickets?estado=ACTIVO` | Listar por estado. |
| GET | `/api/v1/tickets/activo/espacio/{idEspacio}` | Ticket activo de un espacio. |

### 9.7 Robustez (por qué no se cae fácil)
- **Timeouts** a las dependencias: conexión 3 s, lectura 5 s. Si una dependencia
  se cuelga, responde **503** ("microservicio dependiente no disponible") en vez
  de quedarse bloqueado.
- **Concurrencia:** `saveAndFlush` + índice único parcial evitan que dos ingresos
  simultáneos ocupen el mismo espacio/vehículo (el segundo recibe 409).
- **Cliente HTTP:** usa `JdkClientHttpRequestFactory` porque soporta **PATCH**
  (el cliente por defecto del JDK no lo soporta).

---

## 10. El API Gateway (Kong)

Kong 3.7 en modo **DB-less** (configurado por archivo `gateway/kong.yml`).

### 10.1 Rutas (qué prefijo va a qué servicio)
| Prefijo | Servicio | ¿JWT? |
|---|---|---|
| `/api/v1/auth` | usuarios | ❌ pública |
| `/api/v1/personas`, `/usuarios`, `/roles`, `/asignaciones` | usuarios | ✅ |
| `/api/v1/zonas`, `/api/v1/espacios` | zonas | ✅ |
| `/api/vehiculos` | vehiculos | ✅ |
| `/api/v1/asignaciones-vehiculos`, `/api/v1/propietarios` | asignaciones | ✅ |
| `/api/v1/tickets` | tickets | ✅ |

### 10.2 Plugins
- **jwt** (por ruta o servicio): valida firma HS512 + expiración. El consumidor
  `parqueadero-app` tiene la credencial con `key=parqueadero`, `algorithm=HS512`
  y el mismo secreto que `usuarios`.
- **rate-limiting**: 100 req/min por servicio.
- **cors** (global): permite orígenes cruzados.
- **correlation-id** (global): añade `X-Correlation-ID` para rastrear peticiones.

### 10.3 Puerto admin (8001)
Permite inspeccionar la configuración: `GET /services`, `/routes`, `/plugins`,
`/status`. No expone datos de negocio, solo la config del gateway.

---

## 11. Flujos completos de prueba

### 11.1 Flujo de registro y acceso
```
1) POST /api/v1/personas          → crea la Persona (identidad)   → idPersona
2) POST /api/v1/auth/register     → crea el Usuario + rol CLIENTE  → tokens
3) POST /api/v1/auth/login        → devuelve access + refresh token
4) GET  /api/v1/auth/me           → confirma identidad y roles
5) (2h después) POST /auth/refresh → nuevo access token sin re-login
```

### 11.2 Flujo de preparación del parqueadero (datos maestros)
```
Zona → Espacio (tipo AUTO, DISPONIBLE)
Vehículo (Auto, placa ABC-1234)
Asignación: vehículo ↔ propietario (status ACTIVA, entryAuthorized=true)
```

### 11.3 Flujo del ticket (la demo central)
```
Ingreso (POST) → 201 TKT-000001 (ACTIVO)     [espacio pasa a OCUPADO]
Ingreso duplicado → 409
Pagar (PATCH) → 200 (PAGADO, cobra tarifa)   [espacio vuelve a DISPONIBLE]
Pagar de nuevo → 409
Anular pagado → 409
Nuevo ingreso → 201 → Anular (PATCH) → 200 (ANULADO, sin cobro)
```

### 11.4 Flujo de seguridad por rol (lo más vistoso)
```
Login CLIENTE → Ingreso como CLIENTE → 403 (sin permiso)
Otorgar rol RECAUDADOR al cliente → Login de nuevo (roles actualizados)
Ingreso como RECAUDADOR → 201/409 (ya autorizado, ya no 403)
```
> Este flujo demuestra **autenticación** (el token es válido en ambos casos) vs
> **autorización** (solo con el rol adecuado se permite operar).

---

## 12. Códigos HTTP y su significado

Todos los servicios responden con el mismo contrato:
```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "mensaje": "texto claro" }
```

| Código | Significado | Ejemplo |
|---|---|---|
| **200** OK | Operación exitosa | Login, consultas, pagar. |
| **201** Created | Recurso creado | Crear persona, ingreso de ticket. |
| **204** No Content | Éxito sin cuerpo | Activar/desactivar espacio. |
| **400** Bad Request | Datos inválidos | Falta un campo, placa mal formada. |
| **401** Unauthorized | Token ausente/ inválido | Petición sin `Bearer`. |
| **403** Forbidden | Autenticado pero **sin permiso** | CLIENTE intenta operar tickets. |
| **404** Not Found | No existe el recurso | Placa o espacio inexistente. |
| **405** Method Not Allowed | Método no soportado | DELETE sobre un espacio. |
| **409** Conflict | Regla de negocio violada | Ticket duplicado, pagar dos veces. |
| **503** Service Unavailable | Dependencia caída | Un microservicio no responde. |

---

## 13. Guía de repaso para la defensa

**P: ¿Por qué microservicios y no un monolito?**
R: Independencia de despliegue, escalado y fallos aislados; cada servicio tiene su
BD y su responsabilidad única. El costo es la coordinación (resuelta por `tickets`
y el gateway).

**P: ¿Qué hace exactamente Kong?**
R: Es la única entrada. Valida el JWT (firma HS512 + expiración), aplica
rate-limiting y CORS, y enruta al servicio correcto. No decide permisos por rol
(eso lo hace cada servicio).

**P: ¿Diferencia entre access y refresh token?**
R: El access (2 h, con roles) autoriza cada petición; el refresh (7 días, sin
roles) sirve para pedir un nuevo access sin volver a poner la contraseña. El
refresh relee los roles de la BD, así que reflejan cambios recientes.

**P: ¿Persona vs Usuario?**
R: Persona = identidad (sin login). Usuario = credenciales + roles, ligado a una
persona (relación 1:1, comparten PK). `auth/register` convierte una persona en
usuario CLIENTE.

**P: ¿Cómo decide `tickets` si un vehículo puede entrar?**
R: Consulta `vehiculos` (existe y activo), `asignaciones` (asignación `active` +
`status=ACTIVA` + `entryAuthorized` + ventana de validez) y `zonas` (espacio
`DISPONIBLE` y del tipo compatible). Si todo pasa, crea el ticket y ocupa el
espacio; si el último paso falla, revierte todo.

**P: ¿Qué pasa si un CLIENTE intenta registrar un ingreso?**
R: Kong deja pasar el token (es válido), pero `tickets` responde **403** porque el
rol CLIENTE no está en `PUEDE_OPERAR` (RECAUDADOR/ADMIN/ROOT).

**P: ¿Cómo se calcula el cobro?**
R: Tarifa por hora según (tipo vehículo _ tipo espacio), fracción redondeada a
hora completa, mínimo 1 h, multiplicada por el factor del rol del propietario
(ROOT gratis, ADMIN/RECAUDADOR 0.5, CLIENTE 0.6, INVITADO precio completo).

**P: ¿`active` vs `status` en asignaciones?**
R: `active` es borrado lógico (existe o no); `status` es la etapa (ACTIVA/
SUSPENDIDA/FINALIZADA). `tickets` exige `active=true` **y** `status=ACTIVA`.

**P: ¿Por qué el ticket guarda "snapshots"?**
R: Para que el registro sea reproducible: aunque después cambien la placa, el tipo
o el rol, el ticket conserva los datos con los que se creó.

---
---

# PARTE II — CATÁLOGO EXHAUSTIVO DE VALIDACIONES Y REGLAS (VERBATIM)

Esta segunda parte lista, **campo por campo y regla por regla**, todo lo que
valida el sistema, con los **mensajes de error literales** tal como aparecen en
el código, el **código HTTP** que produce cada uno y la **condición exacta** que
lo dispara. Está pensada para responder en un examen cualquier pregunta del tipo
"¿qué pasa si…?" o "¿qué mensaje sale cuando…?".

## Convenciones de esta parte

- **"verbatim"** = copiado tal cual del código fuente (incluidas tildes o su
  ausencia; muchos mensajes están **sin tildes** a propósito).
- Cada regla se presenta como: *condición que la dispara* → **excepción** →
  **código HTTP** → `"mensaje literal"`.
- Mapeo de excepciones a HTTP (común a los servicios Spring, salvo matices):
  - `MethodArgumentNotValidException` (fallo de `@Valid`) → **400**
  - `IllegalArgumentException` → **400**
  - `RecursoNoEncontradoException` → **404**
  - `ReglaNegocioException` → **409**
  - `CredencialesInvalidasException` → **401** (solo `usuarios`)
  - `AccessDeniedException` → **403**
  - `DataIntegrityViolationException` → **409**
  - `RuntimeException` (genérica) → **500**

---

## A. MICROSERVICIO `usuarios` (puerto 8081)

### A.1 DTOs de entrada (anotaciones y mensajes exactos)

**`LoginRequest`**
| Campo | Anotaciones |
|-------|-------------|
| `username` | `@NotBlank` |
| `password` | `@NotBlank` |

**`RegisterRequest`** (convierte una persona existente en usuario CLIENTE)
| Campo | Anotaciones |
|-------|-------------|
| `idPersona` | `@NotNull` |
| `username` | `@NotBlank`, `@Size(min=3, max=15)`, `@Pattern("^[a-zA-Z0-9._-]+$")` |
| `password` | `@NotBlank`, `@Size(min=6, max=30)`, `@Pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$")` |

> La contraseña exige **al menos una minúscula, una mayúscula y un dígito**.

**`UsuarioUpdateDto`** (edición; la contraseña es **opcional**)
| Campo | Anotaciones |
|-------|-------------|
| `username` | `@Size(min=3, max=15)`, `@Pattern("^[a-zA-Z0-9._-]+$")` |
| `password` | opcional — `@Pattern("^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,30}$")` |

> El patrón admite **cadena vacía** (`^$`) para "no cambiar la contraseña"; si
> viene algo, debe cumplir minúscula+mayúscula+dígito y 6–30 caracteres.

**`PersonaRequestDto`**
| Campo | Anotaciones |
|-------|-------------|
| `firstName` | `@NotBlank`, `@Size(max=30)`, `@Pattern("^[\p{L} ]+$")` (solo letras y espacios) |
| `middleName` | opcional — `@Pattern("^[\p{L} ]*$")` |
| `lastName` | `@NotBlank`, `@Size(max=30)`, `@Pattern("^[\p{L} ]+$")` |
| `dni` | `@NotBlank`, `@CedulaEcuatoriana` (validador propio, ver A.2) |
| `email` | `@NotBlank`, `@Email`, `@Size(max=50)` |
| `phone` | `@NotBlank`, `@Pattern("^\d{7,10}$")` (7 a 10 dígitos) |
| `address` | opcional — `@Size(max=255)` |
| `nationality` | `@NotBlank`, `@Size(max=30)`, `@Pattern` (letras) |

**`RolRequestDto`**
| Campo | Anotaciones |
|-------|-------------|
| `name` | `@NotBlank`, `@Size(min=3, max=50)`, `@Pattern("^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9_ ]+$")` |
| `description` | opcional — `@Size(max=255)` |

**`AsignarRolRequestDto`**
| Campo | Anotaciones |
|-------|-------------|
| `idUser` | `@NotNull` |
| `idRole` | `@NotNull` |

### A.2 Validador propio `@CedulaEcuatoriana` (`CedulaEcuatorianaValidator`)

Algoritmo exacto que aplica para dar por válida una cédula:
1. Debe tener **exactamente 10 dígitos**.
2. Los dos primeros (provincia) deben estar entre **01 y 24**.
3. El **tercer dígito** debe ser **0–5**.
4. **Dígito verificador (algoritmo tipo Luhn / módulo 10):**
   - A los dígitos en **posición impar** (1º,3º,5º,7º,9º) se los multiplica ×2;
     si el resultado es **> 9**, se le **resta 9**.
   - Se suman esos resultados con los dígitos en posición par.
   - `verificador = (10 - (suma % 10)) % 10`.
   - Debe coincidir con el **décimo dígito**.

### A.3 Reglas de negocio (servicios `*.impl`) — condición → excepción → HTTP → mensaje

**`AuthServicioImpl`**
- Login con usuario inexistente, inactivo o contraseña incorrecta →
  `CredencialesInvalidasException` → **401** →
  `"Usuario o contrasena incorrectos"`. (En login exitoso actualiza `lastLogin`.)
- Registro cuando no existe el rol base CLIENTE → `ReglaNegocioException` →
  **409** → `"El rol base CLIENTE no existe; no se pudo completar el registro"`.
- Refresh con token inválido/expirado → `CredencialesInvalidasException` →
  **401** → `"Refresh token invalido o expirado: inicie sesion de nuevo"`.
- Refresh con cuenta inactiva → `CredencialesInvalidasException` → **401** →
  `"La cuenta esta inactiva"`. (El refresh **rota** el refresh token.)

**`UsuarioServicioImpl`**
- Crear usuario y no existe la persona → `RecursoNoEncontradoException` →
  **404** → `"Persona no encontrada con ID: {id}"`.
- Persona inactiva → `ReglaNegocioException` → **409** →
  `"No se puede crear un usuario: la persona esta inactiva"`.
- La persona ya tiene usuario → `ReglaNegocioException` → **409** →
  `"La persona ya tiene un usuario asociado"`.
- Username duplicado → `ReglaNegocioException` → **409** →
  `"Ya existe un usuario con el username: {username}"`.
- Intentar cambiar la persona asociada en un update → `IllegalArgumentException`
  → **400** → `"No se permite cambiar la persona asociada al usuario"`.
- En update, si `password` viene vacío se **conserva** la anterior (no re-hashea).
- `desactivarUsuario`: **cascada** → pone `active=false` a sus asignaciones de rol.
- `activarUsuario` con persona inactiva → `ReglaNegocioException` → **409** →
  `"No se puede activar el usuario: su persona esta inactiva"`.

**`PersonaServicioImpl`** (duplicados; **409** `ReglaNegocioException`)
- `"Ya existe una persona con el dni: {dni}"`
- `"Ya existe una persona con el email: {email}"`
- `"Ya existe una persona con el telefono: {phone}"`

**`RolServicioImpl`**
- Normaliza el nombre a **MAYÚSCULAS y sin espacios sobrantes** (`trim`).
- Desactivar un rol que tiene usuarios activos → `ReglaNegocioException` →
  **409** → `"No se puede desactivar el rol: tiene usuarios activos asignados.
  Primero retire (desactive) esas asignaciones."`.

**`AsignacionServicioImpl`** (asignar rol a usuario; **409** `ReglaNegocioException`)
- Rol inactivo → `"No se puede asignar un rol inactivo: {rol}"`.
- Usuario inactivo → `"No se puede asignar roles a un usuario inactivo"`.
- Rol ya asignado → `"El usuario ya tiene asignado el rol: {rol}"`.

### A.4 Entidades y restricciones de tabla

- **`users`**: `id` (`@MapsId`, PK compartida con `persons.id`), `id_person`,
  `username` **único**, `length=15`; `password_hash` `length=60` (BCrypt);
  `last_login`; `active`.
- **`persons`**: `id`; `first/middle/last_name` `length=30`; `dni` **único**
  `length=30`; `email` **único** `length=50`; `phone` **único** `length=15`;
  `address` TEXT; `nationality` `length=30`; `active`.
- **`roles`**: `id`; `name` **único** `length=50`; `description`; `active`.
- **`user_role`**: PK compuesta (`id_user` + `id_role`); atributo propio `active`.

### A.5 Autorización por endpoint (`@PreAuthorize`)

- **`AuthController`**: `login`, `register`, `refresh` **públicos**; `GET /me`
  solo **autenticado** (`Authentication.name` = `idUsuario`).
- **`UsuarioController`**: `listar`, `buscar`, `crear`, `activar`, `desactivar`
  → `hasAnyRole('ADMIN','ROOT')`. `GET /{id}` y `PUT /{id}` añaden
  `or #idUsuario.toString() == authentication.name` (uno puede ver/editar **lo
  suyo**).
- **`PersonaController`, `RolController`, `AsignacionController`**: a nivel de
  **clase** `hasAnyRole('ADMIN','ROOT')`.

### A.6 Seguridad, filtro JWT y seeders

- **`SecurityConfig`**: `STATELESS`, CSRF desactivado; rutas públicas =
  login/register/refresh. 401 = `"Token ausente o invalido: inicie sesion"`;
  403 = `"No tiene permisos para esta operacion"`.
- **`JwtAuthenticationFilter`**: valida **solo** tokens `type="access"`,
  construye authorities `ROLE_<rol>`, y fija como *principal* el `idUsuario`.
- **`RolesSeeder`** (`@Order(1)`) — crea los 5 roles con descripción:
  - `ROOT` = "Super usuario con acceso total al sistema"
  - `ADMIN` = "Gestion de zonas, espacios, vehiculos, roles-usuarios y asignaciones"
  - `RECAUDADOR` = "Cobro de tickets (sin permisos asignados todavia)"
  - `CLIENTE` = "Usuario final: sus datos, sus vehiculos y lectura del catalogo"
  - `INVITADO` = "Acceso publico minimo"
- **`BootstrapAdminSeeder`** (`@Order(2)`) — crea el superusuario inicial:
  username `root`, contraseña `Root2025`, dni `0000000000`, email
  `root@parqueadero.local`, phone `0000000000`, rol `ROOT`.

### A.7 `GlobalExceptionHandler`

`MethodArgumentNotValid`→400 (+ mapa `errores`), `IllegalArgument`→400,
`RecursoNoEncontrado`→404, `ReglaNegocio`→409, `CredencialesInvalidas`→401,
`AccessDenied`→403 (`"No tiene permisos para esta operacion"`),
`RuntimeException`→500.

---

## B. MICROSERVICIO `zonas` (puerto 8080)

> **Sin Spring Security en el servicio**: todos los endpoints son "públicos" a
> nivel de microservicio; la autenticación la impone **solo Kong**. No hay
> comprobación de roles aquí.

### B.1 DTOs de entrada

**`ZonaRequestDto`**
| Campo | Anotaciones |
|-------|-------------|
| `nombre` | `@NotBlank`, `@Size(min=1, max=32)` |
| `descripcion` | `@Size(max=255)` |
| `tipo` | `@NotNull` (`TipoZona`) |
| `capacidad` | `@Min(1)`, `@Max(100)` |

**`EspacioRequestDto`**
| Campo | Anotaciones |
|-------|-------------|
| `idZona` | `@NotNull` |
| `descripcion` | `@Size(max=150)` |
| `tipo` | `@NotNull` (`TipoEspacio`) |
| `estado` | opcional |

> **`codigo` NO se acepta**: siempre se autogenera en el servidor.

### B.2 Enums

- `EstadoEspacio` = `{ DISPONIBLE, OCUPADO, RESERVADO, MANTENIMIENTO }`
- `TipoEspacio` = `{ MOTO, AUTO, BUSETA }`
- `TipoZona` = `{ VIP, REGULAR, INTERNA, EXTERNA, PREFERENCIAL }`

### B.3 Entidades

- **`Zona`**: `nombre` **único** `length=32`; `codigo` **único** `length=16`;
  `@OneToMany` espacios con cascade `PERSIST/MERGE` (**sin** `orphanRemoval`);
  `activo`; `tipoZona`; `fechaCreacion`/`fechaActualizacion`.
- **`Espacio`**: `codigo` **único** `length=20`; `descripcion` `length=150`;
  `tipoEspacio`; `activo`; `estado`; `@ManyToOne` zona LAZY (`id_zona`).

### B.4 Reglas de negocio (condición → excepción → HTTP → mensaje)

**Zona**
- Crear con nombre repetido → `ReglaNegocioException` → **409** →
  `"Ya existe una zona con el nombre: {nombre}"`. (Al crear, siempre
  `activo=true`; código `ZONA-{3 letras}-{NN}` con VIP→`VIP`, REGULAR→`REG`,
  INTERNA→`INT`, EXTERNA→`EXT`, PREFERENCIAL→`PRE`.)
- Actualizar zona inexistente → `RecursoNoEncontradoException` → **404** →
  `"Zona no encontrada con ID: {id}"`.
- Actualizar con nombre que ya usa otra → `ReglaNegocioException` → **409** →
  `"Ya existe otra zona con el nombre: {nombre}"`.
- Reducir capacidad por debajo de los espacios existentes →
  `ReglaNegocioException` → **409** → `"La capacidad ({X}) no puede ser menor a
  los espacios existentes ({Y})"`.
- `activarZona`: **cascada** → espacios `activo=true` y estado `DISPONIBLE`.
- `desactivarZona` con espacios OCUPADOS → `ReglaNegocioException` → **409** →
  `"No se puede desactivar la zona: tiene espacios OCUPADOS"`. Si no,
  **cascada** → espacios `activo=false` y estado `MANTENIMIENTO`.

**Espacio**
- Crear en zona inexistente → `RecursoNoEncontradoException` → **404** →
  `"Zona no encontrada con ID: {id}"`.
- Crear en zona inactiva → `ReglaNegocioException` → **409** →
  `"No se puede crear un espacio en una zona inactiva"`.
- Crear superando la capacidad → `ReglaNegocioException` → **409** →
  `"La zona alcanzó su capacidad máxima ({N})"`. (Código `ESP-{3 letras}-{NN}`
  con MOTO→`MOT`, AUTO→`AUT`, BUSETA→`BUS`; inicia `activo=true` + `DISPONIBLE`.)
- `actualizarEspacio`: solo cambia **descripción** y **tipo**
  (código/zona/estado/activo son **inmutables** por edición).
- `cambiarEstado` sin id → `IllegalArgumentException` → **400** →
  `"El id del espacio es obligatorio"`; sin estado → **400** →
  `"El nuevo estado es obligatorio"`.
- `cambiarEstado` de un espacio inactivo → `ReglaNegocioException` → **409** →
  `"No se puede cambiar el estado de un espacio inactivo"`.
- `cambiarEstado` al mismo estado → `ReglaNegocioException` → **409** →
  `"El espacio ya se encuentra en estado: {X}"`.
- `activarEspacio` ya activo → **409** → `"El espacio ya está activo"`; con zona
  inactiva → **409** → `"No se puede activar el espacio: su zona está inactiva"`
  (al activar lo deja `DISPONIBLE`).
- `desactivarEspacio` ya inactivo → **409** → `"El espacio ya está inactivo"`;
  espacio OCUPADO → **409** → `"No se puede desactivar un espacio OCUPADO"`
  (al desactivar lo deja `MANTENIMIENTO`).

### B.5 `GlobalExceptionHandler` (extra)

Añade manejo de `MethodArgumentTypeMismatchException` → **400** →
`"Valor invalido '{valor}' para el parametro '{param}'"` (p. ej. un enum mal
escrito en la URL). **No existe endpoint DELETE**: Spring devuelve **405** por
defecto ante un DELETE.

---

## C. MICROSERVICIO `vehiculos` (NestJS, puerto 3000, prefijo `/api`)

> **Sin guards de autenticación** en el servicio: solo `ValidationPipe` + CORS.
> La autenticación la impone **solo Kong**.

### C.1 Rango de años y DTO base

- `ANIO_MIN = 1886`, `ANIO_MAX = añoActual + 1`.

**`BaseVehiculoDto`** (campos comunes)
| Campo | Reglas (class-validator) |
|-------|--------------------------|
| `placa` | `@IsString`, `@IsNotEmpty`, `@Matches(/^[A-Z]{3}-\d{4}$/)` → `"La placa debe tener el formato ABC-1234"` |
| `marca` | `min 2`, `max 50`, `/^[a-zA-ZáéíóúÁÉÍÓÚ\s]+$/` |
| `modelo` | `min 1`, `max 100`, `/^[a-zA-Z0-9áéíóúÁÉÍÓÚ\s.\-]+$/` |
| `color` | `min 3`, `max 50`, solo letras |
| `anio` | `@IsInt`, `@Min(1886)`, `@Max(añoActual+1)` |
| `clasificacion` | `@IsEnum(Clasificacion)` |

**`AutoDto`** (además)
| Campo | Reglas |
|-------|--------|
| `numeroPuertas` | `@IsInt`, `@Min(2)`, `@Max(5)` |
| `capacidadMaletero` | `@IsInt`, `@Min(50)`, `@Max(1500)` |

**`MotocicletaDto`** (además; **placa con formato distinto**)
| Campo | Reglas |
|-------|--------|
| `placa` | `@Matches(/^[A-Z]{2}-\d{3}[A-Z]$/)` → `"...formato AB-123A"` |
| `cilindraje` | `@IsNumber`, `@Min(50)`, `@Max(2500)` |
| `tipoMoto` | `@IsEnum(TipoMoto)` |

**`CamionetaDto`** (además)
| Campo | Reglas |
|-------|--------|
| `cabina` | `@IsIn([2, 4])` |
| `capacidadCarga` | `@IsString`, `@Matches(/^[0-9]+(\.[0-9]+)?\s?(kg|KG|t|T)$/)` |

**`CreateVehiculoDto`** (envoltorio polimórfico)
| Campo | Reglas |
|-------|--------|
| `tipo` | `@IsIn(['Auto','Motocicleta','Camioneta'])` |
| `datos` | `@ValidateNested` + `@Type` (elige el DTO hijo según `tipo`) |

### C.2 Enums

- `Clasificacion` = `{ ELECTRICO='Eléctrico', HIBRIDO='Híbrido',
  GASOLINA='Gasolina', DIESEL='Diésel' }`
- `TipoMoto` = `{ Deportiva, Scooter, Motocross }`

### C.3 Herencia y entidad

- **STI** (Single Table Inheritance): base `Vehiculo` con
  `@TableInheritance({ column: 'tipo' })`; `placa` **única**; `activo` por
  defecto `true`; `tipo` virtual expuesto vía `@AfterLoad/@AfterInsert/@AfterUpdate`.

### C.4 Reglas del `VehiculosService` (condición → error → HTTP → mensaje)

- Normaliza `placa` con `trim().toUpperCase()`.
- Falta la placa dentro de `datos` → `BadRequest` → **400** →
  `"La placa es requerida en datos"`.
- Placa duplicada al crear → `Conflict` → **409** →
  `"Ya existe un vehículo con la placa: {placa}"`.
- `findAll(incluirInactivos)`: por defecto filtra `activo:true`.
- `findByPlaca` sin placa → `BadRequest` → **400** → `"La placa es requerida"`;
  no encontrado → `NotFound` → **404** →
  `"Vehículo no encontrado con placa: {placa}"`.
- `update`: elimina `activo` del payload (no se cambia por edición); duplicado
  con `Not(id)` → `Conflict` → **409** →
  `"Ya existe otro vehiculo con esa placa"`.
- `desactivar` ya inactivo → `Conflict` → **409** →
  `"El vehículo ya está inactivo"`.
- `activar` ya activo → `Conflict` → **409** → `"El vehículo ya está activo"`.
- Factory con tipo no soportado → `Error` →
  `"Tipo de vehículo no soportado: {X}"`.

### C.5 `main.ts` (bootstrap)

`setGlobalPrefix('api')`; `ValidationPipe({ whitelist:true,
forbidNonWhitelisted:true, transform:true, enableImplicitConversion:true })`;
`enableCors()`. `ResponseVehiculoDto` **oculta** `activo`. El controlador usa
**PATCH** para `update`, `activar` y `desactivar`.

---

## D. MICROSERVICIO `asignaciones` (puerto 8082)

> **Sin `@PreAuthorize` propio**, pero **reenvía tu `Authorization`** a
> `usuarios` (endpoint que exige ADMIN/ROOT). El header entra como
> `required=false`.

### D.1 DTOs de entrada

**`CreateAssignmentRequest`**
| Campo | Anotaciones / default |
|-------|-----------------------|
| `userId` | `@NotNull` |
| `vehicleId` | `@NotNull` |
| `assignmentType` | default `PROPIETARIO` |
| `vehicleAlias` | `@Size(max=80)` |
| `observation` | `@Size(max=500)` |

**`UpdateAssignmentRequest`** (todos opcionales)
| Campo | Anotaciones |
|-------|-------------|
| `status`, `assignmentType`, `validUntil`, `entryAuthorized` | opcionales |
| `vehicleAlias` | `@Size(max=80)` |
| `observation`, `changeReason` | `@Size(max=500)` |

### D.2 Entidad `VehicleAssignment` y `@PrePersist`

- Clave `@EmbeddedId AssignmentId(user_id, vehicle_id)`.
- `@PrePersist` fija `assignedAt`, `updatedAt`, `validFrom = now`;
  `status = ACTIVA` por defecto; `assignmentType = PROPIETARIO` por defecto;
  `entryAuthorized = (status == ACTIVA)`.

### D.3 Enums

- `AssignmentStatus` = `{ ACTIVA, SUSPENDIDA, FINALIZADA }`
- `AssignmentType` = `{ PROPIETARIO, AUTORIZADO, TEMPORAL }`
- `AuditAction` = `{ CREACION, MODIFICACION, ELIMINACION }`

### D.4 Reglas de negocio (RF1–RF4)

**RF1 · `crearAsignacion`** (en orden)
1. `validarUsuarioActivo` (GET a `usuarios`, reenvía Authorization):
   no existe → **404** → `"Usuario no encontrado: {userId}"`; inactivo →
   **409** → `"El usuario no esta activo: {userId}"`.
2. `validarVehiculoActivo` (GET a `vehiculos`): no existe → **404** →
   `"Vehiculo no encontrado: {vehicleId}"`; inactivo → **409** →
   `"El vehiculo no esta activo: {vehicleId}"`.
3. `validarRol`: sin rol activo → **409** →
   `"El usuario no tiene un rol activo para autorizar la asignacion"`.
4. `findByIdVehicleIdAndActiveTrue`: si ya hay una activa → **409** →
   `"El vehiculo ya tiene un propietario activo"`.
5. Si la asignación (misma PK) ya existe y está activa → **409** →
   `"La asignacion ya existe y esta activa"`.
6. Publica evento `CREACION`.

**RF2 · `modificarAsignacion`**
- `obtenerAsignacionActiva`: no existe → **404** → `"Asignacion no encontrada"`;
  ya inactiva → **409** → `"La asignacion ya esta inactiva"`.
- `validUntil` anterior a `validFrom` → **409** →
  `"La fecha de fin no puede ser anterior a la fecha de inicio"`.
- `entryAuthorized=true` con `status ≠ ACTIVA` → **409** →
  `"Solo una asignacion ACTIVA puede autorizar ingreso"`.
- `status = FINALIZADA` ⇒ `active = false`. Publica `MODIFICACION`.

**RF3 · `desactivar`**: `active=false`, `status=FINALIZADA`,
`entryAuthorized=false`; publica `ELIMINACION`.

**RF4 · `reactivar`**: revalida usuario/vehículo/rol; si el vehículo ya tiene
otra activa → **409** → `"El vehiculo ya tiene un propietario activo"`; si ya
está activa → **409** → `"La asignacion ya esta activa"`; publica `MODIFICACION`.

**Consultas**
- `consultarAsignacionActivaPorVehiculo`: no hay activa → **404** →
  `"No existe una asignacion activa para el vehiculo: {vehicleId}"`.
- `consultarTrazabilidad`: eventos ordenados por `timestamp DESC`.

### D.5 Cliente externo y manejo de errores

- `ExternalCatalogService` usa `SimpleClientHttpRequestFactory`
  (connect 3 s / read 5 s) — vale porque **solo hace GET**.
- Llamadas: `usuarios` `GET /api/v1/usuarios/{id}` (**reenvía Authorization**) y
  `GET /api/v1/asignaciones/usuario/{id}`; `vehiculos`
  `GET /api/vehiculos/{id}` (sin auth).
- `GlobalExceptionHandler`: `MethodArgumentNotValid`→400,
  `RecursoNoEncontrado`→404, `ReglaNegocio`→409,
  `DataIntegrityViolation`→**409** (`"El vehiculo ya tiene un propietario
  activo"`), `HttpStatusCodeException`→código remoto o **502**
  (`"Error consultando un microservicio externo"`), `RuntimeException`→500.

### D.6 Índice único parcial (BD)

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_active_assignment
ON vehicle_assignments(vehicle_id) WHERE active = true;
```
> Garantiza a nivel de base de datos **una sola asignación activa por
> vehículo**; si dos peticiones concurrentes lo intentan, la segunda choca con
> `DataIntegrityViolationException` → **409**.

---

## E. MICROSERVICIO `tickets` (puerto 8083) — el núcleo

> **Con Spring Security.** `@PreAuthorize` a nivel de **clase** =
> `RolesTickets.PUEDE_OPERAR = hasAnyRole('RECAUDADOR','ADMIN','ROOT')`. El
> `idEmpleado` sale del claim `sub` (`@AuthenticationPrincipal`).

### E.1 DTOs de entrada

**`RegistrarIngresoRequest`**
| Campo | Anotación → mensaje |
|-------|---------------------|
| `placa` | `@NotBlank` → `"La placa es obligatoria"` |
| `idEspacio` | `@NotNull` → `"El id del espacio es obligatorio"` |

**`AnularTicketRequest`**
| Campo | Anotación → mensaje |
|-------|---------------------|
| `motivo` | `@NotBlank` → `"El motivo de anulacion es obligatorio"` |

### E.2 Enum `EstadoTicket` y máquina de estados

`{ ACTIVO, PAGADO, ANULADO }` (PAGADO y ANULADO son **terminales**).

| Transición | ¿Permitida? | Mensaje si se viola |
|-----------|-------------|---------------------|
| `ACTIVO → PAGADO` | ✅ | — |
| `ACTIVO → ANULADO` | ✅ | — |
| `PAGADO → *` | ❌ | `"Solo se pueden pagar tickets activos (estado actual: PAGADO)"` |
| `ANULADO → *` | ❌ | `"Solo se pueden anular tickets activos; un ticket ANULADO no se puede anular"` |

### E.3 `registrarIngreso` — los 12 pasos exactos (con sus validaciones)

1. **Normaliza placa**: `placa.trim().toUpperCase()`.
2. **Vehículo activo**: GET a `vehiculos`; no existe → **404**; inactivo →
   **409** → `"El vehiculo con placa {placa} esta inactivo"`.
3. **Asignación activa**: GET a `asignaciones`; no existe → **404**.
4. **Valida la asignación** (todas **409** `ReglaNegocioException`):
   - `active=false` → `"La asignacion del vehiculo con placa {placa} no esta
     vigente"`.
   - `status ≠ "ACTIVA"` (ignora mayúsculas) → `"La asignacion del vehiculo no
     esta ACTIVA (estado actual: {status})"`.
   - `entryAuthorized=false` → `"El vehiculo con placa {placa} no tiene
     autorizacion de ingreso"`.
   - `now < validFrom` → `"La asignacion aun no es vigente (valida desde
     {validFrom})"`.
   - `now >= validUntil` → `"La asignacion expiro (valida hasta {validUntil})"`.
5. **Sin ticket activo del vehículo**: si existe → **409** →
   `"El vehiculo ya tiene un ticket activo: {codigo}"`.
6. **Espacio**: GET a `zonas`; no existe → **404**.
7. **Espacio disponible** (409):
   - inactivo → `"El espacio {codigo} esta inactivo"`.
   - `estado ≠ "DISPONIBLE"` → `"El espacio {codigo} no esta disponible (estado
     actual: {estado})"`.
8. **Compatibilidad de tipos**: si no son compatibles → **409** →
   `"El vehiculo tipo {tipoVehiculo} no es compatible con un espacio
   {tipoEspacio}; requiere un espacio {esperado}"` (o
   `"; tipo de vehiculo no reconocido"`).
9. **Sin ticket activo del espacio**: si existe → **409** →
   `"El espacio ya tiene un ticket activo: {codigo}"`.
10. **Categoría de tarifa**: usa `rolAutorizacion` si existe; si no,
    `assignmentType`.
11. **Guarda (`saveAndFlush`)**: estado `ACTIVO`, `valorRecaudado=0`. Si choca
    con el índice único → captura `DataIntegrityViolationException` → **409** →
    `"No se pudo registrar el ingreso: el vehiculo o el espacio ya tiene un
    ticket activo (conflicto de concurrencia)"`.
12. **PATCH a `zonas`**: pone el espacio en `OCUPADO`. Si falla, **rollback**
    (`@Transactional`).

### E.4 `pagar`

1. Busca el ticket; no existe → **404**.
2. Si `estado ≠ ACTIVO` → **409** → `"Solo se pueden pagar tickets activos
   (estado actual: {actual})"`.
3. Registra `fechaHoraSalida = now`, calcula tarifa (ver E.6), estado → `PAGADO`,
   guarda `idEmpleado`.
4. PATCH a `zonas`: espacio → `DISPONIBLE` (rollback si falla).

### E.5 `anular`

1. Busca el ticket; no existe → **404**.
2. Si `estado ≠ ACTIVO` → **409** → `"Solo se pueden anular tickets activos; un
   ticket {actual} no se puede anular"`.
3. Estado → `ANULADO`, `valorRecaudado = 0`, `fechaHoraSalida = now`,
   `motivoAnulacion = motivo.trim()`.
4. PATCH a `zonas`: espacio → `DISPONIBLE`.

### E.6 `CalculadoraTarifa` — fórmula exacta

- **Horas cobrables**: `horas = max(ceil(minutos / 60), 1)` (mínimo 1 hora;
  `MINUTOS_POR_HORA=60`, `HORAS_MINIMAS=1`).
- **Cálculo**: si `salida < ingreso` → **409** → `"La fecha de salida no puede
  ser anterior a la de ingreso"`. Si no:
  $$\text{total} = \big(\text{tarifaPorHora}(tipoVeh, tipoEsp) \times \text{horas}\big) \times \text{factorRol}(categoria)$$
  redondeado a 2 decimales (`HALF_UP`).
- **`tarifaPorHora`**: busca en la matriz por clave `TIPOVEH_TIPOESP`
  (normalizada); si no está, usa `porDefecto`.
- **`factorRol`**: normaliza `categoria` (`trim().toUpperCase()`); si es
  nula/vacía o no está en el mapa, usa `factorRolDefecto`.

**Valores por defecto** (de `application.yaml`; `.env` solo trae las tarifas base):
| Matriz (tarifa/hora) | Valor | | Factor por rol | Valor |
|---|---|---|---|---|
| `AUTO_AUTO` | 1.50 | | `INVITADO` | 1.00 |
| `MOTOCICLETA_MOTO` | 0.75 | | `CLIENTE` | 0.60 |
| `CAMIONETA_BUSETA` | 2.00 | | `RECAUDADOR` | 0.50 |
| `por-defecto` | 1.00 | | `ADMIN` | 0.50 |
| | | | `ROOT` | 0.00 |
| | | | `factor-rol-defecto` | 1.00 |

**Ejemplo:** ingreso 10:00, salida 10:45 (45 min) ⇒ `ceil(45/60)=1` hora; AUTO
en espacio AUTO (1.50) × 1 × factor CLIENTE (0.60) = **0.90**.

### E.7 `CompatibilidadTipos`

- Mapa vehículo→espacio: `AUTO→AUTO`, `MOTOCICLETA→MOTO`, `CAMIONETA→BUSETA`.
- `normalizar`: quita tildes (`Normalizer.Form.NFD` + regex `\p{M}`), `trim`,
  `toUpperCase`.
- `claveTarifa(v, e)` = `normalizar(v) + "_" + normalizar(e)`.

### E.8 Entidad `Ticket` (tabla `tickets`)

Campos clave: `codigo` **único** `length=20`; `id_espacio`, `codigo_espacio`,
`tipo_espacio`, `id_usuario`, `id_vehiculo`, `placa`, `tipo_vehiculo`,
`categoria_tarifa` (snapshots); `fecha_hora_ingreso`/`fecha_hora_salida`;
`estado_ticket` (`@Enumerated(STRING)`); `id_empleado`; `valor_recaudado`
(`precision=10, scale=2`, default 0); `motivo_anulacion` `length=300`;
`created_at`/`updated_at`. Índices: `idx_ticket_codigo` (único),
`idx_ticket_vehiculo_estado`, `idx_ticket_espacio_estado`.

### E.9 Cliente externo, seguridad y BD

- **`CatalogoExternoService`**: `JdkClientHttpRequestFactory` (soporta **PATCH**,
  necesario para cambiar el estado del espacio en `zonas`); connect 3 s / read 5 s.
- **`SecurityConfig`**: STATELESS, CSRF off, toda petición autenticada; 401 =
  `"Token ausente o invalido: inicie sesion"`; 403 = `"No tiene permisos para
  operar tickets"`. `JwtService.validarAccessToken` exige `type="access"` e
  `issuer` correcto.
- **`GlobalExceptionHandler`** (extra frente a los otros): `ResourceAccessException`
  → **503** (`"Un microservicio dependiente no esta disponible"`);
  `ServicioExternoException` → **502**; `HttpStatusCodeException` → código remoto;
  `DataIntegrityViolationException` → **409** (`"Conflicto de integridad: el
  ticket viola una restriccion unica"`).
- **`GeneradorCodigoTicket`**: `String.format("TKT-%06d", nextval('ticket_codigo_seq'))`.
- **`schema.sql`**:
  ```sql
  CREATE UNIQUE INDEX IF NOT EXISTS ux_ticket_vehiculo_activo
      ON tickets (id_vehiculo) WHERE estado_ticket = 'ACTIVO';
  CREATE UNIQUE INDEX IF NOT EXISTS ux_ticket_espacio_activo
      ON tickets (id_espacio) WHERE estado_ticket = 'ACTIVO';
  CREATE SEQUENCE IF NOT EXISTS ticket_codigo_seq START 1;
  ```

---

## F. Tabla maestra de códigos autogenerados

| Servicio | Entidad | Formato | Ejemplo | Cómo se genera |
|----------|---------|---------|---------|----------------|
| zonas | Zona | `ZONA-{3L}-{NN}` | `ZONA-VIP-01` | prefijo por `TipoZona` (VIP/REG/INT/EXT/PRE) |
| zonas | Espacio | `ESP-{3L}-{NN}` | `ESP-AUT-03` | prefijo por `TipoEspacio` (MOT/AUT/BUS) |
| tickets | Ticket | `TKT-%06d` | `TKT-000042` | secuencia PostgreSQL `ticket_codigo_seq` |

---

## G. Matriz completa excepción → HTTP por servicio

| Excepción | usuarios | zonas | asignaciones | tickets | vehiculos (NestJS) |
|-----------|:--:|:--:|:--:|:--:|:--:|
| Validación de entrada | 400 | 400 | 400 | 400 | 400 (`ValidationPipe`) |
| `IllegalArgument` | 400 | 400 | — | — | — |
| Type mismatch (enum en URL) | — | 400 | — | — | — |
| No encontrado | 404 | 404 | 404 | 404 | 404 (`NotFound`) |
| Regla de negocio | 409 | 409 | 409 | 409 | 409 (`Conflict`) |
| Credenciales inválidas | 401 | — | — | — | — |
| Token ausente/ inválido | 401 | (Kong) | (Kong) | 401 | (Kong) |
| Sin permisos | 403 | (Kong) | (Kong) | 403 | (Kong) |
| Integridad BD | — | — | 409 | 409 | 409 |
| MS externo caído | — | — | 502 | 503 | — |
| MS externo devuelve error | — | — | 502 / código remoto | código remoto | — |
| Genérica | 500 | 500 | 500 | 500 | 500 |

---

## H. Nota final sobre el algoritmo JWT (aclaración importante)

El comentario del código en `usuarios/.../JwtService.java` dice "HS256", pero es
**engañoso**: el token se firma realmente con **HS512**. La razón es que el
código usa `Keys.hmacShaKeyFor(secret.getBytes())` y luego `.signWith(key)`
**sin indicar algoritmo**; como el secreto tiene **65 bytes (520 bits)**, la
librería JJWT elige automáticamente el algoritmo más fuerte que la clave admite,
que es **HS512**. Además, `gateway/kong.yml` fija explícitamente
`algorithm: HS512` (con un comentario que advierte: si se pusiera HS256, Kong
rechazaría todos los tokens con `401 "Invalid algorithm"`). Conclusión para el
examen: **el sistema firma y valida en HS512**.
