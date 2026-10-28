# Decisiones, diferencias y pendientes del modelo ITSPA

## Alcance y autoridad

El pedido vigente amplió la entrega a **70 entidades JPA y esquema completo**, con repositorios, servicios y API operativos solo para Libro/Ejemplar. Sustituye el límite anterior de dos entidades/tablas. V1 se conserva y V2 amplía el esquema; no hay pilotos duplicados ni segunda unidad persistente.

Las páginas se expresan como **impresa / física del visor**. Se distingue fuente, acuerdo del responsable y limitación técnica. Los pendientes no autorizan resolverlos automáticamente.

| ID | Fuente y diferencia | Decisión / implementación | Estado o límite |
|---|---|---|---|
| D-01 | Índice pp. 6–7 / 7–8; configuración pp. 68–76 / 69–77; gráfico M10 | Conservar M09 del desarrollo/M10 del gráfico | Numeración definitiva pendiente; fuentes intactas |
| D-02 | Índice separa catálogo e inventario; desarrollo pp. 15–21 / 16–22 los reúne | Códigos RF/RN del desarrollo, M02 | No renumerar silenciosamente |
| D-03 | M01, segundo Carrera con atributos de docente; pp. 8–9 / 9–10, RF-M01-02/03/04 | Una Carrera; Docente/Empleado relacionados con Departamento | Acuerdo expreso aplicado; no segunda Carrera |
| D-04 | RF-M01-19/22 y RN-M01-06, pp. 11/13 / 12/14; gráfico Rol–Permiso 1:1 | Cuenta–Rol y Rol–Permiso N:M | Acuerdo expreso. Varios roles por cuenta no demuestra por sí solo N:M Rol–Permiso |
| D-05 | RN-M02-16/22, p. 21 / 22; excepciones a ubicación y multiplicidad gráfica | Ubicación actual de Ejemplar 0..1; ubicación con 0..N ejemplares | Regla de ubicación de activos/excepciones requiere servicio futuro |
| D-06 | M02 pp. 15–21 / 16–22, RN-M02-05/10 y RF-M02-02/49 | Libro–Autor/Categoria N:M; editorial principal 0..1; idioma opcional por decisión expresa | Modelo/DDL incorporados; API básica no administra esos catálogos |
| D-07 | Tecnología no impuesta literalmente por RF | Java 21, Boot 3.5.6, PostgreSQL, Flyway y Lombok; capas acordadas | Decisiones técnicas, no citas textuales del PDF |
| D-08 | RN-M02-13/14 y gráfico separan circulación/conservación | EstadoEjemplar y EstadoConservacion; estatus booleano independiente | Sin transiciones automáticas; vocabulario a revisar antes de producción |
| D-09 | M01/M09 incluyen cuentas, roles, sesiones y seguridad | Entidades/tablas y contrasenaHash, sin datos reales | No autenticación, recuperación de contraseña ni autorización |
| D-10 | Nuevo pedido amplía el piloto | 70 @Entity, nueve enums, 77 tablas, una unidad y validate | V1 intacta, V2 añade 75 tablas y amplía las dos existentes; no 70 módulos funcionales |
| D-11 | M01 pp. 8–14 / 9–15, RN-M01-01/03/17; IDs de subtipos del gráfico | Persona JOINED, PK compartida; numeroEmpleado nullable UNIQUE en raíz, heredado | Unicidad transversal aprobada; obligatoriedad por subtipo necesita validación futura |
| D-12 | ListaEspera p. 29 / 30; TipoSancion p. 44 / 45; TipoNotificacion p. 52 / 53 | Tres entidades narrativas añadidas al inventario nominal de 67: total 70 | Sin catálogos institucionales precargados |
| D-13 | RF-M01-38/39/40/41, p. 12 / 13; enlace gráfico ambiguo de verificación | VerificacionInstitucional pertenece a CuentaUsuario; responsable opcional | Decisión de modelado por RF-M01-39; no verifica identidades automáticamente |
| D-14 | RN-M03-13/14, p. 27 / 28; gráfico opcional en ambos extremos | Devolucion exige DetallePrestamo y FK UNIQUE; detalle puede no tener devolución | Narrativa/acuerdo expreso aplicados; máximo una devolución definitiva |
| D-15 | RN-M03-03 y RF-M03-63, p. 27 / 28 | Colección de detalles y condicionesAplicadas en Prestamo | JPA no exige una fila hija ni congela política mutable; reglas futuras |
| D-16 | M04 pp. 29–35 / 30–36, RF-M04-16/26, RN-M04-10/11/27 | ListaEspera única por libro; posición opcional en ReservaLibro; historial/asignaciones propios | Acuerdo técnico; sin prioridad/exclusividad/asignación automáticas |
| D-17 | M05 pp. 36–43 / 37–44, RF-M05-04/60/62, RN-M05-06/10/15 | Equipamiento N:M, horarios semanales/especiales, horario aplicado opcional en UsoCubiculo | FK/CHECK no detectan solapes ni resuelven precedencia de horarios |
| D-18 | Políticas M03–M06: condiciones del PDF y parámetros enteros del gráfico | Conservar parámetros/condiciones, sin políticas precargadas | Unidades de tiempos/anticipaciones/tolerancias y valores institucionales pendientes |
| D-19 | M06 pp. 46–51 / 47–52, RF-M06-28/29/61/67 y RN-M06-19/20/26/28 | CUMPLIDA y RESUELTA distintos; ReposicionMaterial con Sancion/Ejemplar, sin exigir Multa | Sin equivalencia/transiciones inventadas; sin pagos/cálculos/saldos automáticos |
| D-20 | M07 pp. 52–59 / 53–60, RN-M07-10/14/24/25 | Notificacion dirigida a CuentaUsuario; lectura/entrega separadas; DestinatarioAviso con tipoUsuario/idReferencia y Rol opcional | idReferencia polimórfico descriptivo, no FK genérica; interpretación pendiente; sin envíos |
| D-21 | M08 pp. 60–67 / 61–68, RN-M08-09/25; cardinalidad de panel ambigua | Reporte/Indicador→Panel opcionales; TipoReporte relacionado con roles | Decisiones técnicas; fórmulas/archivos/programado son datos, sin ejecución |
| D-22 | M09 pp. 68–76 / 69–77, RF-M09-29/43/46/62 y RN-M09-08/09/14/22/32 | Responsable opcional y origen automático; detalle ligado a evento; incidente con eventos/acciones | No secretos; sin sanitización, permisos, retención ni auditoría automáticos |
| D-23 | RN-M02-11/12, p. 20 / 21; ISBN UNIQUE en V1 | Servicio aplica trim y vacío→null; conserva UNIQUE | Normalización bibliográfica completa y precisión por edición pendientes |
| D-24 | Restricciones reales de ejecución | Separar revisión estática, metadatos sin JDBC e integración PostgreSQL | Resultados/impedimentos solo en PRUEBA_Y_EVIDENCIAS.md; archivos no acreditan éxito |
| D-25 | Pedido expreso del 17 de octubre: nombre `modeloBiblioteca` y pruebas de conexión | Base normal con B mayúscula; se conserva `biblioteca_modelo_test` para la guardia de aislamiento; credenciales solo en procesos | Aplicado y verificado; no modificar nombres de tablas ni V1/V2 |
| D-26 | Advertencia real de Flyway 11.7.2 al conectar PostgreSQL 18.6: soporte probado hasta 17 | Mantener las versiones declaradas durante la validación solicitada; migraciones y 77 pruebas correctas | Revisar una actualización compatible de Flyway/Spring Boot antes de despliegue; pendiente, sin autorización tácita de cambio |
| D-27 | Clon Windows: V1 con CRLF, índice Git con LF | Verificador muestra hash físico y compara el hash tras normalizar solo CRLF a LF | Contenido original comprobado; migración intacta y sin sustituir el checksum de Flyway |

