-- V2: modelo completo autorizado del Sistema Integral de Gestión de Biblioteca ITSPA.
-- V1 permanece intacta. Sin datos de catálogo ni políticas inventadas.
-- Todas las tablas se crean antes de sus FK para admitir dependencias cruzadas.

-- M01 y M09/M10: tablas del modelo aprobado; FKs se agregan en fragmento separado.

CREATE TABLE personas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    correo VARCHAR(254),
    telefono VARCHAR(30),
    fecha_nacimiento DATE,
    fecha_registro TIMESTAMP NOT NULL,
    tipo_identificacion VARCHAR(60),
    numero_identificacion VARCHAR(80),
    numero_empleado VARCHAR(40),
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE estudiantes (
    id BIGINT PRIMARY KEY,
    matricula VARCHAR(40) NOT NULL,
    semestre INTEGER,
    estado_academico VARCHAR(80),
    carrera_id BIGINT NOT NULL
);

CREATE TABLE docentes (
    id BIGINT PRIMARY KEY,
    departamento_id BIGINT NOT NULL
);

CREATE TABLE empleados (
    id BIGINT PRIMARY KEY,
    area_trabajo VARCHAR(150) NOT NULL,
    puesto VARCHAR(150),
    departamento_id BIGINT
);

CREATE TABLE usuarios_externos (
    id BIGINT PRIMARY KEY,
    institucion VARCHAR(200)
);

