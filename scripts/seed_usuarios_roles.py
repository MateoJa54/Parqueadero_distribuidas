#!/usr/bin/env python3
"""
Crea usuarios de PRUEBA con credenciales fijas, uno por cada rol del sistema,
para poder verificar el control de acceso del frontend end-to-end.
Pasa TODO por Kong (localhost:8000). Idempotente: si el usuario ya existe,
solo se asegura de que tenga el rol correcto asignado y activo.

Usuarios que crea:
  qa.admin     / QaAdmin2025     -> ADMIN
  qa.recauda   / QaRecauda2025   -> RECAUDADOR
  qa.cliente   / QaCliente2025   -> CLIENTE
  qa.invitado  / QaInvitado2025  -> INVITADO
"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8000"
TOKEN = None


def _req(method, url, body=None, auth=True):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if auth and TOKEN:
        req.add_header("Authorization", f"Bearer {TOKEN}")
    try:
        with urllib.request.urlopen(req) as r:
            txt = r.read().decode()
            return r.status, (json.loads(txt) if txt.strip() else {})
    except urllib.error.HTTPError as e:
        txt = e.read().decode()
        try:
            payload = json.loads(txt) if txt.strip() else {}
        except Exception:
            payload = {"raw": txt}
        return e.code, payload
    except Exception as e:
        return 0, {"error": str(e)}


def post(u, b, auth=True):
    return _req("POST", u, b, auth)


def get(u, auth=True):
    return _req("GET", u, None, auth)


def gen_cedula(n):
    prov = (n % 24) + 1
    third = n % 6
    rest = f"{n:06d}"[-6:]
    first9 = f"{prov:02d}{third}{rest}"
    coef = [2, 1, 2, 1, 2, 1, 2, 1, 2]
    s = 0
    for d, c in zip(first9, coef):
        p = int(d) * c
        s += p - 9 if p > 9 else p
    check = (10 - (s % 10)) % 10
    return first9 + str(check)


def login_root():
    global TOKEN
    st, body = post(f"{BASE}/api/v1/auth/login",
                    {"username": "root", "password": "Root2025"}, auth=False)
    if st == 200:
        TOKEN = body.get("token") or body.get("accessToken")
        print("[OK] Login root")
        return True
    print(f"[FALLO] Login root ({st}): {body}")
    return False


def id_rol(nombre):
    st, roles = get(f"{BASE}/api/v1/roles")
    if st == 200 and isinstance(roles, list):
        for r in roles:
            if str(r.get("name", "")).upper() == nombre.upper():
                return r.get("id")
    return None


def id_persona_por_dni(dni):
    st, res = get(f"{BASE}/api/v1/personas/buscar?dni={dni}")
    if st == 200:
        lst = res if isinstance(res, list) else res.get("content", [])
        if lst:
            return lst[0].get("id")
    return None


def id_usuario_por_username(username):
    st, res = get(f"{BASE}/api/v1/usuarios/buscar?username={username}")
    if st == 200:
        lst = res if isinstance(res, list) else res.get("content", [])
        for u in lst:
            if str(u.get("username", "")).lower() == username.lower():
                return u.get("id")
        if lst:
            return lst[0].get("id")
    return None


def crear_usuario_rol(seed, nombre, apellido, username, password, rol):
    role_id = id_rol(rol)
    if not role_id:
        print(f"  [x] Rol {rol} no existe; se omite {username}")
        return
    dni = gen_cedula(seed)
    # 1) persona (o reutilizar si ya existe por dni)
    st, p = post(f"{BASE}/api/v1/personas", {
        "firstName": nombre, "middleName": "QA", "lastName": apellido,
        "dni": dni, "email": f"{username}@espe.edu.ec",
        "phone": f"09{seed:08d}"[:10], "address": "Sangolqui",
        "nationality": "Ecuatoriana"})
    pid = p.get("id") if st in (200, 201) else id_persona_por_dni(dni)
    if not pid:
        print(f"  [x] No se pudo crear/hallar persona de {username}: {st} {p}")
        return
    # 2) usuario (o reutilizar si ya existe por username)
    st, u = post(f"{BASE}/api/v1/usuarios",
                 {"idPersona": pid, "username": username, "password": password})
    uid = u.get("id") if st in (200, 201) else id_usuario_por_username(username)
    if not uid:
        print(f"  [x] No se pudo crear/hallar usuario {username}: {st} {u}")
        return
    # 3) asignar rol (si ya lo tiene, el backend responde 409; se ignora)
    sa, ra = post(f"{BASE}/api/v1/asignaciones", {"idUser": uid, "idRole": role_id})
    marca = "asignado" if sa in (200, 201) else f"ya existente ({sa})"
    print(f"  [OK] {username:<12} / {password:<14} -> {rol:<11} (uid={uid}, rol {marca})")


def main():
    print("==== USUARIOS DE PRUEBA POR ROL ====")
    if not login_root():
        return
    usuarios = [
        (9001, "Ana",   "AdminQA",    "qa.admin",    "QaAdmin2025",    "ADMIN"),
        (9002, "Raul",  "RecaudaQA",  "qa.recauda",  "QaRecauda2025",  "RECAUDADOR"),
        (9003, "Clara", "ClienteQA",  "qa.cliente",  "QaCliente2025",  "CLIENTE"),
        (9004, "Ivan",  "InvitadoQA", "qa.invitado", "QaInvitado2025", "INVITADO"),
    ]
    for seed, nom, ape, user, pwd, rol in usuarios:
        crear_usuario_rol(seed, nom, ape, user, pwd, rol)
    print("\n==== LISTO ====")
    print("Inicia sesion con cada uno para validar los permisos por rol.")


if __name__ == "__main__":
    main()