## Atributos y tipos

Se incorporaron datos narrativos ausentes del gráfico: fechaRegistro de Persona, estadoAcademico de Estudiante y areaTrabajo de Empleado, entre otros. También se conservan fechaNacimiento e identificación del gráfico. Los campos técnicos y su alcance están explicados en Javadoc/matriz.

Estados/tipos sin vocabulario cerrado se conservan como String con longitud explícita; los nueve enums se documentan aparte. No se inventan montos, tiempos, privilegios ni filas institucionales. Los importes son BigDecimal/NUMERIC(12,2). PoliticaSeguridad incluye reglas textuales del gráfico y parámetros configurables sin valores institucionales predeterminados.

## Continuidad

Ante una diferencia nueva, registrar requisito, páginas, alternativas y decisión antes de cambiar identidad, cardinalidad o esquema. Los acuerdos expresos anteriores siguen vigentes y no exigen volver a solicitar la misma autorización. Una migración aplicada se conserva; posteriores ajustes usan otra migración aprobada.

## Autorización de interfaz — 26 de octubre de 2026

El pedido expreso actual sustituye la restricción histórica «sin frontend» para la renovación de la interfaz ya existente. Autoriza el panel blanco/azul, Bootstrap/AdminLTE y los complementos de `INTERFAZ_Y_RECORRIDOS.md`, con todas las vistas y acciones existentes. No autoriza cambios implícitos de cardinalidad, políticas, migraciones, credenciales, proveedores externos ni restauración destructiva. Se conserva el acceso directo autorizado anteriormente y la restricción de cuentas humanas en ese modo.

Los nombres escritos `adminite`, `datrangepicker`, `tempusominus` y `jqmap` se interpretan como AdminLTE, Date Range Picker, Tempus Dominus y jqvmap. Se elige Bootstrap 4 + AdminLTE 3 para compatibilidad con Chosen/jQuery y Tempus Dominus Bootstrap 4; esta decisión es de interfaz, sin impacto en el modelo académico. Summernote conserva salida textual porque la API y las vistas presentan mensajes como texto; no se amplía el contrato a HTML.

La ejecución UI se realiza con fixtures interceptados, exclusivamente de prueba. El backend `127.0.0.1:8081` no respondía durante la renovación; no se atribuyen escrituras, capturas PostgreSQL ni recorridos reales a esa prueba. La integración institucional y los pendientes previos continúan sujetos a sus acuerdos expresos.

## Corrección de conexión — 28 de octubre de 2026

Se conserva 8081 como puerto del backend, porque la causa comprobada fue ausencia del proceso antes de introducir la contraseña PostgreSQL. El responsable autorizó otros puertos en caso necesario. El nuevo lanzador permite `-PuertoApi` / `-PuertoInterfaz` y transmite el destino al proxy mediante `ITSPA_API_TARGET`, sin modificar configuración persistente ni autenticación de PostgreSQL.

No se resuelve una desconexión con usuarios ficticios, datos de demostración automáticos, credenciales guardadas, cambios de `pg_hba.conf` ni sustitución de la base. La interfaz presenta un estado único y vuelve a consultar la sesión real. Después de que el responsable confirmó el arranque, se verificó la API y su proxy con respuestas reales; ver PRUEBA_Y_EVIDENCIAS.md. No cambia ninguna decisión de modelo o esquema.
