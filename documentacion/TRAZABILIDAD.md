# Trazabilidad del modelo y del circuito Libro–Ejemplar

Las páginas se indican como **impresa / física del visor**. Cada entidad tiene referencias particulares en Javadoc; la [matriz completa](MATRIZ_MODELO_COMPLETO.md) relaciona atributos, columnas, asociaciones y fuentes. «Modelo y DDL» describe archivos, sin certificar ejecución PostgreSQL.

| Fuente del PDF | Obligación representada | Clases / implementación | Estado y límite |
|---|---|---|---|
| M01, pp. 8–14 / 9–15; RF-M01-01/05/11/19/24/38, RN-M01-01/06/12/19 | Personas, subtipos, cuenta, roles/permisos, visitas y verificación | 13 entidades en modelo/personas; JOINED y asociaciones | Modelo y DDL; sin autenticación/control de acceso operativos |
| M02, pp. 15–21 / 16–22; RF-M02-01/02/05/13/19/23/49, RN-M02-03/09/12 | Catálogo, autores, categorías, editorial e idioma | Ocho entidades y dos enums en modelo/catalogo | Modelo y DDL; API solo de Libro/Ejemplar básico |
| M02, p. 16 / 17; RF-M02-01/02/06 | Registrar Libro y datos bibliográficos | Libro, repositorio, servicio, DTOs y controlador | Flujo implementado; resultado real en bitácora |
| M02, p. 18 / 19; RF-M02-26/27/28/30/33 | Registrar y consultar Ejemplar | Ejemplar, repositorio, servicio, DTOs y controlador | Flujo implementado; código único y FK en SQL |
| M02, p. 20 / 21; RN-M02-03/04 | Libro admite 0..N ejemplares; cada copia es de un libro | Libro.ejemplares / Ejemplar.libro / libro_id | JPA y FK; pruebas de recarga preparadas |
| M02, p. 20 / 21; RN-M02-11/12 | ISBN opcional y control de duplicados | Libro.isbn, servicio, repositorio y UNIQUE de V1 | Trim y vacío→null; normalización completa pendiente |
| M02, pp. 20–21 / 21–22; RN-M02-13/14/16/17/24 | Circulación, conservación, ubicación e historial | Enums, Ubicacion, HistorialEstadoEjemplar | Modelo y estado inicial del alta; reglas/historial automático pendientes |
| M03, pp. 22–28 / 23–29; RF-M03-07/27/40/63, RN-M03-03/04/13/14/22 | Préstamo, detalle, devolución única, renovación y política | Cinco entidades y dos enums en modelo/prestamos | Modelo y DDL; sin autorizar préstamos/devolver/renovar |
| M04, pp. 29–35 / 30–36; RF-M04-16/20/25/26/56, RN-M04-05/10/11/18/27 | Reserva por libro, lista, asignación e historial | Cinco entidades y un enum en modelo/reservas | Modelo y DDL; prioridad/exclusividad vigentes pendientes |
| M05, pp. 36–43 / 37–44; RF-M05-02/19/42/60/63/76, RN-M05-06/10/15/18 | Cubículos, equipamiento, reserva, uso, horario y bloqueo | Siete entidades y dos enums en modelo/cubiculos | Modelo y DDL; sin disponibilidad/solapamientos/tolerancias |
| M06, pp. 44–51 / 45–52; RF-M06-21/23/43/51/61/80, RN-M06-04/12/19/27/28 | Incidencia, sanción, tipo, multa, pago y reposición | Nueve entidades y dos enums en modelo/sanciones | Modelo y DDL; sin pagos/cálculos/bloqueos/transiciones |
| M07, pp. 52–59 / 53–60; RF-M07-06/38/47/54/67/70, RN-M07-10/14/15/24 | Notificación, lectura, intentos, preferencias y avisos | Ocho entidades en modelo/notificaciones | Modelo y DDL; sin envíos ni interpretación de destinos polimórficos |
| M08, pp. 60–67 / 61–68; RF-M08-01/70/78/85, RN-M08-05/09/24/25/29 | Reportes, indicadores, exportación e historial | Siete entidades en modelo/reportes | Modelo y DDL; sin consultas/cálculos/archivos/tareas |
| M09/M10, pp. 68–76 / 69–77; RF-M09-01/10/26/43/47/59/67, RN-M09-02/14/17/22/27/32 | Configuración, política, auditoría, sesiones, incidentes y respaldos | Ocho entidades en modelo/configuracion | Modelo y DDL; sin seguridad/respaldos/auditoría automáticos |
| RNF-MAN-03/07/08, p. 81 / 82; RNF-INT-01/03/04, p. 82 / 83 | Capas, modelo persistente y acceso a datos | Unidad JPA, Flyway V1/V2, validate y DTOs | Arquitectura en archivos; esquema real PostgreSQL pendiente |

## Pruebas preparadas y evidencia

| Archivo | Qué pretende comprobar | Qué no demuestra por sí solo |
|---|---|---|
| LibroServicioTest.java | M02 RF-M02-01/06/07 y RN-M02-11/12, pp. 16–20 / 17–21: alta, ISBN, consultas y listas | Persistencia, transacciones y normalización bibliográfica completa |
| EjemplarServicioTest.java | M02 RF-M02-26/27/30 y RN-M02-03/04, pp. 18–20 / 19–21: alta, relación, conservación, fechas y consultas | FK/UNIQUE en PostgreSQL y concurrencia real |
| ValidacionSolicitudesTest.java | M02 RF-M02-01/02/26/27, pp. 16–20 / 17–21: restricciones actuales de los DTO | Reglas de otros módulos ni validaciones adicionales no implementadas |
| LibroEjemplarRelacionTest.java | M02 RN-M02-03/04, p. 20 / 21: relación en memoria mediante agregarEjemplar | Recarga de asociaciones JPA o integridad SQL |
| ApiPilotoTest.java | M02 RF-M02-01/04/26/30, pp. 16/18 / 17/19: entrada/salida de los recursos piloto | Arranque Boot completo o recorrido HTTP–BD; services simulados |
| ModeloMetadatosTest.java | Las 70 entidades forman un modelo Hibernate sin JDBC ni DDL | Aplicación de Flyway o coincidencia con PostgreSQL |
| ModeloPersistenciaIntegracionTest.java | Metamodelo/migraciones, Libro sin copias/idioma, recarga, services, ISBN, duplicados, FK, CHECK y rollback | Cumplimiento operativo de los otros módulos |
| GuardiaBasePruebasTest.java | Ocho escenarios de protección de destino, overrides independientes y alias locales | Aplicación de esquema ni conectividad PostgreSQL |
| scripts/verificar_modelo.py | Correspondencia estática Java/DDL sin red ni BD | Compilación Java, metadatos ejecutados o migraciones aplicadas |
| GuardiaBasePruebas.java | Detener la integración si el destino no es biblioteca_modelo_test | Disponibilidad o permisos de la BD |

RNF-MAN-07 (p. 81 / 82) respalda las pruebas y el ejecutor auxiliar. Los 66 casos sin BD pasaron mediante JUnit local; los 11 PostgreSQL permanecen pendientes. La normalización completa del ISBN sigue pendiente y no se cambió al añadir pruebas. Consulta [PRUEBA_Y_EVIDENCIAS.md](PRUEBA_Y_EVIDENCIAS.md) para comandos, resultados reales, impedimentos y capturas pendientes.
