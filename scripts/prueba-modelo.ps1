[CmdletBinding(DefaultParameterSetName = 'Completa')]
param(
    [Parameter(ParameterSetName = 'Metadatos')][switch]$SoloMetadatos,
    [Parameter(ParameterSetName = 'SinBD')][switch]$SinBD,
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { throw 'Configura JAVA_HOME con JDK 21.' }
$itspaJava = Join-Path $env:JAVA_HOME 'bin/java.exe'
if (-not (Test-Path -LiteralPath $itspaJava)) { throw 'Configura JAVA_HOME con JDK 21.' }
$itspaJavaVersion = & $itspaJava -version 2>&1 | Out-String
if ($itspaJavaVersion -notmatch 'version "21\.') { throw 'Este proyecto requiere JDK 21.' }
$itspaMavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($itspaMavenCommand) { $itspaMaven = $itspaMavenCommand.Source }
else {
    $itspaWrapperCache = Join-Path $env:USERPROFILE '.m2/wrapper/dists'
    $itspaMaven = Get-ChildItem -LiteralPath $itspaWrapperCache -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName | Select-Object -Last 1 -ExpandProperty FullName
}
if (-not $itspaMaven) { throw 'Instala Maven 3.9+ o agrega mvn.cmd al PATH.' }
$itspaArguments = @('--batch-mode', '--no-transfer-progress', 'clean', 'test')
if ($SoloMetadatos) { $itspaArguments += '-Dtest=ModeloMetadatosTest,GuardiaBasePruebasTest' }
if ($SinBD) { $itspaArguments += '-Dtest=*Test,!*IntegracionTest' }
if ($Offline) { $itspaArguments += '-o' }
$itspaExit = 1
Push-Location (Join-Path $PSScriptRoot '../backend')
try { & $itspaMaven @itspaArguments; $itspaExit = $LASTEXITCODE }
finally { Pop-Location }
exit $itspaExit
