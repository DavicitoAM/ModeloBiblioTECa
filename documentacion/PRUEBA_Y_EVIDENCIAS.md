# Bitácora real de implementación y evidencias

## Alcance implementado — 15 de octubre de 2026

Modelo de 70 entidades JPA y 9 enums, organizado en nueve paquetes. Una sola unidad de persistencia.
V1 conserva su contenido original; V2 declara el resto del esquema y amplía libros/ejemplares.
Los repositorios, services, DTOs y controladores operativos permanecen en Libro–Ejemplar.
Las políticas, autenticación, préstamos, sanciones, avisos y reportes todavía no tienen implementación funcional.

**Estados distintos:** código escrito; comprobación estática; compilación; mapeo inicializado; esquema PostgreSQL validado; operaciones probadas.
Ningún estado implica automáticamente los siguientes.

**Estado actual:** compilación Maven disponible; **66 casos sin BD ejecutados correctamente con JUnit Jupiter local**. `mvn test` y `mvn clean test` siguen bloqueados por dependencias de plugins incompletas. PostgreSQL, Flyway y las 11 pruebas de integración no se ejecutaron en esta ampliación. Los intentos iniciales que aparecen más abajo son históricos y no describen el estado final de compilación.

## Entorno observado

- Windows / fecha de referencia America/Mexico_City: 2026-10-15.
- JDK: Temurin 21.0.12.1, localizado mediante JAVA_HOME.
- Maven: 3.9.16, localizado en la caché de Maven Wrapper del usuario; no estaba en PATH.
- PostgreSQL 18 instalado. No se utilizaron credenciales ni datos de proyectos anteriores.
- Aplicación: Spring Boot 3.5.6 y Lombok 1.18.42, conservados como están declarados en el POM.
- Las dependencias de compilación y JUnit ya están disponibles; faltan dependencias de los plugins Surefire 3.5.4 y Clean 3.4.1. No se cambió ninguna versión del proyecto.

## Comandos y resultados reales

| Fecha | Comprobación | Resultado observado |
|---|---|---|
| 2026-10-15 | JAVA_HOME/bin/java.exe -version | JDK 21.0.12.1 disponible |
| 2026-10-15 | mvn.cmd -version | Maven 3.9.16 disponible |
| 2026-10-15 | Desde backend: mvn -o -Dtest=ModeloMetadatosTest clean test | Falló antes de compilar: spring-boot-starter-parent:3.5.6 no disponible en caché |
| 2026-10-15 | Maven online, -f backend/pom.xml, repositorio temporal local, -Dtest=ModeloMetadatosTest clean test | Falló antes de compilar: Host desconocido (repo.maven.apache.org) dentro del sandbox |
| 2026-10-15 | Reintento de Maven fuera del sandbox para descargar dependencias, sin JDBC | Permiso rechazado por el usuario; el comando no se ejecutó |
| 2026-10-15 | initdb de una instancia temporal del proyecto, distinta de la BD normal | Inicialización completada; sin aplicar migraciones |
| 2026-10-15 | createdb contra 127.0.0.1:55438 | TCP bloqueado por sandbox: Permission denied (10013) |
| 2026-10-15 | Reintento de createdb fuera del sandbox | Permiso rechazado por el usuario; el comando no se ejecutó |
| 2026-10-15 | python scripts/verificar_modelo.py --output .itspa-tmp/verificacion_estatica.txt | Exit 0: sin discrepancias estáticas; 70 entidades, 9 enums, 77 tablas y V1 intacta |
| 2026-10-15 | Parser de PowerShell sobre scripts/prueba-modelo.ps1 | Cero errores de sintaxis |
| 2026-10-15 | Hash SHA-256 de V1 | 1A6E65BA794EC18B1B6BC1B400EA615CDE7AC889C65419CD90815DBBC602CAEB, conservado |

En esos intentos iniciales no hubo compilación ni ejecución JUnit. Posteriormente la caché disponible permitió compilar y ejecutar el motor JUnit local, como se registra a continuación.
**No se aplicaron V1/V2 ni se ejecutaron consultas de la aplicación en PostgreSQL desde esta sesión.**
No se realizaron descargas alternativas ni accesos PostgreSQL después de los permisos rechazados. La instancia temporal no queda como servicio instalado.

## Ampliación autorizada: pruebas sin BD

Antes de modificar archivos se citaron M02, RF-M02-01/06/07/26/27/30 y RN-M02-03/04/11/12, pp. impresas 16–20 / visor 17–21, y RNF-MAN-07, p. impresa 81 / visor 82. Se añadieron pruebas del comportamiento existente, sin modificar producción, entidades, migraciones, contratos HTTP ni POM.

Se asignaron tareas expresas a los agentes existentes después de leer sus instrucciones reutilizables: services (`LibroServicioTest` y `EjemplarServicioTest`), validación/relación (`ValidacionSolicitudesTest` y `LibroEjemplarRelacionTest`) y API (`ApiPilotoTest`). La coordinación revisó y compiló los archivos, ajustó recursos exclusivos de test, ejecutó las pruebas y documentó los resultados. La mera presencia de `agentes/*.md` no se toma como evidencia de ejecución autónoma.

