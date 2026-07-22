# ============================================================
#  detener.ps1  ·  Detiene todo lo que arranco iniciar.ps1
#  (Windows · PowerShell)
# ------------------------------------------------------------
#  Uso:
#     .\detener.ps1          # detiene servicios + Kong (conserva datos)
#     .\detener.ps1 -Wipe    # ademas BORRA los datos de la BD (down -v)
# ============================================================
param([switch]$Wipe)
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host ">> deteniendo microservicios y frontend ..." -ForegroundColor Cyan
# 1) por PIDs de las ventanas que abrio iniciar.ps1
if (Test-Path "$Root\logs\pids.txt") {
  Get-Content "$Root\logs\pids.txt" | ForEach-Object {
    if ($_) { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
  }
  Remove-Item "$Root\logs\pids.txt" -ErrorAction SilentlyContinue
}
# 2) respaldo: por puerto (procesos que quedaron escuchando)
foreach ($port in 8081,8080,8082,8083,3000,3002,5173) {
  $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
  foreach ($conn in $c) { Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue }
}

Write-Host ">> deteniendo Kong ..." -ForegroundColor Cyan
Push-Location "$Root\gateway"; docker compose down; Pop-Location

if ($Wipe) {
  Write-Host ">> deteniendo y BORRANDO datos de PostgreSQL/RabbitMQ ..." -ForegroundColor Yellow
  docker compose down -v
} else {
  Write-Host ">> deteniendo PostgreSQL/RabbitMQ (conserva datos) ..." -ForegroundColor Cyan
  docker compose down
}

Write-Host ">> listo. Todo detenido." -ForegroundColor Green
