<#
.SYNOPSIS
Inicia la API y la interfaz juntas; Ctrl+C detiene solo los procesos de esta ejecucion.
.DESCRIPTION
Usa el JAR ya construido, JDK 21 y las dependencias locales del frontend.
No instala paquetes, construye el JAR, cambia autenticacion ni carga datos de demostracion.
La API usa 8081 por defecto; -PuertoApi cambia tambien el destino privado del proxy Vite.
Los logs se guardan en .itspa-tmp (ignorado por Git). Nunca se pasan claves por argumentos.
.EXAMPLE
./scripts/iniciar-sistema.ps1 -Comprobar
.EXAMPLE
./scripts/iniciar-sistema.ps1 -AbrirNavegador
.EXAMPLE
./scripts/iniciar-sistema.ps1 -PuertoApi 8082 -PuertoInterfaz 5174 -Comprobar
.EXAMPLE
./scripts/iniciar-sistema.ps1 -PuertoApi 8082 -PuertoInterfaz 5174 -AbrirNavegador
#>
[CmdletBinding()]
param(
    [switch]$Comprobar,
    [switch]$AbrirNavegador,
    [string]$DirectorioJava = $env:JAVA_HOME,
    [ValidateRange(1, 65535)][int]$PuertoApi = 8081,
    [ValidateRange(1, 65535)][int]$PuertoInterfaz = 5173,
    [ValidateRange(10, 600)][int]$EsperaSegundos = 120
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$itspaRaiz = Split-Path $PSScriptRoot -Parent
$itspaFrontend = Join-Path $itspaRaiz 'frontend'
$itspaJar = Join-Path $itspaRaiz 'backend/target/biblioteca-modelo-0.1.0-SNAPSHOT.jar'
$itspaApiDestino = "http://127.0.0.1:$PuertoApi"
$itspaApiUrl = "$itspaApiDestino/api/app/auth/estado"
$itspaInterfazUrl = "http://127.0.0.1:$PuertoInterfaz"
$itspaHijos = [Collections.Generic.List[Diagnostics.Process]]::new()

function Test-ItspaRespuesta {
    param([string]$Url, [ValidateSet('Api', 'Html')][string]$Tipo)
    try {
        $respuesta = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -MaximumRedirection 0 -ErrorAction Stop
        if ([int]$respuesta.StatusCode -ne 200) { return $false }
        if ($Tipo -eq 'Html') {
            return ($respuesta.Content -match '<title>[^<]*BiblioTECa[^<]*ITSPA[^<]*</title>' -and
                $respuesta.Content -match 'id=["'']root["'']' -and
                [string]$respuesta.Headers['Content-Type'] -match 'text/html')
        }
        $estado = $respuesta.Content | ConvertFrom-Json -ErrorAction Stop
        return ($estado.inicializado -is [bool] -and $estado.accesoDirecto -is [bool] -and
            $null -ne $estado.PSObject.Properties['usuario'] -and
            [string]$respuesta.Headers['Content-Type'] -match 'application/json')
    } catch { return $false }
}

function Test-ItspaPuerto {
    param([int]$Puerto)
    return [bool](@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() |
        Where-Object { $_.Port -eq $Puerto }).Count)
}

function Assert-ItspaDestinoInterfaz {
    # La configuracion del proxy se consulta incluso con la API apagada. Un Vite
    # antiguo u otra terminal no se reinician ni se reconfiguran silenciosamente.
    $destino = $null
    try {
        $respuesta = Invoke-WebRequest -Uri "$itspaInterfazUrl/__itspa_runtime" -UseBasicParsing -TimeoutSec 3 -MaximumRedirection 0 -ErrorAction Stop
        if ([int]$respuesta.StatusCode -eq 200 -and [string]$respuesta.Headers['Content-Type'] -match 'application/json') {
            $configuracion = $respuesta.Content | ConvertFrom-Json -ErrorAction Stop
            if ($configuracion.apiTarget -is [string]) { $destino = $configuracion.apiTarget }
        }
    } catch { $destino = $null }
    if ([string]::IsNullOrWhiteSpace($destino)) {
        throw "La interfaz existente en $PuertoInterfaz no permite comprobar su proxy (/__itspa_runtime). Reinicia la terminal que la sirve para aplicar la configuracion actual o elige otro -PuertoInterfaz. No se detuvo ese servidor."
    }
    if ($destino -cne $itspaApiDestino) {
        throw "La interfaz en $PuertoInterfaz no apunta a la API seleccionada ($itspaApiDestino). Reinicia la terminal que la sirve con ITSPA_API_TARGET correcto o elige otro -PuertoInterfaz. No se detuvo ese servidor."
    }
}

