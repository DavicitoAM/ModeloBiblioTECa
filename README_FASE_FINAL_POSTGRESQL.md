# Fase final: clonar, levantar y probar el proyecto con PostgreSQL

Esta guía corresponde exclusivamente al **nuevo Sistema Integral de Gestión de Biblioteca del ITSPA**. Permite preparar otra máquina, crear las bases vacías, aplicar el esquema y comprobar el recorrido funcional de **Libro–Ejemplar**.

El proyecto contiene el modelo de 70 entidades y nueve enums, con 77 tablas de negocio declaradas. Los servicios y la API disponibles son los de Libro y Ejemplar. Los demás módulos tienen modelo persistente preparado; sus operaciones de negocio requieren desarrollo posterior. Levantar este backend no equivale a tener todos los módulos del sistema funcionando.

**Estado de partida, 15 de octubre de 2026:** compilaron las fuentes de producción y pruebas; pasaron 66 casos sin BD mediante el motor JUnit local. La integración PostgreSQL y el arranque con persistencia real siguen pendientes. En la máquina de origen, Maven encontró dependencias incompletas de Surefire y Clean; no se debe presentar `mvn test` como exitoso hasta obtener su resultado real.

Fuentes: M02, RF-M02-01/06/07/26/27/30 y RN-M02-03/04/11/12, páginas impresas 16–20 / visor 17–21; RNF-MAN-07, impresa 81 / visor 82. La arquitectura y decisiones están en [README.md](README.md), [AGENTS.md](AGENTS.md) y [DECISIONES_PENDIENTES.md](documentacion/DECISIONES_PENDIENTES.md).

## 1. Qué hace cada archivo de preparación

| Archivo | Función |
|---|---|
| [scripts/crear_bases_piloto.sql](scripts/crear_bases_piloto.sql) | Crea `biblioteca_modelo` y `biblioteca_modelo_test` vacías; se ejecuta expresamente mediante PostgreSQL |
| [V1](backend/src/main/resources/db/migration/V1__piloto_libros_ejemplares.sql) y [V2](backend/src/main/resources/db/migration/V2__modelo_integral_itspa.sql) | Flyway las aplica automáticamente al iniciar el contexto de integración o arrancar la aplicación; construyen tablas, restricciones y relaciones |
| [application.yml](backend/src/main/resources/application.yml) | Configura la base de la aplicación, Flyway y Hibernate `validate` |
| [application-test.yml](backend/src/test/resources/application-test.yml) | Configura la base separada para integración |
| [scripts/prueba-modelo.ps1](scripts/prueba-modelo.ps1) | Ejecuta Maven; sin opciones incluye integración, con `-SinBD` la excluye |
| [scripts/verificar_esquema.sql](scripts/verificar_esquema.sql) | Consulta la base efectiva, historial Flyway, tablas, FK y relación Libro–Ejemplar |

**La aplicación no crea por sí misma la base de datos ni instala PostgreSQL.** Primero se prepara el servidor y se ejecuta el script de bases; después Flyway crea el esquema dentro de la base correspondiente. No hay que ejecutar V1/V2 manualmente.

## 2. Requisitos en la nueva máquina

- Git.
- JDK **21**; `JAVA_HOME` debe apuntar al JDK y Maven debe usar esa misma versión.
- Maven **3.9.x** instalado y disponible en `PATH`. Este repositorio no incluye Maven Wrapper.
- PostgreSQL instalado, con el servicio activo y la herramienta `psql` accesible. pgAdmin es opcional.
- Un usuario PostgreSQL que pueda crear las dos bases y sea propietario de sus objetos. Si otro administrador las crea, debe conceder al usuario de ejecución los permisos necesarios sobre ellas.
- Acceso a los repositorios Maven y permiso de escritura en la caché local `.m2` para resolver las dependencias declaradas. La primera ejecución en una máquina nueva necesita descargarlas.

Los ejemplos principales usan **PowerShell en Windows**, servidor local `127.0.0.1`, puerto `5432` y API en `8080`. Si cambias servidor o puerto, usa los mismos valores en PostgreSQL y las variables indicadas. Las rutas de archivos son relativas al clon; no dependen de la carpeta del equipo de origen.

## 3. Clonar y comprobar herramientas

Publica previamente el contenido de este proyecto en tu repositorio remoto. En la nueva máquina, sustituye `URL_REAL_DE_TU_REPOSITORIO` por su URL:

