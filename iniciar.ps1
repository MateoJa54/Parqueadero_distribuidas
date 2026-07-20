# ============================================================
#  iniciar.ps1  ·  Arranque COMPLETO del sistema de parqueadero
#  (Windows · PowerShell)
# ------------------------------------------------------------
#  Deja todo listo para probar en un solo comando:
#    1. Verifica requisitos (Java 25, Node, Docker, Python).
#    2. Genera las claves JWT (RS256) si no existen.
#    3. Levanta PostgreSQL + RabbitMQ + Kong (Docker).
#    4. Compila y arranca los 6 microservicios (ventanas aparte).
#    5. Arranca el frontend (Vite).
#    6. Carga datos de demostracion (usuarios QA + catalogo).
#
#  Como ejecutar (PowerShell, en la raiz del repo):
#     Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass   # solo esta sesion
#     .\iniciar.ps1                # arranca todo y siembra datos
#     .\iniciar.ps1 -NoSeed        # arranca todo SIN sembrar datos
#     .\iniciar.ps1 -NoFront       # no arranca el frontend
#
#  Requisito: define JAVA_HOME apuntando al JDK 25 antes de correr.
#  Para detener todo:  .\detener.ps1
# ============================================================
param([switch]$NoSeed, [switch]$NoFront)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
New-Item -ItemType Directory -Force -Path "$Root\logs" | Out-Null

function Info($m){ Write-Host $m -ForegroundColor Cyan }
function Ok($m){ Write-Host $m -ForegroundColor Green }
function Warn($m){ Write-Host $m -ForegroundColor Yellow }
function Err($m){ Write-Host $m -ForegroundColor Red }

function Need($cmd){ if(-not (Get-Command $cmd -ErrorAction SilentlyContinue)){ Err "Falta '$cmd'. Instalalo y reintenta."; exit 1 } }

# --- 0) Requisitos -----------------------------------------------------------
Need docker; Need node; Need npm
$Py = if (Get-Command python -ErrorAction SilentlyContinue) { "python" } elseif (Get-Command py -ErrorAction SilentlyContinue) { "py" } else { Err "Falta Python 3."; exit 1 }

if (-not $env:JAVA_HOME) { Err "Define JAVA_HOME apuntando al JDK 25 (Panel de control > Variables de entorno)."; exit 1 }
$jv = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Select-Object -First 1
if ($jv -notmatch '"25') { Warn "OJO: JAVA_HOME no parece JDK 25 -> $jv" }
Ok "JAVA_HOME = $env:JAVA_HOME"

# --- helpers -----------------------------------------------------------------
function Start-Svc($name, $dir, $cmd) {
  Info ">> arrancando $name ..."
  $full = "Set-Location '$Root\$dir'; `$env:JAVA_HOME='$env:JAVA_HOME'; $cmd"
  $p = Start-Process powershell -ArgumentList "-NoExit","-Command",$full -PassThru
  Add-Content "$Root\logs\pids.txt" $p.Id
}
function Wait-Port($port, $name, $max = 120) {
  Write-Host ("   esperando {0} (:{1}) " -f $name,$port) -NoNewline
  for ($i=0; $i -lt $max; $i++) {
    $r = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue
    if ($r.TcpTestSucceeded) { Ok " OK"; return $true }
    Write-Host "." -NoNewline; Start-Sleep 1
  }
  Err (" timeout en {0} (:{1})" -f $name,$port); return $false
}

Remove-Item "$Root\logs\pids.txt" -ErrorAction SilentlyContinue

# --- 1) Claves JWT -----------------------------------------------------------
if (-not (Test-Path "$Root\keys\jwt_private.pem")) {
  Info ">> generando claves JWT (RS256) ..."
  if (Get-Command bash -ErrorAction SilentlyContinue) {
    bash scripts/setup_jwt_keys.sh
  } elseif (Get-Command openssl -ErrorAction SilentlyContinue) {
    New-Item -ItemType Directory -Force -Path "$Root\keys" | Out-Null
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$Root\keys\jwt_private.pem"
    openssl rsa -pubout -in "$Root\keys\jwt_private.pem" -out "$Root\keys\jwt_public.pem" 2>$null
    $pubB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$Root\keys\jwt_public.pem"))
    foreach ($f in @("ms-audit\.env","vehiculos\vehiculos\.env")) {
      $path = "$Root\$f"
      "JWT_ISSUER=parqueadero`nJWT_PUBLIC_KEY=$pubB64" | Set-Content -Encoding ascii $path
    }
    Ok "Claves generadas con openssl."
  } else {
    Err "No hay 'bash' ni 'openssl'. Instala Git para Windows (trae ambos) y reintenta."; exit 1
  }
} else {
  Ok "Claves JWT ya existen (keys/)."
}

