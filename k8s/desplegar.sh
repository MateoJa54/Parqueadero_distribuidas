#!/usr/bin/env bash
# ============================================================
#  k8s/desplegar.sh · Despliegue COMPLETO en Kubernetes (Minikube)
# ------------------------------------------------------------
#  1. Verifica/inicia Minikube y habilita el addon ingress.
#  2. Construye las imagenes de los 6 microservicios + frontend
#     DENTRO del Docker de Minikube (no necesita registry).
#  3. Aplica todos los manifiestos (namespace, config, secrets,
#     Postgres, RabbitMQ, microservicios, Kong, frontend, ingress).
#  4. Espera a que todo este listo y siembra datos de demo.
#
#  Uso:   ./k8s/desplegar.sh            # todo + seed
#         ./k8s/desplegar.sh --no-seed  # sin datos de demo
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
NS=parqueadero-iza-garcia-criollo
HOST=parqueadero.iza.garcia.criollo
SEED=1
for a in "$@"; do [ "$a" = "--no-seed" ] && SEED=0; done

c_ok(){   printf '\033[1;32m%s\033[0m\n' "$*"; }
c_info(){ printf '\033[1;36m%s\033[0m\n' "$*"; }
c_err(){  printf '\033[1;31m%s\033[0m\n' "$*"; }

# --- 0) Requisitos -----------------------------------------------------------
for bin in docker minikube kubectl; do
  command -v "$bin" >/dev/null 2>&1 || { c_err "Falta '$bin'."; exit 1; }
done

# Los microservicios Spring exigen Java 25. Si el JAVA_HOME actual no es un
# JDK 25 (p.ej. el shell trae el runtime de JetBrains 17), buscamos uno valido.
is_jdk25(){ [ -x "$1/bin/javac" ] && "$1/bin/javac" -version 2>&1 | grep -q '^javac 25'; }

if [ -z "${JAVA_HOME:-}" ] || ! is_jdk25 "${JAVA_HOME:-}"; then
  for cand in /usr/lib/jvm/java-25-openjdk /usr/lib/jvm/java-25 /usr/lib/jvm/jdk-25; do
    is_jdk25 "$cand" && export JAVA_HOME="$cand" && break
  done
fi
is_jdk25 "${JAVA_HOME:-}" || { c_err "No se encontro un JDK 25. Instala uno o define JAVA_HOME."; exit 1; }
c_ok "JAVA_HOME = $JAVA_HOME ($("$JAVA_HOME/bin/javac" -version 2>&1))"

[ -f keys/jwt_public.pem ] && [ -f keys/jwt_private.pem ] || {
  c_info ">> generando claves JWT ..."; bash scripts/setup_jwt_keys.sh; }

# --- 1) Minikube -------------------------------------------------------------
if ! minikube status 2>/dev/null | grep -q "host: Running"; then
  c_info ">> iniciando Minikube ..."
  minikube start --driver=docker
fi
c_info ">> habilitando addon ingress ..."
minikube addons enable ingress >/dev/null

# Apunta el cliente Docker al demonio de Minikube: las imagenes quedan
# disponibles para el cluster sin necesidad de un registry externo.
c_info ">> usando el Docker interno de Minikube ..."
eval "$(minikube docker-env)"

# --- 2) Compilar los JAR de los servicios Spring -----------------------------
c_info ">> compilando servicios Spring (mvnw package) ..."
for s in usuarios zonas asignaciones tickets; do
  c_info "   - $s"
  ( cd "$s" && JAVA_HOME="$JAVA_HOME" ./mvnw -q -DskipTests package )
done

