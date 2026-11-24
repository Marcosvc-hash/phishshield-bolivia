# =====================================================================
# Crea el administrador de prueba del Centro de Pruebas QA en la base
# phishshield_qa (NUNCA en la base real) y guarda sus datos en backend\.env.
#
# Uso (desde cualquier carpeta):
#   powershell -ExecutionPolicy Bypass -File C:\proyectos\phishshield-bolivia\backend\herramientas\crear-usuario-qa.ps1
#
# Que hace:
#   1. Genera una contrasena aleatoria de 24 caracteres (no se muestra).
#   2. Calcula su hash BCrypt con herramientas\GenerarHashBcrypt.java.
#   3. Inserta qa.admin@phishshield.bo (rol admin, activo) en phishshield_qa.
#      Si ya existe, le pone la contrasena nueva.
#   4. Escribe QA_BASE_URL, QA_FRONTEND_URL, QA_TEST_USER, QA_TEST_PASSWORD y
#      QA_AMBIENTE en backend\.env (ignorado por git), conservando las demas
#      lineas del archivo.
#
# Este archivo va en ASCII a proposito (PowerShell 5.1 rompe las tildes).
# =====================================================================

$ErrorActionPreference = 'Stop'
$backend = Split-Path -Parent $PSScriptRoot
Set-Location $backend
$correo = 'qa.admin@phishshield.bo'

# 1. Contrasena aleatoria, sin imprimirla
$letras = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
$bytes = New-Object byte[] 24
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$contrasena = -join ($bytes | ForEach-Object { $letras[$_ % $letras.Length] })

# 2. Hash BCrypt (la herramienta lee la contrasena por la entrada estandar)
& .\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt" | Out-Null
$hash = ($contrasena | java -cp (Get-Content target\cp.txt) herramientas\GenerarHashBcrypt.java).Trim()
if (-not $hash.StartsWith('$2')) { throw 'No se pudo calcular el hash BCrypt' }

# 3. Usuario en phishshield_qa
$sql = "INSERT INTO usuarios (nombres, apellidos, email, password_hash, rol, estado, email_verificado) " +
       "VALUES ('Admin', 'de Pruebas QA', '$correo', '$hash', 'admin', 'activo', true) " +
       "ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, estado = 'activo', intentos_fallidos = 0;"
$sql | docker exec -i phishshield-db psql -U phishshield -d phishshield_qa -v ON_ERROR_STOP=1 -q
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el usuario en phishshield_qa' }

# 4. backend\.env: se reemplazan solo las variables QA_*
$nuevas = [ordered]@{
    QA_BASE_URL      = 'http://localhost:8080'
    QA_FRONTEND_URL  = 'http://localhost:4200'
    QA_TEST_USER     = $correo
    QA_TEST_PASSWORD = $contrasena
    QA_AMBIENTE      = 'phishshield_qa'
}
$archivo = Join-Path $backend '.env'
$lineas = @()
if (Test-Path $archivo) {
    $lineas = @(Get-Content $archivo | Where-Object { $_ -notmatch '^\s*(QA_BASE_URL|QA_FRONTEND_URL|QA_TEST_USER|QA_TEST_PASSWORD|QA_AMBIENTE)\s*=' })
} else {
    $lineas = @('# Variables del backend (NO va a git). Ver .env.example.')
}
foreach ($nombre in $nuevas.Keys) { $lineas += "$nombre=$($nuevas[$nombre])" }
Set-Content -Path $archivo -Value $lineas -Encoding ascii

Remove-Variable contrasena, hash, sql
Write-Host "Usuario $correo listo en phishshield_qa. Credenciales guardadas en backend\.env (no se muestran)."
