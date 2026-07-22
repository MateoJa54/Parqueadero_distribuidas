#!/usr/bin/env bash
# ============================================================
#  detener.sh  ·  Detiene todo lo que arranco iniciar.sh
#  (Linux / macOS)
# ------------------------------------------------------------
#  Uso:
#     ./detener.sh          # detiene servicios + Kong (conserva datos)
#     ./detener.sh --wipe   # ademas BORRA los datos de la BD (down -v)
# ============================================================
set -uo pipefail
cd "$(dirname "$0")"

if docker compose version >/dev/null 2>&1; then DC="docker compose"; else DC="docker-compose"; fi

echo ">> deteniendo microservicios y frontend ..."
# 1) por PIDs registrados
if [ -f logs/pids.txt ]; then
  while read -r pid; do [ -n "$pid" ] && kill "$pid" 2>/dev/null || true; done < logs/pids.txt
  rm -f logs/pids.txt
fi
# 2) respaldo: por puerto (por si quedaron procesos sueltos)
for port in 8081 8080 8082 8083 3000 3002 5173; do
  pid="$(lsof -ti:"$port" 2>/dev/null || true)"
  [ -n "$pid" ] && kill $pid 2>/dev/null || true
done

echo ">> deteniendo Kong ..."
( cd gateway && $DC down ) 2>/dev/null || true

if [ "${1:-}" = "--wipe" ]; then
  echo ">> deteniendo y BORRANDO datos de PostgreSQL/RabbitMQ ..."
  $DC down -v
else
  echo ">> deteniendo PostgreSQL/RabbitMQ (conserva datos) ..."
  $DC down
fi

echo ">> listo. Todo detenido."
