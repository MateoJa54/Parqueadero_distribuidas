#!/usr/bin/env python3
"""Parche idempotente sobre la coleccion Postman generada.

Corrige problemas de orden y dependencias de datos detectados al correr newman:
  A. {{placa}} nunca se seteaba -> el flujo de ingreso de Tickets fallaba.
  B. "Eliminar vehiculo" usaba DELETE (endpoint inexistente en vehiculos) y ademas
     apuntaba al auto principal; ahora desactiva una camioneta desechable.
  C. El folder Auth (JWT) corria antes de crear la persona y pisaba el token admin:
     ahora crea su propia persona dedicada y guarda el token del cliente en variables
     separadas (tokenCliente/refreshTokenCliente/idUsuarioCliente) sin tocar {{token}}.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PATH = os.path.join(ROOT, "postman", "Gestion_Parqueaderos.postman_collection.json")

with open(PATH, encoding="utf-8") as f:
    c = json.load(f)


def folder(name):
    for f in c["item"]:
        if f["name"] == name:
            return f
    raise KeyError(name)


def item(fold, prefix):
    for x in fold["item"]:
        if x["name"].startswith(prefix):
            return x
    raise KeyError(prefix)


def set_test(x, lines):
    x["event"] = [e for e in x.get("event", []) if e.get("listen") != "test"]
    x.setdefault("event", []).append(
        {"listen": "test", "script": {"type": "text/javascript", "exec": lines}})


def set_body(x, obj):
    x["request"]["body"] = {
        "mode": "raw",
        "raw": json.dumps(obj, indent=2, ensure_ascii=False),
        "options": {"raw": {"language": "json"}},
    }


def ensure_var(key):
    if not any(v["key"] == key for v in c["variable"]):
        c["variable"].append({"key": key, "value": ""})


# ---- Variables nuevas ----
for k in ("idPersonaRegistro", "refreshTokenCliente", "idUsuarioCliente", "idVehiculoTmp"):
    ensure_var(k)

# =========================================================================
# A + B. VEHICULOS
# =========================================================================
veh = folder("Vehiculos")

# A. Crear auto guarda {{placa}}
crear_auto = item(veh, "Crear auto")
set_test(crear_auto, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.id) {",
    "  pm.collectionVariables.set('idVehiculo', json.id);",
    "  pm.collectionVariables.set('placa', json.placa || 'PBX-1234');",
    "  console.log('idVehiculo =', json.id, 'placa =', json.placa);",
    "}",
    "pm.test('Auto creado (201)', function () {",
    "  pm.response.to.have.status(201);",
    "  pm.expect(json.id, 'id').to.be.a('string').and.not.empty;",
    "});",
])

# B. Crear camioneta guarda un id desechable para la prueba de borrado logico
crear_cam = item(veh, "Crear camioneta")
# La clasificacion del enum es 'Diésel' (con tilde); el payload traia 'Diesel' -> 400
set_body(crear_cam, {
    "tipo": "Camioneta",
    "datos": {
        "placa": "PCX-5678", "marca": "Chevrolet", "modelo": "Dmax", "color": "Blanco",
        "anio": 2020, "clasificacion": "Diésel", "cabina": 4, "capacidadCarga": "3.5t",
    },
})
set_test(crear_cam, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.id) {",
    "  pm.collectionVariables.set('idVehiculoTmp', json.id);",
    "  console.log('idVehiculoTmp =', json.id);",
    "}",
    "pm.test('Camioneta creada (201)', function () {",
    "  pm.response.to.have.status(201);",
    "});",
])

# B. "Eliminar vehiculo" -> desactivar (soft-delete) sobre el vehiculo desechable
elim = item(veh, "Eliminar vehiculo")
elim["name"] = "Desactivar vehiculo (soft-delete)"
elim["request"]["method"] = "PATCH"
elim["request"]["url"] = {
    "raw": "{{urlVehiculos}}/vehiculos/{{idVehiculoTmp}}/desactivar",
    "host": ["{{urlVehiculos}}"],
    "path": ["vehiculos", "{{idVehiculoTmp}}", "desactivar"],
}
elim["request"]["description"] = (
    "Borrado logico (soft-delete): pone activo=false en un vehiculo desechable. "
    "El auto principal {{idVehiculo}} se conserva para los flujos de asignaciones y tickets."
)
set_test(elim, [
    "pm.test('Vehiculo desactivado (200)', function () {",
    "  pm.response.to.have.status(200);",
    "});",
])

# =========================================================================
# TICKETS: Prep 1 requiere roleId (asignaciones lo exige, si no -> 400)
# =========================================================================
tickets = folder("Tickets")
prep1 = item(tickets, "Prep 1")
set_body(prep1, {
    "userId": "{{idUsuario}}",
    "vehicleId": "{{idVehiculo}}",
    "roleId": "{{idRol}}",
    "assignmentType": "PROPIETARIO",
    "vehicleAlias": "Vehiculo prueba tickets",
})

# =========================================================================
# C. AUTH (JWT): persona dedicada + no pisar el token admin
# =========================================================================
auth = folder("Auth (JWT)")

# 1) Nuevo request: crear persona dedicada para el registro publico (usa token admin)
crear_persona_reg = {
    "name": "Crear persona (para registro publico)",
    "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": {
            "raw": "{{urlUsuarios}}/api/v1/personas",
            "host": ["{{urlUsuarios}}"],
            "path": ["api", "v1", "personas"],
        },
        "description": (
            "Crea una persona SIN usuario asociado (cedula 1712345675) usando el token "
            "de admin. Guarda su id en {{idPersonaRegistro}} para el registro publico."),
    },
    "response": [],
}
set_body(crear_persona_reg, {
    "firstName": "Cliente", "middleName": "Publico", "lastName": "Registro",
    "dni": "1712345675", "email": "cliente.registro@espe.edu.ec",
    "phone": "0987654321", "address": "Sangolqui", "nationality": "Ecuatoriana",
})
set_test(crear_persona_reg, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.id) {",
    "  pm.collectionVariables.set('idPersonaRegistro', json.id);",
    "  console.log('idPersonaRegistro =', json.id);",
    "}",
    "pm.test('Persona para registro creada (201)', function () {",
    "  pm.response.to.have.status(201);",
    "  pm.expect(json.id, 'id').to.be.a('string').and.not.empty;",
    "});",
])

# Insertar justo despues de "Login root (admin)"
idx_login = next(i for i, x in enumerate(auth["item"]) if x["name"].startswith("Login root"))
# evitar duplicar si ya se aplico el parche
if not any(x["name"].startswith("Crear persona (para registro") for x in auth["item"]):
    auth["item"].insert(idx_login + 1, crear_persona_reg)

# 2) Registro cliente: persona propia + variables separadas (no pisa {{token}})
reg = item(auth, "Registro cliente")
reg["name"] = "Registro cliente (rol CLIENTE)"
reg["request"]["auth"] = {"type": "noauth"}
reg["request"]["description"] = (
    "Registro publico: crea un usuario (rol CLIENTE) sobre la persona dedicada "
    "{{idPersonaRegistro}}. Guarda el token del cliente en {{tokenCliente}} y su id en "
    "{{idUsuarioCliente}} SIN pisar el token de admin ({{token}}).")
set_body(reg, {
    "idPersona": "{{idPersonaRegistro}}",
    "username": "cliente_espe",
    "password": os.environ.get("USER_PASSWORD", "Espe2025"),
})
set_test(reg, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.token) {",
    "  pm.collectionVariables.set('tokenCliente', json.token);",
    "  pm.collectionVariables.set('refreshTokenCliente', json.refreshToken);",
    "  pm.collectionVariables.set('idUsuarioCliente', json.idUsuario);",
    "  console.log('token CLIENTE guardado. roles =', json.roles);",
    "}",
    "pm.test('Registro exitoso (201) con token', function () {",
    "  pm.response.to.have.status(201);",
    "  pm.expect(json.token, 'token').to.be.a('string').and.not.empty;",
    "});",
])

# 3) Login cliente: username propio + variables separadas
logc = item(auth, "Login cliente")
logc["request"]["auth"] = {"type": "noauth"}
logc["request"]["description"] = (
    "Inicia sesion con el cliente registrado (cliente_espe). Guarda {{tokenCliente}} "
    "para probar restricciones por rol, sin tocar el token de admin.")
set_body(logc, {"username": "cliente_espe", "password": os.environ.get("USER_PASSWORD", "Espe2025")})
set_test(logc, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.token) {",
    "  pm.collectionVariables.set('tokenCliente', json.token);",
    "  pm.collectionVariables.set('refreshTokenCliente', json.refreshToken);",
    "  pm.collectionVariables.set('idUsuarioCliente', json.idUsuario);",
    "  console.log('token CLIENTE guardado. roles =', json.roles);",
    "}",
    "pm.test('Login cliente (200)', function () {",
    "  pm.response.to.have.status(200);",
    "});",
])

# 4) /me con el token del cliente
me = item(auth, "Mis datos")
me["request"]["auth"] = {"type": "bearer", "bearer": [
    {"key": "token", "value": "{{tokenCliente}}", "type": "string"}]}
me["request"]["description"] = "Devuelve los datos del cliente autenticado (usa {{tokenCliente}})."
set_test(me, [
    "pm.test('Perfil del cliente (200)', function () {",
    "  pm.response.to.have.status(200);",
    "});",
])

# 5) Refresh con el refresh token del cliente (no pisa {{token}})
ref = item(auth, "Refrescar token")
ref["request"]["auth"] = {"type": "noauth"}
ref["request"]["description"] = (
    "Rota el token del cliente usando {{refreshTokenCliente}}. Actualiza las variables "
    "del cliente sin afectar el token de admin.")
set_body(ref, {"refreshToken": "{{refreshTokenCliente}}"})
set_test(ref, [
    "var json = {};",
    "try { json = pm.response.json(); } catch (e) {}",
    "if (json && json.token) {",
    "  pm.collectionVariables.set('tokenCliente', json.token);",
    "  pm.collectionVariables.set('refreshTokenCliente', json.refreshToken);",
    "  console.log('tokens del cliente rotados. roles =', json.roles);",
    "}",
    "pm.test('Refresh exitoso (200)', function () {",
    "  pm.response.to.have.status(200);",
    "  pm.expect(json.token, 'token').to.be.a('string').and.not.empty;",
    "});",
])

with open(PATH, "w", encoding="utf-8") as f:
    json.dump(c, f, indent=2, ensure_ascii=False)

print("Coleccion parcheada:", PATH)
print("Vars:", [v["key"] for v in c["variable"]])
