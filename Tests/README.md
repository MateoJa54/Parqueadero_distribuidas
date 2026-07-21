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

### 5. Cobertura de código (JaCoCo + Vitest)

JaCoCo está declarado directamente en el `<build><plugins>` de los 4 `pom.xml` Spring Boot (`usuarios`, `zonas`, `asignaciones`, `tickets`), con el goal `report` amarrado a la fase **`verify`** (no `test`) — hay que correr `mvn verify`, no `mvn test`, o el reporte no se genera.

```bash
# Backend — un servicio a la vez, desde su carpeta:
cd usuarios && ./mvnw.cmd -q verify -DskipITs        # genera target/site/jacoco/index.html
cd zonas && ./mvnw.cmd -q verify -DskipITs -Dtest='!ZonasApplicationTests' -DfailIfNoTests=false
cd asignaciones && ./mvnw.cmd -q verify -DskipITs
cd tickets && ./mvnw.cmd -q verify -DskipITs

# Frontend (requiere @vitest/coverage-v8, ya en devDependencies; correr npm install tras cada pull):
cd frontend && npm install && npm run test:cov          # vitest run --coverage
```

`ZonasApplicationTests.contextLoads` se excluye porque es el smoke test genérico de Spring Initializr y requiere una conexión Postgres real en `localhost:5433`; no es una prueba de reglas de negocio y ningún otro servicio tiene un test equivalente. Si el ambiente Docker (paso 1) está arriba, puede correrse sin la exclusión.

## Estado actual (última corrida: 2026-07-21)

- **API (Newman):** 40/40 requests, 117/117 assertions — 0 fallos.
- **Unitarias frontend (Vitest, suite QA de P4):** 36/36 — 0 fallos.
- **E2E manual frontend:** 3 guiones ejecutados (26 pasos) — 23 PASS / 3 FAIL, con 4 defectos registrados (DEF-01 a DEF-04, ver `defects.md` y `test-summary.md`). **DEF-04 sigue abierto** — verificado de nuevo el 2026-07-21, el bug de foco en `Modal.tsx`/`TicketsPage.tsx` sigue presente en el código actual.
- **Cobertura de línea/rama (JaCoCo 0.8.15 / Vitest v8), corrida 2026-07-21 tras el port de tests de P3 (`port/sonar-fixes-from-fork`, ~211 tests nuevos por servicio backend + 353 en frontend):**
  | Servicio | Líneas (total) | Ramas (total) |
  |---|---|---|
  | usuarios | 98.3% (742/755) | 91.5% (205/224) |
  | zonas | 91.9% (531/578) | 87.9% (123/140) |
  | asignaciones | 97.7% (473/484) | 90.2% (120/133) |
  | tickets | 97.6% (453/464) | 88.7% (110/124) |
  | frontend (toda la app, 44 archivos de test) | 97.64% statements | 88.07% branches |

  Los 4 servicios y el frontend superan el umbral X-05 del SQAP (≥70% líneas / ≥60% ramas). Antes de este port (medido el 2026-07-21 en la mañana), la cobertura real de la capa de negocio (`services`/`services.Impl`) era 0%/1.9%/17.7%/0% — ver Apéndice C.8 del SQAP para el detalle de ese hallazgo y su cierre.
- **Huecos de cobertura funcional conocidos** (distintos de la cobertura unitaria de arriba): ver filas `NO_CUBIERTO` en `traceability-matrix.csv` (CRUD de Roles vía QA collection, pantallas admin A2–A9/A11 y portal C2–C6 sin guion E2E de P4 — aunque ahora sí tienen tests unitarios propios del port de P3).