| Comando / operación del 15 de octubre | Resultado real |
|---|---|
| Maven offline, `test-compile`, JDK 21 | BUILD SUCCESS; las fuentes de producción habían compilado (95 archivos) y se compilaron los nueve archivos de pruebas. La última comprobación incremental también terminó correctamente |
| Maven normal para completar Surefire | Acceso denegado al escribir metadatos de la caché; se solicitó permiso fuera del sandbox y el usuario lo rechazó. No se ejecutó ese reintento |
| `./scripts/prueba-modelo.ps1 -SinBD -Offline` | Exit 1 antes de JUnit: Clean 3.4.1 no encuentra `org.codehaus.plexus.util.Os`; caché incompleta |
| Desde backend, `mvn -o --batch-mode --no-transfer-progress '-Dtest=*Test,!*IntegracionTest' test` | Compilación disponible; exit 1 al cargar Surefire 3.5.4: falta `org/apache/maven/plugin/surefire/SurefireReportParameters`; cero casos ejecutados por Surefire |
| `mvn -o ... org.apache.maven.plugins:maven-dependency-plugin:3.10.0:build-classpath -DincludeScope=test -Dmdep.outputFile=target/classpath-test.txt` | BUILD SUCCESS, classpath resuelto del POM con dependencias ya instaladas; sin descargas |
| Primer arranque del ejecutor local | Se corrigió el proveedor de directorios requerido por la API JUnit del ejecutor auxiliar |
| Inicialización Mockito con configuración predeterminada | Intentó instrumentación de Java 21; ejecución interrumpida sin resultado válido. Se configuraron `mock-maker-proxy` y `member-accessor-reflection` exclusivamente en recursos de test |
| `javac` + `java @target/junit-local-run.args`, ejecutor `scripts/JUnitSinBDLocal.java` | Exit 0: **66 ejecutados, 66 correctos, 0 fallidos/abortados, 0 omitidos, 0 contenedores fallidos**. Selección explícita de siete clases sin BD |

La ejecución real del motor JUnit usa sus ciclos de vida y parametrización; no llama a métodos de test manualmente. Se repitió usando el ejecutor entregado y los comandos documentados para comprobar su reproducción. El resultado no se presenta como éxito de Surefire.

| Grupo | Casos correctos |
|---|---:|
| LibroServicioTest | 10 |
| EjemplarServicioTest | 7 |
| ValidacionSolicitudesTest | 18 |
| LibroEjemplarRelacionTest | 4 |
| ApiPilotoTest | 18 |
| ModeloMetadatosTest | 1 |
| GuardiaBasePruebasTest | 8 |
| **Total sin BD** | **66** |

Archivos añadidos: cinco clases de pruebas, dos recursos de Mockito y un ejecutor JUnit auxiliar. Se añadió `-SinBD` al helper PowerShell, con selección `*Test,!*IntegracionTest` y exclusión mutua con `-SoloMetadatos`. El [inventario](INVENTARIO_ARCHIVOS.md) explica sus responsabilidades; [PRUEBAS_SIN_BD.md](PRUEBAS_SIN_BD.md) contiene cobertura y comandos reproducibles.

Evidencias originales de ejecución:

- [Informe por caso y resumen de las 66 pruebas](evidencias/resultado-junit-sin-bd-2026-10-15.txt).
- [Consola JUnit: MockMvc y metadatos Hibernate](evidencias/junit-sin-bd-consola-2026-10-15.log).
- [Maven test-compile, BUILD SUCCESS de compilación](evidencias/maven-test-compile-2026-10-15.log).
- [Maven clean test, fallo de Clean](evidencias/maven-sin-bd-2026-10-15.log).
- [Maven test, fallo de Surefire](evidencias/maven-test-sin-bd-2026-10-15.log).

No se detectó una contradicción nueva que exigiera cambiar el comportamiento. Permanece la diferencia ya documentada entre la normalización completa de ISBN y el `trim`/vacío→null actual; las pruebas comprueban únicamente este comportamiento disponible. La prueba del helper de asociación en memoria no garantiza coherencia si un futuro consumidor modifica directamente sus colecciones o setters.

## Comprobación estática ejecutada

El verificador stdlib compara las fuentes Java/JPA con V1+V2: 70 entidades, 9 enums, 77 tablas (7 puentes),
552 columnas, 126 claves foráneas, 84 índices, 171 relaciones y 56 mappedBy; cuatro subclases JOINED.
Salida: SIN DISCREPANCIAS DETECTADAS; 0 errores, 0 advertencias.

Coteja nombres, tipos, longitudes, precisión/escala, nulabilidad, PK, unicidad, destinos FK, asociaciones inversas,
vocabularios enum, imports y anotaciones prohibidas. No ejecuta SQL ni valida todas las expresiones CHECK;
no sustituye compilación Java, inicialización Hibernate o pruebas PostgreSQL.

