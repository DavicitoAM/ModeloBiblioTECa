# Sistema Integral de Gestión de Biblioteca del ITSPA

Proyecto nuevo e independiente. El alcance vigente es el **modelo completo de 70 entidades JPA y nueve enums**, organizado en nueve paquetes bajo `modelo`. Todas las entidades pertenecen a una sola unidad de persistencia y cuentan con estructura SQL mediante Flyway. Hibernate permanece en `ddl-auto=validate`.

Los repositorios, servicios, DTOs y controladores operativos se concentran en **Libro y Ejemplar**. Los demás módulos aportan clases, atributos, asociaciones y tablas; sus casos de uso todavía están pendientes. Esta entrega no constituye el sistema operativo completo.

## Fuentes y decisiones

1. Pedido vigente del responsable y [AGENTS.md](AGENTS.md).
2. [PDF académico](documentacion/fuentes/Proyecto_Spring_Boot_Capa_de_Modelo.pdf), de 89 páginas físicas.
3. [Diagrama original](documentacion/fuentes/Diagrama_de_clases_ITSPA.png).
4. [Decisiones, diferencias y pendientes](documentacion/DECISIONES_PENDIENTES.md).
5. Código y migraciones de este repositorio.

Las referencias distinguen **página impresa** y **página física del visor**. Por ejemplo, M02 empieza en la impresa 15, física 16. El índice inicial separa catálogo e inventario, mientras el desarrollo funcional los agrupa en M02. Configuración/auditoría corresponde a M09 en el desarrollo y M10 en el gráfico. Se conserva esta discrepancia sin renumerar las fuentes.

Java 21, Spring Boot 3.5.6, PostgreSQL, Flyway y Lombok son decisiones técnicas. Cada entidad contiene referencias y notas de modelado en Javadoc. Las fuentes académicas no se modificaron.

## Alcance en archivos

| Elemento | Alcance |
|---|---|
| Modelo | 70 `@Entity`, nueve enums y nueve paquetes por módulo |
| Esquema de negocio | 77 tablas declaradas: 70 de entidades y siete de unión N:M |
| Migraciones | V1 conserva las dos tablas iniciales; V2 añade 75 tablas y amplía `libros`/`ejemplares` |
| Persistencia | Una unidad JPA; estructura mediante Flyway y validación Hibernate |
| Repositorios | `LibroRepositorio` y `EjemplarRepositorio` |
| Servicios | Dos contratos y sus dos implementaciones en `services/impl` |
| HTTP | Dos recursos: `/api/v1/libros` y `/api/v1/ejemplares` |
| Pruebas | 77 casos ejecutados con Maven: 66 sin BD y 11 de integración PostgreSQL |

El 17 de octubre se verificaron las 77 tablas de negocio y las 126 FK en `modeloBiblioteca` y `biblioteca_modelo_test`, mediante PostgreSQL real y Hibernate `validate`. `flyway_schema_history` es una tabla técnica adicional. Los comandos y resultados corresponden a [PRUEBA_Y_EVIDENCIAS.md](documentacion/PRUEBA_Y_EVIDENCIAS.md).

## Organización

```text
backend/
├── pom.xml
├── src/main/java/mx/edu/itspa/biblioteca/
│   ├── BibliotecaApplication.java
│   ├── modelo/
│   │   ├── personas/        # M01
│   │   ├── catalogo/        # M02; Libro y Ejemplar están aquí
│   │   ├── prestamos/       # M03
│   │   ├── reservas/        # M04
│   │   ├── cubiculos/       # M05
│   │   ├── sanciones/       # M06
│   │   ├── notificaciones/  # M07
│   │   ├── reportes/        # M08
│   │   └── configuracion/   # M09 texto / M10 gráfico
│   ├── repositorio/        # Solo Libro y Ejemplar
│   ├── services/impl/      # Contratos en services; dos implementaciones
│   ├── controlador/       # Dos adaptadores HTTP
│   ├── dto/               # Cuatro registros de entrada/salida
│   └── excepcion/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__piloto_libros_ejemplares.sql
│       └── V2__modelo_integral_itspa.sql
└── src/test/
    ├── java/.../LibroServicioTest.java
    ├── java/.../EjemplarServicioTest.java
    ├── java/.../ValidacionSolicitudesTest.java
    ├── java/.../LibroEjemplarRelacionTest.java
    ├── java/.../ApiPilotoTest.java
    ├── java/.../ModeloMetadatosTest.java
    ├── java/.../ModeloPersistenciaIntegracionTest.java
    ├── java/.../GuardiaBasePruebas.java
    ├── java/.../GuardiaBasePruebasTest.java
    └── resources/          # application-test.yml y mockito-extensions/
```

Flujo operativo: `Controlador → Service → Repositorio → modelo.catalogo → PostgreSQL`. El resto del modelo participa en la misma unidad de persistencia, sin servicios ficticios.

## Preparación y ejecución local

