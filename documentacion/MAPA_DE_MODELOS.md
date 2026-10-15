# Mapa del modelo completo aprobado

El alcance vigente comprende **70 entidades JPA y nueve enums**. Todas las entidades tienen tabla declarada en Flyway y pertenecen a una sola unidad de persistencia. Los casos de uso, repositorios, servicios y API permanecen limitados a Libro/Ejemplar. Modelo implementado no equivale a módulo operativo.

| Módulo funcional | Paquete bajo modelo | Entidades | Clases | Páginas impresas / físicas |
|---|---|---:|---|---|
| M01: Personas y usuarios | `personas` | 13 | `Carrera`, `CuentaUsuario`, `Departamento`, `Docente`, `Empleado`, `Estudiante`, `MotivoVisita`, `Permiso`, `Persona`, `RegistroVisita`, `Rol`, `UsuarioExterno`, `VerificacionInstitucional` | 8–14 / 9–15 |
| M02: Catálogo e inventario | `catalogo` | 8 | `Autor`, `Categoria`, `Editorial`, `Ejemplar`, `HistorialEstadoEjemplar`, `Idioma`, `Libro`, `Ubicacion` | 15–21 / 16–22 |
| M03: Préstamos, devoluciones y renovaciones | `prestamos` | 5 | `DetallePrestamo`, `Devolucion`, `PoliticaPrestamo`, `Prestamo`, `Renovacion` | 22–28 / 23–29 |
| M04: Reservaciones de libros | `reservas` | 5 | `AsignacionReserva`, `HistorialReserva`, `ListaEspera`, `PoliticaReservaLibro`, `ReservaLibro` | 29–35 / 30–36 |
| M05: Reservaciones de cubículos | `cubiculos` | 7 | `BloqueoCubiculo`, `Cubiculo`, `Equipamiento`, `HorarioBiblioteca`, `PoliticaReservaCubiculo`, `ReservaCubiculo`, `UsoCubiculo` | 36–43 / 37–44 |
| M06: Sanciones y multas | `sanciones` | 9 | `HistorialSancion`, `Incidencia`, `Multa`, `PagoMulta`, `PoliticaSancion`, `ReposicionMaterial`, `RestriccionServicio`, `Sancion`, `TipoSancion` | 44–51 / 45–52 |
| M07: Notificaciones y avisos | `notificaciones` | 8 | `AvisoGeneral`, `CanalNotificacion`, `DestinatarioAviso`, `EnvioNotificacion`, `Notificacion`, `PlantillaNotificacion`, `PreferenciaNotificacion`, `TipoNotificacion` | 52–59 / 53–60 |
| M08: Reportes y estadísticas | `reportes` | 7 | `ConfiguracionReporte`, `ExportacionReporte`, `HistorialReporte`, `Indicador`, `PanelEstadistico`, `Reporte`, `TipoReporte` | 60–67 / 61–68 |
| M09 / M10: Configuración y auditoría | `configuracion` | 8 | `BitacoraAuditoria`, `ConfiguracionSistema`, `DetalleCambio`, `IncidenteSeguridad`, `ParametroSistema`, `PoliticaSeguridad`, `RespaldoSistema`, `SesionUsuario` | 68–76 / 69–77 |

El inventario inicial de 67 nombres se completó con ListaEspera, TipoSancion y TipoNotificacion, presentes en la narrativa. Se conserva una sola Carrera tras conciliar la casilla duplicada del gráfico. Los subtipos Persona usan PK heredada, sin IDs duplicados.

## Enums de apoyo