# --- 3) Construir imagenes ---------------------------------------------------
c_info ">> construyendo imagenes de contenedor ..."
docker build -t parqueadero/usuarios:latest     usuarios
docker build -t parqueadero/zonas:latest        zonas
docker build -t parqueadero/asignaciones:latest asignaciones
docker build -t parqueadero/tickets:latest      tickets
docker build -t parqueadero/vehiculos:latest    vehiculos/vehiculos
docker build -t parqueadero/ms-audit:latest     ms-audit
docker build -t parqueadero/frontend:latest \
  --build-arg VITE_API_USUARIOS=http://$HOST/api/v1 \
  --build-arg VITE_API_ZONAS=http://$HOST/api/v1 \
  --build-arg VITE_API_ASIGNACIONES=http://$HOST/api/v1 \
  --build-arg VITE_API_TICKETS=http://$HOST/api/v1 \
  --build-arg VITE_API_VEHICULOS=http://$HOST/api/vehiculos \
  --build-arg VITE_API_AUDIT=http://$HOST/api/v1/audit \
  frontend

# --- 4) Manifiestos base (namespace, config, secret) -------------------------
c_info ">> aplicando namespace, configmaps y secret JWT ..."
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap-initdb.yaml
kubectl apply -f k8s/02-configmap-kong.yaml
kubectl -n "$NS" create secret generic jwt-keys \
  --from-file=jwt_public.pem=keys/jwt_public.pem \
  --from-file=jwt_private.pem=keys/jwt_private.pem \
  --dry-run=client -o yaml | kubectl apply -f -

# --- 5) Infraestructura ------------------------------------------------------
c_info ">> desplegando Postgres y RabbitMQ ..."
kubectl apply -f k8s/03-postgres.yaml
kubectl apply -f k8s/04-rabbitmq.yaml
kubectl -n "$NS" rollout status deploy/postgres --timeout=180s
kubectl -n "$NS" rollout status deploy/rabbitmq --timeout=180s

# --- 6) Microservicios + gateway + frontend ----------------------------------
c_info ">> desplegando microservicios ..."
kubectl apply -f k8s/10-usuarios.yaml
kubectl apply -f k8s/11-zonas.yaml
kubectl apply -f k8s/12-asignaciones.yaml
kubectl apply -f k8s/13-tickets.yaml
kubectl apply -f k8s/14-vehiculos.yaml
kubectl apply -f k8s/15-ms-audit.yaml
kubectl apply -f k8s/20-kong.yaml
kubectl apply -f k8s/21-frontend.yaml
kubectl apply -f k8s/30-ingress.yaml

c_info ">> esperando a que los servicios esten listos ..."
for d in usuarios zonas asignaciones tickets vehiculos ms-audit kong frontend; do
  kubectl -n "$NS" rollout status deploy/$d --timeout=240s
done

IP="$(minikube ip)"

# --- 7) Datos de demostracion (via port-forward a Kong) ----------------------
if [ "$SEED" -eq 1 ]; then
  c_info ">> sembrando datos de demo (port-forward Kong :8000) ..."
  kubectl -n "$NS" port-forward svc/kong 8000:8000 >/tmp/pf-kong.log 2>&1 &
  PF=$!
  sleep 4
  PY=python3; command -v python3 >/dev/null 2>&1 || PY=python
  "$PY" scripts/seed_usuarios_roles.py || c_err "seed_usuarios_roles.py con avisos."
  "$PY" scripts/seed_datos.py          || c_err "seed_datos.py con avisos."
  kill "$PF" 2>/dev/null || true
fi

echo
c_ok "==================== DESPLEGADO EN KUBERNETES ===================="
echo "  1) Agrega esta linea a /etc/hosts (una sola vez):"
echo "        $IP   $HOST"
echo "     (sudo sh -c 'echo \"$IP $HOST\" >> /etc/hosts')"
echo
echo "  2) Abre el frontend:   http://$HOST"
echo "     API via gateway:    http://$HOST/api/v1/..."
echo
echo "  Cuentas de prueba:"
echo "     root / Root2025 · qa.admin / QaAdmin2025 · qa.recauda / QaRecauda2025"
echo "     qa.cliente / QaCliente2025 · qa.invitado / QaInvitado2025"
echo
echo "  Ver estado:   kubectl -n $NS get pods,svc,ingress"
echo "  Detener:      ./k8s/detener.sh"
c_ok "================================================================="
