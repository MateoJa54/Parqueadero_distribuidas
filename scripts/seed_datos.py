#!/usr/bin/env python3
"""
Carga masiva de datos de demostracion para el sistema de parqueadero.
Pasa TODO por Kong (localhost:8000) respetando las reglas de negocio:
- Cedulas ecuatorianas validas (digito verificador).
- Placas con formato valido por tipo.
- Tipos vehiculo<->espacio compatibles (Auto->AUTO, Motocicleta->MOTO, Camioneta->BUSETA).
- Cada usuario recibe un rol activo antes de asignarle un vehiculo.
- Incluye un caso de "cambio de dominio" (vendio el auto): reasignacion a nuevo dueno.
No borra nada; solo agrega. Idempotencia parcial: si algo ya existe (409), continua.
"""
import json
import urllib.request
import urllib.error

# --- Config: todo por Kong ---
USUARIOS = "http://localhost:8000"
ZONAS = "http://localhost:8000"
VEHICULOS = "http://localhost:8000/api"
ASIGNACIONES = "http://localhost:8000"
TICKETS = "http://localhost:8000"

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


def post(url, body, auth=True):
    return _req("POST", url, body, auth)


def patch(url, body=None, auth=True):
    return _req("PATCH", url, body, auth)


def get(url, auth=True):
    return _req("GET", url, None, auth)


# --- Generador de cedulas ecuatorianas validas ---
def gen_cedula(n):
    prov = (n % 24) + 1          # 01..24
    third = n % 6                # <6 persona natural
    rest = f"{n:06d}"[-6:]
    first9 = f"{prov:02d}{third}{rest}"
    coef = [2, 1, 2, 1, 2, 1, 2, 1, 2]
    s = 0
    for d, c in zip(first9, coef):
        p = int(d) * c
        s += p - 9 if p > 9 else p
    check = (10 - (s % 10)) % 10
    return first9 + str(check)


LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def placa_auto(i):
    a = LETRAS[i % 26]; b = LETRAS[(i // 26) % 26]; c = LETRAS[(i // 7) % 26]
    return f"{a}{b}{c}-{1000 + i}"


def placa_moto(i):
    a = LETRAS[i % 26]; b = LETRAS[(i + 3) % 26]; d = LETRAS[(i + 7) % 26]
    return f"{a}{b}-{100 + i}{d}"


# ------------------------------------------------------------------
def login_root():
    global TOKEN
    st, body = post(f"{USUARIOS}/api/v1/auth/login",
                    {"username": "root", "password": "Root2025"}, auth=False)
    if st == 200:
        TOKEN = body.get("token") or body.get("accessToken")
        print(f"[OK] Login root -> token obtenido")
        return True
    print(f"[FALLO] Login root ({st}): {body}")
    return False


def asegurar_rol(nombre, desc):
    st, roles = get(f"{USUARIOS}/api/v1/roles")
    if st == 200:
        for r in roles:
            if str(r.get("name", "")).upper() == nombre.upper():
                print(f"[=] Rol '{nombre}' ya existe -> {r.get('id')}")
                return r.get("id")
    st, body = post(f"{USUARIOS}/api/v1/roles", {"name": nombre, "description": desc})
    print(f"[{st}] Crear rol '{nombre}' -> {body.get('id')}")
    return body.get("id")


NOMBRES = ["Andres", "Maria", "Carlos", "Valentina", "Diego", "Camila", "Sebastian",
           "Daniela", "Luis", "Gabriela", "Jorge", "Paula", "Bryan", "Doménica",
           "Kevin", "Anahi"]
APELLIDOS = ["Jacome", "Torres", "Vaca", "Suarez", "Mena", "Ponce", "Cevallos",
             "Rivera", "Naranjo", "Salazar", "Zambrano", "Chiluisa", "Iza", "Yepez"]
CIUDADES = ["Sangolqui", "Quito", "Latacunga", "Ambato", "Riobamba"]


def crear_persona_usuario(i, role_id):
    nom = NOMBRES[i % len(NOMBRES)]
    ape = APELLIDOS[i % len(APELLIDOS)]
    ced = gen_cedula(1000 + i)
    email = f"{nom.lower()}.{ape.lower()}{i}@espe.edu.ec"
    tel = f"09{80000000 + i:08d}"[:10]
    st, p = post(f"{USUARIOS}/api/v1/personas", {
        "firstName": nom, "middleName": "Israel", "lastName": ape,
        "dni": ced, "email": email, "phone": tel,
        "address": CIUDADES[i % len(CIUDADES)], "nationality": "Ecuatoriana"})
    if st not in (200, 201):
        print(f"  [skip persona {nom} {ape}] {st}: {p}")
        return None
    pid = p.get("id")
    username = f"{nom[0].lower()}{ape.lower()}{i}"[:15]
    st, u = post(f"{USUARIOS}/api/v1/usuarios",
                 {"idPersona": pid, "username": username, "password": "Espe2025"})
    if st not in (200, 201):
        print(f"  [skip usuario {username}] {st}: {u}")
        return None
    uid = u.get("id")
    # asignar rol activo (requisito para asociar vehiculos)
    post(f"{USUARIOS}/api/v1/asignaciones", {"idUser": uid, "idRole": role_id})
    print(f"  [OK] {nom} {ape} -> usuario {username} ({uid})")
    return {"idUsuario": uid, "username": username, "nombre": f"{nom} {ape}"}


def crear_zona(nombre, desc, tipo, cap):
    st, z = post(f"{ZONAS}/api/v1/zonas",
                 {"nombre": nombre, "descripcion": desc, "tipo": tipo, "capacidad": cap})
    zid = z.get("idZona") or z.get("id")
    if not zid:  # ya existe (409) u otro caso: buscar por nombre
        s2, zs = get(f"{ZONAS}/api/v1/zonas")
        if s2 == 200 and isinstance(zs, list):
            for zz in zs:
                if zz.get("nombre") == nombre:
                    zid = zz.get("idZona") or zz.get("id")
                    break
    print(f"[{st}] Zona '{nombre}' -> {zid}")
    return zid


def crear_espacio(id_zona, desc, tipo):
    st, e = post(f"{ZONAS}/api/v1/espacios",
                 {"idZona": id_zona, "descripcion": desc, "tipo": tipo})
    if st in (200, 201):
        return {"id": e.get("idEspacio") or e.get("id"), "tipo": tipo}
    print(f"  [skip espacio {desc}] {st}: {e}")
    return None


def crear_vehiculo(i, tipo):
    color = COLORES[i % len(COLORES)]
    anio = 2019 + (i % 7)
    if tipo == "Auto":
        marca, modelo = MARCAS_AUTO[i % len(MARCAS_AUTO)]
        clas = ["Gasolina", "Híbrido", "Eléctrico"][i % 3]
        datos = {"placa": placa_auto(i), "marca": marca, "modelo": modelo,
                 "color": color, "anio": anio, "clasificacion": clas,
                 "numeroPuertas": 2 + (i % 4), "capacidadMaletero": 300 + (i % 6) * 100}
    elif tipo == "Motocicleta":
        marca, modelo = MARCAS_MOTO[i % len(MARCAS_MOTO)]
        clas = ["Gasolina", "Eléctrico"][i % 2]
        datos = {"placa": placa_moto(i), "marca": marca, "modelo": modelo,
                 "color": color, "anio": anio, "clasificacion": clas,
                 "cilindraje": [125, 150, 200, 250][i % 4],
                 "tipoMoto": ["Deportiva", "Scooter", "Motocross"][i % 3]}
    else:  # Camioneta
        marca, modelo = MARCAS_CAM[i % len(MARCAS_CAM)]
        clas = ["Diésel", "Gasolina"][i % 2]
        datos = {"placa": placa_auto(i), "marca": marca, "modelo": modelo,
                 "color": color, "anio": anio, "clasificacion": clas,
                 "cabina": 2 if i % 2 else 4, "capacidadCarga": f"{2 + i % 4}.5t"}
    st, v = post(f"{VEHICULOS}/vehiculos", {"tipo": tipo, "datos": datos})
    if st in (200, 201):
        return {"id": v.get("id"), "placa": datos["placa"], "tipo": tipo}
    print(f"  [skip vehiculo {datos['placa']}] {st}: {v}")
    return None


def asignar(uid, vid, role_id, alias):
    st, a = post(f"{ASIGNACIONES}/api/v1/asignaciones-vehiculos", {
        "userId": uid, "vehicleId": vid, "roleId": role_id,
        "assignmentType": "PROPIETARIO", "vehicleAlias": alias,
        "observation": "Carga inicial de demostracion"})
    return st, a


TIPO_ESPACIO = {"Auto": "AUTO", "Motocicleta": "MOTO", "Camioneta": "BUSETA"}

# Variedad para que el catalogo de vehiculos no sea monotono en el frontend.
MARCAS_AUTO = [("Toyota", "Corolla"), ("Kia", "Rio"), ("Hyundai", "Accent"),
               ("Chevrolet", "Aveo"), ("Nissan", "Sentra"), ("Mazda", "3"),
               ("Volkswagen", "Gol")]
MARCAS_MOTO = [("Yamaha", "FZ"), ("Suzuki", "GN125"), ("Honda", "CB190"),
               ("Bajaj", "Pulsar"), ("KTM", "Duke")]
MARCAS_CAM = [("Chevrolet", "Dmax"), ("Toyota", "Hilux"), ("Ford", "Ranger"),
              ("Mazda", "BT-50"), ("Nissan", "Frontier")]
COLORES = ["Rojo", "Negro", "Blanco", "Gris", "Azul", "Plata", "Verde"]


def main():
    if not login_root():
        return
    print("\n=== ROLES ===")
    role_id = asegurar_rol("COMUNIDAD ESPE", "Rol para asociar vehiculos a usuarios")

    print("\n=== ZONAS Y ESPACIOS ===")
    espacios = []  # {id, tipo, ocupado}
    zonas_def = [
        ("Zona Norte", "Ingreso principal norte", "REGULAR", 40),
        ("Zona Sur", "Salida sur", "REGULAR", 40),
        ("Zona VIP", "Cubierta preferencial", "VIP", 40),
    ]
    for zn, zd, zt, zc in zonas_def:
        zid = crear_zona(zn, zd, zt, zc)
        if not zid:
            continue
        plan = [("AUTO", 8), ("MOTO", 5), ("BUSETA", 3)]
        for tipo, cant in plan:
            for k in range(1, cant + 1):
                e = crear_espacio(zid, f"{zn} {tipo}-{k:02d}", tipo)
                if e:
                    e["ocupado"] = False
                    espacios.append(e)
    print(f"  Total espacios creados: {len(espacios)}")

    print("\n=== PERSONAS / USUARIOS ===")
    usuarios = []
    for i in range(200, 240):
        u = crear_persona_usuario(i, role_id)
        if u:
            usuarios.append(u)
    print(f"  Total usuarios creados: {len(usuarios)}")

    print("\n=== VEHICULOS ===")
    tipos_ciclo = ["Auto", "Motocicleta", "Camioneta"]
    vehiculos = []
    for i in range(30):
        v = crear_vehiculo(700 + i, tipos_ciclo[i % 3])
        if v:
            vehiculos.append(v)
    print(f"  Total vehiculos creados: {len(vehiculos)}")

    print("\n=== ASIGNACIONES (dueno de cada vehiculo) ===")
    pares = []  # (usuario, vehiculo)
    for i, v in enumerate(vehiculos):
        if i >= len(usuarios):
            break
        u = usuarios[i]
        st, _ = asignar(u["idUsuario"], v["id"], role_id, f"Vehiculo de {u['nombre']}")
        print(f"  [{st}] {u['nombre']} <- {v['placa']} ({v['tipo']})")
        if st in (200, 201):
            pares.append((u, v))

    print("\n=== TICKETS (ingresos + algunos pagos/anulaciones) ===")
    creados = 0
    for idx, (u, v) in enumerate(pares):
        tipo_esp = TIPO_ESPACIO[v["tipo"]]
        libre = next((e for e in espacios if e["tipo"] == tipo_esp and not e["ocupado"]), None)
        if not libre:
            continue
        st, t = post(f"{TICKETS}/api/v1/tickets",
                     {"placa": v["placa"], "idEspacio": libre["id"]})
        if st in (200, 201):
            libre["ocupado"] = True
            tid = t.get("id")
            creados += 1
            accion = "ACTIVO"
            if idx % 3 == 0:      # paga
                sp, _ = patch(f"{TICKETS}/api/v1/tickets/{tid}/pagar")
                if sp == 200:
                    libre["ocupado"] = False
                    accion = "PAGADO"
            elif idx % 3 == 1:    # anula
                sa, _ = patch(f"{TICKETS}/api/v1/tickets/{tid}/anular",
                              {"motivo": "Correccion de demostracion"})
                if sa == 200:
                    libre["ocupado"] = False
                    accion = "ANULADO"
            print(f"  [{st}] Ingreso {v['placa']} -> {accion}")
        else:
            print(f"  [skip ticket {v['placa']}] {st}: {t}")
    print(f"  Total tickets creados: {creados}")

    # --- CASO CAMBIO DE DOMINIO (vendio el auto) ---
    print("\n=== CASO: CAMBIO DE DOMINIO (venta de vehiculo) ===")
    if pares:
        vendedor, veh = pares[0]
        comprador = next((u for u in usuarios
                          if u["idUsuario"] != vendedor["idUsuario"]), None)
        vid = veh["id"]
        # 1) desactivar la relacion del vendedor (que SI es el dueno actual)
        sd, _ = patch(f"{ASIGNACIONES}/api/v1/asignaciones-vehiculos/"
                      f"{vendedor['idUsuario']}/{vid}/desactivar")
        print(f"  [{sd}] Desactivar asignacion de {vendedor['nombre']} (vende {veh['placa']})")
        # 2) asignar al comprador (nuevo dueno)
        if comprador:
            sc, _ = asignar(comprador["idUsuario"], vid, role_id, "Nuevo dueno")
            print(f"  [{sc}] Nueva asignacion a {comprador['nombre']} (compra {veh['placa']})")
            print("  -> Si sc=201, el cambio de dominio funciono; el historial del vendedor se conserva.")

    print("\n==== CARGA FINALIZADA ====")
    print(f"Zonas: {len(zonas_def)} | Espacios: {len(espacios)} | "
          f"Usuarios: {len(usuarios)} | Vehiculos: {len(vehiculos)} | "
          f"Asignaciones: {len(pares)} | Tickets: {creados}")


if __name__ == "__main__":
    main()
