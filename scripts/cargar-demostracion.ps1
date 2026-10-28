[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080'
)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

# M02, RF-M02-01/26/27/30 y RN-M02-03/04, pp. 16-20 / visor 17-21.
# Registro academico del README; se guarda por services, con sus validaciones.
# Repetir el script conserva las filas existentes y no crea duplicados.
$itspaIsbn = '9780000000019'
$itspaLibros = Invoke-RestMethod -Uri "$BaseUrl/api/v1/libros"
$itspaLibro = $itspaLibros | Where-Object isbn -EQ $itspaIsbn | Select-Object -First 1
if (-not $itspaLibro) {
    $itspaLibro = Invoke-RestMethod -Method POST -Uri "$BaseUrl/api/v1/libros" -ContentType 'application/json; charset=utf-8' -Body (@{
        titulo = 'Fundamentos de Programacion'
        isbn = $itspaIsbn
        edicion = 'Primera'
        anioPublicacion = 2025
        numPaginas = 320
        descripcion = 'Registro de demostracion academica; no representa inventario institucional real'
    } | ConvertTo-Json)
}
if ($itspaLibro.descripcion -ne 'Registro de demostracion academica; no representa inventario institucional real') {
    throw 'El ISBN ya pertenece a otro registro; revisar antes de agregar ejemplares de demostracion.'
}
$itspaEjemplares = Invoke-RestMethod -Uri "$BaseUrl/api/v1/ejemplares?libroId=$($itspaLibro.id)"
foreach ($itspaCodigo in @('ITSPA-PRACTICA-001', 'ITSPA-PRACTICA-002')) {
    if (-not ($itspaEjemplares | Where-Object codigoBarras -EQ $itspaCodigo)) {
        $null = Invoke-RestMethod -Method POST -Uri "$BaseUrl/api/v1/ejemplares" -ContentType 'application/json; charset=utf-8' -Body (@{
            libroId = $itspaLibro.id
            codigoBarras = $itspaCodigo
            estadoConservacion = 'BUENO'
        } | ConvertTo-Json)
    }
}
$itspaLibro
Invoke-RestMethod -Uri "$BaseUrl/api/v1/ejemplares?libroId=$($itspaLibro.id)"
