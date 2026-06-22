# 🎓 CONTEXTO PARA EXAMEN — Sistema de Parqueadero (Microservicios)

> **Instrucciones para la IA que use este archivo:** Eres un examinador de la materia
> *Sistemas Distribuidos*. Usa EXCLUSIVAMENTE la información de este documento para
> generar preguntas tipo examen y evaluar mis respuestas. Hazme preguntas una a una,
> de dificultad creciente (teoría → práctica → casos). Después de cada respuesta mía,
> corrígeme con precisión citando la sección correspondiente. Incluye preguntas
> prácticas del estilo: *"¿qué validaciones tiene una zona?"*, *"¿cómo creo/borro un
> espacio con Postman?"*, *"¿qué pasa si envío una placa mal formada?"*, *"¿cómo enruta
> Kong una petición a /api/v1/zonas?"*. Al final, dame una nota /20 y un resumen de mis
> puntos débiles. Banco de preguntas sugerido al final del documento.

---

## 1. Visión general del sistema

Sistema de gestión de parqueadero compuesto por **3 microservicios independientes**,
una **base de datos PostgreSQL unificada** y un **API Gateway Kong**.

| Microservicio | Stack | Puerto | Base de datos | Prefijo de rutas |
|---|---|---|---|---|
| **usuarios** | Spring Boot 3.5 / Java 25 | `8081` | `usuarios` | `/api/v1/...` |
| **zonas** | Spring Boot 3.5 / Java 25 | `8080` | `zonas` | `/api/v1/...` |
| **vehiculos** | NestJS 11 / TypeORM | `3000` | `vehiculos_db` | `/api/...` |
| **PostgreSQL** | Docker postgres:16 | `5432` | las 3 anteriores | — |
| **Kong** | Docker kong:3.7 (DB-less) | `8000` proxy / `8001` admin | — | — |

### Principios de arquitectura (¡clave para teoría!)
- **Independencia total:** los microservicios **NO se llaman entre sí**. Cada uno es
  autónomo y tiene **su propia base de datos** (patrón *Database per Service*).
- **Base de datos física única, lógicamente separada:** un solo contenedor PostgreSQL
  aloja 3 bases distintas (`usuarios`, `zonas`, `vehiculos_db`). Se crean automáticamente
  con `init-db/01-init.sql` la primera vez que se levanta el volumen.
- **Kong = único punto de entrada (opcional):** centraliza el acceso. El cliente puede
  pegarle directo a cada API (desarrollo) o a Kong en el puerto 8000 (producción/demo).
- **Sin estado compartido:** la escalabilidad y el aislamiento de fallos son las ventajas
  principales de esta separación.

```
Cliente ──► Kong (:8000) ──► usuarios  (:8081) ──► BD usuarios
                          ├─► zonas     (:8080) ──► BD zonas
                          └─► vehiculos (:3000) ──► BD vehiculos_db
            (todas las BD viven en el mismo PostgreSQL :5432)
```

---

## 2. Microservicio USUARIOS (`:8081`)

Paquete base: `ec.edu.espe.usuarios`. Gestiona personas, usuarios (credenciales), roles
y la asignación de roles a usuarios.

### 2.1 Entidades
- **Persona** (tabla `persons`): `id` (UUID), `firstName`, `middleName`, `lastName`,
  `dni` (**único**), `email` (**único**), `phone` (**único**), `address`, `nationality`,
  `active` (boolean).
- **Usuario** (tabla `users`): comparte la **PK con Persona** (`@OneToOne` +
  `@PrimaryKeyJoinColumn id_person`) → un Usuario *ES* una Persona con credenciales.
  Campos: `username` (**único**, 3–15), `password_hash` (60, cifrada con **BCrypt**).
- **Rol** (tabla `roles`): `name` (texto libre, **único**, 3–50, se normaliza a MAYÚSCULAS),
  `description`, `active` (boolean, soft-delete).
- **UsuarioRol** (tabla `user_role`): relación N:M Usuario↔Rol con PK compuesta
  (`UsuarioRolId`) y `active` (boolean, soft-delete de la asignación).

### 2.2 Nota de diseño (roles)
- El nombre del rol **NO** es un enum cerrado: es texto libre validado, para
  permitir crear cualquier rol (ADMINISTRADOR, GUARDIA, CAJERO, etc.) sin
  recompilar. Unicidad garantizada normalizando a MAYÚSCULAS.

