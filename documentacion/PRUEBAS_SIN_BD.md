# Pruebas antes de PostgreSQL

Fuentes: M02, RF-M02-01/06/07/26/27/30 y RN-M02-03/04/11/12, pp. impresas 16–20 / visor 17–21; RNF-MAN-07, p. impresa 81 / visor 82. Se comprueba el comportamiento ya implementado. No se modifican entidades, services, contratos HTTP, migraciones ni versiones del POM.

## Cobertura ejecutada el 15 de octubre de 2026

| Clase | Casos | Resultado |
|---|---:|---|
| LibroServicioTest | 10 | Correctos |
| EjemplarServicioTest | 7 | Correctos |
| ValidacionSolicitudesTest | 18 | Correctos |
| LibroEjemplarRelacionTest | 4 | Correctos |
| ApiPilotoTest | 18 | Correctos |
| ModeloMetadatosTest | 1 | Correcto; 70 entidades |
| GuardiaBasePruebasTest | 8 | Correctos |
| **Total** | **66** | **0 fallidos, 0 abortados, 0 omitidos** |

Se incluyen 57 casos nuevos y nueve conservados. Las entradas parametrizadas cuentan individualmente. El [informe real](evidencias/resultado-junit-sin-bd-2026-10-15.txt) identifica cada caso; el [log de consola](evidencias/junit-sin-bd-consola-2026-10-15.log) conserva los mensajes de MockMvc y Hibernate. Los avisos por solicitudes inválidas son escenarios esperados.

Los services usan repositorios simulados; MockMvc usa controladores/advice reales con services simulados y no inicia un servidor. Las validaciones ejecutan Jakarta Validation. El modelo Hibernate se construye con acceso a metadatos JDBC deshabilitado, proveedor de conexiones suministradas por el usuario y generación de esquema deshabilitada. No se carga la aplicación completa, inicia Flyway ni se abre una BD. Los recursos `mockito-extensions` eligen proxies y reflexión: todos los mocks son interfaces y no requieren instrumentación de la JVM.

Esto no acredita consultas reales, restricciones SQL, concurrencia, rollback ni integración PostgreSQL. Tampoco ejecuta operaciones de los demás módulos. `ModeloPersistenciaIntegracionTest` queda fuera de la selección.

## Ejecución habitual con Maven

Desde la raíz, con JDK 21 y las dependencias completas:

```powershell
./scripts/prueba-modelo.ps1 -SinBD
# Si todas las dependencias ya están en caché:
./scripts/prueba-modelo.ps1 -SinBD -Offline
```

El helper ejecuta `mvn clean test '-Dtest=*Test,!*IntegracionTest'`. `-SinBD` y `-SoloMetadatos` son excluyentes. Sin ninguna de esas opciones, se incluye integración y se necesita PostgreSQL.

En esta sesión `test-compile` terminó con BUILD SUCCESS. `test` no ejecutó casos porque falta `SurefireReportParameters` de Surefire 3.5.4. `clean test` se detuvo antes por `org.codehaus.plexus.util.Os` de Clean 3.4.1. El permiso para completar la caché fuera del sandbox fue rechazado; no se intentaron descargas alternativas ni se cambiaron versiones. Por eso el éxito de los 66 casos corresponde al motor JUnit local, no a Maven Surefire.

## Reproducción con el motor JUnit ya instalado

Se conserva [JUnitSinBDLocal.java](../scripts/JUnitSinBDLocal.java), ejecutor auxiliar que usa el descubrimiento, ciclo de vida, parametrización y aserciones reales de JUnit Jupiter. No invoca métodos de test manualmente. Selecciona siete clases explícitas y genera `backend/target/resultado-junit-sin-bd-local.txt`. Devuelve error ante pruebas fallidas/abortadas, omitidas, contenedores fallidos o clases ausentes.

Requiere que el compilador, el plugin Dependency 3.10.0 y las dependencias del proyecto ya estén en caché. Los comandos siguientes son offline; si falta una dependencia se detienen. Usar Maven en PATH o sustituir `mvn.cmd` por su ejecutable local. Ejecutar desde `backend`:

```powershell
mvn.cmd -o --batch-mode --no-transfer-progress test-compile
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
mvn.cmd -o --batch-mode --no-transfer-progress org.apache.maven.plugins:maven-dependency-plugin:3.10.0:build-classpath '-DincludeScope=test' '-Dmdep.outputFile=target/classpath-test.txt'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo obtener el classpath local.' }

$itspaCP = 'target/test-classes;target/classes;' + (Get-Content -LiteralPath target/classpath-test.txt -Raw).Trim()
$itspaCP = $itspaCP.Replace('\', '/')
Set-Content -LiteralPath target/junit-local-compile.args -Encoding utf8 -Value ('--release 21 -proc:none -cp "' + $itspaCP + '" -d target/junit-local ../scripts/JUnitSinBDLocal.java')
& (Join-Path $env:JAVA_HOME 'bin/javac.exe') '@target/junit-local-compile.args'
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del ejecutor.' }
Set-Content -LiteralPath target/junit-local-run.args -Encoding utf8 -Value ('-cp "' + $itspaCP + ';target/junit-local" JUnitSinBDLocal')
& (Join-Path $env:JAVA_HOME 'bin/java.exe') '@target/junit-local-run.args'
if ($LASTEXITCODE -ne 0) { throw 'Falló la suite JUnit local.' }
```

`target` es una salida temporal: `mvn clean` la elimina cuando Clean esté disponible. El informe entregado en `documentacion/evidencias` conserva la ejecución de esta sesión. La bitácora distingue esa ejecución de los intentos Maven y de la futura integración.