Se requieren JDK 21, Maven 3.9+ y PostgreSQL. No hay Maven Wrapper. Comprueba `java -version` y `mvn -version`. Maven debe resolver las versiones de `pom.xml` antes de compilar.

Desde pgAdmin, conectado a la base administrativa `postgres`, prepara bases separadas cuando corresponda:

```sql
CREATE DATABASE "modeloBiblioteca";
CREATE DATABASE biblioteca_modelo_test;
```

Flyway debe aplicar **V1 y V2** en cada base de destino. Si V1 ya está aplicada, conserva su historial y permite que Flyway añada V2. No ejecutes otro esquema manual, no reescribas V1 ni habilites `ddl-auto=update`.

Desde `backend`, carga tus credenciales mediante variables de entorno sin guardarlas en archivos del repositorio. Las variables previstas son `DB_USER`, `DB_PASSWORD`, `DB_TEST_USER` y `DB_TEST_PASSWORD`; las URL opcionales son `DB_URL` y `DB_TEST_URL`.

```powershell
# Suite sin BD: services, DTOs, relación, HTTP standalone, metadatos y guardia.
mvn '-Dtest=*Test,!*IntegracionTest' clean test

# Metadatos sin conexión a PostgreSQL.
mvn '-Dtest=ModeloMetadatosTest' test

# Suite completa: necesita biblioteca_modelo_test y ambas migraciones.
mvn clean test

# Aplicación: necesita modeloBiblioteca.
mvn spring-boot:run
```

Desde la raíz, `./scripts/prueba-modelo.ps1 -SinBD` selecciona todos los tests excepto `*IntegracionTest`; no arranca PostgreSQL ni Flyway. `-SoloMetadatos` selecciona únicamente metadatos y guardia. Son opciones excluyentes y requieren Maven y sus dependencias. `-Offline` evita descargas. El script busca Maven en PATH o en la caché local del usuario, sin crear un Wrapper.

Para una revisión estática sin red ni BD: `python scripts/verificar_modelo.py`. Contrasta las anotaciones Java y el DDL mediante Python estándar; **no compila Java ni ejecuta tests**. La suite contiene 64 métodos (58 `@Test` y seis parametrizados): 66 casos sin BD y 11 de integración. El número de casos incluye cada entrada parametrizada.

La integración usa el perfil `test`. Su guardia exige la BD `biblioteca_modelo_test` antes de iniciar Flyway. Las pruebas escriben datos únicamente de Libro/Ejemplar y verifican reversión transaccional; las migraciones crean el esquema completo de test y permanecen aplicadas. Comprueba el destino efectivo antes de ejecutar.

Estado del 17 de octubre: `mvn clean verify` terminó con **BUILD SUCCESS: 77 pruebas, cero fallos, errores u omisiones**, incluyendo integración PostgreSQL y empaquetado ejecutable. Se resolvieron las dependencias pendientes con autorización de ejecución. V1/V2 están aplicadas en ambas bases. `modeloBiblioteca` conserva un libro académico y dos ejemplares incluso después de reiniciar. Se utilizó JDK 21 de Temurin, descargado y verificado dentro de `.itspa-tmp/jdk21/`, sin cambiar el Java global. La [bitácora](documentacion/PRUEBA_Y_EVIDENCIAS.md) conserva las pruebas y la advertencia de compatibilidad Flyway/PostgreSQL 18.

Para arrancar el JAR construido desde la raíz: `./scripts/iniciar-modelo.ps1 -PuertoHttp 8081`. Solicita la contraseña sin guardarla; usa `JAVA_HOME` con JDK 21 o, si no está definido, el JDK local mencionado. El puerto 8080 estaba ocupado, por lo que la ejecución de esta sesión usa **8081**. La conexión sigue siendo PostgreSQL `127.0.0.1:5432`, usuario `postgres`, base `modeloBiblioteca`. En pgAdmin basta actualizar el nodo de bases del servidor correspondiente; la base ya está preparada.

Para repetir la demostración sin duplicar sus filas: `./scripts/cargar-demostracion.ps1 -BaseUrl http://localhost:8081`. Para consultar esquema y registros en pgAdmin: [scripts/verificar_esquema.sql](scripts/verificar_esquema.sql). Las comillas de `CREATE DATABASE "modeloBiblioteca"` conservan la B mayúscula; no crear otra base con el nombre convertido a minúsculas.

## Demostración HTTP de Libro y Ejemplar

La aplicación utiliza `http://localhost:8080`, configurable mediante `SERVER_PORT`.

| Método | Ruta | Operación |
|---|---|---|
| POST | `/api/v1/libros` | Registrar datos bibliográficos básicos |
| GET | `/api/v1/libros` | Listar libros |
| GET | `/api/v1/libros/{id}` | Consultar un libro |
| POST | `/api/v1/ejemplares` | Registrar una copia de un libro existente |
| GET | `/api/v1/ejemplares?libroId=...` | Consultar sus ejemplares |

