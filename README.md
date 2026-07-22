# 🅿️ Sistema de Parqueadero — Microservicios Distribuidos

Proyecto de **Sistemas Distribuidos** (Parcial 2). El sistema está compuesto por
**6 microservicios independientes**, una **base de datos PostgreSQL unificada**, un
**bus de eventos RabbitMQ**, un **API Gateway Kong** que expone todo desde un único punto de
entrada, y un **frontend React** para usarlo desde el navegador.

| Componente | Tecnología | Puerto | Base de datos |
|---|---|---|---|
| **usuarios** | Spring Boot 3.5 (Java 25) | `8081` | `usuarios` |
| **zonas** | Spring Boot 3.5 (Java 25) | `8080` | `zonas` |
| **vehiculos** | NestJS 11 + TypeORM | `3000` (prefijo `/api`) | `vehiculos_db` |
| **asignaciones** | Spring Boot 3.5 (Java 25) | `8082` | `asignaciones` |
| **tickets** | Spring Boot 3.5 (Java 25) | `8083` | `tickets` |
| **ms-audit** | NestJS 11 + TypeORM | `3002` (prefijo `/api/v1`) | `audit_db` |
| **frontend** | React 18 + Vite + TS | `5173` | — |
| **PostgreSQL** | Docker (postgres:16) | `5433` | todas las anteriores |
| **RabbitMQ** | Docker (rabbitmq:3-management) | `5672` AMQP / `15672` panel | — |
| **Kong Gateway** | Docker (kong:3.7, DB-less) | `8000` proxy / `8001` admin | — |

```
Frontend (:5173) ──► Kong (:8000) ──► usuarios (:8081)
                                   ├─► zonas       (:8080)
                                   ├─► vehiculos    (:3000)
                                   ├─► asignaciones (:8082)
                                   ├─► tickets      (:8083)
                                   └─► ms-audit     (:3002)
```

---

## ⚡ Arranque rápido (para el equipo)

**¿Solo quieres compilar y probar todo con datos?** Usa el script automático: genera las
claves JWT, levanta Docker + Kong, **compila y arranca los 6 microservicios + el frontend**, y
**carga los datos de prueba** — todo en un comando.

**Linux / macOS**
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk   # tu ruta al JDK 25
chmod +x iniciar.sh detener.sh
./iniciar.sh
```

**Windows (PowerShell, en la raíz del repo — antes define `JAVA_HOME` al JDK 25)**
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\iniciar.ps1
```

Al terminar tendrás:
- **Frontend:** http://localhost:5173  ·  **Gateway:** http://localhost:8000
- **Cuentas de prueba:** `qa.admin / QaAdmin2025` (ADMIN), `qa.recauda / QaRecauda2025`
  (RECAUDADOR), `qa.cliente / QaCliente2025` (CLIENTE), `qa.invitado / QaInvitado2025`
  (INVITADO) y `root / Root2025` (súper admin).

Detener todo: `./detener.sh` (Linux/macOS) o `.\detener.ps1` (Windows). Opciones: `--no-seed`
/ `-NoSeed` (sin datos), `--no-front` / `-NoFront` (sin frontend), `--wipe` / `-Wipe` (borra la BD).

> 📘 **Guía completa (auto + manual, Windows y Linux, cuentas y pruebas):** [`GUIA-EQUIPO.md`](GUIA-EQUIPO.md).
> Si prefieres arrancar servicio por servicio a mano, sigue la sección **Puesta en marcha** de abajo.

---

## 📋 Requisitos previos

Instala esto en tu equipo (sirve igual para **Windows**, Linux y macOS):

| Herramienta | Versión | Para qué |
|---|---|---|
| **JDK 25** | 25 | compilar/ejecutar usuarios, zonas, asignaciones y tickets |
| **Node.js** | 20 o superior | ejecutar vehiculos, ms-audit y el frontend |
| **Docker Desktop** | reciente | PostgreSQL, RabbitMQ y Kong |
| **Python** | 3.8+ | scripts de carga de datos de prueba |
| **Git** | reciente | clonar el repositorio (en Windows aporta `bash`/`openssl`) |
| **Postman** | reciente | probar las APIs |

