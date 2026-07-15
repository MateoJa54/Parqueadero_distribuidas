#!/usr/bin/env bash
# ============================================================
#  setup_jwt_keys.sh — Claves RSA para firmar/verificar JWT (RS256)
# ------------------------------------------------------------
#  Modelo asimetrico:
#    - usuarios FIRMA con la clave PRIVADA (keys/jwt_private.pem).  <- secreto
#    - El resto de servicios y Kong solo VERIFICAN con la PUBLICA. <- publica
#
#  Esto elimina la escalada de privilegios por re-firmado en jwt.io:
#  sin la clave privada nadie puede generar un token valido con otro rol.
#
#  Uso:
#    ./scripts/setup_jwt_keys.sh          # genera claves si no existen
#    ./scripts/setup_jwt_keys.sh --force  # regenera (invalida tokens vigentes)
#
#  Ejecutar SIEMPRE desde la raiz del repo.
# ============================================================
set -euo pipefail

KEYS_DIR="keys"
PRIV="$KEYS_DIR/jwt_private.pem"
PUB="$KEYS_DIR/jwt_public.pem"

FORCE="${1:-}"

mkdir -p "$KEYS_DIR"

if [[ -f "$PRIV" && "$FORCE" != "--force" ]]; then
  echo "==> Ya existen claves en $KEYS_DIR (usa --force para regenerarlas)."
else
  echo "==> Generando par RSA 2048 (RS256)..."
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIV"
  openssl rsa -pubout -in "$PRIV" -out "$PUB" 2>/dev/null
  chmod 600 "$PRIV"
  echo "==> Claves creadas: $PRIV (privada) y $PUB (publica)."
fi

# base64 en una sola linea (compatible Linux/macOS)
b64() { base64 "$1" | tr -d '\n'; }
PUB_B64="$(b64 "$PUB")"

# --- .env para los servicios NestJS (leen JWT_PUBLIC_KEY como base64 del PEM) ---
write_node_env() {
  local file="$1"
  touch "$file"
  # elimina lineas previas de estas variables y las reescribe
  grep -v -E '^(JWT_PUBLIC_KEY|JWT_ISSUER)=' "$file" > "$file.tmp" 2>/dev/null || true
  mv "$file.tmp" "$file"
  {
    echo "JWT_ISSUER=parqueadero"
    echo "JWT_PUBLIC_KEY=$PUB_B64"
  } >> "$file"
  echo "==> Actualizado $file (JWT_PUBLIC_KEY, JWT_ISSUER)."
}

write_node_env "ms-audit/.env"
write_node_env "vehiculos/vehiculos/.env"

cat <<EOF

============================================================
 Listo. Notas de ejecucion:

 - Servicios Spring (usuarios, zonas, asignaciones, tickets):
     Leen las claves por RUTA de archivo por defecto
     (keys/jwt_private.pem / keys/jwt_public.pem), relativa a la
     raiz del repo. Lanzalos desde AQUI, p. ej.:
       ./usuarios/mvnw -f usuarios/pom.xml spring-boot:run

 - Servicios NestJS (vehiculos, ms-audit):
     Ya tienen JWT_PUBLIC_KEY en su .env (base64 del PEM).

 - Kong: la clave publica esta incrustada en gateway/kong.yml.
     Si regeneras con --force, pega el nuevo keys/jwt_public.pem alli.

 - Docker / otros CWD: exporta las claves como base64 del PEM:
     export JWT_PRIVATE_KEY=\$(base64 keys/jwt_private.pem | tr -d '\n')
     export JWT_PUBLIC_KEY=\$(base64 keys/jwt_public.pem | tr -d '\n')
============================================================
EOF