function Start-ItspaHijo {
    param([string]$Ejecutable, [string[]]$Argumentos, [string]$Directorio,
        [string]$Nombre, [hashtable]$Variables = @{})
    # Start-Process hereda el entorno. Restaurarlo inmediatamente evita que la clave
    # del backend llegue al frontend o permanezca en la terminal del lanzador.
    $anteriores = @{}
    $nombres = @(@('DB_PASSWORD') + @($Variables.Keys) | Select-Object -Unique)
    foreach ($nombreVariable in $nombres) {
        $anteriores[$nombreVariable] = [Environment]::GetEnvironmentVariable($nombreVariable, 'Process')
    }
    try {
        [Environment]::SetEnvironmentVariable('DB_PASSWORD', $null, 'Process')
        foreach ($nombreVariable in $Variables.Keys) {
            [Environment]::SetEnvironmentVariable($nombreVariable, [string]$Variables[$nombreVariable], 'Process')
        }
        $proceso = Start-Process -FilePath $Ejecutable -ArgumentList $Argumentos -WorkingDirectory $Directorio `
            -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $itspaLogs "$Nombre.log") `
            -RedirectStandardError (Join-Path $itspaLogs "$Nombre-error.log")
        $itspaHijos.Add($proceso)
        return $proceso
    } finally {
        foreach ($nombreVariable in $nombres) {
            [Environment]::SetEnvironmentVariable($nombreVariable, $anteriores[$nombreVariable], 'Process')
        }
    }
}

function Wait-ItspaRespuesta {
    param([string]$Url, [string]$Tipo, [Diagnostics.Process]$Proceso, [string]$Nombre)
    $limite = [DateTime]::UtcNow.AddSeconds($EsperaSegundos)
    do {
        if ($Proceso.HasExited) { throw "$Nombre termino antes de estar disponible. Revisa los logs locales: $itspaLogs" }
        if (Test-ItspaRespuesta -Url $Url -Tipo $Tipo) { return }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $limite)
    throw "$Nombre no respondio correctamente en $EsperaSegundos segundos. Revisa los logs locales: $itspaLogs"
}

