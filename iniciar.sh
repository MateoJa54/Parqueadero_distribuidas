#!/usr/bin/env bash
# ============================================================
#  iniciar.sh  ·  Arranque COMPLETO del sistema de parqueadero
#  (Linux / macOS)
# ------------------------------------------------------------
#  Deja todo listo para probar en un solo comando:
#    1. Verifica requisitos (Java 25, Node, Docker, Python).
#    2. Genera las claves JWT (RS256) si no existen.
#    3. Levanta PostgreSQL + RabbitMQ + Kong (Docker).
#    4. Compila y arranca los 6 microservicios en segundo plano.
#    5. Arranca el frontend (Vite).
#    6. Carga datos de demostracion (usuarios QA + catalogo).
#
#  Uso:
#     ./iniciar.sh            # arranca todo y siembra datos
#     ./iniciar.sh --no-seed  # arranca todo SIN sembrar datos
#     ./iniciar.sh --no-front # no arranca el frontend
#
#  Logs de cada servicio:  ./logs/<servicio>.log
#  Para detener todo:      ./detener.sh
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"
mkdir -p logs
PIDFILE="logs/pids.txt"; : > "$PIDFILE"

SEED=1; FRONT=1
for arg in "$@"; do
  case "$arg" in
    --no-seed)  SEED=0 ;;
    --no-front) FRONT=0 ;;
  esac
done

c_ok(){ printf '\033[1;32m%s\033[0m\n' "$*"; }
c_info(){ printf '\033[1;36m%s\033[0m\n' "$*"; }
c_warn(){ printf '\033[1;33m%s\033[0m\n' "$*"; }
c_err(){ printf '\033[1;31m%s\033[0m\n' "$*"; }

# --- 0) Requisitos -----------------------------------------------------------
need(){ command -v "$1" >/dev/null 2>&1 || { c_err "Falta '$1'. Instalalo y reintenta."; exit 1; }; }
need docker; need node; need npm
PY=python3; command -v python3 >/dev/null 2>&1 || PY=python
command -v "$PY" >/dev/null 2>&1 || { c_err "Falta Python 3."; exit 1; }

# JAVA_HOME -> debe ser JDK 25
if [ -z "${JAVA_HOME:-}" ]; then
  for cand in /usr/lib/jvm/java-25-openjdk /usr/lib/jvm/jdk-25 /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home; do
    [ -x "$cand/bin/java" ] && export JAVA_HOME="$cand" && break
  done
fi
[ -n "${JAVA_HOME:-}" ] || { c_err "Define JAVA_HOME apuntando al JDK 25 (ej: export JAVA_HOME=/usr/lib/jvm/java-25-openjdk)."; exit 1; }
JV="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
echo "$JV" | grep -q '"25' || c_warn "OJO: JAVA_HOME no parece JDK 25 -> $JV"
c_ok "JAVA_HOME = $JAVA_HOME"

# docker compose (v2) o docker-compose (v1)
if docker compose version >/dev/null 2>&1; then DC="docker compose"; else DC="docker-compose"; fi