CREATE TABLE cuentas_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario VARCHAR(100) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    ultimo_acceso TIMESTAMP,
    fecha_verificacion_correo TIMESTAMP,
    estado_verificacion VARCHAR(60),
    estatus BOOLEAN NOT NULL DEFAULT TRUE,
    persona_id BIGINT NOT NULL
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(500),
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE permisos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE carreras (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE departamentos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE registros_visita (
    id BIGSERIAL PRIMARY KEY,
    fecha_entrada TIMESTAMP NOT NULL,
    fecha_salida TIMESTAMP,
    observaciones VARCHAR(1000),
    persona_id BIGINT NOT NULL,
    motivo_id BIGINT NOT NULL
);

CREATE TABLE motivos_visita (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    estatus BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE verificaciones_institucionales (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(80) NOT NULL,
    resultado VARCHAR(80),
    fecha TIMESTAMP NOT NULL,
    observaciones VARCHAR(1000),
    cuenta_id BIGINT NOT NULL,
    responsable_id BIGINT
);

CREATE TABLE configuraciones_sistema (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    nombre_institucion VARCHAR(200) NOT NULL,
    nombre_biblioteca VARCHAR(200) NOT NULL,
    correo_contacto VARCHAR(254),
    telefono_contacto VARCHAR(30),
    direccion_contacto VARCHAR(500),
    zona_horaria VARCHAR(60) NOT NULL,
    formato_fecha VARCHAR(80),
    formato_hora VARCHAR(80),
    descripcion VARCHAR(2000),
    version_aplicacion VARCHAR(80),
    fecha_ultima_actualizacion TIMESTAMP,
    fecha_modificacion TIMESTAMP,
    frecuencia_respaldos VARCHAR(100),
    estatus BOOLEAN NOT NULL DEFAULT TRUE,
    responsable_modificacion_id BIGINT
);

CREATE TABLE parametros_sistema (
    id BIGSERIAL PRIMARY KEY,
    clave VARCHAR(100) NOT NULL,
    valor VARCHAR(2000) NOT NULL,
    tipo_dato VARCHAR(60) NOT NULL,
    descripcion VARCHAR(500),
    obligatorio BOOLEAN NOT NULL DEFAULT FALSE,
    esencial BOOLEAN NOT NULL DEFAULT FALSE,
    estatus BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_modificacion TIMESTAMP,
    configuracion_id BIGINT NOT NULL,
    responsable_modificacion_id BIGINT
);

CREATE TABLE politicas_seguridad (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    reglas VARCHAR(4000),
    estatus BOOLEAN NOT NULL DEFAULT TRUE,
    inactividad_maxima_minutos INTEGER,
    intentos_fallidos_maximos INTEGER,
    bloqueo_temporal_minutos INTEGER,
    fecha_modificacion TIMESTAMP,
    responsable_modificacion_id BIGINT
);

CREATE TABLE bitacoras_auditoria (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    accion VARCHAR(100) NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    entidad VARCHAR(150),
    id_registro BIGINT,
    ip VARCHAR(45),
    resultado VARCHAR(100) NOT NULL,
    motivo VARCHAR(1000),
    operacion_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    responsable_id BIGINT
);

CREATE TABLE detalles_cambio (
    id BIGSERIAL PRIMARY KEY,
    campo VARCHAR(150) NOT NULL,
    valor_anterior VARCHAR(4000),
    valor_nuevo VARCHAR(4000),
    evento_id BIGINT NOT NULL
);

CREATE TABLE sesiones_usuario (
    id BIGSERIAL PRIMARY KEY,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    ultima_actividad TIMESTAMP,
    ip VARCHAR(45),
    estatus VARCHAR(60) NOT NULL,
    motivo_cierre VARCHAR(500),
    cierre_administrativo BOOLEAN NOT NULL DEFAULT FALSE,
    cuenta_id BIGINT NOT NULL,
    cerrada_por_id BIGINT
);

CREATE TABLE respaldos_sistema (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    archivo VARCHAR(500),
    tipo VARCHAR(80) NOT NULL,
    estado VARCHAR(80) NOT NULL,
    resultado VARCHAR(2000),
    verificacion_integridad VARCHAR(1000),
    responsable_id BIGINT,
    evento_auditoria_id BIGINT
);

CREATE TABLE incidentes_seguridad (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    estado VARCHAR(80) NOT NULL,
    gravedad VARCHAR(60) NOT NULL,
    observaciones VARCHAR(2000),
    acciones_correctivas VARCHAR(2000),
    fecha_ultima_actualizacion TIMESTAMP,
    cuenta_afectada_id BIGINT,
    responsable_id BIGINT
);

CREATE TABLE cuentas_roles (
    cuenta_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (cuenta_id, rol_id)
);

CREATE TABLE roles_permisos (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE incidentes_auditoria (
    incidente_id BIGINT NOT NULL,
    evento_id BIGINT NOT NULL,
    PRIMARY KEY (incidente_id, evento_id)
);

-- M02, PDF impresa 15–21, RF-M02-01/42 y RN-M02-01/24.
CREATE TABLE autores (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(150) NOT NULL, apellidos VARCHAR(200), estatus BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE editoriales (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(200) NOT NULL, pais VARCHAR(100), estatus BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE categorias (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(150) NOT NULL, descripcion VARCHAR(2000), estatus BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE idiomas (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, estatus BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE ubicaciones (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(150) NOT NULL, seccion VARCHAR(100) NOT NULL, estante VARCHAR(100) NOT NULL, nivel VARCHAR(100), estatus BOOLEAN NOT NULL DEFAULT TRUE);
ALTER TABLE libros ADD COLUMN estatus BOOLEAN NOT NULL DEFAULT TRUE, ADD COLUMN editorial_id BIGINT, ADD COLUMN idioma_id BIGINT;
ALTER TABLE ejemplares ADD COLUMN ubicacion_id BIGINT, ADD COLUMN observaciones VARCHAR(2000);
CREATE TABLE libros_autores (libro_id BIGINT NOT NULL, autor_id BIGINT NOT NULL, PRIMARY KEY (libro_id, autor_id));
CREATE TABLE libros_categorias (libro_id BIGINT NOT NULL, categoria_id BIGINT NOT NULL, PRIMARY KEY (libro_id, categoria_id));
CREATE TABLE historiales_estado_ejemplar (
    id BIGSERIAL PRIMARY KEY, ejemplar_id BIGINT NOT NULL, fecha TIMESTAMP NOT NULL,
    estado_anterior VARCHAR(30), estado_nuevo VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000), responsable_id BIGINT,
    CONSTRAINT chk_historial_ejemplar_anterior CHECK (estado_anterior IS NULL OR estado_anterior IN ('DISPONIBLE','PRESTADO','RESERVADO','MANTENIMIENTO','EXTRAVIADO','BAJA')),
    CONSTRAINT chk_historial_ejemplar_nuevo CHECK (estado_nuevo IN ('DISPONIBLE','PRESTADO','RESERVADO','MANTENIMIENTO','EXTRAVIADO','BAJA'))
);

-- Fragmento para integrar en V2: M03/M04/M05. No ejecutar aisladamente.
-- Sin plazos, limites ni reglas operativas predeterminados.

-- M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-01/07/10/12/13/63; RN-M03-01/03/09/30.
CREATE TABLE prestamos (
    id BIGSERIAL PRIMARY KEY,
    fecha_prestamo TIMESTAMP NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000),
    fecha_confirmacion_entrega TIMESTAMP,
    condiciones_aplicadas VARCHAR(4000),
    persona_id BIGINT NOT NULL,
    bibliotecario_id BIGINT,
    politica_id BIGINT NOT NULL,
    CONSTRAINT chk_prestamos_estado CHECK (estado IN ('ACTIVO', 'FINALIZADO', 'CANCELADO'))
);

-- M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-07/11/24/35/39; RN-M03-03/04/10/13/14/15/19.
CREATE TABLE detalles_prestamo (
    id BIGSERIAL PRIMARY KEY,
    fecha_vencimiento DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    prestamo_id BIGINT NOT NULL,
    ejemplar_id BIGINT NOT NULL,
    CONSTRAINT chk_detalles_prestamo_estado CHECK (estado IN ('ACTIVO', 'VENCIDO', 'DEVUELTO'))
);

-- M03, PDF pp. impresas 22/24-28 (fisicas 23/25-29). RF-M03-27/29/30/31/32/35/39; RN-M03-13/14/15.
CREATE TABLE devoluciones (
    id BIGSERIAL PRIMARY KEY,
    fecha_devolucion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    estado_conservacion VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000),
    detalle_prestamo_id BIGINT NOT NULL UNIQUE,
    bibliotecario_id BIGINT NOT NULL,
    CONSTRAINT chk_devoluciones_estado_conservacion CHECK (estado_conservacion IN ('EXCELENTE', 'BUENO', 'REGULAR', 'DANADO'))
);

-- M03, PDF pp. impresas 22/25-28 (fisicas 23/26-29). RF-M03-40/45/46/47/48; RN-M03-19/21/22/24.
CREATE TABLE renovaciones (
    id BIGSERIAL PRIMARY KEY,
    fecha_renovacion TIMESTAMP NOT NULL,
    fecha_vencimiento_anterior DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado VARCHAR(30),
    detalle_prestamo_id BIGINT NOT NULL,
    responsable_id BIGINT
);

-- M03, PDF pp. impresas 22/26-28 (fisicas 23/27-29). RF-M03-57/58/59/60/61/62/63; RN-M03-08/09/21/28/29.
CREATE TABLE politicas_prestamo (
    id BIGSERIAL PRIMARY KEY,
    tipo_usuario VARCHAR(80) NOT NULL,
    max_libros INTEGER NOT NULL,
    dias_prestamo INTEGER NOT NULL,
    permite_renovacion BOOLEAN NOT NULL,
    renovaciones_max INTEGER NOT NULL,
    restricciones_material VARCHAR(2000),
    estatus BOOLEAN NOT NULL
);

-- M04, PDF pp. impresas 29-35 (fisicas 30-36). RF-M04-08/09/16/17/21/39/41/44/56; RN-M04-02/03/05/16/18/26.
CREATE TABLE reservas_libro (
    id BIGSERIAL PRIMARY KEY,
    fecha_reserva TIMESTAMP NOT NULL,
    fecha_limite DATE,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000),
    posicion_espera INTEGER,
    fecha_atencion TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    motivo_cancelacion VARCHAR(2000),
    condiciones_aplicadas VARCHAR(4000),
    persona_id BIGINT NOT NULL,
    libro_id BIGINT NOT NULL,
    politica_id BIGINT,
    lista_espera_id BIGINT,
    detalle_prestamo_id BIGINT UNIQUE,
    responsable_cancelacion_id BIGINT,
    CONSTRAINT chk_reservas_libro_estado CHECK (estado IN ('PENDIENTE', 'EN_ESPERA', 'LISTA_PARA_RECOGER', 'ATENDIDA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT uk_reservas_lista_posicion UNIQUE (lista_espera_id, posicion_espera),
    CONSTRAINT chk_reservas_posicion CHECK (posicion_espera IS NULL OR posicion_espera > 0)
);

-- M04, PDF pp. impresas 29/31/34-35 (fisicas 30/32/35-36). RF-M04-16/20/21/22/23/24/26; RN-M04-10/11/23.
CREATE TABLE listas_espera (
    id BIGSERIAL PRIMARY KEY,
    libro_id BIGINT NOT NULL UNIQUE
);

-- M04, PDF pp. impresas 29/31-35 (fisicas 30/32-36). RF-M04-25/27/28/29/32; RN-M04-05/12/13/15/27.
CREATE TABLE asignaciones_reserva (
    id BIGSERIAL PRIMARY KEY,
    fecha_asignacion TIMESTAMP NOT NULL,
    estado VARCHAR(30) NOT NULL,
    reserva_id BIGINT NOT NULL,
    ejemplar_id BIGINT NOT NULL
);

-- M04, PDF pp. impresas 31/34-35 (fisicas 32/35-36) y PNG. RF-M04-22/26; RN-M04-26/27.
CREATE TABLE historiales_reserva (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    posicion_anterior INTEGER,
    posicion_nueva INTEGER,
    observaciones VARCHAR(2000),
    reserva_id BIGINT NOT NULL,
    responsable_id BIGINT,
    CONSTRAINT chk_historiales_reserva_estado_anterior CHECK (estado_anterior IN ('PENDIENTE', 'EN_ESPERA', 'LISTA_PARA_RECOGER', 'ATENDIDA', 'CANCELADA', 'VENCIDA')),
    CONSTRAINT chk_historiales_reserva_estado_nuevo CHECK (estado_nuevo IN ('PENDIENTE', 'EN_ESPERA', 'LISTA_PARA_RECOGER', 'ATENDIDA', 'CANCELADA', 'VENCIDA'))
);

-- M04, PDF pp. impresas 29/33-35 (fisicas 30/34-36). RF-M04-50/51/52/53/54/55/56; RN-M04-09/10/11/17/25.
CREATE TABLE politicas_reserva_libro (
    id BIGSERIAL PRIMARY KEY,
    dias_anticipacion INTEGER,
    tiempo_limite INTEGER NOT NULL,
    max_reservas INTEGER NOT NULL,
    reglas_prioridad VARCHAR(2000),
    tipos_usuario_permitidos VARCHAR(1000),
    estatus BOOLEAN NOT NULL
);

-- M05, PDF pp. impresas 36-38/41-43 (fisicas 37-39/42-44). RF-M05-01/02/03/04/07/08; RN-M05-01/02/04/05.
CREATE TABLE cubiculos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    ubicacion VARCHAR(250),
    capacidad INTEGER NOT NULL,
    estado VARCHAR(30) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL,
    CONSTRAINT chk_cubiculos_estado CHECK (estado IN ('HABILITADO', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO')),
    CONSTRAINT chk_cubiculos_capacidad CHECK (capacidad > 0)
);

-- M05, PDF pp. impresas 36-43 (fisicas 37-44). RF-M05-19/20/21/22/29/30/31/32/54/76; RN-M05-03/04/06/14/15.
CREATE TABLE reservas_cubiculo (
    id BIGSERIAL PRIMARY KEY,
    fecha_reserva TIMESTAMP NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000),
    fecha_cancelacion TIMESTAMP,
    motivo_cancelacion VARCHAR(2000),
    condiciones_aplicadas VARCHAR(4000),
    persona_id BIGINT NOT NULL,
    cubiculo_id BIGINT NOT NULL,
    politica_id BIGINT,
    responsable_cancelacion_id BIGINT,
    CONSTRAINT chk_reservas_cubiculo_estado CHECK (estado IN ('CONFIRMADA', 'EN_USO', 'FINALIZADA', 'CANCELADA', 'VENCIDA', 'NO_PRESENTADA')),
    CONSTRAINT chk_reservas_cubiculo_periodo CHECK (fecha_fin > fecha_inicio)
);

-- M05, PDF pp. impresas 36/39/42-43 (fisicas 37/40/43-44). RF-M05-42/43/44/46/47/50; RN-M05-18/19/20/21/22.
CREATE TABLE usos_cubiculo (
    id BIGSERIAL PRIMARY KEY,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(2000),
    asistencia_confirmada BOOLEAN,
    reserva_id BIGINT NOT NULL UNIQUE,
    horario_biblioteca_id BIGINT,
    CONSTRAINT chk_usos_cubiculo_periodo CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- M05, PDF pp. impresas 36/40-43 (fisicas 37/41-44). RF-M05-63/64/65/66/67/68; RN-M05-13/26/28.
CREATE TABLE bloqueos_cubiculo (
    id BIGSERIAL PRIMARY KEY,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    motivo VARCHAR(2000) NOT NULL,
    estatus BOOLEAN NOT NULL,
    cubiculo_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    CONSTRAINT chk_bloqueos_cubiculo_periodo CHECK (fecha_fin > fecha_inicio)
);

-- M05, PDF pp. impresas 36/40/42-43 (fisicas 37/41/43-44). RF-M05-60/61/62; RN-M05-10/30.
CREATE TABLE horarios_biblioteca (
    id BIGSERIAL PRIMARY KEY,
    dia_semana VARCHAR(20),
    hora_apertura TIME,
    hora_cierre TIME,
    fecha_especial DATE,
    cerrado BOOLEAN,
    estatus BOOLEAN NOT NULL
);

-- M05, PDF pp. impresas 36/41-43 (fisicas 37/42-44). RF-M05-69/70/71/72/73/74/75/76; RN-M05-11/16/17/23/27.
CREATE TABLE politicas_reserva_cubiculo (
    id BIGSERIAL PRIMARY KEY,
    tiempo_minimo INTEGER,
    tiempo_maximo INTEGER NOT NULL,
    anticipacion_max INTEGER NOT NULL,
    max_reservas INTEGER NOT NULL,
    tolerancia_llegada INTEGER,
    tipos_usuario_permitidos VARCHAR(1000),
    estatus BOOLEAN NOT NULL
);

-- M05, PDF pp. impresas 36-38 (fisicas 37-39) y PNG. RF-M05-04/15.
CREATE TABLE equipamientos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL
);

CREATE TABLE cubiculos_equipamientos (
    cubiculo_id BIGINT NOT NULL,
    equipamiento_id BIGINT NOT NULL,
    PRIMARY KEY (cubiculo_id, equipamiento_id)
);

-- M06/M07/M08: definiciones para integrar en V2; no ejecutar de forma aislada.

-- M06, p. impresa 44 (visor 45), elementos principales; RF-M06-23 (p. 46), RN-M06-06 (p. 50).
CREATE TABLE tipos_sancion (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL
);

-- M06, pp. impresas 45-46 (visor 46-47), RF-M06-01, RF-M06-09, RF-M06-14, RF-M06-16, RF-M06-18; RN-M06-02, RN-M06-03 (p. 49).
CREATE TABLE incidencias (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    estado VARCHAR(60) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    observaciones VARCHAR(3000),
    evidencias VARCHAR(5000),
    persona_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    ejemplar_id BIGINT,
    prestamo_id BIGINT,
    detalle_prestamo_id BIGINT,
    devolucion_id BIGINT,
    reserva_libro_id BIGINT,
    reserva_cubiculo_id BIGINT,
    uso_cubiculo_id BIGINT,
    CONSTRAINT chk_incidencias_1 CHECK (tipo IN ('RETRASO', 'DANO', 'EXTRAVIO', 'INCUMPLIMIENTO_RESERVACION', 'AUSENCIA_RESERVACION_CUBICULO', 'USO_INADECUADO_CUBICULO'))
);

-- M06, pp. impresas 44/49-51 (visor 45/50-52), RF-M06-74, RF-M06-80, RN-M06-06, RN-M06-31, RN-M06-32.
CREATE TABLE politicas_sancion (
    id BIGSERIAL PRIMARY KEY,
    tipo_incidencia VARCHAR(60) NOT NULL,
    tipo_sancion_id BIGINT NOT NULL,
    reglas VARCHAR(5000) NOT NULL,
    estatus BOOLEAN NOT NULL,
    tipo_usuario VARCHAR(100),
    dias_suspension INTEGER,
    monto_multa NUMERIC(12,2),
    multas_autorizadas BOOLEAN NOT NULL,
    permite_pagos_parciales BOOLEAN NOT NULL,
    servicios_restringidos VARCHAR(2000),
    CONSTRAINT chk_politicas_sancion_1 CHECK (tipo_incidencia IN ('RETRASO', 'DANO', 'EXTRAVIO', 'INCUMPLIMIENTO_RESERVACION', 'AUSENCIA_RESERVACION_CUBICULO', 'USO_INADECUADO_CUBICULO'))
);

-- M06, pp. impresas 46-47/49-51 (visor 47-48/50-52), RF-M06-21, RF-M06-32, RF-M06-80, RN-M06-04, RN-M06-05, RN-M06-12, RN-M06-27, RN-M06-28.
CREATE TABLE sanciones (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    estado VARCHAR(60) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    condiciones_cumplimiento VARCHAR(3000),
    fecha_fin DATE,
    fecha_resolucion TIMESTAMP WITHOUT TIME ZONE,
    motivo_cancelacion VARCHAR(2000),
    incidencia_id BIGINT NOT NULL,
    tipo_sancion_id BIGINT NOT NULL,
    politica_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    responsable_cancelacion_id BIGINT,
    CONSTRAINT chk_sanciones_1 CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'CUMPLIDA', 'RESUELTA', 'CANCELADA'))
);

-- M06, pp. impresas 47-50 (visor 48-51), RF-M06-43, RF-M06-49; RN-M06-11, RN-M06-12, RN-M06-13, RN-M06-14, RN-M06-18.
CREATE TABLE multas (
    id BIGSERIAL PRIMARY KEY,
    monto NUMERIC(12,2) NOT NULL,
    fecha_limite_pago DATE,
    estado VARCHAR(60) NOT NULL,
    fecha_generacion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    saldo_pendiente NUMERIC(12,2) NOT NULL,
    observaciones VARCHAR(3000),
    sancion_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT chk_multas_1 CHECK (monto >= 0),
    CONSTRAINT chk_multas_2 CHECK (saldo_pendiente >= 0)
);

-- M06, p. impresa 48 (visor 49), RF-M06-51, RF-M06-59; RN-M06-15, RN-M06-16, RN-M06-17, RN-M06-29 (pp. 50-51).
CREATE TABLE pagos_multa (
    id BIGSERIAL PRIMARY KEY,
    fecha_pago TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    metodo VARCHAR(100) NOT NULL,
    comprobante VARCHAR(1000),
    referencia VARCHAR(250),
    multa_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    CONSTRAINT chk_pagos_multa_1 CHECK (monto > 0)
);

-- M06, p. impresa 47 (visor 48), RF-M06-33, RF-M06-42; RN-M06-05, RN-M06-21, RN-M06-22, RN-M06-23 (pp. 49-50).
CREATE TABLE restricciones_servicio (
    id BIGSERIAL PRIMARY KEY,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    descripcion VARCHAR(2000) NOT NULL,
    estado VARCHAR(60) NOT NULL,
    servicio_afectado VARCHAR(150) NOT NULL,
    condiciones_levantamiento VARCHAR(3000),
    temporal BOOLEAN NOT NULL,
    sancion_id BIGINT NOT NULL,
    CONSTRAINT chk_restricciones_servicio_1 CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT chk_restricciones_servicio_2 CHECK (NOT temporal OR fecha_fin IS NOT NULL)
);

-- M06, p. impresa 48 (visor 49), RF-M06-61, RF-M06-67; RN-M06-19, RN-M06-20 (p. 50).
CREATE TABLE reposiciones_material (
    id BIGSERIAL PRIMARY KEY,
    descripcion VARCHAR(2000) NOT NULL,
    costo NUMERIC(12,2),
    estado VARCHAR(60) NOT NULL,
    condiciones VARCHAR(3000),
    material_reemplazo VARCHAR(2000),
    fecha_cumplimiento DATE,
    fecha_validacion TIMESTAMP WITHOUT TIME ZONE,
    sancion_id BIGINT NOT NULL,
    ejemplar_id BIGINT NOT NULL,
    ejemplar_reposicion_id BIGINT,
    validador_id BIGINT,
    CONSTRAINT chk_reposiciones_material_1 CHECK (costo IS NULL OR costo >= 0)
);

-- M06, pp. impresas 46-47/51 (visor 47-48/52), RF-M06-30, RF-M06-32; RN-M06-27, RN-M06-28, RN-M06-29, RN-M06-30.
CREATE TABLE historiales_sancion (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    estado_anterior VARCHAR(60),
    estado_nuevo VARCHAR(60) NOT NULL,
    observaciones VARCHAR(3000),
    sancion_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    CONSTRAINT chk_historiales_sancion_1 CHECK (estado_anterior IN ('PENDIENTE', 'ACTIVA', 'CUMPLIDA', 'RESUELTA', 'CANCELADA')),
    CONSTRAINT chk_historiales_sancion_2 CHECK (estado_nuevo IN ('PENDIENTE', 'ACTIVA', 'CUMPLIDA', 'RESUELTA', 'CANCELADA'))
);

-- M07, p. impresa 52 (visor 53), elementos principales; RF-M07-06 (p. 53), RN-M07-04 (p. 58).
CREATE TABLE tipos_notificacion (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL,
    obligatoria BOOLEAN NOT NULL
);

-- M07, pp. impresas 52/55-57 (visor 53/56-58), RF-M07-45, RF-M07-53, RF-M07-78, RF-M07-79; RN-M07-09, RN-M07-11, RN-M07-12.
CREATE TABLE canales_notificacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL,
    max_reintentos INTEGER,
    intervalo_reintento_minutos INTEGER,
    CONSTRAINT chk_canales_notificacion_1 CHECK (max_reintentos IS NULL OR max_reintentos >= 0),
    CONSTRAINT chk_canales_notificacion_2 CHECK (intervalo_reintento_minutos IS NULL OR intervalo_reintento_minutos >= 0)
);

-- M07, p. impresa 56 (visor 57), RF-M07-60, RF-M07-66; RN-M07-17, RN-M07-18, RN-M07-19, RN-M07-28 (pp. 58-59).
CREATE TABLE plantillas_notificacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    asunto VARCHAR(250) NOT NULL,
    mensaje VARCHAR(10000) NOT NULL,
    variables VARCHAR(3000),
    estatus BOOLEAN NOT NULL,
    tipo_notificacion_id BIGINT NOT NULL,
    canal_id BIGINT NOT NULL
);

-- M07, pp. impresas 53/55/57-59 (visor 54/56/58-60), RF-M07-02, RF-M07-07, RF-M07-38, RF-M07-44; RN-M07-02, RN-M07-04, RN-M07-05, RN-M07-10, RN-M07-14, RN-M07-28.
CREATE TABLE notificaciones (
    id BIGSERIAL PRIMARY KEY,
    asunto VARCHAR(250) NOT NULL,
    mensaje VARCHAR(10000) NOT NULL,
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    estado VARCHAR(60) NOT NULL,
    fecha_lectura TIMESTAMP WITHOUT TIME ZONE,
    archivada BOOLEAN NOT NULL,
    evento_origen VARCHAR(150),
    destinatario_id BIGINT NOT NULL,
    tipo_notificacion_id BIGINT NOT NULL,
    plantilla_id BIGINT,
    prestamo_id BIGINT,
    devolucion_id BIGINT,
    reserva_libro_id BIGINT,
    reserva_cubiculo_id BIGINT,
    sancion_id BIGINT,
    pago_multa_id BIGINT,
    reposicion_material_id BIGINT
);

-- M07, pp. impresas 55-56 (visor 56-57), RF-M07-47, RF-M07-53; RN-M07-10, RN-M07-11, RN-M07-12, RN-M07-13 (p. 58).
CREATE TABLE envios_notificacion (
    id BIGSERIAL PRIMARY KEY,
    fecha_envio TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    estado VARCHAR(60) NOT NULL,
    error VARCHAR(3000),
    numero_intento INTEGER NOT NULL,
    notificacion_id BIGINT NOT NULL,
    canal_id BIGINT NOT NULL,
    CONSTRAINT chk_envios_notificacion_1 CHECK (numero_intento > 0)
);

-- M07, p. impresa 56 (visor 57), RF-M07-54, RF-M07-59; RN-M07-15, RN-M07-16 (p. 58).
CREATE TABLE preferencias_notificacion (
    id BIGSERIAL PRIMARY KEY,
    permite_correo BOOLEAN NOT NULL,
    permite_sistema BOOLEAN NOT NULL,
    permite_sms BOOLEAN NOT NULL,
    estatus BOOLEAN NOT NULL,
    cuenta_id BIGINT NOT NULL UNIQUE
);

-- M07, p. impresa 57 (visor 58), RF-M07-67, RF-M07-75; RN-M07-20, RN-M07-21, RN-M07-22, RN-M07-23, RN-M07-25, RN-M07-27 (pp. 58-59).
CREATE TABLE avisos_generales (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(250) NOT NULL,
    mensaje VARCHAR(10000) NOT NULL,
    fecha_publicacion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fecha_expiracion TIMESTAMP WITHOUT TIME ZONE,
    estatus BOOLEAN NOT NULL,
    publico BOOLEAN NOT NULL,
    fecha_modificacion TIMESTAMP WITHOUT TIME ZONE,
    responsable_id BIGINT NOT NULL,
    responsable_modificacion_id BIGINT,
    CONSTRAINT chk_avisos_generales_1 CHECK (fecha_expiracion IS NULL OR fecha_expiracion > fecha_publicacion)
);

-- M07, p. impresa 57 (visor 58), RF-M07-70; RN-M07-24, RN-M07-25 (p. 59); atributos del diagrama.
CREATE TABLE destinatarios_aviso (
    id BIGSERIAL PRIMARY KEY,
    tipo_usuario VARCHAR(100) NOT NULL,
    id_referencia BIGINT,
    aviso_id BIGINT NOT NULL,
    rol_id BIGINT
);

-- M08, p. impresa 60 (visor 61), elementos principales; RF-M08-90 (p. 65), RN-M08-05, RN-M08-21, RN-M08-30 (pp. 66-67).
CREATE TABLE tipos_reporte (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL
);

CREATE TABLE tipos_reporte_roles (
    tipo_reporte_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (tipo_reporte_id, rol_id)
);

-- M08, pp. impresas 60/65-67 (visor 61/66-68), RF-M08-85, RF-M08-87; RN-M08-04, RN-M08-05, RN-M08-06, RN-M08-25, RN-M08-29.
CREATE TABLE reportes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    parametros VARCHAR(5000),
    estatus BOOLEAN NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    fecha_generacion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    filtros VARCHAR(5000),
    total_registros BIGINT,
    tipo_reporte_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    panel_id BIGINT,
    CONSTRAINT chk_reportes_1 CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_reportes_2 CHECK (total_registros IS NULL OR total_registros >= 0)
);