### 2.3 Validaciones (DTOs de entrada)
**`PersonaRequestDto`** (crear/actualizar persona):
| Campo | Reglas |
|---|---|
| `firstName` | obligatorio, máx 30, solo letras y espacios |
| `middleName` | opcional, máx 30, solo letras y espacios |
| `lastName` | obligatorio, máx 30, solo letras y espacios |
| `dni` | obligatorio, **cédula ecuatoriana válida de 10 dígitos** (validador custom `@CedulaEcuatoriana`) |
| `email` | obligatorio, formato email, máx 50 |
| `phone` | obligatorio, **7 a 10 dígitos numéricos** (`^\d{7,10}$`) |
| `address` | opcional, máx 255 |
| `nationality` | obligatorio, máx 30, solo letras y espacios |

**`UsuarioRequestDto`** (crear usuario):
| Campo | Reglas |
|---|---|
| `idPersona` | obligatorio (UUID de una Persona existente) |
| `username` | obligatorio, 3–15, solo `letras números . _ -` (sin espacios) |
| `password` | obligatorio, 6–30, **al menos una mayúscula, una minúscula y un número** |

**`RolRequestDto`**: `name` obligatorio, 3–50, solo letras/números/espacios/`_` (se normaliza a MAYÚSCULAS); `description` máx 255.
**`AsignarRolRequestDto`**: `idUser` obligatorio, `idRole` obligatorio.

### 2.4 Endpoints
```
# Personas
GET    /api/v1/personas                 # listar
GET    /api/v1/personas/{id}            # obtener por id
GET    /api/v1/personas/buscar?dni=...        # búsqueda exacta por cédula
GET    /api/v1/personas/buscar?apellido=...   # búsqueda parcial por apellido
GET    /api/v1/personas/buscar?nombre=...     # búsqueda parcial por nombre
POST   /api/v1/personas                 # crear (body: PersonaRequestDto)
PUT    /api/v1/personas/{id}            # actualizar
PATCH  /api/v1/personas/{id}/desactivar # soft-delete (cascada: desactiva su usuario)
PATCH  /api/v1/personas/{id}/activar    # reactivar persona (NO reactiva el usuario)

# Usuarios
GET    /api/v1/usuarios                 # listar
GET    /api/v1/usuarios/{id}
GET    /api/v1/usuarios/buscar?username=...   # búsqueda parcial por username
POST   /api/v1/usuarios                 # crear (body: UsuarioRequestDto)
PUT    /api/v1/usuarios/{id}
PATCH  /api/v1/usuarios/{id}/desactivar # soft-delete (revoca acceso)
PATCH  /api/v1/usuarios/{id}/activar    # reactivar (falla si la persona está inactiva)

# Roles
GET    /api/v1/roles
POST   /api/v1/roles                    # crear (body: RolRequestDto)
GET    /api/v1/roles/{id}
PUT    /api/v1/roles/{id}
PATCH  /api/v1/roles/{id}/desactivar    # GUARDA: 409 si hay usuarios activos con ese rol
PATCH  /api/v1/roles/{id}/activar

# Asignaciones (rol ↔ usuario)
GET    /api/v1/asignaciones
POST   /api/v1/asignaciones             # body: AsignarRolRequestDto
GET    /api/v1/asignaciones/usuario/{idUsuario}
PATCH  /api/v1/asignaciones/usuario/{idUsuario}/rol/{idRol}/desactivar  # quitar rol (conserva historial)
PATCH  /api/v1/asignaciones/usuario/{idUsuario}/rol/{idRol}/activar     # devolver rol
```

### 2.5 Ejemplo de body — crear persona
```json
{
  "firstName": "Ana", "middleName": "María", "lastName": "Lopez",
  "dni": "1710034065", "email": "ana@espe.edu.ec", "phone": "0991234567",
  "address": "Av. Siempre Viva 123", "nationality": "Ecuatoriana"
}
```

---

## 3. Microservicio ZONAS (`:8080`)

Paquete base: `ec.edu.espe.zonas`. Gestiona **zonas** del parqueadero y los **espacios**
(cupos) que contiene cada zona.

### 3.1 Entidades
- **Zona** (tabla `zonas`): `id` (UUID), `nombre` (**único**, máx 32), `codigo`
  (**único**, máx 16, **autogenerado** por el servicio), `descripcion`, `capacidad` (int),
  `espacios` (1:N), `activo` (boolean), `tipoZona` (enum), `fechaCreacion`,
  `fechaActualizacion` (gestionadas con `@PrePersist` / `@PreUpdate`).
