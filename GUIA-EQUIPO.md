# 🧪 Guía para el equipo — compilar, arrancar y probar TODO

Esta guía es para que cualquier compañero (**Windows** o **Linux/macOS**) deje el sistema
completo funcionando **con datos de prueba** y pueda testear por Postman y por el frontend.

> El sistema son **6 microservicios** + **PostgreSQL** + **RabbitMQ** + **Kong (gateway)** + **frontend React**.
> Detalle técnico de cada uno: ver [`README.md`](README.md).

---

## 0) Requisitos (instalar una sola vez)

| Herramienta | Versión | Sirve para |
|---|---|---|
| **JDK 25** | 25 exacto | usuarios, zonas, asignaciones, tickets |
| **Node.js** | 20+ | vehiculos, ms-audit, frontend |
| **Docker Desktop** | reciente | PostgreSQL, RabbitMQ, Kong |
| **Python** | 3.8+ | scripts de carga de datos |
| **Git** | reciente | clonar (y en Windows aporta `bash`/`openssl`) |

> No necesitas Maven: cada servicio trae su wrapper (`mvnw` / `mvnw.cmd`).

### Definir `JAVA_HOME` al JDK 25 (obligatorio)
- **Windows:** Panel de control → *Editar variables de entorno* → nueva variable
  `JAVA_HOME = C:\Program Files\Java\jdk-25` y agrega `%JAVA_HOME%\bin` al `Path`.
  Verifica en una consola nueva: `java -version` → debe decir `25`.
- **Linux/macOS:** `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk` (ajusta la ruta).

---

## 1) Clonar

```bash
git clone https://github.com/MateoJa54/Parqueadero_distribuidas.git
cd Parqueadero_distribuidas
```

---

## 2) Arranque AUTOMÁTICO (recomendado) ⚡

Un solo comando: genera claves JWT, levanta Docker + Kong, compila y arranca los 6
servicios + frontend, y **carga los datos de prueba**.

### Linux / macOS
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk   # tu ruta al JDK 25
chmod +x iniciar.sh detener.sh
./iniciar.sh
```

### Windows (PowerShell, en la raíz del repo)
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\iniciar.ps1
```

Cuando termine verás las URLs y las cuentas de prueba. Para **detener todo**:

```bash
./detener.sh          # Linux/macOS   (agrega --wipe para borrar la BD)
```
```powershell
.\detener.ps1         # Windows       (agrega -Wipe para borrar la BD)
```

**Opciones útiles:** `--no-seed` / `-NoSeed` (no cargar datos) · `--no-front` / `-NoFront` (sin frontend).

> Logs (Linux/macOS): `./logs/<servicio>.log`. En Windows cada servicio abre su propia ventana.

---

## 3) Arranque MANUAL (paso a paso)

Úsalo si el automático falla o quieres entender el flujo. Cada servicio va en **su propia terminal**.

### 3.1 Claves JWT (una vez)
`usuarios` firma los tokens con la clave privada; los demás solo verifican con la pública.
```bash
# Linux/macOS y Windows (Git Bash):
bash scripts/setup_jwt_keys.sh
```
Genera `keys/jwt_private.pem`, `keys/jwt_public.pem` y escribe el `.env` de `ms-audit` y `vehiculos`.
> Estas claves **no están en Git** (son secretos): hay que generarlas al clonar.

### 3.2 Base de datos + RabbitMQ (Docker)
```bash
docker compose up -d
```
Crea automáticamente todas las bases la primera vez. Panel RabbitMQ: http://localhost:15672 (`guest`/`guest`).

### 3.3 Microservicios Spring (Java 25)
En **4 terminales** distintas. Linux/macOS usa `./mvnw`; Windows usa `.\mvnw.cmd`.

| Servicio | Carpeta | Puerto | Comando (Linux/macOS) | Comando (Windows) |
|---|---|---|---|---|
| usuarios | `usuarios/` | 8081 | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
| zonas | `zonas/` | 8080 | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
| asignaciones | `asignaciones/` | 8082 | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
| tickets | `tickets/` | 8083 | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

> En Linux/macOS exporta `JAVA_HOME` en cada terminal antes del comando.

### 3.4 Microservicios NestJS (Node)
En **2 terminales**:
```bash
cd vehiculos/vehiculos && npm install && npm run start:dev    # puerto 3000
cd ms-audit            && npm install && npm run start:dev    # puerto 3002
```

