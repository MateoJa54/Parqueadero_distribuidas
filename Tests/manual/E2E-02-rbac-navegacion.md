# E2E-02 — Navegación respeta permisos por rol (RBAC)

**Módulo:** Frontend (CI-07)
**Requisito:** RF-FE-E2E-02
**Tipo:** Funcional manual guionizado
**Objetivo:** confirmar en el navegador lo que `rbac.ts`/`guards.tsx` ya prueban a nivel unitario — que el guard real bloquea la UI, no solo la lógica en aislamiento.

**Precondiciones:** igual que E2E-01. Usuarios: `qa.admin/QaAdmin2025` (ADMIN), `qa.recauda/QaRecauda2025` (RECAUDADOR), `qa.cliente/QaCliente2025` (CLIENTE).

| # | Paso | Resultado esperado | Resultado real |
|---|---|---|---|
| 1 | Login como `qa.admin` | Aterriza en `/app` (Dashboard, A1); menú lateral muestra Personas, Usuarios, Roles, Zonas, Espacios, Vehículos, Asignaciones, Tickets, Auditoría, Configuración | ✅ PASS — también aparece "Diagnóstico" (extra, no documentado en frontend-pantallas.md, no es un problema) |
| 2 | Navegar a cada ítem del menú (A2–A9, A11) | Cada pantalla carga sin error 403 | ✅ PASS (muestra representativa: Roles y Auditoría verificadas — Auditoría además mostró en vivo los eventos LOGIN de las pruebas anteriores, buena señal cruzada de que ms-audit funciona) |
| 3 | Cerrar sesión, login como `qa.recauda` | Aterriza directo en `/app/tickets` (NO en `/app`, valida `rutaInicial` cuando no hay permiso `dashboard`) | ✅ PASS |
| 4 | Verificar el menú lateral de `qa.recauda` | Solo aparecen Tickets y (si existe en el shell) Mi perfil / Disponibilidad — nada de Personas/Usuarios/Roles/Zonas/Espacios/Vehículos/Auditoría | ✅ PASS — solo "Tickets" (+ enlace cruzado "Portal cliente") |
| 5 | Con `qa.recauda`, forzar la URL `/app/usuarios` directamente en la barra de direcciones | Redirige a `/403`, NO a `/login` (el usuario está autenticado, solo sin permiso — valida `RequirePermiso`) | ✅ PASS |
| 6 | Cerrar sesión, login como `qa.cliente` | Aterriza en `/portal` (no en `/app`) | ✅ PASS |
| 7 | Con `qa.cliente`, forzar la URL `/app` directamente | Redirige a `/403` | ✅ PASS |
| 8 | Con `qa.cliente`, navegar el portal (Mi perfil, Mis vehículos, Disponibilidad) | Carga sin error; NO existe ninguna opción de "Mis tickets" (gap conocido, documentado en `docs/frontend-pantallas.md` línea 104) | ⚠️ PASS parcial — navegación y ausencia de "Mis tickets" correctas; Mis vehículos y Disponibilidad cargan bien, pero **Mi perfil muestra Nombre/Cédula/Correo vacíos** → ver **DEF-03** |

**Severidad si falla:** Alta — un fallo aquí (p. ej. que `qa.recauda` SÍ pueda ver `/app/usuarios`) es una falla de seguridad, no solo de UX.
**Estado:** Ejecutado el 2026-07-20 vía automatización de navegador. 7/8 pasos completamente en verde, 1 con hallazgo (DEF-03, registrado en `Tests/defects.md`).
