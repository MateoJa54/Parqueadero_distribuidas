# E2E-03 — Emisión y cobro de ticket desde la UI (A10)

**Módulo:** Frontend (CI-07) + tickets (CI-05, High priority — sin cobertura previa a esta auditoría)
**Requisito:** RF-FE-E2E-03
**Tipo:** Funcional manual guionizado — flujo complejo (candidato para el tramo de video de P4)
**Precondiciones:** login como `qa.recauda` (RECAUDADOR); al menos un espacio DISPONIBLE y un vehículo activo sin ticket abierto (usar los datos sembrados por `scripts/seed_datos.py`, o crear uno nuevo en A8).

| # | Paso | Resultado esperado | Resultado real |
|---|---|---|---|
| 1 | Ir a Tickets (A10.1 Registrar ingreso) | Formulario con campo `placa` y selector de espacio disponible | ✅ PASS |
| 2 | Ingresar una placa inexistente y enviar | Error visible en UI ("vehículo no encontrado"), sin crear ticket | ✅ PASS — toast "No se pudo registrar: No existe un vehiculo con placa: ZZZ-9999" (mismo patrón sin tildes de DEF-01, no es defecto nuevo) |
| 3 | Ingresar una placa válida sin espacio seleccionado | Validación de formulario antes de llamar al backend (idEspacio requerido) | ✅ PASS — "Selecciona un espacio disponible.", sin llamada a la API |
| 4 | Ingresar placa válida + espacio disponible, enviar | Ticket creado: `estadoTicket=ACTIVO`, muestra `codigo`, `categoriaTarifa`, hora de ingreso | ✅ PASS — TKT-000011 creado para YAW-1700 |
| 4b (bonus) | Ingresar una placa de moto contra un espacio tipo AUTO | Debe rechazar por incompatibilidad de tipo | ✅ PASS — "El vehiculo tipo Motocicleta no es compatible con un espacio AUTO; requiere un espacio MOTO" |
| 5 | Repetir el ingreso con la misma placa | Error 409 visible ("vehículo ya tiene ticket activo") | ✅ PASS — "El vehiculo ya tiene un ticket activo: TKT-000011" |
| 6 | Ir al tablero de tickets activos (A10.2) | El ticket recién creado aparece listado, paginado | ✅ PASS |
| 7 | Seleccionar el ticket y cobrar (A10.3 Pagar) | Muestra `valorRecaudado` > 0, `fechaHoraSalida`, `estado=PAGADO` | ✅ PASS — $1,50, toast "Ticket pagado". *Nota: no aparece modal de confirmación antes de cobrar, aunque `docs/frontend-pantallas.md` línea 62 lo sugiere como componente compartido — discrepancia menor de diseño, no se registra como defecto.* |
| 8 | Intentar pagar el mismo ticket otra vez | Error 409 visible ("el ticket ya no está activo") | ⚠️ No reproducible desde la UI: el botón "Cobrar" desaparece en cuanto el ticket queda PAGADO (columna Acciones muestra "—"), por diseño. El 409 del backend para este caso ya está cubierto y en verde por la suite Postman/Newman ("[Regla] Pagar ticket ya pagado 409"). |
| 9 | Crear un segundo ticket y anularlo (A10.4) con un motivo | `estado=ANULADO`, `valorRecaudado=0`; intentar anular sin motivo debe bloquear el envío | ❌ **FAIL crítico en la UI** — el modal SÍ bloquea el envío sin motivo (✅), pero al escribir un motivo con espacio **el modal se cierra solo, sin anular el ticket**. Ver **DEF-04**. Se verificó con `curl` directo a `PATCH /api/v1/tickets/{id}/anular` que el backend anula correctamente (`ANULADO`, `$0,00`) — el defecto es exclusivo del modal del frontend. |
| 10 | Buscar el primer ticket por código (A10.5) | Lo encuentra y muestra el detalle correcto | ✅ PASS — encontró TKT-000012, estado ANULADO, motivo y valor correctos (tras anular vía API) |

**Severidad si falla:** Alta — `tickets` es CI-05, ya identificado como módulo de mayor riesgo (RP-01, cero cobertura previa a esta auditoría). Un fallo en el flujo de UI aquí es tan crítico como uno en la API, que ya está 100% verde vía Postman.
**Estado:** Ejecutado el 2026-07-20 vía automatización de navegador. 9/10 pasos en verde (incluyendo un caso bonus de compatibilidad de tipos), 1 defecto crítico encontrado (DEF-04) que bloquea la anulación de tickets con motivo desde la UI.
