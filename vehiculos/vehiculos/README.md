# API Vehiculos

Proyecto NestJS con TypeORM y PostgreSQL.

## Flujo de trabajo (simple)

- Base de datos en Docker.
- Aplicacion en local.
- Debug en VS Code.
- Pruebas manuales con Postman.

## Requisitos

- Node.js 20+
- npm
- Docker + Docker Compose

## Variables de entorno

Archivo `.env`:

```env
DB_HOST=localhost
DB_PORT=5433
DB_USUARIO=postgres
DB_CONTRASENA=postgres
DB_NOMBRE=vehiculos_db
```

## Comandos principales

```bash
# instalar dependencias
npm install

# levantar solo postgres
npm run db:up

# apagar postgres
npm run db:down

# reiniciar postgres limpio (borra datos)
npm run db:reset

# compilar
npm run build

# ejecutar en watch (sin debug)
npm run start:dev

# ejecutar con debug
npm run start:debug
```

## Endpoints

Base URL:

```text
http://localhost:3000/api
```

Rutas:

- `POST /vehiculos`
- `GET /vehiculos`
- `GET /vehiculos/:id`
- `PATCH /vehiculos/:id`
- `DELETE /vehiculos/:id`

Coleccion lista para importar:

- `postman/vehiculos.postman_collection.json`

## Debug paso a paso (VS Code + Postman)

1. Levanta la base:

```bash
npm run db:up
```

2. Abre VS Code en el proyecto.

3. Ve a Run and Debug (`Ctrl+Shift+D`).

4. Selecciona la configuracion:

- `Debug NestJS (vehiculos)`

5. Pon breakpoints en tu codigo, por ejemplo:

- `src/vehiculos/vehiculos.controller.ts`
- `src/vehiculos/vehiculos.service.ts`
- `src/vehiculos/factory/factory.vehiculo.ts`

6. Inicia debug con `F5`.

7. Cuando veas en consola:

```text
Aplicacion corriendo en: http://localhost:3000/api
```

8. Ejecuta requests desde Postman.

9. Cuando el breakpoint se detenga, usa:

- `F10` Step Over (avanza sin entrar a funciones internas)
- `F11` Step Into (entra a la funcion)
- `Shift+F11` Step Out (sale de la funcion)
- `F5` Continue (continua hasta el siguiente breakpoint)

10. Mira variables en:

- panel `VARIABLES`
- panel `WATCH`
- `Debug Console`

## Flujo recomendado diario

1. `npm run db:up`
2. `F5` con `Debug NestJS (vehiculos)`
3. Probar en Postman
4. `Shift+F5` para detener debug
5. `npm run db:down` al terminar

## Notas

- El debugger esta configurado para saltar `node_modules` y enfocarse en tu codigo.
- Si no conecta a la BD, revisa primero `.env` y que Docker este activo.