Con la aplicación y la BD preparadas, una prueba manual puede usar:

```powershell
$libro = Invoke-RestMethod -Method POST -Uri 'http://localhost:8080/api/v1/libros' -ContentType 'application/json' -Body (@{
  titulo = 'Fundamentos de Programación'
  isbn = '9780000000019'
  edicion = 'Primera'
  anioPublicacion = 2025
  numPaginas = 320
  descripcion = 'Registro de demostración académica'
} | ConvertTo-Json)

$ejemplar = Invoke-RestMethod -Method POST -Uri 'http://localhost:8080/api/v1/ejemplares' -ContentType 'application/json' -Body (@{
  libroId = $libro.id
  codigoBarras = 'ITSPA-PRACTICA-001'
  estadoConservacion = 'BUENO'
} | ConvertTo-Json)

$libro
$ejemplar
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/ejemplares?libroId=$($libro.id)"
```

La demostración HTTP crea filas en la BD de aplicación; las filas de los tests se revierten. Repetir ISBN/código genera conflicto. El ISBN actual solo elimina espacios exteriores; su normalización bibliográfica completa queda pendiente.

## Documentación y continuidad

- [Mapa de modelos](documentacion/MAPA_DE_MODELOS.md).
- [Matriz completa de clases, tablas, atributos, columnas, relaciones y fuentes](documentacion/MATRIZ_MODELO_COMPLETO.md).
- [Inventario de archivos](documentacion/INVENTARIO_ARCHIVOS.md).
- [Trazabilidad](documentacion/TRAZABILIDAD.md).
- [Anotaciones JPA y alias](documentacion/ANOTACIONES_Y_ALIAS.md).
- [Plan por capas](documentacion/PLAN_POR_CAPAS.md).
- [Pruebas y capturas académicas](documentacion/PRUEBA_Y_EVIDENCIAS.md).

Los ficheros de `agentes/` son instrucciones reutilizables. Una herramienta debe leerlos y asignar trabajo expresamente para producir delegación real. Aplica el [skill ITSPA](skills/modelado-jpa-itspa/SKILL.md). Otros repositorios, servicios, endpoints, frontend y motores de reglas requieren una nueva tarea autorizada.

## Interfaz renovada — 26 de octubre de 2026

El pedido actual autoriza la renovación del frontend existente. Está implementada en blanco y azul con Bootstrap Grid, AdminLTE, DataTables, Chosen, jQuery, Date Range Picker, OverlayScrollbars, Ionicons, Tempus Dominus, jqvmap, Summernote, SweetAlert2 y Toastr. Incluye menú plegable, apartados desplegables y vistas operativas conectadas a los recursos existentes. El modelo y las migraciones se conservan.

El diseño, la cobertura de módulos y los comandos están en [INTERFAZ_Y_RECORRIDOS.md](documentacion/INTERFAZ_Y_RECORRIDOS.md), sección del 26 de octubre. La salida compilada está incluida en el JAR local de `backend/target`; al arrancar el backend se sirve la interfaz desde la misma aplicación. Para desarrollo separado: `cd frontend` y `npm run dev`, con el proxy existente al backend en 8081. Para regenerar los recursos: `./scripts/preparar-interfaz.ps1` antes de empaquetar Maven.

Verificación de esta renovación: 4 pruebas de navegación, suite de navegador de 10 casos contabilizados (API interceptada), build frontend, auditoría sin vulnerabilidades reportadas y Maven `clean verify` con Java 21 y 69 pruebas sin BD. La conexión real a PostgreSQL se verifica por separado; consultar [PRUEBA_Y_EVIDENCIAS.md](documentacion/PRUEBA_Y_EVIDENCIAS.md).

## Arranque conjunto y conexión — 28 de octubre de 2026

Para iniciar ambos servicios, desde la raíz usa `./scripts/iniciar-sistema.ps1` e introduce la contraseña PostgreSQL en la terminal cuando se solicite. El lanzador espera a que el backend responda antes de abrir la interfaz. Se accede en `http://127.0.0.1:5173/`; conserva abierta la terminal del lanzador.

`./scripts/iniciar-sistema.ps1 -Comprobar` verifica dependencias, servicios y destino del proxy sin solicitar claves ni iniciar procesos. Para otros puertos: `./scripts/iniciar-sistema.ps1 -PuertoApi 8082 -PuertoInterfaz 5174`. El destino del frontend se adapta al puerto elegido; no se debe cambiar solo uno de los servicios manualmente. Una interfaz ya abierta con otro destino debe reiniciarse o utilizar otro puerto.

La desconexión ahora muestra un único aviso y recupera automáticamente la sesión real al estar disponible el backend. El arranque correcto del 28 de octubre se comprobó con la API real y el navegador, sin errores de conexión. Detalles y límites de verificación en `documentacion/PRUEBA_Y_EVIDENCIAS.md`.