# --- helpers -----------------------------------------------------------------
run_bg(){ # run_bg <nombre> <dir> <comando...>
  local name="$1" dir="$2"; shift 2
  c_info ">> arrancando $name ..."
  ( cd "$dir" && JAVA_HOME="$JAVA_HOME" nohup "$@" > "$ROOT/logs/$name.log" 2>&1 & echo $! ) >> "$PIDFILE"
}
wait_port(){ # wait_port <puerto> <nombre> [timeout_seg]
  local port="$1" name="$2" max="${3:-120}" i=0
  printf "   esperando %s (:%s) " "$name" "$port"
  while ! (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; do
    i=$((i+1)); [ "$i" -ge "$max" ] && { c_err "timeout en $name (:$port). Revisa logs/$name.log"; return 1; }
    printf "."; sleep 1
  done
  exec 3>&- 2>/dev/null || true
  c_ok " OK"
}

# --- 1) Claves JWT -----------------------------------------------------------
if [ ! -f keys/jwt_private.pem ]; then
  c_info ">> generando claves JWT (RS256) ..."
  bash scripts/setup_jwt_keys.sh
else
  c_ok "Claves JWT ya existen (keys/)."
  # asegura que ms-audit y vehiculos tengan su .env con la clave publica
  bash scripts/setup_jwt_keys.sh >/dev/null 2>&1 || true
fi

# --- 2) Infraestructura Docker ----------------------------------------------
c_info ">> levantando PostgreSQL + RabbitMQ ..."
$DC up -d
wait_port 5433 postgres 60
wait_port 5672 rabbitmq 60

c_info ">> levantando Kong (gateway) ..."
( cd gateway && $DC up -d )
wait_port 8000 kong 60

# --- 3) Frontend .env --------------------------------------------------------
[ -f frontend/.env ] || cp frontend/.env.example frontend/.env

# --- 4) Microservicios -------------------------------------------------------
DB_USER=usuarios     DB_NAME=usuarios     DB_PASSWORD=usuarios123     ADMIN_ROOT_PASSWORD="${ADMIN_ROOT_PASSWORD:-Root2025}" run_bg usuarios     usuarios     ./mvnw -q -DskipTests spring-boot:run
DB_USER=zonas        DB_NAME=zonas        DB_PASSWORD=zonas123        run_bg zonas        zonas        ./mvnw -q -DskipTests spring-boot:run
DB_USER=asignaciones DB_NAME=asignaciones DB_PASSWORD=asignaciones123 run_bg asignaciones asignaciones ./mvnw -q -DskipTests spring-boot:run
DB_USER=tickets      DB_NAME=tickets      DB_PASSWORD=tickets123      run_bg tickets      tickets      ./mvnw -q -DskipTests spring-boot:run

c_info ">> instalando dependencias Node (vehiculos, ms-audit) ..."
( cd vehiculos/vehiculos && npm install --silent )
( cd ms-audit && npm install --silent )
run_bg vehiculos vehiculos/vehiculos npm run start:dev
run_bg ms-audit  ms-audit            npm run start:dev

wait_port 8081 usuarios
wait_port 8080 zonas
wait_port 8082 asignaciones
wait_port 8083 tickets
wait_port 3000 vehiculos
wait_port 3002 ms-audit

# --- 5) Datos de demostracion ------------------------------------------------
if [ "$SEED" -eq 1 ]; then
  c_info ">> sembrando datos (esto pasa por Kong) ..."
  sleep 3
  "$PY" scripts/seed_usuarios_roles.py || c_warn "seed_usuarios_roles.py reporto avisos (revisa arriba)."
  "$PY" scripts/seed_datos.py          || c_warn "seed_datos.py reporto avisos (revisa arriba)."
fi

# --- 6) Frontend -------------------------------------------------------------
if [ "$FRONT" -eq 1 ]; then
  c_info ">> instalando dependencias del frontend ..."
  ( cd frontend && npm install --silent )
  run_bg frontend frontend npm run dev
  wait_port 5173 frontend 60 || true
fi

echo
c_ok "==================== TODO ARRIBA ===================="
echo "  Frontend .......... http://localhost:5173"
echo "  Gateway (Kong) .... http://localhost:8000"
echo "  RabbitMQ panel .... http://localhost:15672  (guest/guest)"
echo
echo "  Cuentas de prueba (usuario / contrasena -> rol):"
echo "    root         / Root2025       -> ROOT (super admin)"
echo "    qa.admin     / QaAdmin2025    -> ADMIN"
echo "    qa.recauda   / QaRecauda2025  -> RECAUDADOR"
echo "    qa.cliente   / QaCliente2025  -> CLIENTE"
echo "    qa.invitado  / QaInvitado2025 -> INVITADO"
echo
echo "  Logs:   ./logs/<servicio>.log       Detener todo:   ./detener.sh"
c_ok "====================================================="
