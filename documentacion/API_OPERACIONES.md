# API operativa de biblioteca (20 de octubre de 2026)

M02, PDF pp. 15–21; M03 pp. 22–28; M04 pp. 29–35; M05 pp. 36–43; M06 pp. 44–51 (páginas impresas). El pedido actual amplía el piloto a UI y operaciones reales. V1/V2 y las entidades permanecen intactas. Los cuerpos y respuestas usan `snake_case`; las lecturas devuelven arreglos JSON y los detalles objetos JSON. Errores: 400 entrada, 403 permisos, 404 inexistente, 409 regla o conflicto. Todas las operaciones son transaccionales.

## Catálogo e inventario

- `GET /api/app/catalogo?q=&categoria=&disponibilidad=` público: libros con `id,titulo,isbn,edicion,anio_publicacion,num_paginas,descripcion,estatus,editorial_id,idioma_id,editorial,idioma,autores,categorias,total_ejemplares,disponibles`. `autores`/`categorias` son textos agregados; disponibilidad `DISPONIBLE` limita a títulos con copias disponibles.
- `GET /api/app/catalogo/{id}` público: mismo objeto más `autores_ids,categorias_ids,ejemplares`.
- `POST /api/app/catalogo`, `PUT /api/app/catalogo/{id}` gestión: `{titulo,isbn?,edicion?,anio_publicacion?,num_paginas?,descripcion?,editorial_id?,idioma_id?,autores_ids:[],categorias_ids:[],estatus:true}`.
- `GET /api/app/ejemplares?libro_id=`; `POST /api/app/ejemplares`, `PUT /api/app/ejemplares/{id}` gestión: `{libro_id,codigo_barras,fecha_adquisicion,estado_conservacion,estado,ubicacion_id?,observaciones?,estatus:true}`. Crear omite estado para DISPONIBLE; no se puede forzar PRESTADO/RESERVADO desde inventario. La ubicación es obligatoria para copias activas disponibles.
- `GET /api/app/catalogos`: objeto con `autores,categorias,editoriales,idiomas,ubicaciones,equipamientos,tipos_sancion` (arreglos); `POST /api/app/catalogos/{tipo}` y `PUT /api/app/catalogos/{tipo}/{id}` gestión. Campos mínimos nombre, más ubicaciones `seccion,estante`, tipos_sancion `codigo`; opcionales `apellidos,pais,descripcion,nivel,estatus` según tipo.

## Préstamos

- `GET /api/app/prestamos`: propios para lectores, todos para gestión; objetos con persona y `detalles` anidados, devoluciones/renovaciones y retraso.
- `POST /api/app/prestamos` gestión: `{persona_id,ejemplares_ids:[1,2],observaciones?}`. Entrega física confirmada por bibliotecario, política activa por tipo real de persona, límite, restricciones y disponibilidad bajo bloqueo.
- `POST /api/app/prestamos/{id}/devolver` gestión: `{devoluciones:[{detalle_id,estado_conservacion,observaciones?,estado_ejemplar?:"DISPONIBLE"}]}`. Parcial; copia dañada pasa a MANTENIMIENTO. EXTRAVIADO/BAJA también admitidos; conserva devolución única.
- `POST /api/app/prestamos/{id}/renovar`: `{detalle_id}`; propio o gestión. Rechaza vencidos, reservas pendientes de otra persona y límite de política original. Conserva fechas anterior/nueva.

## Reservas de libros

- `GET /api/app/reservas`: propias/todas, con título, persona, asignaciones e historial.
- `POST /api/app/reservas`: `{libro_id,persona_id? (solo gestión),observaciones?}`. FIFO, sin duplicados, política y restricciones; asigna copia disponible o posición real de espera.
- `POST /api/app/reservas/{id}/cancelar`: `{motivo}` propio/gestión. Libera ejemplar y asigna siguiente de la cola.
- `POST /api/app/reservas/{id}/asignar` gestión: `{}`; intenta asignar respetando FIFO.
- `POST /api/app/reservas/{id}/atender` gestión: `{}`; convierte en préstamo real aplicando política de préstamo y vincula detalle.

## Cubículos

- `GET /api/app/cubiculos`, `POST /api/app/cubiculos`, `PUT /api/app/cubiculos/{id}` gestión para escritura: `{codigo,nombre,ubicacion?,capacidad,estado,descripcion?,estatus,equipamientos_ids:[]}`.
- `GET /api/app/horarios`; `POST /api/app/horarios`, `PUT /api/app/horarios/{id}` gestión: `{dia_semana?:"MONDAY",fecha_especial?:"2026-10-20",hora_apertura?:"08:00",hora_cierre?:"17:00",cerrado:false,estatus:true}`. Fecha especial prevalece sobre semana. No asumir horario institucional.
- `GET /api/app/bloqueos-cubiculo`; `POST /api/app/bloqueos-cubiculo` gestión: `{cubiculo_id,fecha_inicio,fecha_fin,motivo}`; `POST /api/app/bloqueos-cubiculo/{id}/cancelar`: `{motivo}`.
- `GET /api/app/reservas-cubiculo` propias/todas; `POST /api/app/reservas-cubiculo`: `{cubiculo_id,persona_id?,fecha_inicio,fecha_fin,observaciones?}`. Fechas ISO locales de America/Mexico_City. Valida horarios, solapes de cubículo/persona, bloqueos, anticipación, duración y máximo.
- `POST /api/app/reservas-cubiculo/{id}/cancelar` `{motivo}` propio/gestión; `/iniciar`, `/finalizar`, `/no-presentada` gestión `{observaciones?}`. Asistencia y fechas reales persistidas.

## Sanciones

