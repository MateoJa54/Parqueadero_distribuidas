# API de Gestión de Usuarios y Roles - Parqueadero

Microservicio Spring Boot 3.5.2 que gestiona personas, usuarios y asignación de roles para el sistema de parqueadero distribuido.

## Características

- **Gestión de Personas**: CRUD de personas con validación de DNI, email y teléfono únicos
- **Gestión de Usuarios**: Creación y autenticación de cuentas de sistema (1:1 con Persona)
- **Gestión de Roles**: 5 roles disponibles (ADMINISTRADOR, SUPERVISOR, OPERADOR, CAJERO, CLIENTE)
- **Asignación de Roles**: Mapeo N:N entre usuarios y roles con estado activo
- **Manejo centralizado de errores**: GlobalExceptionHandler con respuestas JSON estructuradas
- **Validación automática**: Constraints Jakarta Validation en DTOs

## Stack Tecnológico

- **Java**: 25 (OpenJDK)
- **Framework**: Spring Boot 3.5.2
- **ORM**: Spring Data JPA + Hibernate
- **Base de datos**: PostgreSQL 16
- **Builder**: Maven 3.x
- **Utilidades**: Lombok 1.18.38

## Requisitos Previos

```bash
# Java 25 (requerido para @GeneratedValue(strategy = GenerationType.UUID))
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk

# Docker y Docker Compose (para PostgreSQL)
docker --version
docker-compose --version
```

## Instalación y Ejecución

### 1. Iniciar la Base de Datos

```bash
cd /path/to/usuarios
docker compose up -d
# Esperar 5 segundos para que PostgreSQL esté listo
sleep 5
```

Verifica la conexión:
```bash
docker compose ps
# Debería mostrar el contenedor 'usuarios-postgres-1' en estado 'Up'
```

### 2. Compilar el Proyecto

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
./mvnw clean compile
# Debería terminar con "[INFO] BUILD SUCCESS"
```

### 3. Ejecutar el Servidor

```bash
./mvnw spring-boot:run
# El servidor estará disponible en http://localhost:8081
```

## Endpoints de la API

### Personas

```bash
# Listar todas las personas
GET /api/v1/personas

# Obtener una persona por ID
GET /api/v1/personas/{idPersona}

# Crear una nueva persona
POST /api/v1/personas
Content-Type: application/json
{
  "firstName": "Juan",
  "middleName": "Carlos",
  "lastName": "Pérez",
  "dni": "1234567890",
  "email": "juan@example.com",
  "phone": "+593987654321",
  "address": "Calle Principal 123",
  "nationality": "Ecuador"
}
```

### Usuarios

```bash
# Listar todos los usuarios
GET /api/v1/usuarios

# Obtener un usuario por ID
GET /api/v1/usuarios/{idUsuario}

# Crear un nuevo usuario
POST /api/v1/usuarios
Content-Type: application/json
{
  "idPersona": "550e8400-e29b-41d4-a716-446655440000",
  "username": "jperez",
  "password": "SecurePass123"
}
```

### Roles

```bash
# Listar todos los roles disponibles
GET /api/v1/roles

# Crear un nuevo rol
POST /api/v1/roles
Content-Type: application/json
{
  "name": "OPERADOR",
  "description": "Operador del sistema de parqueadero"
}
```

### Asignaciones

```bash
# Listar todas las asignaciones de roles
GET /api/v1/asignaciones

# Asignar un rol a un usuario
POST /api/v1/asignaciones
Content-Type: application/json
{
  "idUser": "550e8400-e29b-41d4-a716-446655440000",
  "idRole": "550e8400-e29b-41d4-a716-446655440001"
}

