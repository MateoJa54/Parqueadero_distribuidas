# E2E-01 — Login y sesión (Portal C1 / Admin A0)

**Módulo:** Frontend (CI-07)
**Requisito:** RF-FE-E2E-01
**Tipo:** Funcional manual guionizado
**Precondiciones:**
- Ambiente completo arriba (`docker compose up`, los 6 microservicios y el frontend en `http://localhost:5173`).
- Usuarios de prueba sembrados (`scripts/seed_usuarios_roles.py`): `qa.admin/QaAdmin2025`, `qa.cliente/QaCliente2025`.

| # | Paso | Resultado esperado | Resultado real |
|---|---|---|---|
| 1 | Ir a `http://localhost:5173/login` | Se muestra el formulario de login (usuario/contraseña) | ✅ PASS |
| 2 | Ingresar `qa.admin` / contraseña incorrecta y enviar | Mensaje de error visible, permanece en `/login`, no guarda token | ✅ PASS (funcional) — pero ver **DEF-01**: el texto del error dice "contrasena" sin ñ |
| 3 | Ingresar `qa.admin` / `QaAdmin2025` y enviar | Redirige a `/app` (dashboard admin), token guardado | ✅ PASS — Dashboard carga con KPIs reales (tickets activos, espacios, recaudado) |
| 4 | Recargar la página (F5) / navegar directo a `/app` | La sesión persiste (no vuelve a pedir login) — valida `AuthProvider.userFromToken` | ✅ PASS |
| 5 | Cerrar sesión desde el menú de usuario | Redirige a `/login`, token eliminado; si se navega manualmente a `/app` vuelve a `/login` | ✅ PASS |
| 6 | Ingresar `qa.cliente` / `QaCliente2025` (sesión limpia, sin `from` residual) | Redirige a `/portal` (no a `/app`, valida `rutaInicial`) | ✅ PASS — aterriza en `/portal` → "Mi perfil" |
| 6b (exploratorio) | Estando deslogueado, forzar `/app` (queda `from=/app` guardado), luego loguear como `qa.cliente` | Debería aterrizar en `/portal` (rutaInicial), ya que CLIENTE no tiene permiso sobre `/app` | ❌ **FAIL** — aterriza en `/403 Acceso denegado`. Ver **DEF-02** |

**Severidad si falla:** Alta (bloquea todo acceso al sistema).
**Estado:** Ejecutado el 2026-07-20 vía automatización de navegador (Chrome). 6/7 pasos en verde; 2 defectos encontrados y registrados en `Tests/defects.md` (DEF-01, DEF-02).