- `GET /api/app/incidencias` propias/todas; `POST` gestión `{persona_id,tipo,descripcion,motivo,evidencias?,observaciones?,ejemplar_id?,prestamo_id?,detalle_prestamo_id?,devolucion_id?,reserva_libro_id?,reserva_cubiculo_id?,uso_cubiculo_id?}`. No duplica evento/motivo. `/sanciones` admite `{incidencia_id,politica_id}` para aplicar una política a la incidencia pendiente. Retrasos/daños en devolución e inasistencias generan incidencia; solo aplican sanción automática si existe política expresa compatible, sin inventar montos.

- `GET /api/app/sanciones` propias/todas: sanciones con persona, multa, pagos, restricciones e historial.
- `POST /api/app/sanciones` gestión: `{persona_id,politica_id,tipo,descripcion,motivo,ejemplar_id?,detalle_prestamo_id?,prestamo_id?,reserva_libro_id?,reserva_cubiculo_id?,evidencias?}`. Política expresa activa y compatible; genera incidencia/sanción y, si autoriza, multa/restricciones.
- `POST /api/app/sanciones/{id}/pago` gestión: `{monto,metodo,referencia?,comprobante?}`. Pago positivo y no superior al saldo, parcial solo si política permite.
- `POST /api/app/sanciones/{id}/resolver` gestión: `{motivo}` requiere obligaciones cumplidas; `/cancelar` `{motivo}` conserva historial. `/reposicion` `{ejemplar_reposicion_id,observaciones?}` valida reposición del ejemplar.

## Configuración explícita

`GET /api/app/politicas/{tipo}`, `POST`, `PUT /{id}`. Escritura solo ADMINISTRADOR; no hay políticas precargadas ni valores institucionales inventados. Tipos/campos:

- `prestamo`: `tipo_usuario` ESTUDIANTE/DOCENTE/EMPLEADO/EXTERNO, `max_libros,dias_prestamo,permite_renovacion,renovaciones_max,restricciones_material?,estatus`.
- `reserva-libro`: `dias_anticipacion?,tiempo_limite` **días de recogida**, `max_reservas,reglas_prioridad:"FIFO"` o CSV de los cuatro tipos en orden de prioridad (`DOCENTE,ESTUDIANTE,EMPLEADO,EXTERNO`), `tipos_usuario_permitidos` CSV, `estatus`. Desempate por fecha/id; la prioridad aplicada se conserva en la reserva.
- `cubiculo`: `tiempo_minimo?,tiempo_maximo` **minutos**, `anticipacion_max` **días**, `max_reservas,tolerancia_llegada?` **minutos**, `tipos_usuario_permitidos` CSV, `estatus`.
- `sancion`: `tipo_incidencia,tipo_sancion_id,reglas,tipo_usuario?,dias_suspension?,monto_multa?,multas_autorizadas,permite_pagos_parciales,servicios_restringidos?` CSV PRESTAMO/RESERVA_LIBRO/CUBICULO, `estatus`.

Sin política/horario activo la operación informa qué debe configurar administración. Las unidades quedan visibles y explícitas en UI; no se atribuyen al PDF. No se ejecuta texto libre como regla ni se interpreta automáticamente como una fórmula monetaria.

La política de préstamo interpreta `restricciones_material` como nombres de categorías excluidas separados por comas, expresamente visibles en el formulario; no interpreta prosa libre como código. El monto de sanción es el importe fijo configurado: no se supone una tarifa diaria. La condición de reposición se selecciona mediante un tipo de sanción cuyo código contenga `REPOSICION`.

## Consistencia y pruebas reales

`OperacionBibliotecaServicioImpl` delimita las transacciones y valida identidad, propiedad, políticas y transiciones. `OperacionBibliotecaRepositorio` ejecuta SQL parametrizado sobre el mismo datasource/esquema de JPA. Un bloqueo transaccional PostgreSQL serializa las transiciones de varios recursos, además de los bloqueos de fila, para impedir doble préstamo, doble asignación, solapes y sobrepagos. Esta decisión conservadora prioriza integridad; antes de un despliegue con alta concurrencia se puede sustituir por bloqueos ordenados por recurso manteniendo los mismos invariantes.

Los vencimientos de recogida y las inasistencias se procesan al consultar u operar su módulo; la consulta de préstamos detecta los vencidos. No existe un trabajador de fondo que se atribuya un responsable ficticio. Las incidencias quedan pendientes cuando aún no hay una política sancionadora explícita. Los avisos se guardan en la bandeja interna mediante M07. La aplicación conserva las fechas reales y el historial.

Validación del 20/10/2026, 09:51 America/Mexico_City: compilación de 114 fuentes y 14 casos `OperacionesBibliotecaIntegracionTest` correctos contra `biblioteca_modelo_test` PostgreSQL 18.6; cero fallos, errores u omisiones; `BUILD SUCCESS`. Flyway validó V1/V2 sin modificarlas y Hibernate validó la unidad de 70 entidades. Cada caso revierte sus filas; los IDs de secuencia pueden avanzar. La primera ejecución restringida no pudo abrir JDBC; la repetición autorizada con conexión local fue satisfactoria. No se presenta esa primera ejecución como prueba funcional.

Cobertura: entrega múltiple, devolución parcial/final, deterioro, doble préstamo, reservas/cola/asignación/cancelación/atención, renovación y límites, prioridad de otro lector, solapes de recurso/persona, bloqueos administrativos, pagos parciales/no sobrepago y condiciones originales, privacidad por propietario, verificación pendiente, protección de inventario activo, vencimiento de recogida, retraso y prioridad configurable. Son pruebas de integración transaccional; no se atribuye a esta suite una prueba de carga concurrente ni capturas de navegador.
