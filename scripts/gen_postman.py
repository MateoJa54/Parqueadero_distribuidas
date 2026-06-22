#!/usr/bin/env python3
"""Genera la coleccion unica Gestion_Parqueaderos.postman_collection.json
y agrega la variable urlKongAdmin a los environments.

Estructura de carpetas: Persona, Roles, Usuarios (con subcarpeta Asignaciones),
Zonas, Espacios, Vehiculos, Kong, Kong Configurado.
Los "Crear" guardan el id en variables de coleccion para encadenar el flujo.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "postman", "Gestion_Parqueaderos.postman_collection.json")


def url(var, segments, query=None):
    raw = "{{" + var + "}}/" + "/".join(segments)
    u = {"raw": raw, "host": ["{{" + var + "}}"], "path": list(segments)}
    if query:
        u["query"] = [{"key": k, "value": v} for k, v in query]
        u["raw"] = raw + "?" + "&".join(f"{k}={v}" for k, v in query)
    return u


def req(name, method, var, segments, body=None, desc="", query=None, save=None):
    request = {
        "method": method,
        "header": [],
        "url": url(var, segments, query),
    }
    if desc:
        request["description"] = desc
    if body is not None:
        request["header"].append({"key": "Content-Type", "value": "application/json"})
        request["body"] = {
            "mode": "raw",
            "raw": json.dumps(body, indent=2, ensure_ascii=False),
            "options": {"raw": {"language": "json"}},
        }
    item = {"name": name, "request": request, "response": []}
    if save:
        var_name, json_path = save
        lines = [
            "var json = {};",
            "try { json = pm.response.json(); } catch (e) {}",
            f"if (json && json.{json_path}) {{",
            f"  pm.collectionVariables.set('{var_name}', json.{json_path});",
            f"  console.log('{var_name} =', json.{json_path});",
            "}",
        ]
        item["event"] = [{"listen": "test", "script": {"type": "text/javascript", "exec": lines}}]
    return item


def folder(name, items, desc=""):
    f = {"name": name, "item": items}
    if desc:
        f["description"] = desc
    return f


# ---------------- PERSONA ----------------
U = "urlUsuarios"
persona = folder("Persona", [
    req("Crear persona", "POST", U, ["api", "v1", "personas"],
        body={
            "firstName": "Mateo", "middleName": "Andres", "lastName": "Jacome",
            "dni": "1750123456", "email": "mateo.jacome@espe.edu.ec",
            "phone": "0991234567", "address": "Sangolqui", "nationality": "Ecuatoriana",
        },
        desc="Crea una persona. Guarda el id en {{idPersona}}.",
        save=("idPersona", "id")),
    req("Listar personas", "GET", U, ["api", "v1", "personas"]),
    req("Obtener persona por ID", "GET", U, ["api", "v1", "personas", "{{idPersona}}"]),
    req("Buscar por DNI", "GET", U, ["api", "v1", "personas", "buscar"], query=[("dni", "1750123456")]),
    req("Buscar por apellido", "GET", U, ["api", "v1", "personas", "buscar"], query=[("apellido", "Jacome")]),
    req("Actualizar persona", "PUT", U, ["api", "v1", "personas", "{{idPersona}}"],
        body={
            "firstName": "Mateo", "middleName": "Andres", "lastName": "Jacome",
            "dni": "1750123456", "email": "mateo.jacome@espe.edu.ec",
            "phone": "0991234567", "address": "Quito", "nationality": "Ecuatoriana",
        }),
    req("Desactivar persona (cascada a usuario)", "PATCH", U,
        ["api", "v1", "personas", "{{idPersona}}", "desactivar"],
        desc="Soft-delete. Si la persona tiene usuario, tambien se desactiva (cascada)."),
    req("Activar persona", "PATCH", U, ["api", "v1", "personas", "{{idPersona}}", "activar"],
        desc="Reactiva la persona. NO reactiva el usuario (minimo privilegio)."),
])

# ---------------- ROLES ----------------
roles = folder("Roles", [
    req("Crear rol", "POST", U, ["api", "v1", "roles"],
        body={"name": "SUPERVISOR", "description": "Supervisa la operacion del parqueadero"},
        desc="Nombre de texto libre (3-50, letras/numeros/espacios/_). Se normaliza a MAYUSCULAS. Guarda {{idRol}}.",
        save=("idRol", "id")),
    req("Listar roles", "GET", U, ["api", "v1", "roles"]),
    req("Obtener rol por ID", "GET", U, ["api", "v1", "roles", "{{idRol}}"]),
    req("Actualizar rol", "PUT", U, ["api", "v1", "roles", "{{idRol}}"],
        body={"name": "SUPERVISOR GENERAL", "description": "Rol actualizado"}),
    req("Crear rol (nombre invalido -> 400)", "POST", U, ["api", "v1", "roles"],
        body={"name": "ab", "description": "muy corto"},
        desc="Demuestra la validacion: name < 3 caracteres -> 400."),
    req("Desactivar rol (guarda asignaciones activas)", "PATCH", U,
        ["api", "v1", "roles", "{{idRol}}", "desactivar"],
        desc="No deja desactivar si hay usuarios activos con ese rol -> 409 (igual que zona con espacio OCUPADO)."),
    req("Activar rol", "PATCH", U, ["api", "v1", "roles", "{{idRol}}", "activar"]),
])

# ---------------- USUARIOS (+ Asignaciones) ----------------
asignaciones = folder("Asignaciones", [
    req("Asignar rol a usuario", "POST", U, ["api", "v1", "asignaciones"],
        body={"idUser": "{{idUsuario}}", "idRole": "{{idRol}}"},
        desc="Crea la relacion user_role (active=true)."),
    req("Listar asignaciones", "GET", U, ["api", "v1", "asignaciones"]),
    req("Roles de un usuario", "GET", U, ["api", "v1", "asignaciones", "usuario", "{{idUsuario}}"]),
    req("Desactivar asignacion (quitar rol)", "PATCH", U,
        ["api", "v1", "asignaciones", "usuario", "{{idUsuario}}", "rol", "{{idRol}}", "desactivar"],
        desc="Quita el rol sin perder historial (active=false). Permite luego desactivar el rol."),
    req("Activar asignacion (devolver rol)", "PATCH", U,
        ["api", "v1", "asignaciones", "usuario", "{{idUsuario}}", "rol", "{{idRol}}", "activar"],
        desc="Reactiva la relacion. Falla si el rol o el usuario estan inactivos."),
])
usuarios = folder("Usuarios", [
    req("Crear usuario", "POST", U, ["api", "v1", "usuarios"],
        body={"idPersona": "{{idPersona}}", "username": "mjacome", "password": "Espe2025"},
        desc="PK compartida con persona (1 a 1). Guarda {{idUsuario}}.",
        save=("idUsuario", "id")),
    req("Listar usuarios", "GET", U, ["api", "v1", "usuarios"]),
    req("Obtener usuario por ID", "GET", U, ["api", "v1", "usuarios", "{{idUsuario}}"]),
    req("Buscar usuario por username", "GET", U, ["api", "v1", "usuarios", "buscar"], query=[("username", "mjacome")]),
    req("Actualizar usuario", "PUT", U, ["api", "v1", "usuarios", "{{idUsuario}}"],
        body={"idPersona": "{{idPersona}}", "username": "mjacome", "password": ""},
        desc="password vacio = se conserva la actual."),
    req("Desactivar usuario", "PATCH", U, ["api", "v1", "usuarios", "{{idUsuario}}", "desactivar"]),
    req("Activar usuario", "PATCH", U, ["api", "v1", "usuarios", "{{idUsuario}}", "activar"],
        desc="Falla si la persona esta inactiva."),
    asignaciones,
])

# ---------------- ZONAS ----------------
Z = "urlZonas"
zonas = folder("Zonas", [
    req("Crear zona", "POST", Z, ["api", "v1", "zonas"],
        body={"nombre": "Zona Norte", "descripcion": "Zona principal norte", "tipo": "REGULAR", "capacidad": 20},
        desc="El codigo se autogenera. Guarda {{idZona}}.",
        save=("idZona", "id")),
    req("Listar zonas", "GET", Z, ["api", "v1", "zonas"]),
    req("Obtener zona por ID", "GET", Z, ["api", "v1", "zonas", "{{idZona}}"]),
    req("Actualizar zona", "PUT", Z, ["api", "v1", "zonas", "{{idZona}}"],
        body={"nombre": "Zona Norte A", "descripcion": "Actualizada", "tipo": "VIP", "capacidad": 25}),
    req("Desactivar zona (cascada + guarda)", "PATCH", Z, ["api", "v1", "zonas", "{{idZona}}", "desactivar"],
        desc="Desactiva los espacios en cascada. Falla si algun espacio esta OCUPADO."),
    req("Activar zona (cascada)", "PATCH", Z, ["api", "v1", "zonas", "{{idZona}}", "activar"]),
])

# ---------------- ESPACIOS ----------------
espacios = folder("Espacios", [
    req("Crear espacio", "POST", Z, ["api", "v1", "espacios"],
        body={"idZona": "{{idZona}}", "descripcion": "Espacio A1", "tipo": "AUTO"},
        desc="El codigo se autogenera. Guarda {{idEspacio}}.",
        save=("idEspacio", "id")),
    req("Listar espacios", "GET", Z, ["api", "v1", "espacios"]),
    req("Obtener espacio por ID", "GET", Z, ["api", "v1", "espacios", "{{idEspacio}}"]),
    req("Actualizar espacio", "PUT", Z, ["api", "v1", "espacios", "{{idEspacio}}"],
        body={"idZona": "{{idZona}}", "descripcion": "Espacio A1 mod", "tipo": "AUTO"}),
    req("Cambiar estado", "PATCH", Z, ["api", "v1", "espacios", "{{idEspacio}}", "estado"],
        body={"estado": "OCUPADO"}),
    req("Espacios disponibles", "GET", Z, ["api", "v1", "espacios", "disponibles"]),
    req("Espacios por estado", "GET", Z, ["api", "v1", "espacios", "estado", "DISPONIBLE"]),
    req("Espacios por zona y estado", "GET", Z,
        ["api", "v1", "espacios", "zona", "{{idZona}}", "estado", "DISPONIBLE"]),
    req("Disponibilidad de un espacio", "GET", Z, ["api", "v1", "espacios", "{{idEspacio}}", "disponibilidad"]),
    req("Desactivar espacio", "PATCH", Z, ["api", "v1", "espacios", "{{idEspacio}}", "desactivar"]),
    req("Activar espacio", "PATCH", Z, ["api", "v1", "espacios", "{{idEspacio}}", "activar"]),
    req("Eliminar espacio", "DELETE", Z, ["api", "v1", "espacios", "{{idEspacio}}"]),
])

# ---------------- VEHICULOS ----------------
V = "urlVehiculos"
vehiculos = folder("Vehiculos", [
    req("Crear auto", "POST", V, ["vehiculos"],
        body={"tipo": "Auto", "datos": {
            "placa": "PBX-1234", "marca": "Toyota", "modelo": "Corolla", "color": "Rojo",
            "anio": 2022, "clasificacion": "Gasolina", "numeroPuertas": 4, "capacidadMaletero": 470}},
        desc="Guarda {{idVehiculo}}.", save=("idVehiculo", "id")),
    req("Crear motocicleta", "POST", V, ["vehiculos"],
        body={"tipo": "Motocicleta", "datos": {
            "placa": "AB-123A", "marca": "Yamaha", "modelo": "FZ", "color": "Negro",
            "anio": 2021, "clasificacion": "Gasolina", "cilindraje": 150, "tipoMoto": "Deportiva"}}),
    req("Crear camioneta", "POST", V, ["vehiculos"],
        body={"tipo": "Camioneta", "datos": {
            "placa": "PCX-5678", "marca": "Chevrolet", "modelo": "Dmax", "color": "Blanco",
            "anio": 2020, "clasificacion": "Diesel", "cabina": 4, "capacidadCarga": "3.5t"}}),
    req("Listar vehiculos", "GET", V, ["vehiculos"]),
    req("Obtener por placa", "GET", V, ["vehiculos", "placa", "PBX-1234"]),
    req("Obtener por ID", "GET", V, ["vehiculos", "{{idVehiculo}}"]),
    req("Actualizar vehiculo", "PATCH", V, ["vehiculos", "{{idVehiculo}}"],
        body={"color": "Azul"}),
    req("Eliminar vehiculo", "DELETE", V, ["vehiculos", "{{idVehiculo}}"]),
])

# ---------------- KONG (proxy 8000) ----------------
G = "urlGateway"
kong = folder("Kong", [
    req("Usuarios via gateway", "GET", G, ["api", "v1", "usuarios"],
        desc="Mismo endpoint pero atravesando Kong (puerto 8000)."),
    req("Zonas via gateway", "GET", G, ["api", "v1", "zonas"]),
    req("Vehiculos via gateway", "GET", G, ["api", "vehiculos"]),
    req("Probar rate-limiting", "GET", G, ["api", "v1", "roles"],
        desc="Enviar varias veces seguidas para ver el header RateLimit-Remaining y eventual 429."),
], desc="Pruebas atravesando el API Gateway Kong (proxy en :8000).")

# ---------------- KONG CONFIGURADO (admin 8001) ----------------
KA = "urlKongAdmin"
kong_cfg = folder("Kong Configurado", [
    req("Listar services", "GET", KA, ["services"]),
    req("Listar routes", "GET", KA, ["routes"]),
    req("Listar plugins", "GET", KA, ["plugins"]),
    req("Estado del nodo", "GET", KA, ["status"]),
], desc="Inspeccion de la configuracion declarativa de Kong (Admin API en :8001).")

collection = {
    "info": {
        "name": "Gestion_Parqueaderos",
        "description": "Coleccion unica del sistema distribuido de parqueaderos (ESPE). "
                       "Carpetas por entidad. Usar el environment Parqueadero-Local o "
                       "Parqueadero-Kong. Los 'Crear' guardan los IDs para encadenar el flujo.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [persona, roles, usuarios, zonas, espacios, vehiculos, kong, kong_cfg],
    "variable": [
        {"key": "idPersona", "value": ""},
        {"key": "idRol", "value": ""},
        {"key": "idUsuario", "value": ""},
        {"key": "idZona", "value": ""},
        {"key": "idEspacio", "value": ""},
        {"key": "idVehiculo", "value": ""},
    ],
}

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2, ensure_ascii=False)
print("Coleccion escrita en:", OUT)

# ---- Agregar urlKongAdmin a los environments ----
def add_kong_admin(path, value):
    with open(path, encoding="utf-8") as f:
        env = json.load(f)
    keys = {v["key"] for v in env["values"]}
    if "urlKongAdmin" not in keys:
        env["values"].append({"key": "urlKongAdmin", "value": value, "enabled": True})
        with open(path, "w", encoding="utf-8") as f:
            json.dump(env, f, indent=2, ensure_ascii=False)
        print("urlKongAdmin agregada a", os.path.basename(path))
    else:
        print("urlKongAdmin ya existe en", os.path.basename(path))

add_kong_admin(os.path.join(ROOT, "postman", "Parqueadero-Local.postman_environment.json"), "http://localhost:8001")
add_kong_admin(os.path.join(ROOT, "postman", "Parqueadero-Kong.postman_environment.json"), "http://localhost:8001")