- **Espacio** (tabla `espacios`): `id` (UUID), `codigo` (**único**, máx 20,
  **autogenerado**), `descripcion` (máx 150), `tipoEspacio` (enum), `activo` (boolean),
  `estado` (enum), `zona` (N:1, relación `@ManyToOne`).

### 3.2 Enums
- `TipoZona`: **VIP, REGULAR, INTERNA, EXTERNA, PREFERENCIAL**.
- `TipoEspacio`: **MOTO, AUTO, BUSETA**.
- `EstadoEspacio`: **DISPONIBLE, OCUPADO, RESERVADO, MANTENIMIENTO**.

### 3.3 Validaciones
**`ZonaRequestDto`** (crear/actualizar zona):
| Campo | Reglas |
|---|---|
| `nombre` | obligatorio, 1–32 caracteres |
| `descripcion` | opcional, máx 255 |
| `tipo` | obligatorio (uno de `TipoZona`) |
| `capacidad` | mínimo **1**, máximo **100** |

> **Importante:** el `codigo` de la zona **NO se envía**, lo **autogenera el servicio**.

**`EspacioRequestDto`** (crear/actualizar espacio):
| Campo | Reglas |
|---|---|
| `idZona` | obligatorio (UUID de una zona existente) |
| `descripcion` | opcional, máx 150 |
| `tipo` | obligatorio (uno de `TipoEspacio`) |
| `estado` | opcional (uno de `EstadoEspacio`) |

> El `codigo` del espacio tampoco se envía: lo **autogenera el servicio**.

### 3.4 Endpoints
```
# Zonas
GET    /api/v1/zonas                    # listar
GET    /api/v1/zonas/{id}
POST   /api/v1/zonas                    # crear (body: ZonaRequestDto)
PUT    /api/v1/zonas/{id}               # actualizar
PATCH  /api/v1/zonas/{id}/activar       # activar (204)
PATCH  /api/v1/zonas/{id}/desactivar    # desactivar (204)

# Espacios
GET    /api/v1/espacios                                 # listar todos
GET    /api/v1/espacios/{id}
POST   /api/v1/espacios                                 # crear (body: EspacioRequestDto)
PUT    /api/v1/espacios/{id}                            # actualizar
DELETE /api/v1/espacios/{id}                            # eliminar (204)
PATCH  /api/v1/espacios/{id}/estado?estado=OCUPADO      # cambiar estado
PATCH  /api/v1/espacios/{id}/activar                    # activar (204)
PATCH  /api/v1/espacios/{id}/desactivar                 # desactivar (204)
GET    /api/v1/espacios/estado/{estado}                 # filtrar por estado
GET    /api/v1/espacios/disponibles?idZona=&tipo=AUTO   # cupos libres (filtros opcionales)
GET    /api/v1/espacios/{id}/disponibilidad             # ¿este espacio está libre ahora?
GET    /api/v1/espacios/zona/{idZona}/estado/{estado}   # por zona y estado
```
> **Regla de "disponibilidad":** un espacio está disponible cuando `activo == true` **y**
> `estado == DISPONIBLE`. El endpoint `/disponibles` solo lista espacios activos en estado
> DISPONIBLE; `/{id}/disponibilidad` devuelve un `DisponibilidadResponseDto`
> `{ idEspacio, codigo, disponible, activo, estado }`.

### 3.5 Ejemplos de body
```json
// Crear zona
{ "nombre": "Zona A", "descripcion": "Planta baja", "tipo": "VIP", "capacidad": 20 }

// Crear espacio (idZona = UUID devuelto al crear la zona)
{ "idZona": "b3f1...uuid", "descripcion": "Cupo junto a la puerta", "tipo": "AUTO", "estado": "DISPONIBLE" }
```

### 3.6 Cómo CREAR y BORRAR un espacio con Postman (flujo típico de examen)
1. **Crear zona:** `POST {{urlZonas}}/api/v1/zonas` con el body de zona → respuesta `201`
   con el `id` de la zona (guárdalo en la variable `idZona`).
2. **Crear espacio:** `POST {{urlZonas}}/api/v1/espacios` con `idZona` en el body →
   respuesta `201` con el `id` del espacio (guárdalo en `idEspacio`).
3. **Verificar:** `GET {{urlZonas}}/api/v1/espacios/{{idEspacio}}` → `200`.
4. **Borrar espacio:** `DELETE {{urlZonas}}/api/v1/espacios/{{idEspacio}}` → `204 No Content`.
5. **Confirmar borrado:** `GET {{urlZonas}}/api/v1/espacios/{{idEspacio}}` → `404 Not Found`.

---

## 4. Microservicio VEHICULOS (`:3000`, prefijo global `/api`)