try {
    if ($PuertoInterfaz -eq $PuertoApi) { throw 'Los parametros -PuertoApi y -PuertoInterfaz deben indicar puertos diferentes.' }
    if ([string]::IsNullOrWhiteSpace($DirectorioJava)) {
        $DirectorioJava = Get-ChildItem -LiteralPath (Join-Path $itspaRaiz '.itspa-tmp/jdk21') -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name | Select-Object -Last 1 -ExpandProperty FullName
    }
    if ([string]::IsNullOrWhiteSpace($DirectorioJava)) { throw 'Configura JAVA_HOME con JDK 21 o indica -DirectorioJava.' }
    $itspaJava = Join-Path $DirectorioJava 'bin/java.exe'
    $itspaRelease = Join-Path $DirectorioJava 'release'
    if (-not (Test-Path -LiteralPath $itspaJava -PathType Leaf) -or
        -not (Test-Path -LiteralPath (Join-Path $DirectorioJava 'bin/javac.exe') -PathType Leaf) -or
        -not (Test-Path -LiteralPath $itspaRelease -PathType Leaf) -or
        (Get-Content -LiteralPath $itspaRelease -Raw) -notmatch '(?m)^JAVA_VERSION="21(?:\.|"|-)' ) {
        throw 'Se requiere un JDK 21 completo (java.exe, javac.exe y archivo release).'
    }
    if (-not (Test-Path -LiteralPath $itspaJar -PathType Leaf)) {
        throw 'Falta el JAR. Genera backend/target/biblioteca-modelo-0.1.0-SNAPSHOT.jar con Maven antes de iniciar.'
    }
    $itspaNode = Get-Command node.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source
    if (-not $itspaNode) { throw 'Falta Node.js en PATH. Vite 7 requiere Node 20.19+ o 22.12+.' }
    $itspaPaquete = Get-Content -LiteralPath (Join-Path $itspaFrontend 'package.json') -Raw | ConvertFrom-Json
    $itspaFaltantes = @()
    foreach ($itspaDependencia in @($itspaPaquete.dependencies.PSObject.Properties) + @($itspaPaquete.devDependencies.PSObject.Properties)) {
        if (-not (Test-Path -LiteralPath (Join-Path $itspaFrontend "node_modules/$($itspaDependencia.Name)/package.json") -PathType Leaf)) {
            $itspaFaltantes += $itspaDependencia.Name
        }
    }
    $itspaVite = Join-Path $itspaFrontend 'node_modules/vite/bin/vite.js'
    if ($itspaFaltantes.Count -gt 0 -or -not (Test-Path -LiteralPath $itspaVite -PathType Leaf)) {
        throw "Faltan dependencias del frontend. Ejecuta npm ci dentro de frontend. Paquetes pendientes: $($itspaFaltantes -join ', ')"
    }

    $itspaApiLista = Test-ItspaRespuesta -Url $itspaApiUrl -Tipo Api
    $itspaInterfazLista = Test-ItspaRespuesta -Url $itspaInterfazUrl -Tipo Html
    if (-not $itspaApiLista -and (Test-ItspaPuerto -Puerto $PuertoApi)) {
        throw "Puerto $PuertoApi ocupado, pero no responde la API esperada. Elige otro -PuertoApi o revisa el programa que lo utiliza; este lanzador no lo detendra."
    }
    if (-not $itspaInterfazLista -and (Test-ItspaPuerto -Puerto $PuertoInterfaz)) {
        throw "Puerto $PuertoInterfaz ocupado, pero no responde el HTML de BiblioTECa. Revisa el programa que lo utiliza; este lanzador no lo detendra."
    }
    if ($itspaInterfazLista) { Assert-ItspaDestinoInterfaz }
    Write-Host 'JDK 21, JAR, Node.js y archivos de dependencias: disponibles.'
    Write-Host "API ${PuertoApi}: $(if ($itspaApiLista) { 'disponible; se reutilizara' } else { 'detenida; requiere arranque' })."
    Write-Host "Interfaz ${PuertoInterfaz}: $(if ($itspaInterfazLista) { 'disponible; se reutilizara' } else { 'detenida; requiere arranque' })."
    if ($Comprobar) {
        Write-Host 'Comprobacion terminada: no se solicitaron credenciales ni se iniciaron procesos.'
        Write-Host 'La version efectiva de Node se verificara al iniciar; debe ser 20.19+ o 22.12+.'
        return
    }

    $itspaVersionNode = (& $itspaNode --version | Out-String).Trim().TrimStart('v')
    if ($LASTEXITCODE -ne 0 -or $itspaVersionNode -notmatch '^\d+\.\d+\.\d+') { throw 'No se pudo comprobar la version de Node.js.' }
    $itspaVersion = [version]($Matches[0])
    if (-not (($itspaVersion.Major -eq 20 -and $itspaVersion.Minor -ge 19) -or $itspaVersion -ge [version]'22.12.0')) {
        throw 'La version instalada de Node no es compatible: usa Node 20.19+ o 22.12+.'
    }
    $itspaLogs = Join-Path $itspaRaiz ('.itspa-tmp/inicio-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Path $itspaLogs -Force | Out-Null
    Write-Host "Logs de esta ejecucion: $itspaLogs"
    if (-not $itspaApiLista) {
        $itspaClave = $null
        $itspaVariables = @{}
        try {
            $itspaClave = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'Process')
            if ([string]::IsNullOrWhiteSpace($itspaClave)) {
                $itspaSegura = Read-Host 'Contrasena PostgreSQL (no se guarda)' -AsSecureString
                try { $itspaClave = [Net.NetworkCredential]::new('', $itspaSegura).Password } finally { $itspaSegura.Dispose(); Remove-Variable itspaSegura }
            }
            if ([string]::IsNullOrWhiteSpace($itspaClave)) { throw 'No se proporciono DB_PASSWORD. No se iniciara la API.' }
            # DB_URL/DB_USER heredados conservan la configuracion existente de application.yml.
            $itspaVariables = @{ DB_PASSWORD = $itspaClave; SERVER_PORT = [string]$PuertoApi; SERVER_ADDRESS = '127.0.0.1' }
            Write-Host 'Iniciando API. Esperando conexion real con PostgreSQL...'
            $itspaBackend = Start-ItspaHijo -Ejecutable $itspaJava -Argumentos @('-Duser.timezone=America/Mexico_City', '-jar', ('"' + $itspaJar + '"')) `
                -Directorio (Join-Path $itspaRaiz 'backend') -Nombre 'backend' -Variables $itspaVariables
        } finally {
            $itspaClave = $null
            $itspaVariables.Clear()
            Remove-Variable itspaClave, itspaVariables -ErrorAction SilentlyContinue
        }
        Wait-ItspaRespuesta -Url $itspaApiUrl -Tipo Api -Proceso $itspaBackend -Nombre 'La API'
    }
    if (-not $itspaInterfazLista) {
        Write-Host 'API disponible. Iniciando interfaz...'
        $itspaServidorVite = Start-ItspaHijo -Ejecutable $itspaNode -Argumentos @(('"' + $itspaVite + '"'), '--host', '127.0.0.1', '--port', [string]$PuertoInterfaz, '--strictPort') `
            -Directorio $itspaFrontend -Nombre 'frontend' -Variables @{ ITSPA_API_TARGET = $itspaApiDestino }
        Wait-ItspaRespuesta -Url $itspaInterfazUrl -Tipo Html -Proceso $itspaServidorVite -Nombre 'La interfaz'
    }
    Assert-ItspaDestinoInterfaz
    if (-not (Test-ItspaRespuesta -Url "$itspaInterfazUrl/api/app/auth/estado" -Tipo Api)) {
        throw "La interfaz responde, pero su proxy no llega a la API $PuertoApi. Revisa frontend/vite.config.ts."
    }
    Write-Host "Sistema disponible: $itspaInterfazUrl"
    Write-Host 'Conserva esta terminal abierta. Ctrl+C detiene solo los procesos creados aqui.'
    if ($AbrirNavegador) { Start-Process -FilePath $itspaInterfazUrl | Out-Null }
    while ($true) {
        foreach ($itspaHijo in $itspaHijos) {
            if ($itspaHijo.HasExited) { throw "Un proceso del sistema termino. Revisa los logs locales: $itspaLogs" }
        }
        Start-Sleep -Seconds 1
    }
} finally {
    for ($itspaIndice = $itspaHijos.Count - 1; $itspaIndice -ge 0; $itspaIndice--) {
        $itspaHijo = $itspaHijos[$itspaIndice]
        try {
            if (-not $itspaHijo.HasExited) {
                # El objeto Process conserva la identidad del hijo; nunca se seleccionan
                # procesos por nombre ni puertos. /T incluye el esbuild creado por Vite.
                & "$env:SystemRoot/System32/taskkill.exe" /PID $itspaHijo.Id /T /F 2>&1 | Out-Null
            }
        } catch { Write-Warning 'No se pudo detener uno de los procesos propios. Revisa su estado en el Administrador de tareas.' }
        finally { $itspaHijo.Dispose() }
    }
}
