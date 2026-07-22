#!/usr/bin/env bash
# ============================================================
#  k8s/detener.sh · Elimina los recursos del sistema en k8s.
#    ./k8s/detener.sh          # borra el namespace (conserva el cluster)
#    ./k8s/detener.sh --all    # ademas apaga Minikube
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."
NS=parqueadero-iza-garcia-criollo

echo ">> eliminando namespace '$NS' (todos sus recursos) ..."
kubectl delete namespace "$NS" --ignore-not-found

if [ "${1:-}" = "--all" ]; then
  echo ">> apagando Minikube ..."
  minikube stop
fi
echo "Listo."