NestJS + TypeORM. Usa **herencia de tabla única** (`@TableInheritance`, columna
discriminadora `tipo`): un tipo abstracto `Vehiculo` con 3 subtipos.

### 4.1 Entidad y enums
- **Vehiculo** (abstract): `id` (UUID), `placa` (**única**), `marca`, `modelo`, `color`,
  `anio`, `clasificacion` (enum). Subtipos: **Auto**, **Motocicleta**, **Camioneta**.
- `Clasificacion`: **Eléctrico, Híbrido, Gasolina, Diésel**.
- `TipoMoto`: **Deportiva, Scooter, Motocross** (y otros).

### 4.2 Validaciones (`CreateVehiculoDto`) — payload **anidado y discriminado**
El body tiene 2 partes: `tipo` (string) y `datos` (objeto que se valida según el `tipo`).

**Comunes a todos (`BaseVehiculoDto`):**
| Campo | Reglas |
|---|---|
| `placa` | formato **`ABC-1234`** (`^[A-Z]{3}-\d{4}$`) — *en motos es `AB-123A`* |
| `marca` | 2–50, solo letras y espacios |
| `modelo` | 1–100, letras/números/espacios/puntos/guiones |
| `color` | 3–50, solo letras y espacios |
| `anio` | entero, entre **1886** y **(año actual + 1)** |
| `clasificacion` | uno del enum `Clasificacion` |

**Según subtipo:**
- **Auto:** `numeroPuertas` (2–5), `capacidadMaletero` (50–1500 litros).
- **Motocicleta:** `placa` formato **`AB-123A`**, `cilindraje` (50–2500 cc), `tipoMoto` (enum).
- **Camioneta:** `cabina` ∈ {2, 4} (simple/doble), `capacidadCarga` formato **`750kg`** o **`3.5t`**.

### 4.3 Endpoints (recordar el prefijo `/api`)
```
POST   /api/vehiculos                   # crear (body: CreateVehiculoDto)
GET    /api/vehiculos                   # listar
GET    /api/vehiculos/placa/{placa}     # buscar por placa (declarado ANTES de /:id)
GET    /api/vehiculos/{id}              # obtener por id
PATCH  /api/vehiculos/{id}              # actualizar
DELETE /api/vehiculos/{id}              # eliminar
```
> **Detalle técnico de examen:** la ruta literal `placa/:placa` debe declararse **antes**
> de `:id`, si no NestJS interpretaría `placa` como un id. (En Spring pasa igual con
> `/buscar` antes de `/{id}` y `/disponibles` antes de `/{id}`.)

### 4.4 Ejemplo de body — crear auto
```json
{
  "tipo": "Auto",
  "datos": {
    "placa": "ABC-1234", "marca": "Toyota", "modelo": "Corolla",
    "color": "Rojo", "anio": 2022, "clasificacion": "Gasolina",
    "numeroPuertas": 4, "capacidadMaletero": 470
  }
}
```

---

## 5. API Gateway Kong (`:8000` proxy / `:8001` admin)

### 5.1 ¿Qué es y cómo está configurado?
- **Kong en modo DB-less (declarativo):** no usa base de datos; lee toda su configuración
  del archivo `gateway/kong.yml` al arrancar. "Sin código": el ruteo y las reglas son
  declarativos en YAML.
- Corre en **Docker**; las APIs corren en el **host**, por eso Kong las alcanza con
  `host.docker.internal` (resuelto con `extra_hosts: host-gateway` en el docker-compose).

### 5.2 Ruteo (cómo decide a qué servicio enviar)
Kong enruta **por el path** de la petición, **preservando la ruta completa**
(`strip_path: false`, es decir NO recorta el prefijo):
| Path que llega a Kong (:8000) | Servicio destino |
|---|---|
| `/api/v1/personas`, `/api/v1/usuarios`, `/api/v1/roles`, `/api/v1/asignaciones` | usuarios `:8081` |
| `/api/v1/zonas`, `/api/v1/espacios` | zonas `:8080` |
| `/api/vehiculos` | vehiculos `:3000` |

> Ejemplo: `GET http://localhost:8000/api/v1/zonas` → Kong ve el path `/api/v1/zonas`,
> lo asocia al service `zonas`, y reenvía a `http://host.docker.internal:8080/api/v1/zonas`
> (path intacto porque `strip_path: false`).

### 5.3 Plugins (las "reglas" del gateway)
- **rate-limiting** (por servicio): máximo **100 peticiones/minuto**, `policy: local`.
  Si se supera → Kong responde **`429 Too Many Requests`**.