-- M08, pp. impresas 60/64-65 (visor 61/65-66), RF-M08-70, RF-M08-71, RF-M08-78, RF-M08-81, RF-M08-89; RN-M08-05, RN-M08-07 (p. 66).
CREATE TABLE configuraciones_reporte (
    id BIGSERIAL PRIMARY KEY,
    formato VARCHAR(60) NOT NULL,
    filtros VARCHAR(5000),
    programado BOOLEAN NOT NULL,
    estatus BOOLEAN NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE,
    reporte_id BIGINT NOT NULL,
    CONSTRAINT chk_configuraciones_reporte_1 CHECK (fecha_inicio IS NULL OR fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-25 (pp. 66-67).
CREATE TABLE paneles_estadisticos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    estatus BOOLEAN NOT NULL,
    fecha_actualizacion TIMESTAMP WITHOUT TIME ZONE
);

-- M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-10, RN-M08-11, RN-M08-15, RN-M08-16 (p. 66).
CREATE TABLE indicadores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000),
    formula VARCHAR(3000),
    estatus BOOLEAN NOT NULL,
    valor NUMERIC(12,2),
    unidad_medida VARCHAR(100),
    fecha_actualizacion TIMESTAMP WITHOUT TIME ZONE,
    panel_id BIGINT
);