## Integración preparada y límites

Hay ocho clases de pruebas y un inicializador (nueve archivos Java). Siete clases, con 66 casos incluyendo entradas parametrizadas, ya se ejecutaron sin BD. La integración conserva 11 casos pendientes:

- ModeloMetadatosTest: 1 prueba ejecutada que construye el modelo Hibernate de las 70 entidades sin JDBC.
  La ausencia de generación/validación de esquema en este test no cambia ddl-auto=validate de la aplicación.
- GuardiaBasePruebasTest: 8 pruebas ejecutadas de rechazo de destinos incorrectos o coincidentes con la BD normal.
- ModeloPersistenciaIntegracionTest: 11 pruebas PostgreSQL: metamodelo completo e historial Flyway;
  libro sin ejemplares ni idioma; recarga de varios ejemplares; services y estado inicial;
  ISBN opcional/consulta; duplicados por service; libro inexistente; FK, UNIQUE y CHECK por BD.
  AfterTransaction verifica que los libros/ejemplares creados se revierten.

La guardia de conexión se ejecuta antes de refrescar el contexto Spring y antes de Flyway. Requiere el nombre
biblioteca_modelo_test y rechaza que DB_URL apunte al mismo destino, incluidos alias locales. Rechaza URLs independientes de Flyway/Hikari y JNDI. No mostrar contraseñas en capturas.

## Procedimiento reproducible

Desde la raíz del proyecto, con JDK 21 y Maven/dependencias disponibles:

1. Ejecutar la comprobación estática con Python: python scripts/verificar_modelo.py.
2. Para todos los casos sin PostgreSQL: ./scripts/prueba-modelo.ps1 -SinBD. Si falta Surefire, consultar la ejecución local documentada; no atribuir éxito a Maven. Solo para metadatos/guardia: -SoloMetadatos.
3. Preparar biblioteca_modelo_test vacía en la instalación PostgreSQL autorizada.
4. Establecer DB_TEST_URL, DB_TEST_USER y DB_TEST_PASSWORD como variables de sesión, con valores locales reales.
5. Ejecutar ./scripts/prueba-modelo.ps1, que lanza mvn clean test desde backend.
6. Examinar backend/target/surefire-reports. Registrar conteo y resultado reales.
7. En pgAdmin, conectado explícitamente a biblioteca_modelo_test, ejecutar scripts/verificar_esquema.sql.

El script PowerShell encuentra Maven en PATH o en la caché del Wrapper. No descarga un Wrapper ni crea bases automáticamente.
La opción -Offline solo funciona cuando ya están todas las dependencias en caché.

Flyway conserva las estructuras/historial y las secuencias pueden avanzar aunque las pruebas reviertan sus filas.
Por ello, una consulta SQL posterior no debe prometer mostrar los registros transaccionales del test.
Para una demostración HTTP persistente, ejecutar la aplicación contra una base de demostración autorizada
y usar las solicitudes del README; no confundirla con los datos revertidos del test.

## Capturas académicas que debe tomar el estudiante

| Captura | Contenido | Estado |
|---|---|---|
| 1 | Nueve paquetes de modelo y separación repositorio/services/controlador/dto | Pendiente de captura personal |
| 2 | Persona JOINED y un subtipo con PK heredada | Pendiente de captura personal |
| 3 | Libro/Ejemplar: alias, Lombok, FK, LAZY, enums y relación inversa | Pendiente de captura personal |
| 4 | Ejemplos Cuenta–Rol–Permiso, reserva y detalle/devolución | Pendiente de captura personal |
| 5 | V1 intacta y V2 con tablas, uniones y FK | Pendiente de captura personal |
| 6 | Dos repositorios y services con transacciones; DTOs sin serializar entidades | Pendiente de captura personal |
| 7 | Terminal Maven `test-compile`: BUILD SUCCESS de compilación; mostrar también el fallo pendiente de `test` | Ejecución disponible; captura personal pendiente |
| 8 | pgAdmin: nombre biblioteca_modelo_test, esquema completo e historial Flyway | Pendiente de conexión |
| 9 | Consulta académica Libro–Ejemplar sobre datos persistentes de demostración | Pendiente de demostración |
| 10 | Cinco clases nuevas de pruebas: Mockito, Jakarta Validation y MockMvc standalone | Archivos disponibles; captura personal pendiente |
| 11 | Informe JUnit local: siete grupos, 66 correctos, 0 fallos/omisiones; rotular «sin BD, ejecución directa JUnit» | Informe real disponible; captura personal pendiente |

No se fabricaron capturas. Las respuestas HTTP de esta ampliación son pruebas MockMvc sin servidor ni persistencia; las capturas de pgAdmin y del recorrido HTTP con PostgreSQL siguen pendientes. No presentar el BUILD SUCCESS de compilación como resultado exitoso de `mvn test`.