# --- 2) Infraestructura Docker ----------------------------------------------
Info ">> levantando PostgreSQL + RabbitMQ ..."
docker compose up -d
Wait-Port 5433 "postgres" 60 | Out-Null
Wait-Port 5672 "rabbitmq" 60 | Out-Null

Info ">> levantando Kong (gateway) ..."
Push-Location "$Root\gateway"; docker compose up -d; Pop-Location
Wait-Port 8000 "kong" 60 | Out-Null

# --- 3) Frontend .env --------------------------------------------------------
if (-not (Test-Path "$Root\frontend\.env")) { Copy-Item "$Root\frontend\.env.example" "$Root\frontend\.env" }

# --- 4) Microservicios -------------------------------------------------------
Start-Svc "usuarios"     "usuarios"     ".\mvnw.cmd -DskipTests spring-boot:run"
Start-Svc "zonas"        "zonas"        ".\mvnw.cmd -DskipTests spring-boot:run"
Start-Svc "asignaciones" "asignaciones" ".\mvnw.cmd -DskipTests spring-boot:run"
Start-Svc "tickets"      "tickets"      ".\mvnw.cmd -DskipTests spring-boot:run"

Info ">> instalando dependencias Node (vehiculos, ms-audit) ..."
Push-Location "$Root\vehiculos\vehiculos"; npm install --silent; Pop-Location
Push-Location "$Root\ms-audit"; npm install --silent; Pop-Location
Start-Svc "vehiculos" "vehiculos\vehiculos" "npm run start:dev"
Start-Svc "ms-audit"  "ms-audit"            "npm run start:dev"

Wait-Port 8081 "usuarios" | Out-Null
Wait-Port 8080 "zonas" | Out-Null
Wait-Port 8082 "asignaciones" | Out-Null
Wait-Port 8083 "tickets" | Out-Null
Wait-Port 3000 "vehiculos" | Out-Null
Wait-Port 3002 "ms-audit" | Out-Null

# --- 5) Datos de demostracion ------------------------------------------------
if (-not $NoSeed) {
  Info ">> sembrando datos (esto pasa por Kong) ..."
  Start-Sleep 3
  & $Py "$Root\scripts\seed_usuarios_roles.py"
  & $Py "$Root\scripts\seed_datos.py"
}

# --- 6) Frontend -------------------------------------------------------------
if (-not $NoFront) {
  Info ">> instalando dependencias del frontend ..."
  Push-Location "$Root\frontend"; npm install --silent; Pop-Location
  Start-Svc "frontend" "frontend" "npm run dev"
  Wait-Port 5173 "frontend" 60 | Out-Null
}

Write-Host ""
Ok "==================== TODO ARRIBA ===================="
Write-Host "  Frontend .......... http://localhost:5173"
Write-Host "  Gateway (Kong) .... http://localhost:8000"
Write-Host "  RabbitMQ panel .... http://localhost:15672  (guest/guest)"
Write-Host ""
Write-Host "  Cuentas de prueba (usuario / contrasena -> rol):"
Write-Host "    root         / Root2025       -> ROOT (super admin)"
Write-Host "    qa.admin     / QaAdmin2025    -> ADMIN"
Write-Host "    qa.recauda   / QaRecauda2025  -> RECAUDADOR"
Write-Host "    qa.cliente   / QaCliente2025  -> CLIENTE"
Write-Host "    qa.invitado  / QaInvitado2025 -> INVITADO"
Write-Host ""
Write-Host "  Cada servicio abrio su propia ventana.  Detener todo:  .\detener.ps1"
Ok "====================================================="