> No necesitas instalar Maven: cada servicio Spring trae su **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

### Verificar el JDK 25
```bash
java -version      # debe decir 25
```
- **Windows:** descarga el JDK 25 (Eclipse Temurin u Oracle), instálalo y crea la variable de entorno
  `JAVA_HOME` apuntando a la carpeta del JDK (ej: `C:\Program Files\Java\jdk-25`), y agrega `%JAVA_HOME%\bin` al `Path`.
- **Linux/macOS:** asegúrate de exportar `JAVA_HOME` (ver más abajo).

---

## 🚀 Puesta en marcha (paso a paso)

> Esta es la vía **manual** (servicio por servicio). Si prefieres un solo comando, usa el
> **Arranque rápido** de arriba (`iniciar.sh` / `iniciar.ps1`).

### 1) Clonar el repositorio
```bash
git clone https://github.com/MateoJa54/Parqueadero_distribuidas.git
cd Parqueadero_distribuidas
```

### 2) Generar las claves JWT (RS256) — obligatorio la primera vez
`usuarios` **firma** los tokens con la clave privada; los demás servicios y Kong solo los
**verifican** con la pública. Las claves **no están en Git** (son secretos), así que hay que
generarlas al clonar:
```bash
# Linux/macOS y Windows con Git Bash:
bash scripts/setup_jwt_keys.sh
```
Esto crea `keys/jwt_private.pem`, `keys/jwt_public.pem` y escribe el `.env` de `ms-audit` y
`vehiculos` con la clave pública. Sin este paso los servicios fallan al arrancar con
*"No se pudo cargar la clave privada RSA"*.

### 3) Levantar la base de datos y RabbitMQ (Docker)
Una sola instancia de PostgreSQL crea **automáticamente** todas las bases (incluida
`audit_db`) la primera vez (gracias a `init-db/01-init.sql`), y RabbitMQ es el bus de
eventos que usa el sistema de auditoría:
```bash
docker compose up -d
```
Verifica que estén arriba:
```bash
docker compose ps          # parqueadero-postgres y parqueadero-rabbitmq deben verse "healthy"
```
Panel de RabbitMQ: http://localhost:15672 (usuario/clave `guest`/`guest`).

> Detener sin borrar datos: `docker compose down` · Borrar TODO (resetear): `docker compose down -v`
> Si ya tenías el volumen de Postgres de una instalación anterior (sin `audit_db`), el script de
> init no se re-ejecuta solo; crea la base manualmente una vez:
> `docker exec parqueadero-postgres psql -U postgres -c "CREATE USER audit_user WITH PASSWORD 'audit_pass';" -c "CREATE DATABASE audit_db OWNER audit_user;"`

### 4) Levantar el microservicio **usuarios** (puerto 8081)

**Windows (PowerShell o CMD):**
```powershell
cd usuarios
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
cd usuarios
export JAVA_HOME=/ruta/al/jdk-25      # ej: /usr/lib/jvm/java-25-openjdk
./mvnw spring-boot:run
```

### 5) Levantar el microservicio **zonas** (puerto 8080)
En **otra terminal**:

**Windows:**
```powershell
cd zonas
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
cd zonas
export JAVA_HOME=/ruta/al/jdk-25
./mvnw spring-boot:run
```

### 6) Levantar el microservicio **vehiculos** (puerto 3000)
En **otra terminal**:
```bash
cd vehiculos/vehiculos
npm install
npm run start:dev
```

En este punto las APIs core corren en el host y la base en Docker. Ya puedes probar directo
(`http://localhost:8081`, `:8080`, `:3000/api`, `:8082`) o levantar el gateway (siguiente sección).

### 7) Levantar el microservicio **asignaciones** (puerto 8082)
En **otra terminal**:

**Windows:**
```powershell
cd asignaciones
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
cd asignaciones
export JAVA_HOME=/ruta/al/jdk-25
./mvnw spring-boot:run
```

### 8) Levantar el microservicio **tickets** (puerto 8083)
En **otra terminal**:

**Windows:**
```powershell
cd tickets
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
cd tickets
export JAVA_HOME=/ruta/al/jdk-25
./mvnw spring-boot:run
```