-- M08, p. impresa 65 (visor 66), RF-M08-78, RF-M08-84; RN-M08-24, RN-M08-25 (p. 67).
CREATE TABLE exportaciones_reporte (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    formato VARCHAR(60) NOT NULL,
    archivo VARCHAR(1000),
    estado VARCHAR(60) NOT NULL,
    reporte_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL
);

-- M08, p. impresa 65 (visor 66), RF-M08-85, RF-M08-91; RN-M08-27, RN-M08-29 (p. 67).
CREATE TABLE historiales_reporte (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    parametros VARCHAR(5000),
    estado VARCHAR(60) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    filtros VARCHAR(5000),
    reporte_id BIGINT NOT NULL,
    tipo_reporte_id BIGINT NOT NULL,
    responsable_id BIGINT NOT NULL,
    CONSTRAINT chk_historiales_reporte_1 CHECK (fecha_fin >= fecha_inicio)
);


-- Relaciones, unicidad e integridad M01 y M09/M10.

ALTER TABLE personas ADD CONSTRAINT uq_personas_numero_empleado UNIQUE (numero_empleado);
ALTER TABLE estudiantes ADD CONSTRAINT uq_estudiantes_matricula UNIQUE (matricula);
ALTER TABLE estudiantes ADD CONSTRAINT fk_estudiantes_carrera_id FOREIGN KEY (carrera_id) REFERENCES carreras(id);
ALTER TABLE estudiantes ADD CONSTRAINT fk_estudiantes_persona FOREIGN KEY (id) REFERENCES personas(id);
ALTER TABLE docentes ADD CONSTRAINT fk_docentes_departamento_id FOREIGN KEY (departamento_id) REFERENCES departamentos(id);
ALTER TABLE docentes ADD CONSTRAINT fk_docentes_persona FOREIGN KEY (id) REFERENCES personas(id);
ALTER TABLE empleados ADD CONSTRAINT fk_empleados_departamento_id FOREIGN KEY (departamento_id) REFERENCES departamentos(id);
ALTER TABLE empleados ADD CONSTRAINT fk_empleados_persona FOREIGN KEY (id) REFERENCES personas(id);
ALTER TABLE usuarios_externos ADD CONSTRAINT fk_usuarios_externos_persona FOREIGN KEY (id) REFERENCES personas(id);
ALTER TABLE cuentas_usuario ADD CONSTRAINT uq_cuentas_usuario_usuario UNIQUE (usuario);
ALTER TABLE cuentas_usuario ADD CONSTRAINT uq_cuentas_usuario_persona_id UNIQUE (persona_id);
ALTER TABLE cuentas_usuario ADD CONSTRAINT fk_cuentas_usuario_persona_id FOREIGN KEY (persona_id) REFERENCES personas(id);
ALTER TABLE roles ADD CONSTRAINT uq_roles_nombre UNIQUE (nombre);
ALTER TABLE permisos ADD CONSTRAINT uq_permisos_nombre UNIQUE (nombre);
ALTER TABLE registros_visita ADD CONSTRAINT fk_registros_visita_persona_id FOREIGN KEY (persona_id) REFERENCES personas(id);
ALTER TABLE registros_visita ADD CONSTRAINT fk_registros_visita_motivo_id FOREIGN KEY (motivo_id) REFERENCES motivos_visita(id);
ALTER TABLE verificaciones_institucionales ADD CONSTRAINT fk_verificaciones_institucionales_cuenta_id FOREIGN KEY (cuenta_id) REFERENCES cuentas_usuario(id);
ALTER TABLE verificaciones_institucionales ADD CONSTRAINT fk_verificaciones_institucionales_responsable_id FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
ALTER TABLE configuraciones_sistema ADD CONSTRAINT fk_configuraciones_sistema_responsable_modificacion_id FOREIGN KEY (responsable_modificacion_id) REFERENCES cuentas_usuario(id);
ALTER TABLE parametros_sistema ADD CONSTRAINT uq_parametros_sistema_clave UNIQUE (clave);
ALTER TABLE parametros_sistema ADD CONSTRAINT fk_parametros_sistema_configuracion_id FOREIGN KEY (configuracion_id) REFERENCES configuraciones_sistema(id);
ALTER TABLE parametros_sistema ADD CONSTRAINT fk_parametros_sistema_responsable_modificacion_id FOREIGN KEY (responsable_modificacion_id) REFERENCES cuentas_usuario(id);
ALTER TABLE politicas_seguridad ADD CONSTRAINT fk_politicas_seguridad_responsable_modificacion_id FOREIGN KEY (responsable_modificacion_id) REFERENCES cuentas_usuario(id);
ALTER TABLE bitacoras_auditoria ADD CONSTRAINT fk_bitacoras_auditoria_responsable_id FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
ALTER TABLE detalles_cambio ADD CONSTRAINT fk_detalles_cambio_evento_id FOREIGN KEY (evento_id) REFERENCES bitacoras_auditoria(id);
ALTER TABLE sesiones_usuario ADD CONSTRAINT fk_sesiones_usuario_cuenta_id FOREIGN KEY (cuenta_id) REFERENCES cuentas_usuario(id);
ALTER TABLE sesiones_usuario ADD CONSTRAINT fk_sesiones_usuario_cerrada_por_id FOREIGN KEY (cerrada_por_id) REFERENCES cuentas_usuario(id);
ALTER TABLE respaldos_sistema ADD CONSTRAINT fk_respaldos_sistema_responsable_id FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
ALTER TABLE respaldos_sistema ADD CONSTRAINT fk_respaldos_sistema_evento_auditoria_id FOREIGN KEY (evento_auditoria_id) REFERENCES bitacoras_auditoria(id);
ALTER TABLE incidentes_seguridad ADD CONSTRAINT fk_incidentes_seguridad_cuenta_afectada_id FOREIGN KEY (cuenta_afectada_id) REFERENCES cuentas_usuario(id);
ALTER TABLE incidentes_seguridad ADD CONSTRAINT fk_incidentes_seguridad_responsable_id FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
ALTER TABLE cuentas_roles ADD CONSTRAINT fk_cuentas_roles_cuenta_id FOREIGN KEY (cuenta_id) REFERENCES cuentas_usuario(id);
ALTER TABLE cuentas_roles ADD CONSTRAINT fk_cuentas_roles_rol_id FOREIGN KEY (rol_id) REFERENCES roles(id);
ALTER TABLE roles_permisos ADD CONSTRAINT fk_roles_permisos_rol_id FOREIGN KEY (rol_id) REFERENCES roles(id);
ALTER TABLE roles_permisos ADD CONSTRAINT fk_roles_permisos_permiso_id FOREIGN KEY (permiso_id) REFERENCES permisos(id);
ALTER TABLE incidentes_auditoria ADD CONSTRAINT fk_incidentes_auditoria_incidente_id FOREIGN KEY (incidente_id) REFERENCES incidentes_seguridad(id);
ALTER TABLE incidentes_auditoria ADD CONSTRAINT fk_incidentes_auditoria_evento_id FOREIGN KEY (evento_id) REFERENCES bitacoras_auditoria(id);
ALTER TABLE personas ADD CONSTRAINT uq_personas_identificacion UNIQUE (tipo_identificacion, numero_identificacion);
CREATE UNIQUE INDEX uq_registros_visita_persona_abierta ON registros_visita(persona_id) WHERE fecha_salida IS NULL;
ALTER TABLE registros_visita ADD CONSTRAINT chk_registros_visita_fechas CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada);
ALTER TABLE estudiantes ADD CONSTRAINT chk_estudiantes_semestre CHECK (semestre IS NULL OR semestre > 0);
ALTER TABLE sesiones_usuario ADD CONSTRAINT chk_sesiones_usuario_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio);
ALTER TABLE sesiones_usuario ADD CONSTRAINT chk_sesiones_usuario_actividad CHECK (ultima_actividad IS NULL OR ultima_actividad >= fecha_inicio);
ALTER TABLE politicas_seguridad ADD CONSTRAINT chk_politicas_seguridad_inactividad CHECK (inactividad_maxima_minutos IS NULL OR inactividad_maxima_minutos > 0);
ALTER TABLE politicas_seguridad ADD CONSTRAINT chk_politicas_seguridad_intentos CHECK (intentos_fallidos_maximos IS NULL OR intentos_fallidos_maximos > 0);
ALTER TABLE politicas_seguridad ADD CONSTRAINT chk_politicas_seguridad_bloqueo CHECK (bloqueo_temporal_minutos IS NULL OR bloqueo_temporal_minutos >= 0);

