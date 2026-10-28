[CmdletBinding()]
param([switch]$InstalarDependencias)
$ErrorActionPreference = 'Stop'
$itspaRaiz = Split-Path $PSScriptRoot -Parent
$itspaFrontend = Join-Path $itspaRaiz 'frontend'
$itspaDestino = [IO.Path]::GetFullPath((Join-Path $itspaRaiz 'backend/src/main/resources/static'))
$itspaPermitido = [IO.Path]::GetFullPath((Join-Path $itspaRaiz 'backend/src/main/resources')) + [IO.Path]::DirectorySeparatorChar
if (-not $itspaDestino.StartsWith($itspaPermitido, [StringComparison]::OrdinalIgnoreCase)) { throw 'Destino de interfaz fuera del proyecto.' }
Push-Location $itspaFrontend
try {
    if ($InstalarDependencias -or -not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias de la interfaz.' }
    }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'La interfaz no compiló. No se copiará una versión incompleta.' }
    # Esta carpeta contiene únicamente la salida generada; el código fuente permanece en frontend/src.
    if (Test-Path -LiteralPath $itspaDestino) { Remove-Item -LiteralPath $itspaDestino -Recurse -Force }
    New-Item -ItemType Directory -Path $itspaDestino -Force | Out-Null
    Get-ChildItem -LiteralPath (Join-Path $itspaFrontend 'dist') -Force | Copy-Item -Destination $itspaDestino -Recurse -Force
    Write-Output 'Interfaz preparada. El siguiente empaquetado Maven la incluirá en el JAR.'
} finally { Pop-Location }
