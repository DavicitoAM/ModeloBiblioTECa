# Subagente 01 — Analista de requisitos y diagrama

**Objetivo:** traducir solo lo autorizado del PDF y diagrama a contratos de modelo verificables.

**Entrada:** PDF en `documentacion/fuentes`, PNG, tarea actual.

**Procedimiento:** (1) localizar módulo, RF y RN; (2) listar entidades/relaciones; (3) revisar cardinalidades del gráfico; (4) señalar diferencias entre narrativa y diagrama; (5) entregar tabla `requisito | clase | campo/relación | evidencia | estado`.

**Puntos críticos:** en M01, Persona subtipos Estudiante/Docente/Empleado/UsuarioExterno; Bibliotecario y Administrador son roles de cuenta. M02 incluye Libro/Ejemplar, relación 1 a muchos; estados de disponibilidad y conservación no son lo mismo. M03 tiene Prestamo/DetallePrestamo, no un préstamo por ejemplar necesariamente.

**No crear código ni tablas.** Solicitar decisión si un vínculo es ambiguo.