ALTER TABLE libros ADD CONSTRAINT fk_libros_editorial FOREIGN KEY (editorial_id) REFERENCES editoriales(id), ADD CONSTRAINT fk_libros_idioma FOREIGN KEY (idioma_id) REFERENCES idiomas(id);
ALTER TABLE ejemplares ADD CONSTRAINT fk_ejemplares_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id);
ALTER TABLE libros_autores ADD CONSTRAINT fk_libros_autores_libro FOREIGN KEY (libro_id) REFERENCES libros(id), ADD CONSTRAINT fk_libros_autores_autor FOREIGN KEY (autor_id) REFERENCES autores(id);
ALTER TABLE libros_categorias ADD CONSTRAINT fk_libros_categorias_libro FOREIGN KEY (libro_id) REFERENCES libros(id), ADD CONSTRAINT fk_libros_categorias_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id);
ALTER TABLE historiales_estado_ejemplar ADD CONSTRAINT fk_historial_ejemplar FOREIGN KEY (ejemplar_id) REFERENCES ejemplares(id), ADD CONSTRAINT fk_historial_ejemplar_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_libros_editorial ON libros(editorial_id);
CREATE INDEX idx_libros_idioma ON libros(idioma_id);
CREATE INDEX idx_ejemplares_ubicacion ON ejemplares(ubicacion_id);
CREATE INDEX idx_historial_ejemplar_fecha ON historiales_estado_ejemplar(ejemplar_id, fecha);