### 9) Levantar el microservicio **ms-audit** (puerto 3002)
En **otra terminal**. Consume desde RabbitMQ (exchange `audit_exchange`) los eventos que
publican los demás microservicios y los persiste en `audit_db`:
```bash
cd ms-audit
npm install
npm run start:dev
```
Consultar los eventos guardados: `GET http://localhost:3002/api/v1/audit`.

### 10) Levantar el **gateway Kong** (puerto 8000)
En **otra terminal**. A partir de aquí **todas** las peticiones entran por el puerto `8000`
(ver la sección *API Gateway con Kong* más abajo):
```bash
cd gateway
docker compose up -d
```

### 11) Cargar datos de prueba (usuarios QA + catálogo)
Con `usuarios`, `zonas`, `vehiculos`, `asignaciones` y **Kong** arriba, en **otra terminal**
desde la raíz del repo:
```bash
python3 scripts/seed_usuarios_roles.py   # cuentas QA (admin, recaudador, cliente, invitado)
python3 scripts/seed_datos.py            # catálogo: personas, zonas, espacios, vehículos, asignaciones
```
> En **Windows** usa `python` en lugar de `python3`. Los scripts pasan por Kong y usan la
> cuenta `root / Root2025` (que el servicio `usuarios` crea solo al arrancar).

**Cuentas de prueba que quedan disponibles:**

| Usuario | Contraseña | Rol |
|---|---|---|
| `root` | `Root2025` | ROOT (súper admin) |
| `qa.admin` | `QaAdmin2025` | ADMIN |
| `qa.recauda` | `QaRecauda2025` | RECAUDADOR |
| `qa.cliente` | `QaCliente2025` | CLIENTE |
| `qa.invitado` | `QaInvitado2025` | INVITADO |

(Los usuarios del catálogo de `seed_datos.py` usan la contraseña `Espe2025`.)

### 12) Levantar el **frontend** (puerto 5173)
En **otra terminal**:
```bash
cd frontend
cp .env.example .env       # Windows: copy .env.example .env
npm install
npm run dev                # abre http://localhost:5173
```
Inicia sesión con cualquiera de las cuentas de prueba de arriba.

---

## 🕵️ Sistema de auditoría (ms-audit + RabbitMQ)

Cada microservicio publica un evento por cada operación de escritura relevante (alta,
edición, activar/desactivar, login) hacia el exchange `audit_exchange` (topic, durable) de
RabbitMQ, con la routing key `audit.event`. `ms-audit` mantiene una cola (`audit_queue`)
ligada con el patrón `audit.#`, valida cada mensaje y lo guarda en `audit_db`.

Campos del evento (ver `ms-audit/src/audit/dto/create-audit.dto.ts`):

| Campo | Obligatorio | Formato |
|---|---|---|
| `servicio` | sí | `ms-<nombre>`, ej. `ms-vehiculos` |
| `accion` | sí | `CREATE` \| `UPDATE` \| `DELETE` \| `LOGIN` \| `LOGOUT` \| `SELECT` |
| `entidad` | sí | mayúsculas y guiones, ej. `VEHICULO` |
| `datos` | no | objeto libre con el registro afectado |
| `usuario` | no | usuario/UUID de quien hizo la acción |
| `rol` | no | rol del usuario |
| `ip` | sí | IPv4 de la petición |
| `mac` | sí | dirección MAC del dispositivo cliente, enviada por el frontend/kiosko en el header `X-Device-Mac` (si no llega, se usa `00:00:00:00:00:00`) |

> Nota: la `RABBITMQ_ROUTING_KEY=audit.#` de los `.env` va **entre comillas**. Sin comillas,
> `dotenv` interpreta `#` como inicio de comentario y trunca el valor a `audit.`, rompiendo
> el binding de la cola.

---

## 🌐 API Gateway con Kong

Kong corre en Docker en **modo declarativo (DB-less)**: todo el ruteo y las reglas están
en [`gateway/kong.yml`](gateway/kong.yml), **sin escribir código**. Las APIs siguen corriendo
en el host; Kong las alcanza con `host.docker.internal`.

