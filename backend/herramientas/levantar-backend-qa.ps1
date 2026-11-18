# =====================================================================
# Levanta el backend conectado a la base phishshield_qa (demo del
# Centro de Pruebas QA). La base real phishshield no se toca.
#
# Uso (desde cualquier carpeta):
#   powershell -ExecutionPolicy Bypass -File C:\proyectos\phishshield-bolivia\backend\herramientas\levantar-backend-qa.ps1
#
# Que hace:
#   1. Lee backend\.env si existe (lineas NOMBRE=valor; # es comentario).
#      Ahi van QA_TEST_USER, QA_TEST_PASSWORD y MAIL_*. Ver .env.example.
#   2. DB_PASSWORD: si no esta definida, la toma del contenedor phishshield-db.
#   3. JWT_SECRET: si no esta definida, genera una al azar (las sesiones
#      abiertas se pierden al reiniciar el backend).
#   4. DB_URL apunta siempre a phishshield_qa.
#   5. Arranca el backend con .\mvnw.cmd spring-boot:run (Ctrl+C para parar).
#
# Nunca muestra el valor de una variable: solo dice si esta definida.
# Este archivo va en ASCII a proposito (PowerShell 5.1 rompe las tildes).
# =====================================================================

$ErrorActionPreference = 'Stop'
$backend = Split-Path -Parent $PSScriptRoot
Set-Location $backend

# 1. Variables de backend\.env
$archivoEnv = Join-Path $backend '.env'
if (Test-Path $archivoEnv) {
    foreach ($linea in Get-Content $archivoEnv) {
        if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            $valor = $Matches[2]
            # Quita comillas que envuelvan el valor: CLAVE="abc" -> abc
            if ($valor -match '^"(.*)"$' -or $valor -match "^'(.*)'$") { $valor = $Matches[1] }
            Set-Item -Path "env:$($Matches[1])" -Value $valor
        }
    }
    Write-Host "Variables leidas de backend\.env"
} else {
    Write-Host "No existe backend\.env (copie .env.example y complete sus valores)"
}

# 2. Contrasena de la base, sin imprimirla
if (-not $env:DB_PASSWORD) {
    $env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)
    if (-not $env:DB_PASSWORD) { throw 'No se pudo leer POSTGRES_PASSWORD del contenedor phishshield-db. Esta levantado? (docker ps)' }
}

# 3. Secreto del JWT al azar si no se definio
if (-not $env:JWT_SECRET) {
    $bytes = New-Object byte[] 48
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
}

# 4. Siempre la base de pruebas
$env:DB_URL = 'jdbc:postgresql://localhost:5432/phishshield_qa'

# Resumen sin valores
Write-Host ''
Write-Host 'Base de datos : phishshield_qa'
foreach ($nombre in 'DB_PASSWORD', 'JWT_SECRET', 'QA_TEST_USER', 'QA_TEST_PASSWORD', 'MAIL_HOST', 'MAIL_PORT', 'MAIL_USERNAME', 'MAIL_PASSWORD') {
    $definida = if ([Environment]::GetEnvironmentVariable($nombre)) { 'definida' } else { 'NO definida' }
    Write-Host ('{0,-14}: {1}' -f $nombre, $definida)
}
Write-Host ''

# 5. Arrancar
& .\mvnw.cmd spring-boot:run