- **cors** (global): permite consumir el gateway desde un frontend de otro origen
  (orígenes `*`, métodos GET/POST/PUT/PATCH/DELETE/OPTIONS).
- **correlation-id** (global): añade a cada petición la cabecera **`X-Correlation-ID`**
  (UUID) para **trazabilidad** entre servicios; se devuelve al cliente (`echo_downstream`).

### 5.4 Comprobar Kong
```
curl http://localhost:8001/services    # servicios registrados (3)
curl http://localhost:8001/routes      # rutas registradas
curl http://localhost:8000/api/v1/zonas  # mismo resultado que el acceso directo
```

---

## 6. Cómo probar TODO con Postman

### 6.1 Variables (clave para no equivocarse)
Cada colección usa una variable de URL. Tras unificar, todas coinciden con el environment:
| Colección | Variable | Valor directo |
|---|---|---|
| usuarios | `urlUsuarios` | `http://localhost:8081` |
| zonas | `urlZonas` | `http://localhost:8080` |
| espacios | `urlZonas` | `http://localhost:8080` |
| vehiculos | `urlVehiculos` | `http://localhost:3000/api` |

### 6.2 Dos environments
- **Parqueadero - Local** → pega directo a cada API (8081 / 8080 / 3000).
- **Parqueadero - Kong** → todo apunta a `http://localhost:8000` (y `…:8000/api` para vehículos).
- **Cambiar de modo = cambiar de environment** en el desplegable de Postman. Nada más.

### 6.3 Orden de ejecución
Ejecuta cada colección **de arriba hacia abajo** (o "Run collection"): los requests de
"crear" guardan los IDs en variables que reutilizan los de "consultar/actualizar/borrar".

### 6.4 Códigos HTTP esperados (memorizar)
| Código | Cuándo |
|---|---|
| `200 OK` | GET / PUT / PATCH con cuerpo exitoso |
| `201 Created` | POST que crea un recurso |
| `204 No Content` | DELETE o PATCH activar/desactivar exitoso |
| `400 Bad Request` | falla una **validación** del DTO (campo obligatorio, formato, etc.) |
| `404 Not Found` | el recurso (id) no existe |
| `409 Conflict` | violación de **unicidad** (dni/email/username/placa/nombre de zona repetidos) |
| `429 Too Many Requests` | se superó el rate-limit de Kong (100/min) |

---

## 7. Banco de preguntas sugerido (para la otra IA)

**Teoría / arquitectura**
1. ¿Por qué los microservicios no se comunican entre sí y qué patrón de datos usan?
2. ¿Qué significa que Kong esté en modo *DB-less / declarativo*? ¿De dónde lee su config?
3. ¿Qué hace `strip_path: false` y por qué es importante aquí?
4. ¿Para qué sirve el plugin `correlation-id` en un sistema distribuido?
5. ¿Cómo alcanza Kong (en Docker) a las APIs que corren en el host?

**Validaciones**
6. ¿Qué validaciones tiene una **zona** al crearla? ¿Y un **espacio**?
7. ¿Qué campos de `PersonaRequestDto` son obligatorios y qué formato exige el `dni`?
8. ¿Qué reglas debe cumplir un `password` de usuario?
9. ¿Qué formato debe tener la `placa` de un auto vs. la de una motocicleta?
10. ¿Qué rango de `anio` acepta un vehículo y por qué el mínimo es 1886?

**Práctica con Postman**
11. Describe paso a paso cómo **crear y luego borrar** un espacio (con códigos HTTP).
12. ¿Qué status devuelve crear una zona con `capacidad = 0`? ¿Por qué?
13. ¿Qué pasa si creas dos personas con el mismo `email`? ¿Qué código HTTP?
14. ¿Cómo listas solo los espacios **disponibles** de tipo AUTO de una zona?
15. ¿Cómo cambias el estado de un espacio a `OCUPADO`?

**Kong / ruteo**
16. Si haces `GET http://localhost:8000/api/vehiculos/placa/ABC-1234`, ¿a qué servicio
    y puerto llega y con qué path?
17. ¿Qué environment de Postman usas para probar vía Kong y qué cambia respecto al directo?
18. ¿Qué ocurre si superas las 100 peticiones por minuto?

**Casos / razonamiento**
19. Un compañero dice que el environment marcaba `urlUsuarios = 8080`. ¿Por qué fallaban
    las pruebas de usuarios y cómo se corrige?
20. ¿Por qué la ruta `/buscar` debe declararse antes de `/{id}` en el controlador?