### 3.5 Gateway Kong
```bash
cd gateway && docker compose up -d
```
Desde ahora **todo entra por http://localhost:8000** (mismos paths).

### 3.6 Datos de prueba
Con usuarios + zonas + vehiculos + asignaciones + Kong arriba:
```bash
python3 scripts/seed_usuarios_roles.py   # cuentas QA (admin, recaudador, cliente, invitado)
python3 scripts/seed_datos.py            # catálogo: personas, zonas, espacios, vehículos, asignaciones
```
> En Windows usa `python` en vez de `python3`.

### 3.7 Frontend
```bash
cd frontend
copy .env.example .env     # Windows   (Linux/macOS: cp .env.example .env)
npm install
npm run dev                # http://localhost:5173
```

---

## 4) Cuentas de prueba

Creadas por `seed_usuarios_roles.py` (y `root` lo crea solo el servicio `usuarios` al arrancar):

| Usuario | Contraseña | Rol | Para probar |
|---|---|---|---|
| `root` | `Root2025` | ROOT | súper administrador (todo) |
| `qa.admin` | `QaAdmin2025` | ADMIN | gestión completa (usuarios, zonas, asignaciones…) |
| `qa.recauda` | `QaRecauda2025` | RECAUDADOR | tickets / cobros |
| `qa.cliente` | `QaCliente2025` | CLIENTE | vista de cliente |
| `qa.invitado` | `QaInvitado2025` | INVITADO | acceso mínimo |

Los usuarios del catálogo (`seed_datos.py`) usan la contraseña `Espe2025`.

---

## 5) Probar

### Por el frontend
Abre http://localhost:5173 e inicia sesión con cualquiera de las cuentas de arriba.
Como **admin** puedes ver/crear/filtrar **asignaciones de vehículos**, gestionar usuarios, zonas, etc.

### Por Postman
1. **Import** de las colecciones (dentro de cada servicio, carpeta `postman/`) + el environment
   [`postman/Parqueadero-Local.postman_environment.json`](postman/Parqueadero-Local.postman_environment.json).
2. Selecciona el environment **“Parqueadero - Local”** y ejecuta las colecciones.
3. Para probar **todo a través de Kong**, apunta las URLs del environment a `http://localhost:8000` (ver README).

### Chequeo rápido por consola (todo pasa por Kong :8000)
```bash
# login admin -> token
curl -s -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"qa.admin","password":"QaAdmin2025"}'

# listar asignaciones de vehículos (usa el token del paso anterior)
curl -s http://localhost:8000/api/v1/asignaciones-vehiculos \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 6) Puertos usados

| Puerto | Servicio |
|---|---|
| 5433 | PostgreSQL |
| 5672 / 15672 | RabbitMQ (AMQP / panel) |
| 8000 / 8001 | Kong (proxy / admin) |
| 8081 | usuarios · 8080 zonas · 8082 asignaciones · 8083 tickets |
| 3000 | vehiculos · 3002 ms-audit |
| 5173 | frontend (Vite) |

---

## 7) Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `java -version` no dice 25 | `JAVA_HOME` mal definido. Apúntalo al JDK 25 y abre una consola **nueva**. |
| Kong responde `502 Bad Gateway` | Los microservicios deben estar arriba **antes** de pegarle a `:8000`. Espera a que arranquen. |
| Puerto en uso (8081, 3000, 5433, …) | Cierra el proceso que lo ocupa o corre `./detener.ps1` / `./detener.sh`. |
| Login falla / no hay cuentas QA | Faltó correr `seed_usuarios_roles.py` (necesita `usuarios` + Kong arriba). |
| El frontend no carga datos | Falta `frontend/.env` (cópialo de `.env.example`) o Kong no está arriba. |
| “No se pudo cargar la clave privada RSA” | Faltan las claves: corre `bash scripts/setup_jwt_keys.sh` desde la raíz. |
| Windows: `.ps1` no se ejecuta | `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` antes de correrlo. |
| Bases sin crear (falta `audit_db`) | Se crean con el volumen vacío: `docker compose down -v && docker compose up -d`. |
| Empezar de cero (borrar datos) | `./detener.sh --wipe` (Linux) · `.\detener.ps1 -Wipe` (Windows), luego vuelve a `iniciar`. |