```powershell
git clone URL_REAL_DE_TU_REPOSITORIO biblioteca-itspa
Set-Location biblioteca-itspa

java -version
mvn -version
psql --version
```

Comprueba que `mvn -version` informe Java 21. Si `psql` no se reconoce, agrega la carpeta `bin` de tu instalación PostgreSQL al `PATH` o utiliza la ruta completa del ejecutable. Si falta `JAVA_HOME`, configúralo con la ubicación real de tu JDK 21.

En la raíz del clon deben existir `AGENTS.md`, `backend/pom.xml`, `scripts/` y `documentacion/`, incluidas las fuentes académicas. Se versionan fuentes, migraciones y documentación. `backend/target/`, cachés, credenciales, `.env`, `.itspa-tmp/` y archivos `.log` están excluidos por `.gitignore`; los builds e informes locales se regeneran en cada equipo. El informe histórico de pruebas en `.txt` sí puede acompañar al repositorio. La aplicación lee variables de entorno; no carga automáticamente un archivo `.env`.

## 4. Crear las dos bases vacías

Abre una terminal PowerShell en la raíz del clon y conserva esa terminal para los pasos de configuración, pruebas y arranque:

```powershell
$itspaHostPg = '127.0.0.1'
$itspaPuertoPg = 5432
$itspaUsuarioPg = Read-Host 'Usuario PostgreSQL para esta practica'

psql -h $itspaHostPg -p $itspaPuertoPg -U $itspaUsuarioPg -W -d postgres -v ON_ERROR_STOP=1 -f ./scripts/crear_bases_piloto.sql
if ($LASTEXITCODE -ne 0) { throw 'Revisar la creacion de las bases antes de continuar.' }
```

`psql` solicitará la contraseña. El script crea las bases con el usuario conectado como propietario. Se espera una confirmación `CREATE DATABASE` por cada base.

El script **no es idempotente**: si una base ya existe, se detiene con error. Revisa si pertenece a este proyecto y prepara únicamente la base faltante con la sentencia correspondiente del script. No borres una base existente ni importes datos o esquemas de versiones anteriores. No uses la opción de transacción única `psql -1` para crear bases.

Alternativa con pgAdmin: abre Query Tool sobre la base administrativa `postgres`, activa autocommit y ejecuta **cada `CREATE DATABASE` por separado**, sin un bloque `BEGIN/COMMIT`. Comprueba que ambas bases aparezcan antes de seguir.

## 5. Configurar conexión sin guardar contraseñas en archivos

En la misma terminal del paso anterior:

```powershell
$env:DB_URL = "jdbc:postgresql://${itspaHostPg}:${itspaPuertoPg}/biblioteca_modelo"
$env:DB_TEST_URL = "jdbc:postgresql://${itspaHostPg}:${itspaPuertoPg}/biblioteca_modelo_test"
$env:DB_USER = $itspaUsuarioPg
$env:DB_TEST_USER = $itspaUsuarioPg

$itspaClave = Read-Host 'Contrasena PostgreSQL del usuario indicado' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $itspaClave).Password
$env:DB_TEST_PASSWORD = $env:DB_PASSWORD
Remove-Variable -Name itspaClave

$env:SERVER_PORT = '8080'
```

Este ejemplo usa el mismo usuario propietario para ambas bases locales; siguen siendo bases distintas. Si utilizas usuarios separados, configura `DB_TEST_USER` y `DB_TEST_PASSWORD` con sus credenciales reales y permisos correspondientes.

Las variables pertenecen a esta terminal y sus procesos hijos. Al abrir otra terminal para Maven o para ejecutar el JAR, hay que establecerlas allí de nuevo. No muestres las contraseñas en capturas ni las guardes en Git.

La integración exige que el destino se llame exactamente `biblioteca_modelo_test` y que sea diferente del destino de `DB_URL`. La guardia también rechaza destinos alternativos de Flyway, Hikari o JNDI. Si una configuración heredada provoca un rechazo, revisa su origen antes de continuar; conserva la guardia.

## 6. Comprobar primero los casos sin BD

Desde la raíz del clon:

```powershell
./scripts/prueba-modelo.ps1 -SinBD
if ($LASTEXITCODE -ne 0) { throw 'Resolver el fallo de la suite sin BD antes de continuar.' }
```

Alternativa equivalente con Maven instalado:

