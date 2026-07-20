# Tests — Persona 4 (Pruebas Dinámicas, Defectos e Informe de Cierre)

Carpeta separada del código fuente del SUT, según lo exigido para esta auditoría. Contiene los artefactos de ejecución de pruebas dinámicas: la matriz de trazabilidad, el registro de defectos, el informe de cierre y los guiones de prueba manual.

> Las pruebas **unitarias** de cada módulo viven junto a su código fuente, siguiendo la convención propia de cada stack (igual que ya hacía el SUT antes de esta auditoría): `*/src/test/java/...` en los microservicios Spring Boot, `*.spec.ts` en los NestJS (`vehiculos`, `ms-audit`), y ahora `*.test.ts(x)` en `frontend/src/`. Esta carpeta `Tests/` reúne lo que **no tiene un hogar natural dentro de un módulo**: la suite funcional/API, los guiones E2E manuales del frontend y los reportes de cierre.

## Contenido

| Archivo | Qué es |
|---|---|
| `traceability-matrix.csv` | Requisito ↔ caso de prueba, con estado (ejecutado/diseñado/no cubierto) — fuente de datos plana, fácil de diffear en git |
| `traceability-matrix.xlsx` | Misma matriz con formato: tabla con filtro, colores por estado (verde=pass, amarillo=pendiente, gris=no cubierto) y hoja "Resumen" con totales — para incluir en el informe |
| `defects.md` | Registro de defectos (id, módulo, severidad, prioridad, pasos, estado) |
| `manual/E2E-01-login.md` | Guion manual: login y sesión |
| `manual/E2E-02-rbac-navegacion.md` | Guion manual: navegación respeta permisos por rol |
| `manual/E2E-03-emision-ticket.md` | Guion manual: flujo completo de emisión/cobro de ticket (candidato a video) |
| `evidence/newman-result.json` | Resultado crudo de la corrida de Postman/Newman |
| `evidence/vitest-frontend-result.txt` | Resultado de la suite unitaria del frontend |
| `evidence/DEF-01-*.png` … `DEF-04-*.png` | Capturas reales de cada defecto, referenciadas desde `defects.md` |
| `test-summary.md` | Informe de cierre (Bloque C): % pasa/falla, densidad de defectos |

## Cómo reproducir cada suite

### 1. Levantar el ambiente
```bash
docker compose up -d                 # Postgres + RabbitMQ (raíz del repo)
cd gateway && docker compose up -d   # Kong
# arrancar los 6 microservicios (cada uno en su carpeta):
#   usuarios/zonas/asignaciones/tickets -> ./mvnw.cmd -DskipTests spring-boot:run
#   vehiculos/vehiculos, ms-audit       -> npm run start:dev
python scripts/seed_usuarios_roles.py
python scripts/seed_datos.py
```
Referencia completa (incluye frontend y generación de llaves JWT): `iniciar.ps1` en la raíz del repo.

### 2. Suite funcional / API (Postman + Newman)
```bash
cd postman
npx newman run "QA_Parqueaderos.postman_collection.json" -e "Parqueadero-Kong.postman_environment.json" --reporters cli,json --reporter-json-export ../Tests/evidence/newman-result.json
```

### 3. Suite unitaria del frontend (Vitest + React Testing Library)
```bash
cd frontend
npm run test          # una sola corrida (CI)
npm run test:watch    # modo watch (desarrollo)
```
Cubre lógica crítica de RBAC (`src/auth/rbac.ts`, `src/auth/guards.tsx`) y utilidades de formato/validación (`src/lib/format.ts`).

### 4. Guiones manuales E2E del frontend
Con el frontend corriendo (`npm run dev` en `frontend/`, puerto 5173), seguir paso a paso los archivos en `manual/`. Documentar el resultado real (pasa/falla, capturas) directamente en cada archivo y, si algo falla, dar de alta el defecto en `defects.md`.

## Estado actual (última corrida: 2026-07-20)

- **API (Newman):** 40/40 requests, 117/117 assertions — 0 fallos.
- **Unitarias frontend (Vitest):** 36/36 — 0 fallos.
- **E2E manual frontend:** 3 guiones diseñados, pendientes de ejecución.
- **Huecos de cobertura conocidos:** ver filas `NO_CUBIERTO` en `traceability-matrix.csv` (CRUD de Roles vía QA collection, pantallas admin A2–A9/A11 y portal C2–C6 del frontend).