| Paquete | Enum | Valores Java actuales |
|---|---|---|
| `catalogo` | [EstadoConservacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/EstadoConservacion.java) | `EXCELENTE`, `BUENO`, `REGULAR`, `DANADO` |
| `catalogo` | [EstadoEjemplar](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/EstadoEjemplar.java) | `DISPONIBLE`, `PRESTADO`, `RESERVADO`, `MANTENIMIENTO`, `EXTRAVIADO`, `BAJA` |
| `prestamos` | [EstadoDetallePrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/EstadoDetallePrestamo.java) | `ACTIVO`, `VENCIDO`, `DEVUELTO` |
| `prestamos` | [EstadoPrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/EstadoPrestamo.java) | `ACTIVO`, `FINALIZADO`, `CANCELADO` |
| `reservas` | [EstadoReservaLibro](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/EstadoReservaLibro.java) | `PENDIENTE`, `EN_ESPERA`, `LISTA_PARA_RECOGER`, `ATENDIDA`, `CANCELADA`, `VENCIDA` |
| `cubiculos` | [EstadoCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/EstadoCubiculo.java) | `HABILITADO`, `MANTENIMIENTO`, `FUERA_DE_SERVICIO` |
| `cubiculos` | [EstadoReservaCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/EstadoReservaCubiculo.java) | `CONFIRMADA`, `EN_USO`, `FINALIZADA`, `CANCELADA`, `VENCIDA`, `NO_PRESENTADA` |
| `sanciones` | [EstadoSancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/EstadoSancion.java) | `PENDIENTE`, `ACTIVA`, `CUMPLIDA`, `RESUELTA`, `CANCELADA` |
| `sanciones` | [TipoIncidencia](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/TipoIncidencia.java) | `RETRASO`, `DANO`, `EXTRAVIO`, `INCUMPLIMIENTO_RESERVACION`, `AUSENCIA_RESERVACION_CUBICULO`, `USO_INADECUADO_CUBICULO` |

Los enums no llevan @Entity ni tienen tabla propia: se guardan como EnumType.STRING. Otros estados/tipos siguen como String cuando las fuentes no cierran el vocabulario. No se precargan políticas institucionales.

## Tablas de unión

Las siete relaciones N:M utilizan `cubiculos_equipamientos`, `cuentas_roles`, `incidentes_auditoria`, `libros_autores`, `libros_categorias`, `roles_permisos`, `tipos_reporte_roles`. Son tablas auxiliares sin entidades adicionales: **77 tablas de negocio para 70 entidades**.

## Relaciones acordadas

- Persona usa JOINED; puede no tener cuenta. numeroEmpleado se hereda desde Persona para unicidad transversal entre Docente y Empleado.
- Cuenta–Rol y Rol–Permiso son N:M. Carrera es única; Docente y Empleado se relacionan con Departamento.
- Libro admite cero o más ejemplares; cada Ejemplar pertenece obligatoriamente a un Libro. Editorial e idioma son opcionales. La ubicación actual de Ejemplar es 0..1.
- Libro–Autor y Libro–Categoria son N:M. Los historiales son entidades y no generan contenido automáticamente.
- Prestamo contiene detalles sobre Ejemplar. Devolucion exige un detalle; su FK UNIQUE permite como máximo una devolución definitiva por detalle.
- ReservaLibro nace sobre Libro; el Ejemplar aparece en AsignacionReserva. ListaEspera es una entidad por libro; la posición pertenece a ReservaLibro.
- Cubiculo–Equipamiento es N:M. Horario/política no ejecutan por sí solos validaciones de disponibilidad.
- ReposicionMaterial enlaza Sancion y Ejemplar. CUMPLIDA y RESUELTA son estados distintos.
- Notificacion identifica una CuentaUsuario destinataria. Lectura e intentos de envío son independientes.
- Reporte e Indicador tienen referencias opcionales a PanelEstadistico por decisión técnica. DestinatarioAviso conserva una referencia polimórfica descriptiva, sin FK genérica.
- Configuración/auditoría mantiene M09 del texto/M10 del gráfico. Responsable opcional permite eventos automáticos o no identificados.

La [matriz completa](MATRIZ_MODELO_COMPLETO.md) detalla cada campo/asociación y su fuente. Las diferencias y reglas pendientes están en [DECISIONES_PENDIENTES.md](DECISIONES_PENDIENTES.md).