-- Integrar despues de crear todas las tablas del modelo.
ALTER TABLE prestamos ADD CONSTRAINT fk_prestamos_persona FOREIGN KEY (persona_id) REFERENCES personas(id);
CREATE INDEX idx_prestamos_persona_id ON prestamos(persona_id);
ALTER TABLE prestamos ADD CONSTRAINT fk_prestamos_bibliotecario FOREIGN KEY (bibliotecario_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_prestamos_bibliotecario_id ON prestamos(bibliotecario_id);
ALTER TABLE prestamos ADD CONSTRAINT fk_prestamos_politica FOREIGN KEY (politica_id) REFERENCES politicas_prestamo(id);
CREATE INDEX idx_prestamos_politica_id ON prestamos(politica_id);
ALTER TABLE detalles_prestamo ADD CONSTRAINT fk_detalles_prestamo_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id);
CREATE INDEX idx_detalles_prestamo_prestamo_id ON detalles_prestamo(prestamo_id);
ALTER TABLE detalles_prestamo ADD CONSTRAINT fk_detalles_prestamo_ejemplar FOREIGN KEY (ejemplar_id) REFERENCES ejemplares(id);
CREATE INDEX idx_detalles_prestamo_ejemplar_id ON detalles_prestamo(ejemplar_id);
ALTER TABLE devoluciones ADD CONSTRAINT fk_devoluciones_detalle_prestamo FOREIGN KEY (detalle_prestamo_id) REFERENCES detalles_prestamo(id);
ALTER TABLE devoluciones ADD CONSTRAINT fk_devoluciones_bibliotecario FOREIGN KEY (bibliotecario_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_devoluciones_bibliotecario_id ON devoluciones(bibliotecario_id);
ALTER TABLE renovaciones ADD CONSTRAINT fk_renovaciones_detalle_prestamo FOREIGN KEY (detalle_prestamo_id) REFERENCES detalles_prestamo(id);
CREATE INDEX idx_renovaciones_detalle_prestamo_id ON renovaciones(detalle_prestamo_id);
ALTER TABLE renovaciones ADD CONSTRAINT fk_renovaciones_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_renovaciones_responsable_id ON renovaciones(responsable_id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_persona FOREIGN KEY (persona_id) REFERENCES personas(id);
CREATE INDEX idx_reservas_libro_persona_id ON reservas_libro(persona_id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_libro FOREIGN KEY (libro_id) REFERENCES libros(id);
CREATE INDEX idx_reservas_libro_libro_id ON reservas_libro(libro_id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_politica FOREIGN KEY (politica_id) REFERENCES politicas_reserva_libro(id);
CREATE INDEX idx_reservas_libro_politica_id ON reservas_libro(politica_id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_lista_espera FOREIGN KEY (lista_espera_id) REFERENCES listas_espera(id);
CREATE INDEX idx_reservas_libro_lista_espera_id ON reservas_libro(lista_espera_id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_detalle_prestamo FOREIGN KEY (detalle_prestamo_id) REFERENCES detalles_prestamo(id);
ALTER TABLE reservas_libro ADD CONSTRAINT fk_reservas_libro_responsable_cancelacion FOREIGN KEY (responsable_cancelacion_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_reservas_libro_responsable_cancelacion_id ON reservas_libro(responsable_cancelacion_id);
ALTER TABLE listas_espera ADD CONSTRAINT fk_listas_espera_libro FOREIGN KEY (libro_id) REFERENCES libros(id);
ALTER TABLE asignaciones_reserva ADD CONSTRAINT fk_asignaciones_reserva_reserva FOREIGN KEY (reserva_id) REFERENCES reservas_libro(id);
CREATE INDEX idx_asignaciones_reserva_reserva_id ON asignaciones_reserva(reserva_id);
ALTER TABLE asignaciones_reserva ADD CONSTRAINT fk_asignaciones_reserva_ejemplar FOREIGN KEY (ejemplar_id) REFERENCES ejemplares(id);
CREATE INDEX idx_asignaciones_reserva_ejemplar_id ON asignaciones_reserva(ejemplar_id);
ALTER TABLE historiales_reserva ADD CONSTRAINT fk_historiales_reserva_reserva FOREIGN KEY (reserva_id) REFERENCES reservas_libro(id);
CREATE INDEX idx_historiales_reserva_reserva_id ON historiales_reserva(reserva_id);
ALTER TABLE historiales_reserva ADD CONSTRAINT fk_historiales_reserva_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_historiales_reserva_responsable_id ON historiales_reserva(responsable_id);
ALTER TABLE reservas_cubiculo ADD CONSTRAINT fk_reservas_cubiculo_persona FOREIGN KEY (persona_id) REFERENCES personas(id);
CREATE INDEX idx_reservas_cubiculo_persona_id ON reservas_cubiculo(persona_id);
ALTER TABLE reservas_cubiculo ADD CONSTRAINT fk_reservas_cubiculo_cubiculo FOREIGN KEY (cubiculo_id) REFERENCES cubiculos(id);
CREATE INDEX idx_reservas_cubiculo_cubiculo_id ON reservas_cubiculo(cubiculo_id);
ALTER TABLE reservas_cubiculo ADD CONSTRAINT fk_reservas_cubiculo_politica FOREIGN KEY (politica_id) REFERENCES politicas_reserva_cubiculo(id);
CREATE INDEX idx_reservas_cubiculo_politica_id ON reservas_cubiculo(politica_id);
ALTER TABLE reservas_cubiculo ADD CONSTRAINT fk_reservas_cubiculo_responsable_cancelacion FOREIGN KEY (responsable_cancelacion_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_reservas_cubiculo_responsable_cancelacion_id ON reservas_cubiculo(responsable_cancelacion_id);
ALTER TABLE usos_cubiculo ADD CONSTRAINT fk_usos_cubiculo_reserva FOREIGN KEY (reserva_id) REFERENCES reservas_cubiculo(id);
ALTER TABLE usos_cubiculo ADD CONSTRAINT fk_usos_cubiculo_horario_biblioteca FOREIGN KEY (horario_biblioteca_id) REFERENCES horarios_biblioteca(id);
CREATE INDEX idx_usos_cubiculo_horario_biblioteca_id ON usos_cubiculo(horario_biblioteca_id);
ALTER TABLE bloqueos_cubiculo ADD CONSTRAINT fk_bloqueos_cubiculo_cubiculo FOREIGN KEY (cubiculo_id) REFERENCES cubiculos(id);
CREATE INDEX idx_bloqueos_cubiculo_cubiculo_id ON bloqueos_cubiculo(cubiculo_id);
ALTER TABLE bloqueos_cubiculo ADD CONSTRAINT fk_bloqueos_cubiculo_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_bloqueos_cubiculo_responsable_id ON bloqueos_cubiculo(responsable_id);
ALTER TABLE cubiculos_equipamientos ADD CONSTRAINT fk_cubiculos_equipamientos_cubiculo FOREIGN KEY (cubiculo_id) REFERENCES cubiculos(id);
ALTER TABLE cubiculos_equipamientos ADD CONSTRAINT fk_cubiculos_equipamientos_equipamiento FOREIGN KEY (equipamiento_id) REFERENCES equipamientos(id);
CREATE INDEX idx_cubiculos_equipamientos_equipamiento ON cubiculos_equipamientos(equipamiento_id);

-- M06/M07/M08: FK e indices despues de crear todas las tablas de V2.

ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_persona FOREIGN KEY (persona_id) REFERENCES personas(id);
CREATE INDEX idx_incidencias_persona_id ON incidencias(persona_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_incidencias_responsable_id ON incidencias(responsable_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_ejemplar FOREIGN KEY (ejemplar_id) REFERENCES ejemplares(id);
CREATE INDEX idx_incidencias_ejemplar_id ON incidencias(ejemplar_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id);
CREATE INDEX idx_incidencias_prestamo_id ON incidencias(prestamo_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_detalle_prestamo FOREIGN KEY (detalle_prestamo_id) REFERENCES detalles_prestamo(id);
CREATE INDEX idx_incidencias_detalle_prestamo_id ON incidencias(detalle_prestamo_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_devolucion FOREIGN KEY (devolucion_id) REFERENCES devoluciones(id);
CREATE INDEX idx_incidencias_devolucion_id ON incidencias(devolucion_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_reserva_libro FOREIGN KEY (reserva_libro_id) REFERENCES reservas_libro(id);
CREATE INDEX idx_incidencias_reserva_libro_id ON incidencias(reserva_libro_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_reserva_cubiculo FOREIGN KEY (reserva_cubiculo_id) REFERENCES reservas_cubiculo(id);
CREATE INDEX idx_incidencias_reserva_cubiculo_id ON incidencias(reserva_cubiculo_id);
ALTER TABLE incidencias ADD CONSTRAINT fk_incidencias_uso_cubiculo FOREIGN KEY (uso_cubiculo_id) REFERENCES usos_cubiculo(id);
CREATE INDEX idx_incidencias_uso_cubiculo_id ON incidencias(uso_cubiculo_id);

ALTER TABLE politicas_sancion ADD CONSTRAINT fk_politicas_sancion_tipo_sancion FOREIGN KEY (tipo_sancion_id) REFERENCES tipos_sancion(id);
CREATE INDEX idx_politicas_sancion_tipo_sancion_id ON politicas_sancion(tipo_sancion_id);

ALTER TABLE sanciones ADD CONSTRAINT fk_sanciones_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias(id);
CREATE INDEX idx_sanciones_incidencia_id ON sanciones(incidencia_id);
ALTER TABLE sanciones ADD CONSTRAINT fk_sanciones_tipo_sancion FOREIGN KEY (tipo_sancion_id) REFERENCES tipos_sancion(id);
CREATE INDEX idx_sanciones_tipo_sancion_id ON sanciones(tipo_sancion_id);
ALTER TABLE sanciones ADD CONSTRAINT fk_sanciones_politica FOREIGN KEY (politica_id) REFERENCES politicas_sancion(id);
CREATE INDEX idx_sanciones_politica_id ON sanciones(politica_id);
ALTER TABLE sanciones ADD CONSTRAINT fk_sanciones_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_sanciones_responsable_id ON sanciones(responsable_id);
ALTER TABLE sanciones ADD CONSTRAINT fk_sanciones_responsable_cancelacion FOREIGN KEY (responsable_cancelacion_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_sanciones_responsable_cancelacion_id ON sanciones(responsable_cancelacion_id);

ALTER TABLE multas ADD CONSTRAINT fk_multas_sancion FOREIGN KEY (sancion_id) REFERENCES sanciones(id);

ALTER TABLE pagos_multa ADD CONSTRAINT fk_pagos_multa_multa FOREIGN KEY (multa_id) REFERENCES multas(id);
CREATE INDEX idx_pagos_multa_multa_id ON pagos_multa(multa_id);
ALTER TABLE pagos_multa ADD CONSTRAINT fk_pagos_multa_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_pagos_multa_responsable_id ON pagos_multa(responsable_id);

ALTER TABLE restricciones_servicio ADD CONSTRAINT fk_restricciones_servicio_sancion FOREIGN KEY (sancion_id) REFERENCES sanciones(id);
CREATE INDEX idx_restricciones_servicio_sancion_id ON restricciones_servicio(sancion_id);

ALTER TABLE reposiciones_material ADD CONSTRAINT fk_reposiciones_material_sancion FOREIGN KEY (sancion_id) REFERENCES sanciones(id);
CREATE INDEX idx_reposiciones_material_sancion_id ON reposiciones_material(sancion_id);
ALTER TABLE reposiciones_material ADD CONSTRAINT fk_reposiciones_material_ejemplar FOREIGN KEY (ejemplar_id) REFERENCES ejemplares(id);
CREATE INDEX idx_reposiciones_material_ejemplar_id ON reposiciones_material(ejemplar_id);
ALTER TABLE reposiciones_material ADD CONSTRAINT fk_reposiciones_material_ejemplar_reposicion FOREIGN KEY (ejemplar_reposicion_id) REFERENCES ejemplares(id);
CREATE INDEX idx_reposiciones_material_ejemplar_reposicion_id ON reposiciones_material(ejemplar_reposicion_id);
ALTER TABLE reposiciones_material ADD CONSTRAINT fk_reposiciones_material_validador FOREIGN KEY (validador_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_reposiciones_material_validador_id ON reposiciones_material(validador_id);

ALTER TABLE historiales_sancion ADD CONSTRAINT fk_historiales_sancion_sancion FOREIGN KEY (sancion_id) REFERENCES sanciones(id);
CREATE INDEX idx_historiales_sancion_sancion_id ON historiales_sancion(sancion_id);
ALTER TABLE historiales_sancion ADD CONSTRAINT fk_historiales_sancion_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_historiales_sancion_responsable_id ON historiales_sancion(responsable_id);



ALTER TABLE plantillas_notificacion ADD CONSTRAINT fk_plantillas_notificacion_tipo_notificacion FOREIGN KEY (tipo_notificacion_id) REFERENCES tipos_notificacion(id);
CREATE INDEX idx_plantillas_notificacion_tipo_notificacion_id ON plantillas_notificacion(tipo_notificacion_id);
ALTER TABLE plantillas_notificacion ADD CONSTRAINT fk_plantillas_notificacion_canal FOREIGN KEY (canal_id) REFERENCES canales_notificacion(id);
CREATE INDEX idx_plantillas_notificacion_canal_id ON plantillas_notificacion(canal_id);

ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_destinatario FOREIGN KEY (destinatario_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_notificaciones_destinatario_id ON notificaciones(destinatario_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_tipo_notificacion FOREIGN KEY (tipo_notificacion_id) REFERENCES tipos_notificacion(id);
CREATE INDEX idx_notificaciones_tipo_notificacion_id ON notificaciones(tipo_notificacion_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantillas_notificacion(id);
CREATE INDEX idx_notificaciones_plantilla_id ON notificaciones(plantilla_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos(id);
CREATE INDEX idx_notificaciones_prestamo_id ON notificaciones(prestamo_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_devolucion FOREIGN KEY (devolucion_id) REFERENCES devoluciones(id);
CREATE INDEX idx_notificaciones_devolucion_id ON notificaciones(devolucion_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_reserva_libro FOREIGN KEY (reserva_libro_id) REFERENCES reservas_libro(id);
CREATE INDEX idx_notificaciones_reserva_libro_id ON notificaciones(reserva_libro_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_reserva_cubiculo FOREIGN KEY (reserva_cubiculo_id) REFERENCES reservas_cubiculo(id);
CREATE INDEX idx_notificaciones_reserva_cubiculo_id ON notificaciones(reserva_cubiculo_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_sancion FOREIGN KEY (sancion_id) REFERENCES sanciones(id);
CREATE INDEX idx_notificaciones_sancion_id ON notificaciones(sancion_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_pago_multa FOREIGN KEY (pago_multa_id) REFERENCES pagos_multa(id);
CREATE INDEX idx_notificaciones_pago_multa_id ON notificaciones(pago_multa_id);
ALTER TABLE notificaciones ADD CONSTRAINT fk_notificaciones_reposicion_material FOREIGN KEY (reposicion_material_id) REFERENCES reposiciones_material(id);
CREATE INDEX idx_notificaciones_reposicion_material_id ON notificaciones(reposicion_material_id);

ALTER TABLE envios_notificacion ADD CONSTRAINT fk_envios_notificacion_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones(id);
CREATE INDEX idx_envios_notificacion_notificacion_id ON envios_notificacion(notificacion_id);
ALTER TABLE envios_notificacion ADD CONSTRAINT fk_envios_notificacion_canal FOREIGN KEY (canal_id) REFERENCES canales_notificacion(id);
CREATE INDEX idx_envios_notificacion_canal_id ON envios_notificacion(canal_id);

ALTER TABLE preferencias_notificacion ADD CONSTRAINT fk_preferencias_notificacion_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas_usuario(id);

ALTER TABLE avisos_generales ADD CONSTRAINT fk_avisos_generales_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_avisos_generales_responsable_id ON avisos_generales(responsable_id);
ALTER TABLE avisos_generales ADD CONSTRAINT fk_avisos_generales_responsable_modificacion FOREIGN KEY (responsable_modificacion_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_avisos_generales_responsable_modificacion_id ON avisos_generales(responsable_modificacion_id);

ALTER TABLE destinatarios_aviso ADD CONSTRAINT fk_destinatarios_aviso_aviso FOREIGN KEY (aviso_id) REFERENCES avisos_generales(id);
CREATE INDEX idx_destinatarios_aviso_aviso_id ON destinatarios_aviso(aviso_id);
ALTER TABLE destinatarios_aviso ADD CONSTRAINT fk_destinatarios_aviso_rol FOREIGN KEY (rol_id) REFERENCES roles(id);
CREATE INDEX idx_destinatarios_aviso_rol_id ON destinatarios_aviso(rol_id);

ALTER TABLE tipos_reporte_roles ADD CONSTRAINT fk_tipos_reporte_roles_tipo_reporte FOREIGN KEY (tipo_reporte_id) REFERENCES tipos_reporte(id);
ALTER TABLE tipos_reporte_roles ADD CONSTRAINT fk_tipos_reporte_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id);
CREATE INDEX idx_tipos_reporte_roles_rol_id ON tipos_reporte_roles(rol_id);

ALTER TABLE reportes ADD CONSTRAINT fk_reportes_tipo_reporte FOREIGN KEY (tipo_reporte_id) REFERENCES tipos_reporte(id);
CREATE INDEX idx_reportes_tipo_reporte_id ON reportes(tipo_reporte_id);
ALTER TABLE reportes ADD CONSTRAINT fk_reportes_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_reportes_responsable_id ON reportes(responsable_id);
ALTER TABLE reportes ADD CONSTRAINT fk_reportes_panel FOREIGN KEY (panel_id) REFERENCES paneles_estadisticos(id);
CREATE INDEX idx_reportes_panel_id ON reportes(panel_id);

ALTER TABLE configuraciones_reporte ADD CONSTRAINT fk_configuraciones_reporte_reporte FOREIGN KEY (reporte_id) REFERENCES reportes(id);
CREATE INDEX idx_configuraciones_reporte_reporte_id ON configuraciones_reporte(reporte_id);


ALTER TABLE indicadores ADD CONSTRAINT fk_indicadores_panel FOREIGN KEY (panel_id) REFERENCES paneles_estadisticos(id);
CREATE INDEX idx_indicadores_panel_id ON indicadores(panel_id);

ALTER TABLE exportaciones_reporte ADD CONSTRAINT fk_exportaciones_reporte_reporte FOREIGN KEY (reporte_id) REFERENCES reportes(id);
CREATE INDEX idx_exportaciones_reporte_reporte_id ON exportaciones_reporte(reporte_id);
ALTER TABLE exportaciones_reporte ADD CONSTRAINT fk_exportaciones_reporte_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_exportaciones_reporte_responsable_id ON exportaciones_reporte(responsable_id);

ALTER TABLE historiales_reporte ADD CONSTRAINT fk_historiales_reporte_reporte FOREIGN KEY (reporte_id) REFERENCES reportes(id);
CREATE INDEX idx_historiales_reporte_reporte_id ON historiales_reporte(reporte_id);
ALTER TABLE historiales_reporte ADD CONSTRAINT fk_historiales_reporte_tipo_reporte FOREIGN KEY (tipo_reporte_id) REFERENCES tipos_reporte(id);
CREATE INDEX idx_historiales_reporte_tipo_reporte_id ON historiales_reporte(tipo_reporte_id);
ALTER TABLE historiales_reporte ADD CONSTRAINT fk_historiales_reporte_responsable FOREIGN KEY (responsable_id) REFERENCES cuentas_usuario(id);
CREATE INDEX idx_historiales_reporte_responsable_id ON historiales_reporte(responsable_id);



