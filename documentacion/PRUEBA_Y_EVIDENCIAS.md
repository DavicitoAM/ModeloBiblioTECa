# Bitácora real de implementación y evidencias

## Reporte con capturas solicitado — 17 de octubre de 2026

Se creó y verificó el [reporte nativo en Google Docs](https://docs.google.com/document/d/1P4nmuiTjjBGLzSr9EpMOgwBAYPSAY4yt_Fs3FcqnpoM/edit?usp=drivesdk), enfocado en actores principales y el circuito Libro–Ejemplar. Tiene 11 páginas, 16 figuras y cuatro tablas nativas. Incluye diagrama original, código real de entidades/repositorios/services, anotaciones JPA/alias/Lombok, prueba de recarga con em.clear(), resultados de las 77 pruebas y consultas reales de campos/FK/registros/Flyway en pgAdmin.

Las capturas fueron tomadas por el asistente por autorización expresa del responsable; no se atribuyen al estudiante. Se conservan [originales, recortes, hashes y consultas](evidencias/reporte-2026-10-17/README.md). El [registro del reporte](REPORTE_MODELO_GOOGLE_DOCS.md) identifica su alcance, fuentes, enlace y verificación visual. Los estados de captura personal registrados más abajo corresponden a la entrega anterior. Esta tarea no cambió el modelo ni el esquema y no volvió a ejecutar Maven: documenta la ejecución real del mismo día y las nuevas consultas SELECT para las capturas.

## Validación PostgreSQL — 17 de octubre de 2026

Pedido autorizado: analizar el proyecto, preparar `modeloBiblioteca`, ejecutar los archivos de creación, cargar registros académicos y probar conexión. Fuentes: M02, RF-M02-01/26/27/30 y RN-M02-03/04/11/12, pp. impresas 16–20 / visor 17–21; RNF-MAN-07, impresa 81 / visor 82. Entidades funcionales afectadas: Libro y Ejemplar; dependencias de esquema: las 70 entidades de la unidad JPA y siete tablas N:M. No se cambiaron entidades, relaciones, V1/V2 ni el POM.

**Estado actual:** conexión PostgreSQL real correcta; **Maven `clean verify`: BUILD SUCCESS, 77 pruebas, 0 fallos, 0 errores y 0 omisiones**. Ambas bases tienen V1/V2 aplicadas y Hibernate validó el esquema completo. Se consultaron 77 tablas de negocio y 126 FK por base; `flyway_schema_history` es una tabla técnica adicional. El libro y sus dos ejemplares persisten tras reiniciar el backend.

| Comprobación real | Resultado del 17 de octubre |
|---|---|
| Servidor, usuario y autenticación | PostgreSQL 18.6, `127.0.0.1:5432`, `postgres`; conexión `psql` y JDBC correctas |
| Base solicitada | `modeloBiblioteca` ya existía vacía; se utilizó sin borrarla |
| Base exclusiva de test | Se creó `biblioteca_modelo_test`; la guardia conserva ese nombre y rechaza el destino normal |
| Compilación y paquete | Temurin 21.0.12.1+1, Maven 3.9.16; 95 fuentes de producción, nueve archivos de pruebas y JAR ejecutable |
| Comando Maven desde raíz | `mvn --batch-mode --no-transfer-progress -f backend/pom.xml clean verify` |
| Surefire | 66 casos sin BD y 11 PostgreSQL; todos correctos |
| Test y rollback | V1/V2 permanecen aplicadas; `libros=0`, `ejemplares=0` después de las pruebas |
| Base de demostración | `libros=1`, `ejemplares=2`; Libro ID 1, Ejemplar IDs 1 y 2, ambos DISPONIBLE/BUENO |
| Carga por API | `scripts/cargar-demostracion.ps1 -BaseUrl http://localhost:8081`; repetición comprobada sin duplicar filas |
| Pruebas HTTP con servidor real | GET Libro y ejemplares: 200; título vacío: 400; ISBN/código duplicados: 409; libro inexistente: 404 |
| Reinicio | Se detuvo solo el Java iniciado en esta tarea y se arrancó con `scripts/iniciar-modelo.ps1 -PuertoHttp 8081`; mismos IDs y registros consultados de nuevo |
| Estado del backend al terminar las pruebas | API disponible en `http://localhost:8081/api/v1/libros`; 8080 estaba ocupado por otro proceso |
| Verificación estática | 70 entidades, nueve enums, 77 tablas; cero discrepancias tras reconocer CRLF/LF |

El JDK 21 oficial de Temurin se descargó en `.itspa-tmp/jdk21/` y se comprobó contra su SHA-256 publicado. No se cambió Java global ni se guardaron contraseñas en archivos. Maven pudo completar las dependencias declaradas; los bloqueos del 15 de octubre se conservan abajo como historia.

Incidencias y límites observados:

- V1 tiene CRLF en este clon Windows, pero el contenido normalizado a LF coincide con el original `1A6E65BA794EC18B1B6BC1B400EA615CDE7AC889C65419CD90815DBBC602CAEB`. Se ajustó únicamente el verificador para mostrar ambos hashes. V1 y V2 permanecen intactas.
- Flyway 11.7.2 emitió una advertencia: su soporte probado llega a PostgreSQL 17 y este servidor usa 18.6. Las migraciones, Hibernate y todas las pruebas pasaron. Se registra la revisión de compatibilidad como pendiente D-26, sin cambiar versiones automáticamente.
- Se corrigió la enumeración de arrays de `Invoke-RestMethod` en el helper de carga y se comprobó la repetición sobre las filas existentes. Los intentos iniciales de repetición rechazados por ISBN duplicado no añadieron registros.
- Solo se cargó el ejemplo académico Libro–Ejemplar; no se inventaron personas, cuentas, políticas institucionales ni datos de los otros módulos.

Evidencia reproducible conservada en [resultado-postgresql-2026-10-17.txt](evidencias/resultado-postgresql-2026-10-17.txt). Los informes completos de Surefire están en `backend/target/surefire-reports/`; logs de Maven, arranque/reinicio, consultas SQL y la verificación estática están en `.itspa-tmp/`, fuera de Git. No se han realizado capturas personales del estudiante.

Para pgAdmin: actualizar el nodo Databases del servidor `127.0.0.1:5432`, abrir `modeloBiblioteca → Schemas → public → Tables` y ejecutar `scripts/verificar_esquema.sql` en Query Tool. Conectar por separado a `biblioteca_modelo_test` para comprobar la separación y el rollback.

| Captura personal pendiente | Qué debe mostrar |
|---|---|
| pgAdmin, base normal | Nombre exacto `modeloBiblioteca`, 77 tablas de negocio y tabla Flyway |
| Historial | V1 y V2 con `success=true` |
| Persistencia | JOIN Libro–Ejemplar: Libro 1 y ejemplares 1/2; DISPONIBLE, BUENO y códigos distintos |
| Test | `biblioteca_modelo_test`, esquema completo y conteo cero en libros/ejemplares |
| Maven | `Tests run: 77, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS` |
| API | GET del libro y sus ejemplares desde `localhost:8081`, también tras reiniciar |

Continuidad recomendada: realizar estas capturas, revisar compatibilidad Flyway/PostgreSQL 18 y concretar la normalización ISBN pendiente D-23 antes de ampliar las operaciones de M02. Otros módulos mantienen únicamente modelo y esquema.

## Historial de implementación — 15 de octubre de 2026

Modelo de 70 entidades JPA y 9 enums, organizado en nueve paquetes. Una sola unidad de persistencia.
V1 conserva su contenido original; V2 declara el resto del esquema y amplía libros/ejemplares.
Los repositorios, services, DTOs y controladores operativos permanecen en Libro–Ejemplar.
Las políticas, autenticación, préstamos, sanciones, avisos y reportes todavía no tienen implementación funcional.

**Estados distintos:** código escrito; comprobación estática; compilación; mapeo inicializado; esquema PostgreSQL validado; operaciones probadas.
Ningún estado implica automáticamente los siguientes.

**Estado histórico del 15 de octubre:** compilación Maven disponible y 66 casos sin BD ejecutados correctamente con JUnit Jupiter local; Maven e integración pendientes entonces. El estado vigente es el del 17 de octubre descrito arriba. El resto de esta sección conserva los intentos y límites observados el día 15.

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

## Operación integral M02–M06 — 20 de octubre de 2026

El pedido vigente amplía el piloto a interfaz y operaciones reales. El contrato está en `API_OPERACIONES.md`. Se conservaron V1, V2, las cardinalidades aprobadas, una sola unidad JPA y `ddl-auto=validate`. Los servicios nuevos aplican transacciones, políticas explícitas, bloqueos PostgreSQL, reglas de identidad, DTOs de salida, historiales y avisos internos.

Ejecución real a las 09:51:28 (America/Mexico_City): `mvn --batch-mode --no-transfer-progress -o -Dtest=OperacionesBibliotecaIntegracionTest test`, JDK 21.0.12.1. PostgreSQL `biblioteca_modelo_test`; Flyway validó las dos migraciones sin cambios. Resultado: **14 pruebas correctas; cero fallos, errores u omisiones; BUILD SUCCESS**. La ejecución inicial con red restringida falló antes de cargar el contexto; se repitió con autorización de conexión local y pasó. Los datos de estas pruebas se revierten mediante `@Transactional`; no se cargaron personas de prueba en `modeloBiblioteca`.

Los 14 casos verifican: préstamo múltiple y devolución parcial/final; doble préstamo; asignación de cola al devolver/cancelar y conversión a préstamo; renovación y límite; reserva ajena que impide renovar; solapes de cubículo y de persona; bloqueo y conservación de reservas afectadas; pagos parciales/no sobrepago y condiciones de política originales; privacidad de lector; cuenta pendiente y reserva duplicada; protección de inventario en circulación; vencimiento de recogida; retraso en devolución; prioridad configurable entre tipos de usuario.

Capturas personales adicionales previstas, sin afirmar que ya existen:

1. Pantalla de catálogo con búsqueda y detalle: ubicación, copias y disponibilidad; ficha identificada como demostración.
2. Préstamo de dos ejemplares, devolución parcial y devolución final, con consulta de las filas de `prestamos`, `detalles_prestamo` y `devoluciones` en pgAdmin.
3. Reserva en espera, devolución que aparta la copia, plazo de recogida y vínculo al préstamo; `historiales_reserva` y `historiales_estado_ejemplar`.
4. Rechazo explicado de un solape, horario fuera de apertura o bloqueo de cubículo; reserva válida e historial de uso real.
5. Política sancionadora configurada por administración, multa y pago parcial; rechazo de sobrepago; saldo cero y estados CUMPLIDA/RESUELTA distintos.
6. Terminal con el resultado real de `OperacionesBibliotecaIntegracionTest` y la URL de test visible, sin credenciales en pantalla.

Límites operativos explícitos: los vencimientos de recogida/inasistencias se procesan al consultar u operar su módulo; los préstamos vencidos se detectan al consultar préstamos. No se atribuye a esta suite una prueba de carga concurrente. Los avisos son internos; el importe de multa es fijo y configurable, sin tarifas institucionales inventadas. El runner optativo `biblioteca.demo.enabled=true` crea únicamente ocho fichas bibliográficas y hasta dieciséis copias identificadas como demostración; no crea personas ni políticas.

## Renovación del frontend — 26 de octubre de 2026

Módulos M01–M09; fuentes y recorridos en `INTERFAZ_Y_RECORRIDOS.md`, sección del 26 de octubre. El pedido actual autoriza la interfaz completa; no se modificaron entidades, relaciones, migraciones, políticas institucionales ni datos de PostgreSQL.

Resultados ejecutados en esta sesión:

| Comprobación | Resultado y alcance |
|---|---|
| `npm test` | 4 pruebas de navegación/permisos correctas; cero fallos |
| `npm run test:ui` | 10 casos contabilizados por Node (nueve subpruebas y su prueba contenedora), correctos; Playwright con Edge en escritorio 1440×1000 y móvil 390×844 |
| `$env:UI_USE_BUILD='1'; npm run test:ui` | Mismos 10 casos correctos sobre `frontend/dist`, sin errores JavaScript; log `.itspa-tmp/frontend-browser-production-2026-10-26.log` |
| `./scripts/preparar-interfaz.ps1` | TypeScript y Vite correctos; copia de la salida a `backend/src/main/resources/static` |
| Maven 3.9.16, JDK 21.0.12.1, `--batch-mode --no-transfer-progress -o '-Dtest=*Test,!*IntegracionTest' clean verify` | **BUILD SUCCESS**, 69 pruebas sin BD, cero fallos/errores/omisiones; JAR ejecutable generado. Log `.itspa-tmp/frontend-backend-verify-jdk21-2026-10-26.log` |
| Inspección ZIP del JAR | Índice de interfaz incluido en `BOOT-INF/classes/static/index.html`, SHA-256 igual a `frontend/dist/index.html`; 35 entradas estáticas en el paquete |
| `npm audit` | Cero vulnerabilidades reportadas, incluidas dependencias de desarrollo; salida `.itspa-tmp/frontend-audit-2026-10-26.json` |
| Migraciones | Sin cambios en V1/V2 |

Las pruebas de navegador interceptan todas las peticiones `/api` con fixtures identificados como prueba. Verifican carga de complementos; iconos SVG; menú plegable y persistencia; búsqueda y atajo Ctrl+K; todas las rutas del menú; pestañas; DataTables con búsqueda, paginación y acciones React; filtro de idioma; ficha bibliográfica; Chosen múltiple y conservación de IDs; guardado simulado y Toastr; validación de fecha inválida/corregida y calendario Tempus Dominus; Date Range Picker; Summernote; mapa y desmontaje sin acumular eventos; SweetAlert2 con confirmación, error escapado y reintento; menú móvil, Escape, foco y ausencia de desbordamiento de la página. Ninguna mutación de esa suite llega a la API real.

La primera selección de versiones de dependencias produjo incidencias que se corrigieron antes de la ejecución final: adaptador React/DataTables alineados, representación SVG de Ionicons, formatos/callbacks y validación de calendarios, escape de errores y desmontaje de jqvmap. La auditoría final utiliza jQuery, Moment y Summernote actualizados y compartidos mediante overrides; no se ejecutó `audit fix --force`.

Una primera verificación Maven usó el Java global 25 por una ruta relativa de JAVA_HOME incorrecta. Se corrigió el destino y se repitió **clean verify con JDK 21**, comprobando `java.version=21.0.12.1` en los XML Surefire. Solo el segundo registro acredita la verificación con el Java exigido por el proyecto.

La API `127.0.0.1:8081` no respondió durante esta renovación. **No se ejecutaron pruebas de integración PostgreSQL en esta sesión ni se acreditan operaciones reales por los fixtures.** Las verificaciones de BD de sesiones anteriores conservan sus fechas y alcance.

Capturas automáticas disponibles para revisión del diseño (datos de prueba; no evidencias escolares): `.itspa-tmp/frontend-2026-10-26/inicio-prueba.png`, `inventario-prueba.png` y `movil-prueba.png`. Son capturas efectivamente generadas y revisadas, separadas de las capturas personales pendientes.

Capturas que debe realizar el estudiante con el sistema y la BD reales, sin mostrar datos sensibles:

1. Nuevo panel blanco/azul con indicadores devueltos por el servidor; menú lateral abierto y plegado.
2. Secciones desplegadas y sus enlaces directos: ubicaciones, equipamiento, carreras, motivos de visita, plantillas y canales.
3. Catálogo filtrado por idioma, búsqueda combinada y ficha bibliográfica con copias/ubicaciones.
4. Tabla de ejemplares: búsqueda, orden y paginación; formulario con Chosen y calendario.
5. Préstamo/devolución real y consulta correspondiente en pgAdmin; no reutilizar filas del test de navegador como evidencia de persistencia.
6. Reporte por periodo y descarga real; aviso con editor, confirmación de retirada y mensaje de resultado.
7. Pantalla móvil con menú y formulario legibles; prueba manual de teclado y retorno del foco.
8. Resultado final de Maven y de pruebas UI, rotulando **sin BD / API interceptada** cuando corresponda. Para acreditar integración, ejecutar aparte la suite autorizada contra `biblioteca_modelo_test`.

No se afirman capturas personales ni integración real que no se hayan realizado.

Cierre del empaquetado: tras el último ajuste del editor/calendario y del contraste visual se repitieron build y suite UI sobre producción, con los 10 casos correctos. El JAR final se regeneró con Java 21 mediante `-o -DskipTests clean package` para retirar recursos compilados de iteraciones anteriores; esta última orden no ejecuta pruebas nuevas y no sustituye el `clean verify` de 69 pruebas ya registrado. La salida final y los recursos están en `.itspa-tmp/frontend-build-final-2026-10-26.log` y `.itspa-tmp/frontend-package-final-2026-10-26.log`.

## Corrección del fallo de conexión de la captura — 28 de octubre de 2026

Ámbito autorizado: inicio y soporte M08/M09 (M10 en el gráfico), sin cambios del modelo, migraciones, políticas ni autenticación. Fuentes: M08 pp. 60–67/61–68, RN-M08-09/25; M09 pp. 68–76/69–77. Archivos y decisión de puertos documentados en INTERFAZ_Y_RECORRIDOS.md.

Diagnóstico inicial ejecutado: PostgreSQL escuchaba en 5432 y Vite en 5173; no había ningún proceso en 8081. La contraseña PostgreSQL no estaba en el entorno de esta sesión. No se buscó en archivos ajenos, no se adivinó, no se registró y no se cambió la autenticación. El responsable introdujo la clave localmente y confirmó que el lanzador mostró «Sistema disponible».

Correcciones verificadas:

- Un único estado global cuando falta backend, con reintento y recuperación automática; las vistas no se montan mientras no hay contexto de sesión válido. No aparecen dos alertas por la misma desconexión ni indicadores/cuentas ficticios.
- Se distingue la conexión rechazada/timeout/respuesta HTML de un error JSON real del módulo. Las escrituras no se repiten automáticamente.
- Lanzador conjunto `scripts/iniciar-sistema.ps1`, con validación de dependencias, espera real del backend, comprobación del proxy, contraseña oculta y limpieza limitada a hijos propios.
- Puertos configurables de forma coordinada; el proxy usa ITSPA_API_TARGET. Se mantuvo 8081/5173 porque no existía conflicto real de puertos.

Resultados ejecutados:

| Comprobación | Resultado |
|---|---|
| `npm test` | **12 pruebas correctas**, cero fallos; incluye red/proxy, timeout, sesión inválida, error de módulo, escrituras sin reenvío, 204, permisos y proxy real HTTP de prueba en un puerto alternativo |
| `npm run test:ui` | **14 casos contabilizados por Node** (13 subpruebas y contenedora), correctos; incluye arranque sin backend, una sola alerta, recuperación automática inicial y durante uso, y errores de módulo sin ceros ficticios |
| `$env:UI_USE_BUILD='1'; npm run test:ui` | Los mismos **14 casos correctos** contra la salida de producción; `.itspa-tmp/conexion-ui-2026-10-28.log` |
| `npm run build` y `./scripts/preparar-interfaz.ps1` | TypeScript/Vite correctos; recursos actualizados en backend/src/main/resources/static para el siguiente empaquetado |
| Parser PowerShell y `iniciar-sistema.ps1 -Comprobar` | Correctos; no solicitan claves ni crean procesos. Validados también 8082/5174 y el rechazo de puertos iguales/proxy incompatible |
| Consultas reales HTTP | API directa 8081 y proxy 5173 responden con sesión inicializada y contexto real; `/inicio` devuelve estadísticas y nueve títulos existentes |
| Navegador contra el sistema real | Panel cargado, cuatro indicadores, seis grupos del menú, **cero alertas de error**, ningún estado de desconexión y **cero errores JavaScript**. Script de lectura `.itspa-tmp/verificar-conexion-real-2026-10-28.mjs` |
| Pestaña visible del responsable | Se observó Comunidad/personas con datos cargados y sin avisos de conexión. No se modificaron sus registros ni formularios |

Las suites automatizadas de UI y proxy usan fixtures HTTP aislados e interceptan /api; no escriben en PostgreSQL. Las comprobaciones reales posteriores fueron lecturas del sistema ya arrancado por el responsable. No se ejecutaron pruebas JUnit ni la suite de integración PostgreSQL en esta corrección, porque no cambió código Java. Los resultados de sesiones anteriores mantienen su propio alcance.

La interfaz de desarrollo se dejó disponible en 5173; el backend real del responsable se conservó en 8081. No se sustituyó el JAR en uso ni se detuvo la terminal del responsable para reempaquetar; los recursos actualizados quedan preparados para el siguiente build Maven con la aplicación detenida.

Capturas personales pendientes para el estudiante:

1. Panel real ya conectado en 5173, sin las dos alertas de la captura original.
2. Terminal del lanzador con «Sistema disponible», ocultando cualquier dato sensible.
3. Si se quiere demostrar la recuperación, realizarla en una sesión de demostración controlada; distinguirla de las pruebas automatizadas con API interceptada. No afirmar nuevas capturas personales que no se han realizado.