### Levantar Kong
```bash
cd gateway
docker compose up -d
docker compose ps          # parqueadero-kong debe verse "healthy"
```

A partir de aquí, **todas** las peticiones entran por el puerto **8000** (los paths no cambian):

| Recurso | Directo | A través de Kong |
|---|---|---|
| usuarios | `http://localhost:8081/api/v1/usuarios` | `http://localhost:8000/api/v1/usuarios` |
| zonas | `http://localhost:8080/api/v1/zonas` | `http://localhost:8000/api/v1/zonas` |
| espacios | `http://localhost:8080/api/v1/espacios` | `http://localhost:8000/api/v1/espacios` |
| vehiculos | `http://localhost:3000/api/vehiculos` | `http://localhost:8000/api/vehiculos` |
| asignaciones | `http://localhost:8082/api/v1/asignaciones-vehiculos` | `http://localhost:8000/api/v1/asignaciones-vehiculos` |
| flota propietario | `http://localhost:8082/api/v1/propietarios/{id}/vehiculos` | `http://localhost:8000/api/v1/propietarios/{id}/vehiculos` |
| tickets | `http://localhost:8083/api/v1/tickets` | `http://localhost:8000/api/v1/tickets` |
| auditoría (ms-audit) | `http://localhost:3002/api/v1/audit` | `http://localhost:8000/api/v1/audit` |

Todas las rutas de `tickets` y `ms-audit` exigen JWT en Kong (plugin `jwt`), igual que
`asignaciones`, `zonas` y `vehiculos`; solo las rutas de gestión de `usuarios` (no las de
`/api/v1/auth`) lo exigen. `ms-audit` además valida el token en el propio microservicio
(no solo en Kong), porque contiene datos sensibles (ip, mac, usuario).

### Reglas (plugins) activas en Kong
- **rate-limiting** — máx. 100 peticiones/minuto por servicio (devuelve `429` si se supera).
- **cors** — permite consumir el gateway desde un frontend en otro origen.
- **correlation-id** — añade la cabecera `X-Correlation-ID` a cada petición para trazabilidad.

Inspeccionar Kong (API admin):
```bash
curl http://localhost:8001/services   # servicios registrados
curl http://localhost:8001/routes     # rutas registradas
```

---

## 🧪 Pruebas con Postman

Las colecciones están versionadas dentro de cada servicio:

| Archivo | Contenido |
|---|---|
| [`usuarios/postman/usuarios.postman_collection.json`](usuarios/postman/usuarios.postman_collection.json) | personas, roles, usuarios, asignaciones + **búsquedas** |
| [`zonas/postman/zonas.postman_collection.json`](zonas/postman/zonas.postman_collection.json) | CRUD de zonas |
| [`zonas/postman/espacios.postman_collection.json`](zonas/postman/espacios.postman_collection.json) | espacios + **disponibilidad** |
| [`vehiculos/vehiculos/postman/vehiculos.postman_collection.json`](vehiculos/vehiculos/postman/vehiculos.postman_collection.json) | CRUD de vehículos + **búsqueda por placa** |
| [`postman/Parqueadero-Local.postman_environment.json`](postman/Parqueadero-Local.postman_environment.json) | environment con las URLs |

### Cómo usarlas
1. Abre Postman → **Import** y selecciona las 4 colecciones + el environment.
2. Arriba a la derecha, selecciona el environment **“Parqueadero - Local”**.
3. Ejecuta cada colección con **Run collection** (o request por request, de arriba hacia abajo:
   los IDs creados se guardan solos en variables).

El environment trae estas variables:
```
urlUsuarios  = http://localhost:8081
urlZonas     = http://localhost:8080
urlVehiculos = http://localhost:3000/api
urlAsignaciones = http://localhost:8082
urlGateway   = http://localhost:8000
```

### Probar TODO a través de Kong
Solo cambia 3 valores del environment y vuelve a correr las colecciones (no hay que tocar nada más):
```
urlUsuarios  = http://localhost:8000
urlZonas     = http://localhost:8000
urlVehiculos = http://localhost:8000/api
urlAsignaciones = http://localhost:8000
```