# Listar roles de un usuario
GET /api/v1/asignaciones/usuario/{idUsuario}
```

## Validaciones

### PersonaRequestDto
- `firstName`: Máximo 30 caracteres, obligatorio
- `middleName`: Máximo 30 caracteres, opcional
- `lastName`: Máximo 30 caracteres, obligatorio
- `dni`: Máximo 30 caracteres, único, obligatorio
- `email`: Formato email válido, único, obligatorio
- `phone`: Patrón `^[0-9+\-\s]{7,15}$`, único, obligatorio
- `address`: Obligatorio
- `nationality`: Máximo 30 caracteres, obligatorio

### UsuarioRequestDto
- `idPersona`: UUID válido, obligatorio (debe existir en BD)
- `username`: 3-15 caracteres, único, obligatorio
- `password`: 6-30 caracteres, obligatorio

### RolRequestDto
- `name`: texto libre 3-50 caracteres (letras, números, espacios y `_`), único, obligatorio (se normaliza a MAYÚSCULAS)
- `description`: Máximo 255 caracteres, opcional

## Estructura del Proyecto

```
src/main/java/ec/edu/espe/usuarios/
├── UsuariosApplication.java          # Punto de entrada
├── controlllers/
│   ├── PersonaController.java
│   ├── UsuarioController.java
│   ├── RolController.java
│   └── AsignacionController.java
├── entidades/
│   ├── Persona.java                  # Raíz de identidad
│   ├── Usuario.java                  # Cuenta de sistema (1:1 con Persona)
│   ├── Rol.java                      # Rol disponible (name de texto libre)
│   ├── UsuarioRol.java               # Asignación N:N
│   └── UsuarioRolId.java             # Clave compuesta
├── dtos/
│   ├── PersonaRequestDto.java
│   ├── PersonaResponseDto.java
│   ├── UsuarioRequestDto.java
│   ├── UsuarioResponseDto.java
│   ├── RolRequestDto.java
│   ├── RolResponseDto.java
│   ├── AsignarRolRequestDto.java
│   └── AsignacionResponseDto.java
├── repositorios/
│   ├── PersonaRepositorio.java
│   ├── UsuarioRepositorio.java
│   ├── RolRepositorio.java
│   └── UsuarioRolRepositorio.java
├── services/
│   ├── PersonaServicio.java
│   ├── UsuarioServicio.java
│   ├── RolServicio.java
│   ├── AsignacionServicio.java
│   └── Impl/
│       ├── PersonaServicioImpl.java
│       ├── UsuarioServicioImpl.java
│       ├── RolServicioImpl.java
│       └── AsignacionServicioImpl.java
└── utils/
    ├── PasswordUtil.java             # Hasheo SHA-256 Base64
    ├── UtilMappers.java              # Conversión Entity ↔ DTO
    └── GlobalExceptionHandler.java   # Manejo centralizado de errores
```

## Notas de Diseño

### Identidad: Relación 1:1 Usuario ↔ Persona

Una Persona es la entidad raíz que representa la identidad: nombre, DNI, email, teléfono, dirección.

Un Usuario es la cuenta de acceso al sistema con credenciales, vinculado 1:1 a una Persona:

```
┌──────────────┐         ┌────────────┐
│   persons    │────1:1──│   users    │
├──────────────┤         ├────────────┤
│ id (PK/UUID) │◄────┐   │ id_person  │ (FK+PK)
│ first_name   │     └───┤ (PK)       │
│ email (uq)   │         │ username   │
│ phone (uq)   │         │ pass_hash  │
└──────────────┘         └────────────┘
```

### Hashing de Contraseñas

Las contraseñas se hashean con **SHA-256 + Base64** antes de persistirse. El hash resultante (~44 caracteres) se almacena en una columna de 100 caracteres.

**⚠️ En producción**, se recomienda usar **bcrypt** o **Argon2** para mayor seguridad criptográfica.

### Asignaciones de Roles

La relación N:N Usuario↔Rol se modela con la tabla intermedia `user_role` que incluye metadatos:

```
┌────────────┐         ┌──────────────┐         ┌────────┐
│   users    │────╱╱───│  user_role   │───╱╱────│ roles  │
└────────────┘         ├──────────────┤         └────────┘
                       │ id_user (FK) │
                       │ id_role (FK) │
                       │ active       │
                       │ assigned_at  │
                       └──────────────┘
```

## Manejo de Errores

Respuesta de error JSON:

```json
{
  "timestamp": "2026-06-15T10:30:00.000Z",
  "status": 400,
  "error": "Validation Error",
  "mensaje": "El email ya está registrado"
}
```

Códigos HTTP utilizados:
- `400`: Validación fallida o conflicto (DNI/email/username existente)
- `404`: Recurso no encontrado (implícito en queries sin resultado)
- `500`: Error del servidor

## Testing

```bash
# Ejecutar tests unitarios
./mvnw test

# Con coverage
./mvnw test jacoco:report
```

## Integración con Otros Microservicios

Este servicio expone endpoints REST que pueden ser consumidos por:
- **vehiculos**: Para obtener información de usuarios asignados a vehículos
- **zonas**: Para autorización basada en roles

Ejemplo de integración:
```bash
curl http://localhost:8081/api/v1/usuarios/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json"
```

## Variables de Configuración

En `application.yaml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update              # Crear/actualizar esquema automáticamente
    show-sql: true                  # Mostrar SQL en logs
    properties:
      hibernate:
        dialect: PostgreSQLDialect
server:
  port: 8081
```

## Troubleshooting

### Error: `Connection refused` en BD

```bash
# Verificar si PostgreSQL está corriendo
docker ps

# Reiniciar servicios
docker compose down && docker compose up -d
```

### Error: `java.lang.UnsatisfiedLinkError` de Lombok

```bash
# Verifica que JAVA_HOME apunta a Java 25
echo $JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk

# Relimpiar
./mvnw clean compile
```

### Error: `Duplicate entry` para DNI/email/username

Las validaciones de unicidad se aplican **en la BD**. Verifica el estado anterior antes de reintentar.

## Autor

Proyecto educativo - Sistema Distribuido de Parqueadero

## Licencia

Sin especificar
