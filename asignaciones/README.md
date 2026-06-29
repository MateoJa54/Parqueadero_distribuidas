# Microservicio asignaciones

Servicio Spring Boot para asociar vehiculos a propietarios y mantener trazabilidad historica.

## Responsabilidad

- Crear asignaciones propietario-vehiculo con clave compuesta `user_id + vehicle_id`.
- Impedir que un vehiculo tenga mas de un propietario activo.
- Registrar auditoria automatica en `assignment_audit_events`.
- Consultar la flota activa de un propietario agregando datos desde `vehiculos`.
- Exigir que el usuario tenga activo el rol enviado en `roleId` para autorizar la asignacion.

## Ejecutar

```powershell
cd asignaciones
.\mvnw.cmd spring-boot:run
```

Por defecto usa:

- Puerto HTTP: `8082`
- PostgreSQL: `localhost:5433`
- Base: `asignaciones`
- Usuario: `asignaciones`
- Password: `asignaciones123`

## Endpoints

```http
POST /api/v1/asignaciones-vehiculos
PATCH /api/v1/asignaciones-vehiculos/{userId}/{vehicleId}
PATCH /api/v1/asignaciones-vehiculos/{userId}/{vehicleId}/desactivar
PATCH /api/v1/asignaciones-vehiculos/{userId}/{vehicleId}/activar
GET /api/v1/asignaciones-vehiculos/{userId}/{vehicleId}/trazabilidad
GET /api/v1/propietarios/{userId}/vehiculos
```

Ejemplo de creacion:

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "vehicleId": "00000000-0000-0000-0000-000000000000",
  "roleId": "00000000-0000-0000-0000-000000000000",
  "assignmentType": "PROPIETARIO",
  "vehicleAlias": "Vehiculo principal ESPE",
  "observation": "Asignacion regular para propietario registrado"
}
```