### Endpoints de búsqueda y disponibilidad (nuevos)
```
# usuarios
GET /api/v1/personas/buscar?dni=1710034065      # por cédula (exacta)
GET /api/v1/personas/buscar?apellido=lop        # por apellido (parcial)
GET /api/v1/personas/buscar?nombre=ana          # por nombre (parcial)
GET /api/v1/usuarios/buscar?username=alo         # por username (parcial)

# vehiculos
GET /api/vehiculos/placa/ABC-1234               # por placa

# zonas
GET /api/v1/espacios/disponibles?idZona=&tipo=AUTO   # cupos libres (filtrable)
GET /api/v1/espacios/{id}/disponibilidad             # ¿este espacio está libre?
```

---

## 🗂️ Estructura del repositorio
```
.
├── iniciar.sh / iniciar.ps1  # arranque COMPLETO en 1 comando (Linux / Windows)
├── detener.sh / detener.ps1  # detener todo
├── GUIA-EQUIPO.md            # guía de arranque y pruebas para el equipo
├── docker-compose.yml        # PostgreSQL unificado + RabbitMQ (host 5433 / 5672)
├── init-db/                  # script que crea las bases la 1ª vez
├── scripts/                  # claves JWT (setup_jwt_keys.sh) + carga de datos (seed_*.py)
├── keys/                     # claves JWT RS256 (se generan; NO se versionan)
├── gateway/                  # Kong (kong.yml + docker-compose.yml)
├── postman/                  # environment compartido
├── frontend/                 # frontend React + Vite (5173)
├── usuarios/                 # microservicio Spring Boot (8081)
├── zonas/                    # microservicio Spring Boot (8080)
├── vehiculos/vehiculos/      # microservicio NestJS (3000)
├── asignaciones/             # microservicio Spring Boot (8082)
├── tickets/                  # microservicio Spring Boot (8083)
└── ms-audit/                 # microservicio de auditoria, NestJS (3002)
```

---

## 🛠️ Solución de problemas

| Problema | Causa / Solución |
|---|---|
| `JAVA_HOME` no encontrado / compila con otra versión | Define `JAVA_HOME` apuntando al **JDK 25**. Windows: variable de entorno; Linux/mac: `export JAVA_HOME=...`. |
| `No se pudo cargar la clave privada RSA` al arrancar un servicio | Faltan las claves JWT. Genéralas desde la raíz: `bash scripts/setup_jwt_keys.sh`. |
| El **frontend** no carga datos / errores de red | Falta `frontend/.env` (cópialo de `frontend/.env.example`) o Kong no está arriba. |
| Login falla / no existen las cuentas `qa.*` | Faltó correr `python3 scripts/seed_usuarios_roles.py` (con `usuarios` + Kong arriba). |
| Puerto en uso (8080/8081/8082/8083/3000/3002/5433/5672/8000/5173) | Cierra el proceso que lo ocupa o usa `detener.sh` / `detener.ps1`. El repo usa `5433` para no chocar con PostgreSQL local en `5432`. |
| Las bases no se crearon (falta `audit_db`, etc.) | Se crean solo con el **volumen vacío**. Resetea: `docker compose down -v && docker compose up -d`. Si no quieres perder datos existentes, crea `audit_db` a mano (ver paso 3). |
| Windows: no se ejecuta `iniciar.ps1` | Corre antes `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`. |
| Kong responde `502 Bad Gateway` | Las APIs deben estar **levantadas en el host** antes de probar por Kong. |
| `host.docker.internal` no resuelve (Linux) | Ya está resuelto con `extra_hosts: host-gateway` en `gateway/docker-compose.yml`. |
| `npm install` falla | Usa Node 20+. Borra `node_modules` y reintenta. |
| `ms-audit` no guarda nada aunque los demás servicios publiquen eventos | Revisa que `RABBITMQ_ROUTING_KEY="audit.#"` esté **entre comillas** en el `.env`; sin comillas `dotenv` la trunca a `audit.` y la cola no recibe mensajes. |

---

## 👥 Equipo
Proyecto académico — ESPE, Ingeniería de Software, Sistemas Distribuidos.
