[CmdletBinding()]
param(
    [string]$ServidorPg = '127.0.0.1',
    [ValidateRange(1, 65535)][int]$PuertoPg = 5432,
    [ValidatePattern('^[A-Za-z_][A-Za-z0-9_]*$')][string]$BaseDatos = 'modeloBiblioteca',
    [string]$UsuarioPg = 'postgres',
    [ValidateRange(1, 65535)][int]$PuertoHttp = 8081,
    [switch]$CargarCatalogoDemostracion,
    [string]$DirectorioJava = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$itspaRaiz = Split-Path $PSScriptRoot -Parent
if ([string]::IsNullOrWhiteSpace($DirectorioJava)) {
    $DirectorioJava = Get-ChildItem -LiteralPath (Join-Path $itspaRaiz '.itspa-tmp/jdk21') -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name | Select-Object -Last 1 -ExpandProperty FullName
}
if ([string]::IsNullOrWhiteSpace($DirectorioJava)) { throw 'Configura JAVA_HOME con JDK 21 o indica -DirectorioJava.' }
$itspaJava = Join-Path $DirectorioJava 'bin/java.exe'
if (-not (Test-Path -LiteralPath $itspaJava)) { throw 'No se encontro java.exe en el JDK indicado.' }
$itspaVersion = & $itspaJava -version 2>&1 | Out-String
if ($itspaVersion -notmatch 'version "21\.') { throw 'Este proyecto requiere JDK 21.' }
$itspaJar = Join-Path $itspaRaiz 'backend/target/biblioteca-modelo-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $itspaJar)) { throw 'Primero genera el JAR desde backend con Maven: mvn clean verify.' }
$itspaVariables = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'SERVER_PORT')
$itspaAnteriores = @{}
foreach ($itspaNombre in $itspaVariables) {
    $itspaAnteriores[$itspaNombre] = [Environment]::GetEnvironmentVariable($itspaNombre, 'Process')
}
try {
    $env:DB_URL = "jdbc:postgresql://${ServidorPg}:${PuertoPg}/$BaseDatos"
    $env:DB_USER = $UsuarioPg
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        $itspaClave = Read-Host 'Contrasena PostgreSQL' -AsSecureString
        $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $itspaClave).Password
        Remove-Variable itspaClave
    }
    $env:SERVER_PORT = [string]$PuertoHttp
    $itspaArgumentos = @('-Duser.timezone=America/Mexico_City', '-jar', $itspaJar)
    if ($CargarCatalogoDemostracion) { $itspaArgumentos += '--biblioteca.demo.enabled=true' }
    & $itspaJava @itspaArgumentos
    $itspaResultado = $LASTEXITCODE
} finally {
    foreach ($itspaNombre in $itspaVariables) {
        [Environment]::SetEnvironmentVariable($itspaNombre, $itspaAnteriores[$itspaNombre], 'Process')
    }
}
exit $itspaResultado