```powershell
mvn -f backend/pom.xml '-Dtest=*Test,!*IntegracionTest' clean test
```

Con el código actual se esperan **66 casos**, sin fallos ni omisiones. Este paso no inicia Flyway ni abre conexiones JDBC. En la primera ejecución permite que Maven resuelva sus dependencias; usa `-Offline` únicamente cuando toda la caché necesaria ya esté disponible.

Si Maven no puede cargar Surefire o Clean, resuelve el acceso a dependencias y los permisos de caché del equipo. No cambies versiones para ocultar el problema. La [guía sin BD](documentacion/PRUEBAS_SIN_BD.md) documenta la ejecución local alternativa, pero esta no acredita una integración PostgreSQL exitosa.

## 7. Ejecutar la integración real con PostgreSQL

Comprueba que el servicio PostgreSQL esté activo y que las variables del paso 5 sigan presentes. Desde la raíz:

```powershell
./scripts/prueba-modelo.ps1
if ($LASTEXITCODE -ne 0) { throw 'La integracion no esta aprobada; revisar el informe Maven.' }
```

Alternativa equivalente:

```powershell
mvn -f backend/pom.xml clean test
```

Usa una de las dos alternativas. No pases `-SinBD`, `-SoloMetadatos` ni opciones que omitan tests en esta fase.

El perfil `test` está declarado en la clase de integración. La ejecución debe:

1. Comprobar el destino antes de Flyway.
2. Aplicar V1 y V2 en `biblioteca_modelo_test`.
3. Iniciar Hibernate con `ddl-auto=validate` para las 70 entidades.
4. Probar repositorios y services de Libro–Ejemplar, recarga de relaciones, ISBN opcional, duplicados, libro inexistente, FK, unicidad y estado permitido.
5. Comprobar que los registros transaccionales de las pruebas se revierten.

Con el código actual se esperan **77 casos en total: 66 sin BD y 11 de integración**, con cero fallos, errores u omisiones y `BUILD SUCCESS`. Los informes deben aparecer en `backend/target/surefire-reports/`; comprueba que incluyan `ModeloPersistenciaIntegracionTest` con sus 11 casos. Un build que omite esa clase no aprueba esta fase.

## 8. Verificar el esquema creado en la base de pruebas

Desde la raíz:

```powershell
psql -h $itspaHostPg -p $itspaPuertoPg -U $itspaUsuarioPg -W -d biblioteca_modelo_test -v ON_ERROR_STOP=1 -f ./scripts/verificar_esquema.sql
if ($LASTEXITCODE -ne 0) { throw 'Revisar la verificacion del esquema.' }
```

También puedes abrir ese archivo en pgAdmin, conectado expresamente a `biblioteca_modelo_test`. Comprueba:

- Base efectiva: `biblioteca_modelo_test`.
- Historial Flyway: versiones `1` y `2` exitosas.
- **77 tablas de negocio**, más `flyway_schema_history` como tabla técnica adicional, en el esquema `public` de la base nueva.
- FK de `ejemplares.libro_id` hacia `libros.id` y las demás relaciones del modelo.

El `JOIN` final puede devolver cero filas: los datos transaccionales del test se revierten. Flyway conserva las tablas y el historial, y las secuencias pueden haber avanzado. Para observar registros persistentes utiliza la demostración siguiente en la base de la aplicación.

## 9. Arrancar la aplicación con su propia base

En la terminal donde configuraste las variables, desde la raíz:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

No actives el perfil `test` para esta demostración. La aplicación debe conectar a `biblioteca_modelo`, aplicar allí V1/V2, validar el modelo y permanecer atendiendo en `http://localhost:8080`. Si configuraste otro `SERVER_PORT`, ajusta las URL de la demostración.

Conserva esta terminal abierta. Es un backend REST: no incluye una interfaz web ni una pantalla de inicio. Comprueba los endpoints `/api/v1/libros` y `/api/v1/ejemplares`.

## 10. Registrar y consultar datos reales por HTTP

Abre una **segunda terminal PowerShell**. No necesita las credenciales de BD para estas solicitudes HTTP:

```powershell
$itspaApi = 'http://localhost:8080/api/v1'

# Consultar lo que ya existe; en una base nueva puede ser una lista vacia.
Invoke-RestMethod -Uri "$itspaApi/libros"

$itspaSolicitudLibro = @{
    titulo = 'Libro de demostracion ITSPA'
    descripcion = 'Prueba final de persistencia'
    edicion = 'Primera'
    anioPublicacion = 2025
    isbn = $null
    numPaginas = 120
} | ConvertTo-Json

$itspaLibro = Invoke-RestMethod -Method Post -Uri "$itspaApi/libros" -ContentType 'application/json' -Body $itspaSolicitudLibro
if (-not $itspaLibro.id) { throw 'El registro del libro no devolvio identificador.' }
$itspaLibro

$itspaSolicitudEjemplar = @{
    libroId = $itspaLibro.id
    codigoBarras = ('ITSPA-DEMO-' + [guid]::NewGuid().ToString('N'))
    estadoConservacion = 'BUENO'
} | ConvertTo-Json

$itspaEjemplar = Invoke-RestMethod -Method Post -Uri "$itspaApi/ejemplares" -ContentType 'application/json' -Body $itspaSolicitudEjemplar
if (-not $itspaEjemplar.id) { throw 'El registro del ejemplar no devolvio identificador.' }
$itspaEjemplar

Invoke-RestMethod -Uri "$itspaApi/libros/$($itspaLibro.id)"
Invoke-RestMethod -Uri "$itspaApi/ejemplares?libroId=$($itspaLibro.id)"
```

Se espera `201` en los POST y `200` en las consultas. El ejemplar debe indicar el ID del libro, estado `DISPONIBLE`, conservación `BUENO`, estatus activo y fecha automática. Puedes consultar los estados HTTP con Postman o con los detalles de respuesta de tu cliente.

El ISBN nulo es válido en el contrato actual; el código de barras generado permite repetir la demostración sin reutilizar códigos. Para comprobar el conflicto, vuelve a enviar **el mismo** `$itspaSolicitudEjemplar`: se espera `409`, sin crear otro ejemplar. Una solicitud con título vacío debe devolver `400`; consultar un ID de libro inexistente debe devolver `404`.

## 11. Confirmar que los registros quedaron en PostgreSQL

En pgAdmin, conectado a **`biblioteca_modelo`**, ejecuta:

```sql
SELECT current_database();
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT l.id AS libro_id, l.titulo, e.id AS ejemplar_id,
       e.codigo_barras, e.estado, e.estado_conservacion, e.fecha_adquisicion
FROM libros l
LEFT JOIN ejemplares e ON e.libro_id = l.id
ORDER BY l.id, e.id;
```

Compara los identificadores con las respuestas HTTP. Los registros de esta demostración permanecen en la base de la aplicación. Para comprobar su persistencia, detén la aplicación con `Ctrl+C`, vuelve a arrancarla con las mismas variables y repite las consultas GET de la segunda terminal. No confundas estos registros con los datos revertidos de la base de tests.

## 12. Generar y ejecutar un JAR, si lo necesitas

Detén la aplicación anterior. Desde la raíz, con PostgreSQL disponible y las variables de ambas bases configuradas:

```powershell
mvn -f backend/pom.xml clean package
if ($LASTEXITCODE -ne 0) { throw 'No ejecutar el JAR: fallo la construccion o las pruebas.' }
java -jar ./backend/target/biblioteca-modelo-0.1.0-SNAPSHOT.jar
```

Este build vuelve a ejecutar las pruebas e incluye la integración. El JAR usa las mismas variables de entorno de conexión y puerto. El archivo se genera localmente en `target`; no necesitas subirlo ni subir tu caché Maven para que otro equipo construya desde las fuentes.

## 13. Problemas que pueden impedir terminar la fase

| Síntoma | Qué revisar |
|---|---|
| `mvn`, `java` o `psql` no se reconoce | Instalación, `PATH`, `JAVA_HOME`; Maven debe informar Java 21 |
| Dependencias de Surefire/Clean ausentes o errores de descarga | Conexión, proxy y permisos de `.m2`; conservar las versiones declaradas y no usar offline con una caché incompleta |
| `Connection refused` | Servicio PostgreSQL activo, host y puerto configurados |
| Error de autenticación o permisos | Usuario/contraseña de la base correspondiente y propiedad/permisos sobre el esquema |
| `database ... does not exist` | Completar el paso 4; Flyway crea el esquema, no la base |
| Rechazo de la guardia | Nombre exacto de la base de test, separación de `DB_URL` y overrides heredados |
| Checksum de Flyway o validación Hibernate fallida | Registrar el error y comparar migraciones/esquema; conservar V1, `validate` y el historial; no borrar ni reparar silenciosamente |
| Puerto HTTP ocupado | Detener la instancia anterior o establecer otro `SERVER_PORT` y actualizar las URL |
| No aparecen filas después de los tests | Es el rollback esperado; realizar la demostración HTTP en `biblioteca_modelo` |

