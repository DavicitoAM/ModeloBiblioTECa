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
| Pruebas | 66 casos sin BD ejecutados con JUnit local; 11 de integración PostgreSQL preparados |

Estos conteos describen código y SQL, **no tablas verificadas en una BD ejecutada**. `flyway_schema_history` es una tabla técnica adicional, fuera de las 77 de negocio. Los comandos y resultados reales corresponden a [PRUEBA_Y_EVIDENCIAS.md](documentacion/PRUEBA_Y_EVIDENCIAS.md).

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
CREATE DATABASE biblioteca_modelo;
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

# Aplicación: necesita biblioteca_modelo.
mvn spring-boot:run
```

Desde la raíz, `./scripts/prueba-modelo.ps1 -SinBD` selecciona todos los tests excepto `*IntegracionTest`; no arranca PostgreSQL ni Flyway. `-SoloMetadatos` selecciona únicamente metadatos y guardia. Son opciones excluyentes y requieren Maven y sus dependencias. `-Offline` evita descargas. El script busca Maven en PATH o en la caché local del usuario, sin crear un Wrapper.

Para una revisión estática sin red ni BD: `python scripts/verificar_modelo.py`. Contrasta las anotaciones Java y el DDL mediante Python estándar; **no compila Java ni ejecuta tests**. La suite contiene 64 métodos (58 `@Test` y seis parametrizados): 66 casos sin BD y 11 de integración. El número de casos incluye cada entrada parametrizada.

La integración usa el perfil `test`. Su guardia exige la BD `biblioteca_modelo_test` antes de iniciar Flyway. Las pruebas escriben datos únicamente de Libro/Ejemplar y verifican reversión transaccional; las migraciones crean el esquema completo de test y permanecen aplicadas. Comprueba el destino efectivo antes de ejecutar.

Estado del 15 de octubre: compilan las fuentes de producción y los nueve archivos de pruebas. Pasaron los **66 casos sin BD** mediante el motor JUnit Jupiter 5.12.2 instalado, sin descargas. `mvn test` sigue bloqueado por dependencias incompletas de Surefire; `clean test` encuentra además una dependencia faltante de Clean. No se autorizaron descargas fuera del sandbox. La integración PostgreSQL continúa pendiente. El [procedimiento sin BD](documentacion/PRUEBAS_SIN_BD.md) explica la ejecución habitual y la alternativa local; la [bitácora](documentacion/PRUEBA_Y_EVIDENCIAS.md) conserva los resultados reales.

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