Si una herramienta o conexión está bloqueada por permisos del entorno, registra el impedimento y solicita la autorización correspondiente. No des por superada la fase ni intentes eludir una denegación.

## 14. Cierre de la práctica y capturas académicas

La fase queda aprobada cuando exista evidencia real de:

1. Herramientas y Java 21 correctos en la máquina que ejecuta el proyecto.
2. Suite Maven completa: 77 casos actuales, incluidos los 11 de integración, y `BUILD SUCCESS`.
3. Base de test identificada, V1/V2 exitosas, esquema completo y FK Libro–Ejemplar.
4. Aplicación arrancada contra `biblioteca_modelo`.
5. POST/GET de Libro y Ejemplar con sus IDs, estados HTTP y consulta SQL coincidente.
6. Consulta de los mismos registros después de reiniciar la aplicación.

Toma capturas personales de esos resultados y, para la explicación del código, de los paquetes, anotaciones JPA/Lombok, relación Libro–Ejemplar, repositorios y transacciones de los services. Oculta las contraseñas.

Al realizar esta fase, actualiza [PRUEBA_Y_EVIDENCIAS.md](documentacion/PRUEBA_Y_EVIDENCIAS.md) con fecha, versiones efectivas, comandos, destino de cada base, conteos de pruebas, errores y capturas. Los informes actuales se generan en `backend/target/surefire-reports`; los históricos no sustituyen los resultados del equipo nuevo. Esta guía documenta pasos pendientes y no afirma haberlos ejecutado.

## 15. Instrucciones para continuar con un asistente

Cuando el responsable solicite ejecutar esta fase:

- Leer primero `AGENTS.md`, `README.md` y esta guía; respetar el skill ITSPA y las fuentes académicas.
- Confirmar la carpeta del clon, herramientas, instancia PostgreSQL y separación entre las dos bases usando la configuración real.
- Seguir los pasos en orden, detenerse ante fallos y registrar resultados observados.
- Conservar entidades, contratos, V1/V2 y alcance de módulos; si una prueba revela un defecto, explicar su requisito y la discrepancia antes de proponer un cambio.
- Registrar como pendientes las operaciones que no puedan ejecutarse. No interpretar este documento como autorización para borrar bases, publicar cambios ni desarrollar otros módulos.

## Equivalencias para Linux/macOS con Bash

El código Java y las migraciones se conservan. Instala JDK 21, Maven 3.9.x y PostgreSQL; utiliza `mvn` directamente, porque el helper PowerShell localiza `mvn.cmd`. Clona el repositorio y, desde su raíz, configura `JAVA_HOME` con tu ruta local y establece las variables de sesión. El bloque siguiente supone que las dos bases todavía no existen y se detiene ante errores:

```bash
export DB_URL='jdbc:postgresql://127.0.0.1:5432/biblioteca_modelo'
export DB_TEST_URL='jdbc:postgresql://127.0.0.1:5432/biblioteca_modelo_test'
read -r -p 'Usuario PostgreSQL: ' DB_USER
export DB_USER
export DB_TEST_USER="$DB_USER"
read -r -s -p 'Contrasena PostgreSQL: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD
export DB_TEST_PASSWORD="$DB_PASSWORD"
export SERVER_PORT=8080

psql -h 127.0.0.1 -p 5432 -U "$DB_USER" -W -d postgres -v ON_ERROR_STOP=1 -f ./scripts/crear_bases_piloto.sql || exit 1
mvn -f backend/pom.xml '-Dtest=*Test,!*IntegracionTest' clean test || exit 1
mvn -f backend/pom.xml clean test || exit 1
mvn -f backend/pom.xml spring-boot:run
```

Para la demostración HTTP usa los mismos métodos, rutas y cuerpos JSON del paso 10 con tu cliente REST. Las verificaciones SQL, el empaquetado y las evidencias son los mismos. Las credenciales de sesión deben volver a configurarse al abrir otra terminal.
