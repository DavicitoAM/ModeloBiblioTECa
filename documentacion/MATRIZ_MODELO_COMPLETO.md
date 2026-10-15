# Matriz del modelo completo: clase, tabla, atributos, relaciones y fuente

Inventario de las anotaciones Java y las declaraciones SQL de V1/V2: **70 entidades y nueve enums**. Describe archivos y DDL; no acredita validación contra PostgreSQL. Los resultados reales están en [PRUEBA_Y_EVIDENCIAS.md](PRUEBA_Y_EVIDENCIAS.md).

Los campos mappedBy son inversos y no añaden columna. Las N:M usan tablas de unión. Los subtipos Persona heredan la PK y los campos comunes, que permanecen en personas. Las restricciones UNIQUE, CHECK, FK e índices completos se consultan en V1/V2; esta matriz identifica los campos y contratos JPA. Las reglas pendientes están en [DECISIONES_PENDIENTES.md](DECISIONES_PENDIENTES.md).

## M01 — Personas y usuarios

Paquete modelo.personas; PDF: impresas 8–14, físicas 9–15.

### Carrera

Archivo: [Carrera](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Carrera.java). Tabla: `carreras`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-9 (fisicas 9-10), RF-M01-02; atributos del diagrama M01. Decision aprobada: existe una unica Carrera para estudiantes; no se duplica la casilla rotulada Carrera que contiene atributos de Docente.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `carreras.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `carreras.nombre` | VARCHAR(150) NOT NULL |
| `estatus` | `Boolean` | `carreras.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `estudiantes` | `Set<Estudiante>` | Inversa de `Estudiante.carrera` | OneToMany; LAZY; sin columna propia |

### CuentaUsuario

Archivo: [CuentaUsuario](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/CuentaUsuario.java). Tabla: `cuentas_usuario`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 10-14 (fisicas 11-15), RF-M01-11/18/19/38/39/41, RN-M01-04/05/06/12. Cuenta pertenece a una sola Persona mediante FK UNIQUE; Persona puede no tener cuenta. Decision aprobada: roles N:M. contrasenaHash representa exclusivamente un hash, nunca texto plano; no se incluyen valores de credenciales ni servicios de autenticacion. Estados de verificacion abiertos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `cuentas_usuario.id` | BIGSERIAL PRIMARY KEY |
| `usuario` | `String` | `cuentas_usuario.usuario` | VARCHAR(100) NOT NULL |
| `contrasenaHash` | `String` | `cuentas_usuario.contrasena_hash` | VARCHAR(255) NOT NULL |
| `fechaCreacion` | `LocalDateTime` | `cuentas_usuario.fecha_creacion` | TIMESTAMP NOT NULL |
| `ultimoAcceso` | `LocalDateTime` | `cuentas_usuario.ultimo_acceso` | TIMESTAMP |
| `fechaVerificacionCorreo` | `LocalDateTime` | `cuentas_usuario.fecha_verificacion_correo` | TIMESTAMP |
| `estadoVerificacion` | `String` | `cuentas_usuario.estado_verificacion` | VARCHAR(60) |
| `estatus` | `Boolean` | `cuentas_usuario.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `persona` | `Persona` | `cuentas_usuario.persona_id` → `Persona.id` | BIGINT NOT NULL; OneToOne LAZY; obligatorio |
| `roles` | `Set<Rol>` | `cuentas_roles` (`cuenta_id`, `rol_id`) | ManyToMany; LAZY; unión hacia Rol |
| `verificaciones` | `Set<VerificacionInstitucional>` | Inversa de `VerificacionInstitucional.cuenta` | OneToMany; LAZY; sin columna propia |
| `sesiones` | `Set<SesionUsuario>` | Inversa de `SesionUsuario.cuenta` | OneToMany; LAZY; sin columna propia |

### Departamento

Archivo: [Departamento](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Departamento.java). Tabla: `departamentos`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-9 (fisicas 9-10), RF-M01-03/04; diagrama M01. Decision aprobada: adscripcion de Docente y opcionalmente Empleado a Departamento. La asociacion no reemplaza el area de trabajo narrativa del Empleado.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `departamentos.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `departamentos.nombre` | VARCHAR(150) NOT NULL |
| `estatus` | `Boolean` | `departamentos.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `docentes` | `Set<Docente>` | Inversa de `Docente.departamento` | OneToMany; LAZY; sin columna propia |
| `empleados` | `Set<Empleado>` | Inversa de `Empleado.departamento` | OneToMany; LAZY; sin columna propia |

### Docente

Archivo: [Docente](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Docente.java). Tabla: `docentes`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-03/05, RN-M01-03/17. Decision aprobada: numeroEmpleado y PK se heredan de Persona; departamento academico es una asociacion a Departamento. No se reproduce la segunda casilla Carrera ambigua del diagrama.

Herencia JOINED desde `Persona`. `docentes.id` es PK/FK a personas.id; los demás campos heredados se guardan en personas, sin duplicarlos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| id (heredado) | Long | `docentes.id` → personas.id | BIGINT PRIMARY KEY y FK; sin IDENTITY propio |
| `departamento` | `Departamento` | `docentes.departamento_id` → `Departamento.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Empleado

Archivo: [Empleado](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Empleado.java). Tabla: `empleados`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-04/05, RN-M01-03/17. Hereda PK y numeroEmpleado de Persona. Area de trabajo procede del PDF; la relacion opcional con Departamento permite la adscripcion aprobada sin sustituir el area ni inventar un departamento.

Herencia JOINED desde `Persona`. `empleados.id` es PK/FK a personas.id; los demás campos heredados se guardan en personas, sin duplicarlos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| id (heredado) | Long | `empleados.id` → personas.id | BIGINT PRIMARY KEY y FK; sin IDENTITY propio |
| `areaTrabajo` | `String` | `empleados.area_trabajo` | VARCHAR(150) NOT NULL |
| `puesto` | `String` | `empleados.puesto` | VARCHAR(150) |
| `departamento` | `Departamento` | `empleados.departamento_id` → `Departamento.id` | BIGINT; ManyToOne LAZY; opcional |

### Estudiante

Archivo: [Estudiante](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Estudiante.java). Tabla: `estudiantes`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-02/05, RN-M01-02. Hereda la PK de Persona, sin idEstudiante adicional. Estado academico procede de la narrativa; su vocabulario institucional no esta cerrado, por lo que se conserva como texto sin inventar valores.

Herencia JOINED desde `Persona`. `estudiantes.id` es PK/FK a personas.id; los demás campos heredados se guardan en personas, sin duplicarlos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| id (heredado) | Long | `estudiantes.id` → personas.id | BIGINT PRIMARY KEY y FK; sin IDENTITY propio |
| `matricula` | `String` | `estudiantes.matricula` | VARCHAR(40) NOT NULL |
| `semestre` | `Integer` | `estudiantes.semestre` | INTEGER |
| `estadoAcademico` | `String` | `estudiantes.estado_academico` | VARCHAR(80) |
| `carrera` | `Carrera` | `estudiantes.carrera_id` → `Carrera.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### MotivoVisita

Archivo: [MotivoVisita](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/MotivoVisita.java). Tabla: `motivos_visita`.

**Fuente y alcance del Javadoc:** M01, PDF p. impresa 11 (fisica 12), RF-M01-27; atributos y relacion del diagrama M01. Catalogo persistente de motivos, sin inventar ni precargar valores institucionales.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `motivos_visita.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `motivos_visita.nombre` | VARCHAR(100) NOT NULL |
| `descripcion` | `String` | `motivos_visita.descripcion` | VARCHAR(500) |
| `estatus` | `Boolean` | `motivos_visita.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `visitas` | `Set<RegistroVisita>` | Inversa de `RegistroVisita.motivo` | OneToMany; LAZY; sin columna propia |

### Permiso

Archivo: [Permiso](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Permiso.java). Tabla: `permisos`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 11 y 13-14 (fisicas 12 y 14-15), RF-M01-21/22/23, RN-M01-08/11/13. Decision aprobada: permisos reutilizables entre roles mediante N:M. La existencia del modelo no implementa control de acceso ni autoriza operaciones del usuario.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `permisos.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `permisos.nombre` | VARCHAR(120) NOT NULL |
| `descripcion` | `String` | `permisos.descripcion` | VARCHAR(500) |
| `estatus` | `Boolean` | `permisos.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `roles` | `Set<Rol>` | Inversa de `Rol.permisos` | ManyToMany; LAZY; sin columna propia |

### Persona

Archivo: [Persona](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Persona.java). Tabla: `personas`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8-10 y 13-14 (fisicas 9-11 y 14-15), RF-M01-01/05/09, RN-M01-01/03/04/09/17. Decision aprobada: herencia JOINED e ID Long comun; numeroEmpleado se centraliza aqui para garantizar unicidad entre Docente y Empleado. Fecha de registro proviene de narrativa; fecha de nacimiento e identificacion provienen del diagrama. No contiene credenciales.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `personas.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `personas.nombre` | VARCHAR(100) NOT NULL |
| `apellidos` | `String` | `personas.apellidos` | VARCHAR(150) NOT NULL |
| `correo` | `String` | `personas.correo` | VARCHAR(254) |
| `telefono` | `String` | `personas.telefono` | VARCHAR(30) |
| `fechaNacimiento` | `LocalDate` | `personas.fecha_nacimiento` | DATE |
| `fechaRegistro` | `LocalDateTime` | `personas.fecha_registro` | TIMESTAMP NOT NULL |
| `tipoIdentificacion` | `String` | `personas.tipo_identificacion` | VARCHAR(60) |
| `numeroIdentificacion` | `String` | `personas.numero_identificacion` | VARCHAR(80) |
| `numeroEmpleado` | `String` | `personas.numero_empleado` | VARCHAR(40) |
| `estatus` | `Boolean` | `personas.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `cuenta` | `CuentaUsuario` | Inversa de `CuentaUsuario.persona` | OneToOne; LAZY; sin columna propia |
| `visitas` | `Set<RegistroVisita>` | Inversa de `RegistroVisita.persona` | OneToMany; LAZY; sin columna propia |

### RegistroVisita

Archivo: [RegistroVisita](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/RegistroVisita.java). Tabla: `registros_visita`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 11-14 (fisicas 12-15), RF-M01-24/25/26/27/28/30/45/46, RN-M01-19/20/21/22. Cada visita pertenece a Persona y MotivoVisita. Fecha de salida nula representa una visita abierta. La migracion aplica unicidad parcial de visita abierta por persona y orden temporal; las correcciones se documentan mediante BitacoraAuditoria, sin un servicio operativo adicional.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `registros_visita.id` | BIGSERIAL PRIMARY KEY |
| `fechaEntrada` | `LocalDateTime` | `registros_visita.fecha_entrada` | TIMESTAMP NOT NULL |
| `fechaSalida` | `LocalDateTime` | `registros_visita.fecha_salida` | TIMESTAMP |
| `observaciones` | `String` | `registros_visita.observaciones` | VARCHAR(1000) |
| `persona` | `Persona` | `registros_visita.persona_id` → `Persona.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `motivo` | `MotivoVisita` | `registros_visita.motivo_id` → `MotivoVisita.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Rol

Archivo: [Rol](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/Rol.java). Tabla: `roles`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 9, 11 y 13-14 (fisicas 10, 12 y 14-15), RF-M01-19/20/21/22, RN-M01-06/11/13. Bibliotecario y Administrador son valores de rol, no subclases de Persona. Decision aprobada: Rol-Permiso N:M sustituye el 1:1 ambiguo del grafico; no se precargan roles ni permisos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `roles.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `roles.nombre` | VARCHAR(80) NOT NULL |
| `descripcion` | `String` | `roles.descripcion` | VARCHAR(500) |
| `estatus` | `Boolean` | `roles.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `cuentas` | `Set<CuentaUsuario>` | Inversa de `CuentaUsuario.roles` | ManyToMany; LAZY; sin columna propia |
| `permisos` | `Set<Permiso>` | `roles_permisos` (`rol_id`, `permiso_id`) | ManyToMany; LAZY; unión hacia Permiso |

### UsuarioExterno

Archivo: [UsuarioExterno](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/UsuarioExterno.java). Tabla: `usuarios_externos`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 8 y 12-14 (fisicas 9 y 13-15), RF-M01-36/37, RN-M01-15. Hereda la identidad de Persona. Institucion es atributo del diagrama, no una clase adicional. El modelo no concede permisos ni crea una autenticacion de demostracion.

Herencia JOINED desde `Persona`. `usuarios_externos.id` es PK/FK a personas.id; los demás campos heredados se guardan en personas, sin duplicarlos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| id (heredado) | Long | `usuarios_externos.id` → personas.id | BIGINT PRIMARY KEY y FK; sin IDENTITY propio |
| `institucion` | `String` | `usuarios_externos.institucion` | VARCHAR(200) |

### VerificacionInstitucional

Archivo: [VerificacionInstitucional](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/personas/VerificacionInstitucional.java). Tabla: `verificaciones_institucionales`.

**Fuente y alcance del Javadoc:** M01, PDF pp. impresas 12-14 (fisicas 13-15), RF-M01-38/39/40/41, RN-M01-14/16/18. Decision de modelado: verificacion pertenece a CuentaUsuario por RF-M01-39; no a una visita. Tipo y resultado conservan texto porque las fuentes no fijan vocabularios cerrados. El responsable puede faltar en verificaciones automaticas; no se almacenan tokens ni secretos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `verificaciones_institucionales.id` | BIGSERIAL PRIMARY KEY |
| `tipo` | `String` | `verificaciones_institucionales.tipo` | VARCHAR(80) NOT NULL |
| `resultado` | `String` | `verificaciones_institucionales.resultado` | VARCHAR(80) |
| `fecha` | `LocalDateTime` | `verificaciones_institucionales.fecha` | TIMESTAMP NOT NULL |
| `observaciones` | `String` | `verificaciones_institucionales.observaciones` | VARCHAR(1000) |
| `cuenta` | `CuentaUsuario` | `verificaciones_institucionales.cuenta_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `verificaciones_institucionales.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

## M02 — Catálogo e inventario

Paquete modelo.catalogo; PDF: impresas 15–21, físicas 16–22.

### Autor

Archivo: [Autor](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Autor.java). Tabla: `autores`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 15/17/20, RF-M02-10/14 y RN-M02-05/06.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `autores.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `autores.nombre` | VARCHAR(150) NOT NULL |
| `apellidos` | `String` | `autores.apellidos` | VARCHAR(200) |
| `estatus` | `boolean` | `autores.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `libros` | `List<Libro>` | Inversa de `Libro.autores` | ManyToMany; LAZY; sin columna propia |

### Categoria

Archivo: [Categoria](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Categoria.java). Tabla: `categorias`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 17/20, RF-M02-21/25 y RN-M02-07/08.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `categorias.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `categorias.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `categorias.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `categorias.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `libros` | `List<Libro>` | Inversa de `Libro.categorias` | ManyToMany; LAZY; sin columna propia |

### Editorial

Archivo: [Editorial](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Editorial.java). Tabla: `editoriales`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 17/20, RF-M02-16/20 y RN-M02-09/10.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `editoriales.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `editoriales.nombre` | VARCHAR(200) NOT NULL |
| `pais` | `String` | `editoriales.pais` | VARCHAR(100) |
| `estatus` | `boolean` | `editoriales.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `libros` | `List<Libro>` | Inversa de `Libro.editorial` | OneToMany; LAZY; sin columna propia |

### Ejemplar

Archivo: [Ejemplar](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Ejemplar.java). Tabla: `ejemplares`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 18–21 / visor 19–22, RF-M02-26/34/36, RN-M02-02/04/15/16. Ubicación actual 0..1 acordada; la política de ubicación de activos corresponde al service futuro.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `ejemplares.id` | BIGSERIAL PRIMARY KEY |
| `codigoBarras` | `String` | `ejemplares.codigo_barras` | VARCHAR(80) NOT NULL UNIQUE |
| `fechaAdquisicion` | `LocalDate` | `ejemplares.fecha_adquisicion` | DATE NOT NULL |
| `estado` | `EstadoEjemplar` | `ejemplares.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `estadoConservacion` | `EstadoConservacion` | `ejemplares.estado_conservacion` | VARCHAR(30) NOT NULL; enum STRING |
| `estatus` | `boolean` | `ejemplares.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `observaciones` | `String` | `ejemplares.observaciones` | VARCHAR(2000) |
| `libro` | `Libro` | `ejemplares.libro_id` → `Libro.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ubicacion` | `Ubicacion` | `ejemplares.ubicacion_id` → `Ubicacion.id` | BIGINT; ManyToOne LAZY; opcional |
| `historialEstados` | `List<HistorialEstadoEjemplar>` | Inversa de `HistorialEstadoEjemplar.ejemplar` | OneToMany; LAZY; sin columna propia |

### HistorialEstadoEjemplar

Archivo: [HistorialEstadoEjemplar](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/HistorialEstadoEjemplar.java). Tabla: `historiales_estado_ejemplar`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 18/21, RF-M02-36/62 y RN-M02-15/17/23. Registro histórico sin borrado en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `historiales_estado_ejemplar.id` | BIGSERIAL PRIMARY KEY |
| `ejemplar` | `Ejemplar` | `historiales_estado_ejemplar.ejemplar_id` → `Ejemplar.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `fecha` | `LocalDateTime` | `historiales_estado_ejemplar.fecha` | TIMESTAMP NOT NULL |
| `estadoAnterior` | `EstadoEjemplar` | `historiales_estado_ejemplar.estado_anterior` | VARCHAR(30); enum STRING |
| `estadoNuevo` | `EstadoEjemplar` | `historiales_estado_ejemplar.estado_nuevo` | VARCHAR(30) NOT NULL; enum STRING |
| `observaciones` | `String` | `historiales_estado_ejemplar.observaciones` | VARCHAR(2000) |
| `responsable` | `CuentaUsuario` | `historiales_estado_ejemplar.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

### Idioma

Archivo: [Idioma](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Idioma.java). Tabla: `idiomas`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 15/16, RF-M02-02/49. Catálogo; ningún idioma se impone por defecto.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `idiomas.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `idiomas.nombre` | VARCHAR(100) NOT NULL |
| `estatus` | `boolean` | `idiomas.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `libros` | `List<Libro>` | Inversa de `Libro.idioma` | OneToMany; LAZY; sin columna propia |

### Libro

Archivo: [Libro](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Libro.java). Tabla: `libros`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 15–21 / visor 16–22, RF-M02-01/02/05/13/19/23, RN-M02-03/09/12. Idioma opcional por decisión expresa del responsable; editorial principal 0..1.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `libros.id` | BIGSERIAL PRIMARY KEY |
| `titulo` | `String` | `libros.titulo` | VARCHAR(250) NOT NULL |
| `descripcion` | `String` | `libros.descripcion` | VARCHAR(2000) |
| `edicion` | `String` | `libros.edicion` | VARCHAR(100) |
| `anioPublicacion` | `Integer` | `libros.anio_publicacion` | INTEGER |
| `isbn` | `String` | `libros.isbn` | VARCHAR(20) UNIQUE |
| `numPaginas` | `Integer` | `libros.num_paginas` | INTEGER |
| `estatus` | `boolean` | `libros.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `editorial` | `Editorial` | `libros.editorial_id` → `Editorial.id` | BIGINT; ManyToOne LAZY; opcional |
| `idioma` | `Idioma` | `libros.idioma_id` → `Idioma.id` | BIGINT; ManyToOne LAZY; opcional |
| `autores` | `List<Autor>` | `libros_autores` (`libro_id`, `autor_id`) | ManyToMany; LAZY; unión hacia Autor |
| `categorias` | `List<Categoria>` | `libros_categorias` (`libro_id`, `categoria_id`) | ManyToMany; LAZY; unión hacia Categoria |
| `ejemplares` | `List<Ejemplar>` | Inversa de `Ejemplar.libro` | OneToMany; LAZY; sin columna propia |

### Ubicacion

Archivo: [Ubicacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/Ubicacion.java). Tabla: `ubicaciones`.

**Fuente y alcance del Javadoc:** M02, PDF impresa 18/21, RF-M02-38/42, RN-M02-16/22. Cardinalidad acordada 1 a 0..N ejemplares.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `ubicaciones.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `ubicaciones.nombre` | VARCHAR(150) NOT NULL |
| `seccion` | `String` | `ubicaciones.seccion` | VARCHAR(100) NOT NULL |
| `estante` | `String` | `ubicaciones.estante` | VARCHAR(100) NOT NULL |
| `nivel` | `String` | `ubicaciones.nivel` | VARCHAR(100) |
| `estatus` | `boolean` | `ubicaciones.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `ejemplares` | `List<Ejemplar>` | Inversa de `Ejemplar.ubicacion` | OneToMany; LAZY; sin columna propia |

## M03 — Préstamos

Paquete modelo.prestamos; PDF: impresas 22–28, físicas 23–29.

### DetallePrestamo

Archivo: [DetallePrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/DetallePrestamo.java). Tabla: `detalles_prestamo`.

**Fuente y alcance del Javadoc:** M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-07/11/24/35/39; RN-M03-03/04/10/13/14/15/19. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. La devolucion definitiva es opcional y unica por detalle, segun RN-M03-13/14 y decision expresa del responsable. No existe una relacion directa Libro-Prestamo: la circulacion identifica al Ejemplar. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `detalles_prestamo.id` | BIGSERIAL PRIMARY KEY |
| `fechaVencimiento` | `LocalDate` | `detalles_prestamo.fecha_vencimiento` | DATE NOT NULL |
| `estado` | `EstadoDetallePrestamo` | `detalles_prestamo.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `prestamo` | `Prestamo` | `detalles_prestamo.prestamo_id` → `Prestamo.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ejemplar` | `Ejemplar` | `detalles_prestamo.ejemplar_id` → `Ejemplar.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `devolucion` | `Devolucion` | Inversa de `Devolucion.detallePrestamo` | OneToOne; LAZY; sin columna propia |
| `renovaciones` | `List<Renovacion>` | Inversa de `Renovacion.detallePrestamo` | OneToMany; LAZY; sin columna propia |

### Devolucion

Archivo: [Devolucion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/Devolucion.java). Tabla: `devoluciones`.

**Fuente y alcance del Javadoc:** M03, PDF pp. impresas 22/24-28 (fisicas 23/25-29). RF-M03-27/29/30/31/32/35/39; RN-M03-13/14/15. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Cada devolucion corresponde a un unico detalle; su FK UNIQUE impide dos devoluciones definitivas. estado conserva el atributo textual del diagrama; estadoConservacion registra por separado la condicion fisica exigida por RF-M03-31. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `devoluciones.id` | BIGSERIAL PRIMARY KEY |
| `fechaDevolucion` | `LocalDateTime` | `devoluciones.fecha_devolucion` | TIMESTAMP NOT NULL |
| `estado` | `String` | `devoluciones.estado` | VARCHAR(30) |
| `estadoConservacion` | `EstadoConservacion` | `devoluciones.estado_conservacion` | VARCHAR(30) NOT NULL; enum STRING |
| `observaciones` | `String` | `devoluciones.observaciones` | VARCHAR(2000) |
| `detallePrestamo` | `DetallePrestamo` | `devoluciones.detalle_prestamo_id` → `DetallePrestamo.id` | BIGINT NOT NULL UNIQUE; OneToOne LAZY; obligatorio |
| `bibliotecario` | `CuentaUsuario` | `devoluciones.bibliotecario_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### PoliticaPrestamo

Archivo: [PoliticaPrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/PoliticaPrestamo.java). Tabla: `politicas_prestamo`.

**Fuente y alcance del Javadoc:** M03, PDF pp. impresas 22/26-28 (fisicas 23/27-29). RF-M03-57/58/59/60/61/62/63; RN-M03-08/09/21/28/29. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Condiciones configurables sin duraciones, limites ni autorizaciones predeterminadas; no se implementa motor de politicas. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `politicas_prestamo.id` | BIGSERIAL PRIMARY KEY |
| `tipoUsuario` | `String` | `politicas_prestamo.tipo_usuario` | VARCHAR(80) NOT NULL |
| `maxLibros` | `Integer` | `politicas_prestamo.max_libros` | INTEGER NOT NULL |
| `diasPrestamo` | `Integer` | `politicas_prestamo.dias_prestamo` | INTEGER NOT NULL |
| `permiteRenovacion` | `Boolean` | `politicas_prestamo.permite_renovacion` | BOOLEAN NOT NULL |
| `renovacionesMax` | `Integer` | `politicas_prestamo.renovaciones_max` | INTEGER NOT NULL |
| `restriccionesMaterial` | `String` | `politicas_prestamo.restricciones_material` | VARCHAR(2000) |
| `estatus` | `boolean` | `politicas_prestamo.estatus` | BOOLEAN NOT NULL |
| `prestamos` | `List<Prestamo>` | Inversa de `Prestamo.politica` | OneToMany; LAZY; sin columna propia |

### Prestamo

Archivo: [Prestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/Prestamo.java). Tabla: `prestamos`.

**Fuente y alcance del Javadoc:** M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-01/07/10/12/13/63; RN-M03-01/03/09/30. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. La coleccion representa uno o varios detalles (RN-M03-03); la cardinalidad minima requiere validacion del futuro service. La fecha de cabecera se conserva del diagrama; el vencimiento individual pertenece a cada detalle. Bibliotecario y confirmacion son opcionales antes de confirmar la entrega. No se implementa autorizacion. condicionesAplicadas permite almacenar una instantanea al confirmar; no congela por si sola una politica mutable. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `prestamos.id` | BIGSERIAL PRIMARY KEY |
| `fechaPrestamo` | `LocalDateTime` | `prestamos.fecha_prestamo` | TIMESTAMP NOT NULL |
| `fechaVencimiento` | `LocalDate` | `prestamos.fecha_vencimiento` | DATE |
| `estado` | `EstadoPrestamo` | `prestamos.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `observaciones` | `String` | `prestamos.observaciones` | VARCHAR(2000) |
| `fechaConfirmacionEntrega` | `LocalDateTime` | `prestamos.fecha_confirmacion_entrega` | TIMESTAMP |
| `condicionesAplicadas` | `String` | `prestamos.condiciones_aplicadas` | VARCHAR(4000) |
| `persona` | `Persona` | `prestamos.persona_id` → `Persona.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `bibliotecario` | `CuentaUsuario` | `prestamos.bibliotecario_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `politica` | `PoliticaPrestamo` | `prestamos.politica_id` → `PoliticaPrestamo.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `detalles` | `List<DetallePrestamo>` | Inversa de `DetallePrestamo.prestamo` | OneToMany; LAZY; sin columna propia |

### Renovacion

Archivo: [Renovacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/Renovacion.java). Tabla: `renovaciones`.

**Fuente y alcance del Javadoc:** M03, PDF pp. impresas 22/25-28 (fisicas 23/26-29). RF-M03-40/45/46/47/48; RN-M03-19/21/22/24. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. fechaVencimiento es la nueva fecha del diagrama; se conserva tambien la anterior por RN-M03-22. Responsable opcional admite solicitudes automatizadas; no implementa aprobacion ni modifica el detalle por si sola. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `renovaciones.id` | BIGSERIAL PRIMARY KEY |
| `fechaRenovacion` | `LocalDateTime` | `renovaciones.fecha_renovacion` | TIMESTAMP NOT NULL |
| `fechaVencimientoAnterior` | `LocalDate` | `renovaciones.fecha_vencimiento_anterior` | DATE NOT NULL |
| `fechaVencimiento` | `LocalDate` | `renovaciones.fecha_vencimiento` | DATE NOT NULL |
| `estado` | `String` | `renovaciones.estado` | VARCHAR(30) |
| `detallePrestamo` | `DetallePrestamo` | `renovaciones.detalle_prestamo_id` → `DetallePrestamo.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `renovaciones.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

## M04 — Reservaciones de libros

Paquete modelo.reservas; PDF: impresas 29–35, físicas 30–36.

### AsignacionReserva

Archivo: [AsignacionReserva](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/AsignacionReserva.java). Tabla: `asignaciones_reserva`.

**Fuente y alcance del Javadoc:** M04, PDF pp. impresas 29/31-35 (fisicas 30/32-36). RF-M04-25/27/28/29/32; RN-M04-05/12/13/15/27. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Cada asignacion identifica el ejemplar concreto; pueden conservarse asignaciones historicas de una reserva. La exclusividad de asignaciones vigentes requiere futuras reglas transaccionales; no se afirma implementada por esta FK. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `asignaciones_reserva.id` | BIGSERIAL PRIMARY KEY |
| `fechaAsignacion` | `LocalDateTime` | `asignaciones_reserva.fecha_asignacion` | TIMESTAMP NOT NULL |
| `estado` | `String` | `asignaciones_reserva.estado` | VARCHAR(30) NOT NULL |
| `reserva` | `ReservaLibro` | `asignaciones_reserva.reserva_id` → `ReservaLibro.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ejemplar` | `Ejemplar` | `asignaciones_reserva.ejemplar_id` → `Ejemplar.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### HistorialReserva

Archivo: [HistorialReserva](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/HistorialReserva.java). Tabla: `historiales_reserva`.

**Fuente y alcance del Javadoc:** M04, PDF pp. impresas 31/34-35 (fisicas 32/35-36) y PNG. RF-M04-22/26; RN-M04-26/27. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Las posiciones permiten conservar movimientos de la lista (RF-M04-26); no asignan prioridad. Estado anterior opcional para el alta; responsable opcional para transiciones automaticas. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `historiales_reserva.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `historiales_reserva.fecha` | TIMESTAMP NOT NULL |
| `estadoAnterior` | `EstadoReservaLibro` | `historiales_reserva.estado_anterior` | VARCHAR(30); enum STRING |
| `estadoNuevo` | `EstadoReservaLibro` | `historiales_reserva.estado_nuevo` | VARCHAR(30) NOT NULL; enum STRING |
| `posicionAnterior` | `Integer` | `historiales_reserva.posicion_anterior` | INTEGER |
| `posicionNueva` | `Integer` | `historiales_reserva.posicion_nueva` | INTEGER |
| `observaciones` | `String` | `historiales_reserva.observaciones` | VARCHAR(2000) |
| `reserva` | `ReservaLibro` | `historiales_reserva.reserva_id` → `ReservaLibro.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `historiales_reserva.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

### ListaEspera

Archivo: [ListaEspera](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/ListaEspera.java). Tabla: `listas_espera`.

**Fuente y alcance del Javadoc:** M04, PDF pp. impresas 29/31/34-35 (fisicas 30/32/35-36). RF-M04-16/20/21/22/23/24/26; RN-M04-10/11/23. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Entidad exigida por la narrativa aunque omitida del PNG; se conserva una lista por libro. El orden se almacena en ReservaLibro.posicionEspera y se expone ordenado por posicion y fecha; no se inventa una regla de prioridad. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `listas_espera.id` | BIGSERIAL PRIMARY KEY |
| `libro` | `Libro` | `listas_espera.libro_id` → `Libro.id` | BIGINT NOT NULL UNIQUE; OneToOne LAZY; obligatorio |
| `reservas` | `List<ReservaLibro>` | Inversa de `ReservaLibro.listaEspera` | OneToMany; LAZY; sin columna propia |

### PoliticaReservaLibro

Archivo: [PoliticaReservaLibro](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/PoliticaReservaLibro.java). Tabla: `politicas_reserva_libro`.

**Fuente y alcance del Javadoc:** M04, PDF pp. impresas 29/33-35 (fisicas 30/34-36). RF-M04-50/51/52/53/54/55/56; RN-M04-09/10/11/17/25. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. diasAnticipacion se conserva del PNG; tiempos y limites se configuran sin valores predeterminados. Las reglas de prioridad se almacenan como configuracion descriptiva; no se implementa un algoritmo ni vocabulario de usuarios. La unidad de tiempoLimite debera concretarse antes de implementar el motor de politicas. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `politicas_reserva_libro.id` | BIGSERIAL PRIMARY KEY |
| `diasAnticipacion` | `Integer` | `politicas_reserva_libro.dias_anticipacion` | INTEGER |
| `tiempoLimite` | `Integer` | `politicas_reserva_libro.tiempo_limite` | INTEGER NOT NULL |
| `maxReservas` | `Integer` | `politicas_reserva_libro.max_reservas` | INTEGER NOT NULL |
| `reglasPrioridad` | `String` | `politicas_reserva_libro.reglas_prioridad` | VARCHAR(2000) |
| `tiposUsuarioPermitidos` | `String` | `politicas_reserva_libro.tipos_usuario_permitidos` | VARCHAR(1000) |
| `estatus` | `boolean` | `politicas_reserva_libro.estatus` | BOOLEAN NOT NULL |
| `reservas` | `List<ReservaLibro>` | Inversa de `ReservaLibro.politica` | OneToMany; LAZY; sin columna propia |

### ReservaLibro

Archivo: [ReservaLibro](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/ReservaLibro.java). Tabla: `reservas_libro`.

**Fuente y alcance del Javadoc:** M04, PDF pp. impresas 29-35 (fisicas 30-36). RF-M04-08/09/16/17/21/39/41/44/56; RN-M04-02/03/05/16/18/26. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. La solicitud se relaciona con Libro; el Ejemplar solo se incorpora mediante AsignacionReserva (RN-M04-05). Una lista por libro y una posicion opcional por reserva son decisiones tecnicas expresamente aprobadas; no calculan prioridad. Politica y condiciones aplicadas se completan al evaluar la solicitud; el modelo no garantiza por si solo su inmutabilidad. El responsable de cancelacion es opcional porque una cancelacion puede solicitarla la propia Persona. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `reservas_libro.id` | BIGSERIAL PRIMARY KEY |
| `fechaReserva` | `LocalDateTime` | `reservas_libro.fecha_reserva` | TIMESTAMP NOT NULL |
| `fechaLimite` | `LocalDate` | `reservas_libro.fecha_limite` | DATE |
| `estado` | `EstadoReservaLibro` | `reservas_libro.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `observaciones` | `String` | `reservas_libro.observaciones` | VARCHAR(2000) |
| `posicionEspera` | `Integer` | `reservas_libro.posicion_espera` | INTEGER |
| `fechaAtencion` | `LocalDateTime` | `reservas_libro.fecha_atencion` | TIMESTAMP |
| `fechaCancelacion` | `LocalDateTime` | `reservas_libro.fecha_cancelacion` | TIMESTAMP |
| `motivoCancelacion` | `String` | `reservas_libro.motivo_cancelacion` | VARCHAR(2000) |
| `condicionesAplicadas` | `String` | `reservas_libro.condiciones_aplicadas` | VARCHAR(4000) |
| `persona` | `Persona` | `reservas_libro.persona_id` → `Persona.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `libro` | `Libro` | `reservas_libro.libro_id` → `Libro.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `politica` | `PoliticaReservaLibro` | `reservas_libro.politica_id` → `PoliticaReservaLibro.id` | BIGINT; ManyToOne LAZY; opcional |
| `listaEspera` | `ListaEspera` | `reservas_libro.lista_espera_id` → `ListaEspera.id` | BIGINT; ManyToOne LAZY; opcional |
| `detallePrestamo` | `DetallePrestamo` | `reservas_libro.detalle_prestamo_id` → `DetallePrestamo.id` | BIGINT UNIQUE; OneToOne LAZY; opcional |
| `responsableCancelacion` | `CuentaUsuario` | `reservas_libro.responsable_cancelacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `asignaciones` | `List<AsignacionReserva>` | Inversa de `AsignacionReserva.reserva` | OneToMany; LAZY; sin columna propia |
| `historial` | `List<HistorialReserva>` | Inversa de `HistorialReserva.reserva` | OneToMany; LAZY; sin columna propia |

## M05 — Cubículos

Paquete modelo.cubiculos; PDF: impresas 36–43, físicas 37–44.

### BloqueoCubiculo

Archivo: [BloqueoCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/BloqueoCubiculo.java). Tabla: `bloqueos_cubiculo`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36/40-43 (fisicas 37/41-44). RF-M05-63/64/65/66/67/68; RN-M05-13/26/28. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. El responsable respalda los cambios administrativos; no se cancelan reservas ni se detectan conflictos automaticamente. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `bloqueos_cubiculo.id` | BIGSERIAL PRIMARY KEY |
| `fechaInicio` | `LocalDateTime` | `bloqueos_cubiculo.fecha_inicio` | TIMESTAMP NOT NULL |
| `fechaFin` | `LocalDateTime` | `bloqueos_cubiculo.fecha_fin` | TIMESTAMP NOT NULL |
| `motivo` | `String` | `bloqueos_cubiculo.motivo` | VARCHAR(2000) NOT NULL |
| `estatus` | `boolean` | `bloqueos_cubiculo.estatus` | BOOLEAN NOT NULL |
| `cubiculo` | `Cubiculo` | `bloqueos_cubiculo.cubiculo_id` → `Cubiculo.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `bloqueos_cubiculo.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Cubiculo

Archivo: [Cubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/Cubiculo.java). Tabla: `cubiculos`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36-38/41-43 (fisicas 37-39/42-44). RF-M05-01/02/03/04/07/08; RN-M05-01/02/04/05. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Codigo y ubicacion completan los atributos del PNG con RF-M05-02/03; ubicacion es descriptiva, sin inventar otra entidad. Equipamiento se comparte mediante una relacion muchos a muchos aprobada; no se borra en cascada. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `cubiculos.id` | BIGSERIAL PRIMARY KEY |
| `codigo` | `String` | `cubiculos.codigo` | VARCHAR(80) NOT NULL UNIQUE |
| `nombre` | `String` | `cubiculos.nombre` | VARCHAR(150) NOT NULL |
| `ubicacion` | `String` | `cubiculos.ubicacion` | VARCHAR(250) |
| `capacidad` | `Integer` | `cubiculos.capacidad` | INTEGER NOT NULL |
| `estado` | `EstadoCubiculo` | `cubiculos.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `descripcion` | `String` | `cubiculos.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `cubiculos.estatus` | BOOLEAN NOT NULL |
| `reservas` | `List<ReservaCubiculo>` | Inversa de `ReservaCubiculo.cubiculo` | OneToMany; LAZY; sin columna propia |
| `bloqueos` | `List<BloqueoCubiculo>` | Inversa de `BloqueoCubiculo.cubiculo` | OneToMany; LAZY; sin columna propia |
| `equipamientos` | `Set<Equipamiento>` | `cubiculos_equipamientos` (`cubiculo_id`, `equipamiento_id`) | ManyToMany; LAZY; unión hacia Equipamiento |

### Equipamiento

Archivo: [Equipamiento](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/Equipamiento.java). Tabla: `equipamientos`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36-38 (fisicas 37-39) y PNG. RF-M05-04/15. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Catalogo compartido por cubiculos mediante muchos a muchos; no se inventan cantidades ni asignaciones exclusivas. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `equipamientos.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `equipamientos.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `equipamientos.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `equipamientos.estatus` | BOOLEAN NOT NULL |
| `cubiculos` | `Set<Cubiculo>` | Inversa de `Cubiculo.equipamientos` | ManyToMany; LAZY; sin columna propia |

### HorarioBiblioteca

Archivo: [HorarioBiblioteca](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/HorarioBiblioteca.java). Tabla: `horarios_biblioteca`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36/40/42-43 (fisicas 37/41/43-44). RF-M05-60/61/62; RN-M05-10/30. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. fechaEspecial y cerrado permiten representar cierres/horarios especiales de RF-M05-62; no se fija un calendario. Las horas admiten nulos para un dia cerrado; interpretar precedencia entre horario semanal y especial queda para el futuro service. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `horarios_biblioteca.id` | BIGSERIAL PRIMARY KEY |
| `diaSemana` | `String` | `horarios_biblioteca.dia_semana` | VARCHAR(20) |
| `horaApertura` | `LocalTime` | `horarios_biblioteca.hora_apertura` | TIME |
| `horaCierre` | `LocalTime` | `horarios_biblioteca.hora_cierre` | TIME |
| `fechaEspecial` | `LocalDate` | `horarios_biblioteca.fecha_especial` | DATE |
| `cerrado` | `Boolean` | `horarios_biblioteca.cerrado` | BOOLEAN |
| `estatus` | `boolean` | `horarios_biblioteca.estatus` | BOOLEAN NOT NULL |
| `usos` | `List<UsoCubiculo>` | Inversa de `UsoCubiculo.horarioBiblioteca` | OneToMany; LAZY; sin columna propia |

### PoliticaReservaCubiculo

Archivo: [PoliticaReservaCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/PoliticaReservaCubiculo.java). Tabla: `politicas_reserva_cubiculo`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36/41-43 (fisicas 37/42-44). RF-M05-69/70/71/72/73/74/75/76; RN-M05-11/16/17/23/27. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Parametros configurables sin tiempos, cantidades ni tipos de usuario predeterminados. Las unidades de duracion/anticipacion/tolerancia deberan concretarse antes de implementar reglas; no se infieren del tipo int del PNG. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `politicas_reserva_cubiculo.id` | BIGSERIAL PRIMARY KEY |
| `tiempoMinimo` | `Integer` | `politicas_reserva_cubiculo.tiempo_minimo` | INTEGER |
| `tiempoMaximo` | `Integer` | `politicas_reserva_cubiculo.tiempo_maximo` | INTEGER NOT NULL |
| `anticipacionMax` | `Integer` | `politicas_reserva_cubiculo.anticipacion_max` | INTEGER NOT NULL |
| `maxReservas` | `Integer` | `politicas_reserva_cubiculo.max_reservas` | INTEGER NOT NULL |
| `toleranciaLlegada` | `Integer` | `politicas_reserva_cubiculo.tolerancia_llegada` | INTEGER |
| `tiposUsuarioPermitidos` | `String` | `politicas_reserva_cubiculo.tipos_usuario_permitidos` | VARCHAR(1000) |
| `estatus` | `boolean` | `politicas_reserva_cubiculo.estatus` | BOOLEAN NOT NULL |
| `reservas` | `List<ReservaCubiculo>` | Inversa de `ReservaCubiculo.politica` | OneToMany; LAZY; sin columna propia |

### ReservaCubiculo

Archivo: [ReservaCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/ReservaCubiculo.java). Tabla: `reservas_cubiculo`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36-43 (fisicas 37-44). RF-M05-19/20/21/22/29/30/31/32/54/76; RN-M05-03/04/06/14/15. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. Los timestamps incluyen fecha y hora del periodo; fechaReserva registra la solicitud. El CHECK de fin posterior al inicio cumple RN-M05-15; detectar solapamientos (RN-M05-06/07) requiere reglas posteriores. Politica opcional mientras se evalua la solicitud; condicionesAplicadas es soporte para la instantanea exigida por RF-M05-76. El responsable de cancelacion es opcional para solicitudes canceladas por la propia Persona. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `reservas_cubiculo.id` | BIGSERIAL PRIMARY KEY |
| `fechaReserva` | `LocalDateTime` | `reservas_cubiculo.fecha_reserva` | TIMESTAMP NOT NULL |
| `fechaInicio` | `LocalDateTime` | `reservas_cubiculo.fecha_inicio` | TIMESTAMP NOT NULL |
| `fechaFin` | `LocalDateTime` | `reservas_cubiculo.fecha_fin` | TIMESTAMP NOT NULL |
| `estado` | `EstadoReservaCubiculo` | `reservas_cubiculo.estado` | VARCHAR(30) NOT NULL; enum STRING |
| `observaciones` | `String` | `reservas_cubiculo.observaciones` | VARCHAR(2000) |
| `fechaCancelacion` | `LocalDateTime` | `reservas_cubiculo.fecha_cancelacion` | TIMESTAMP |
| `motivoCancelacion` | `String` | `reservas_cubiculo.motivo_cancelacion` | VARCHAR(2000) |
| `condicionesAplicadas` | `String` | `reservas_cubiculo.condiciones_aplicadas` | VARCHAR(4000) |
| `persona` | `Persona` | `reservas_cubiculo.persona_id` → `Persona.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `cubiculo` | `Cubiculo` | `reservas_cubiculo.cubiculo_id` → `Cubiculo.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `politica` | `PoliticaReservaCubiculo` | `reservas_cubiculo.politica_id` → `PoliticaReservaCubiculo.id` | BIGINT; ManyToOne LAZY; opcional |
| `responsableCancelacion` | `CuentaUsuario` | `reservas_cubiculo.responsable_cancelacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `uso` | `UsoCubiculo` | Inversa de `UsoCubiculo.reserva` | OneToOne; LAZY; sin columna propia |

### UsoCubiculo

Archivo: [UsoCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/UsoCubiculo.java). Tabla: `usos_cubiculo`.

**Fuente y alcance del Javadoc:** M05, PDF pp. impresas 36/39/42-43 (fisicas 37/40/43-44). RF-M05-42/43/44/46/47/50; RN-M05-18/19/20/21/22. Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas. La fecha de fin es opcional mientras el uso este activo; no se calcula ocupacion automaticamente. FK de horario opcional aprobada como referencia al horario aplicado para la dependencia del PNG; no valida horarios. Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `usos_cubiculo.id` | BIGSERIAL PRIMARY KEY |
| `fechaInicio` | `LocalDateTime` | `usos_cubiculo.fecha_inicio` | TIMESTAMP NOT NULL |
| `fechaFin` | `LocalDateTime` | `usos_cubiculo.fecha_fin` | TIMESTAMP |
| `estado` | `String` | `usos_cubiculo.estado` | VARCHAR(30) NOT NULL |
| `observaciones` | `String` | `usos_cubiculo.observaciones` | VARCHAR(2000) |
| `asistenciaConfirmada` | `Boolean` | `usos_cubiculo.asistencia_confirmada` | BOOLEAN |
| `reserva` | `ReservaCubiculo` | `usos_cubiculo.reserva_id` → `ReservaCubiculo.id` | BIGINT NOT NULL UNIQUE; OneToOne LAZY; obligatorio |
| `horarioBiblioteca` | `HorarioBiblioteca` | `usos_cubiculo.horario_biblioteca_id` → `HorarioBiblioteca.id` | BIGINT; ManyToOne LAZY; opcional |

## M06 — Sanciones y multas

Paquete modelo.sanciones; PDF: impresas 44–51, físicas 45–52.

### HistorialSancion

Archivo: [HistorialSancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/HistorialSancion.java). Tabla: `historiales_sancion`.

**Fuente y alcance del Javadoc:** M06, pp. impresas 46-47/51 (visor 47-48/52), RF-M06-30, RF-M06-32; RN-M06-27, RN-M06-28, RN-M06-29, RN-M06-30. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `historiales_sancion.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `historiales_sancion.fecha` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `estadoAnterior` | `EstadoSancion` | `historiales_sancion.estado_anterior` | VARCHAR(60); enum STRING |
| `estadoNuevo` | `EstadoSancion` | `historiales_sancion.estado_nuevo` | VARCHAR(60) NOT NULL; enum STRING |
| `observaciones` | `String` | `historiales_sancion.observaciones` | VARCHAR(3000) |
| `sancion` | `Sancion` | `historiales_sancion.sancion_id` → `Sancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `historiales_sancion.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Incidencia

Archivo: [Incidencia](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/Incidencia.java). Tabla: `incidencias`.

**Fuente y alcance del Javadoc:** M06, pp. impresas 45-46 (visor 46-47), RF-M06-01, RF-M06-09, RF-M06-14, RF-M06-16, RF-M06-18; RN-M06-02, RN-M06-03 (p. 49). Los contextos de origen son opcionales segun RF-M06-09; el modelo no detecta ni genera incidencias. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `incidencias.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `incidencias.fecha` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `tipo` | `TipoIncidencia` | `incidencias.tipo` | VARCHAR(60) NOT NULL; enum STRING |
| `descripcion` | `String` | `incidencias.descripcion` | VARCHAR(2000) NOT NULL |
| `estado` | `String` | `incidencias.estado` | VARCHAR(60) NOT NULL |
| `motivo` | `String` | `incidencias.motivo` | VARCHAR(1000) NOT NULL |
| `observaciones` | `String` | `incidencias.observaciones` | VARCHAR(3000) |
| `evidencias` | `String` | `incidencias.evidencias` | VARCHAR(5000) |
| `persona` | `Persona` | `incidencias.persona_id` → `Persona.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `incidencias.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ejemplar` | `Ejemplar` | `incidencias.ejemplar_id` → `Ejemplar.id` | BIGINT; ManyToOne LAZY; opcional |
| `prestamo` | `Prestamo` | `incidencias.prestamo_id` → `Prestamo.id` | BIGINT; ManyToOne LAZY; opcional |
| `detallePrestamo` | `DetallePrestamo` | `incidencias.detalle_prestamo_id` → `DetallePrestamo.id` | BIGINT; ManyToOne LAZY; opcional |
| `devolucion` | `Devolucion` | `incidencias.devolucion_id` → `Devolucion.id` | BIGINT; ManyToOne LAZY; opcional |
| `reservaLibro` | `ReservaLibro` | `incidencias.reserva_libro_id` → `ReservaLibro.id` | BIGINT; ManyToOne LAZY; opcional |
| `reservaCubiculo` | `ReservaCubiculo` | `incidencias.reserva_cubiculo_id` → `ReservaCubiculo.id` | BIGINT; ManyToOne LAZY; opcional |
| `usoCubiculo` | `UsoCubiculo` | `incidencias.uso_cubiculo_id` → `UsoCubiculo.id` | BIGINT; ManyToOne LAZY; opcional |
| `sanciones` | `List<Sancion>` | Inversa de `Sancion.incidencia` | OneToMany; LAZY; sin columna propia |

### Multa

Archivo: [Multa](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/Multa.java). Tabla: `multas`.

**Fuente y alcance del Javadoc:** M06, pp. impresas 47-50 (visor 48-51), RF-M06-43, RF-M06-49; RN-M06-11, RN-M06-12, RN-M06-13, RN-M06-14, RN-M06-18. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `multas.id` | BIGSERIAL PRIMARY KEY |
| `monto` | `BigDecimal` | `multas.monto` | NUMERIC(12,2) NOT NULL |
| `fechaLimitePago` | `LocalDate` | `multas.fecha_limite_pago` | DATE |
| `estado` | `String` | `multas.estado` | VARCHAR(60) NOT NULL |
| `fechaGeneracion` | `LocalDateTime` | `multas.fecha_generacion` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `saldoPendiente` | `BigDecimal` | `multas.saldo_pendiente` | NUMERIC(12,2) NOT NULL |
| `observaciones` | `String` | `multas.observaciones` | VARCHAR(3000) |
| `sancion` | `Sancion` | `multas.sancion_id` → `Sancion.id` | BIGINT NOT NULL UNIQUE; OneToOne LAZY; obligatorio |
| `pagos` | `List<PagoMulta>` | Inversa de `PagoMulta.multa` | OneToMany; LAZY; sin columna propia |

### PagoMulta

Archivo: [PagoMulta](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/PagoMulta.java). Tabla: `pagos_multa`.

**Fuente y alcance del Javadoc:** M06, p. impresa 48 (visor 49), RF-M06-51, RF-M06-59; RN-M06-15, RN-M06-16, RN-M06-17, RN-M06-29 (pp. 50-51). Registro persistente sin procesamiento de pagos ni actualizacion automatica de saldos. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `pagos_multa.id` | BIGSERIAL PRIMARY KEY |
| `fechaPago` | `LocalDateTime` | `pagos_multa.fecha_pago` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `monto` | `BigDecimal` | `pagos_multa.monto` | NUMERIC(12,2) NOT NULL |
| `metodo` | `String` | `pagos_multa.metodo` | VARCHAR(100) NOT NULL |
| `comprobante` | `String` | `pagos_multa.comprobante` | VARCHAR(1000) |
| `referencia` | `String` | `pagos_multa.referencia` | VARCHAR(250) |
| `multa` | `Multa` | `pagos_multa.multa_id` → `Multa.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `pagos_multa.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### PoliticaSancion

Archivo: [PoliticaSancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/PoliticaSancion.java). Tabla: `politicas_sancion`.

**Fuente y alcance del Javadoc:** M06, pp. impresas 44/49-51 (visor 45/50-52), RF-M06-74, RF-M06-80, RN-M06-06, RN-M06-31, RN-M06-32. Las condiciones configurables se conservan como datos, sin calcular multas ni aplicar restricciones. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `politicas_sancion.id` | BIGSERIAL PRIMARY KEY |
| `tipoIncidencia` | `TipoIncidencia` | `politicas_sancion.tipo_incidencia` | VARCHAR(60) NOT NULL; enum STRING |
| `tipoSancion` | `TipoSancion` | `politicas_sancion.tipo_sancion_id` → `TipoSancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `reglas` | `String` | `politicas_sancion.reglas` | VARCHAR(5000) NOT NULL |
| `estatus` | `boolean` | `politicas_sancion.estatus` | BOOLEAN NOT NULL |
| `tipoUsuario` | `String` | `politicas_sancion.tipo_usuario` | VARCHAR(100) |
| `diasSuspension` | `Integer` | `politicas_sancion.dias_suspension` | INTEGER |
| `montoMulta` | `BigDecimal` | `politicas_sancion.monto_multa` | NUMERIC(12,2) |
| `multasAutorizadas` | `boolean` | `politicas_sancion.multas_autorizadas` | BOOLEAN NOT NULL |
| `permitePagosParciales` | `boolean` | `politicas_sancion.permite_pagos_parciales` | BOOLEAN NOT NULL |
| `serviciosRestringidos` | `String` | `politicas_sancion.servicios_restringidos` | VARCHAR(2000) |
| `sanciones` | `List<Sancion>` | Inversa de `Sancion.politica` | OneToMany; LAZY; sin columna propia |

### ReposicionMaterial

Archivo: [ReposicionMaterial](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/ReposicionMaterial.java). Tabla: `reposiciones_material`.

**Fuente y alcance del Javadoc:** M06, p. impresa 48 (visor 49), RF-M06-61, RF-M06-67; RN-M06-19, RN-M06-20 (p. 50). La narrativa vincula reposicion con sancion y ejemplar, sin exigir una multa: la disposicion grafica es ambigua. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `reposiciones_material.id` | BIGSERIAL PRIMARY KEY |
| `descripcion` | `String` | `reposiciones_material.descripcion` | VARCHAR(2000) NOT NULL |
| `costo` | `BigDecimal` | `reposiciones_material.costo` | NUMERIC(12,2) |
| `estado` | `String` | `reposiciones_material.estado` | VARCHAR(60) NOT NULL |
| `condiciones` | `String` | `reposiciones_material.condiciones` | VARCHAR(3000) |
| `materialReemplazo` | `String` | `reposiciones_material.material_reemplazo` | VARCHAR(2000) |
| `fechaCumplimiento` | `LocalDate` | `reposiciones_material.fecha_cumplimiento` | DATE |
| `fechaValidacion` | `LocalDateTime` | `reposiciones_material.fecha_validacion` | TIMESTAMP WITHOUT TIME ZONE |
| `sancion` | `Sancion` | `reposiciones_material.sancion_id` → `Sancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ejemplar` | `Ejemplar` | `reposiciones_material.ejemplar_id` → `Ejemplar.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `ejemplarReposicion` | `Ejemplar` | `reposiciones_material.ejemplar_reposicion_id` → `Ejemplar.id` | BIGINT; ManyToOne LAZY; opcional |
| `validador` | `CuentaUsuario` | `reposiciones_material.validador_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

### RestriccionServicio

Archivo: [RestriccionServicio](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/RestriccionServicio.java). Tabla: `restricciones_servicio`.

**Fuente y alcance del Javadoc:** M06, p. impresa 47 (visor 48), RF-M06-33, RF-M06-42; RN-M06-05, RN-M06-21, RN-M06-22, RN-M06-23 (pp. 49-50). La fecha final puede faltar para restricciones permanentes; no se bloquea ningun servicio en esta entrega. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `restricciones_servicio.id` | BIGSERIAL PRIMARY KEY |
| `fechaInicio` | `LocalDate` | `restricciones_servicio.fecha_inicio` | DATE NOT NULL |
| `fechaFin` | `LocalDate` | `restricciones_servicio.fecha_fin` | DATE |
| `descripcion` | `String` | `restricciones_servicio.descripcion` | VARCHAR(2000) NOT NULL |
| `estado` | `String` | `restricciones_servicio.estado` | VARCHAR(60) NOT NULL |
| `servicioAfectado` | `String` | `restricciones_servicio.servicio_afectado` | VARCHAR(150) NOT NULL |
| `condicionesLevantamiento` | `String` | `restricciones_servicio.condiciones_levantamiento` | VARCHAR(3000) |
| `temporal` | `boolean` | `restricciones_servicio.temporal` | BOOLEAN NOT NULL |
| `sancion` | `Sancion` | `restricciones_servicio.sancion_id` → `Sancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Sancion

Archivo: [Sancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/Sancion.java). Tabla: `sanciones`.

**Fuente y alcance del Javadoc:** M06, pp. impresas 46-47/49-51 (visor 47-48/50-52), RF-M06-21, RF-M06-32, RF-M06-80, RN-M06-04, RN-M06-05, RN-M06-12, RN-M06-27, RN-M06-28. CUMPLIDA y RESUELTA se mantienen distintos porque ambos terminos aparecen en la fuente; no hay transiciones automaticas. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `sanciones.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `sanciones.fecha` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `descripcion` | `String` | `sanciones.descripcion` | VARCHAR(2000) NOT NULL |
| `estado` | `EstadoSancion` | `sanciones.estado` | VARCHAR(60) NOT NULL; enum STRING |
| `motivo` | `String` | `sanciones.motivo` | VARCHAR(1000) NOT NULL |
| `condicionesCumplimiento` | `String` | `sanciones.condiciones_cumplimiento` | VARCHAR(3000) |
| `fechaFin` | `LocalDate` | `sanciones.fecha_fin` | DATE |
| `fechaResolucion` | `LocalDateTime` | `sanciones.fecha_resolucion` | TIMESTAMP WITHOUT TIME ZONE |
| `motivoCancelacion` | `String` | `sanciones.motivo_cancelacion` | VARCHAR(2000) |
| `incidencia` | `Incidencia` | `sanciones.incidencia_id` → `Incidencia.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `tipo` | `TipoSancion` | `sanciones.tipo_sancion_id` → `TipoSancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `politica` | `PoliticaSancion` | `sanciones.politica_id` → `PoliticaSancion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `sanciones.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsableCancelacion` | `CuentaUsuario` | `sanciones.responsable_cancelacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `multa` | `Multa` | Inversa de `Multa.sancion` | OneToOne; LAZY; sin columna propia |
| `restricciones` | `List<RestriccionServicio>` | Inversa de `RestriccionServicio.sancion` | OneToMany; LAZY; sin columna propia |
| `reposiciones` | `List<ReposicionMaterial>` | Inversa de `ReposicionMaterial.sancion` | OneToMany; LAZY; sin columna propia |
| `historial` | `List<HistorialSancion>` | Inversa de `HistorialSancion.sancion` | OneToMany; LAZY; sin columna propia |

### TipoSancion

Archivo: [TipoSancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/TipoSancion.java). Tabla: `tipos_sancion`.

**Fuente y alcance del Javadoc:** M06, p. impresa 44 (visor 45), elementos principales; RF-M06-23 (p. 46), RN-M06-06 (p. 50). Catalogo exigido por la narrativa, omitido en el grafico; sin filas predeterminadas. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `tipos_sancion.id` | BIGSERIAL PRIMARY KEY |
| `codigo` | `String` | `tipos_sancion.codigo` | VARCHAR(60) NOT NULL UNIQUE |
| `nombre` | `String` | `tipos_sancion.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `tipos_sancion.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `tipos_sancion.estatus` | BOOLEAN NOT NULL |
| `sanciones` | `List<Sancion>` | Inversa de `Sancion.tipo` | OneToMany; LAZY; sin columna propia |
| `politicas` | `List<PoliticaSancion>` | Inversa de `PoliticaSancion.tipoSancion` | OneToMany; LAZY; sin columna propia |

## M07 — Notificaciones y avisos

Paquete modelo.notificaciones; PDF: impresas 52–59, físicas 53–60.

### AvisoGeneral

Archivo: [AvisoGeneral](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/AvisoGeneral.java). Tabla: `avisos_generales`.

**Fuente y alcance del Javadoc:** M07, p. impresa 57 (visor 58), RF-M07-67, RF-M07-75; RN-M07-20, RN-M07-21, RN-M07-22, RN-M07-23, RN-M07-25, RN-M07-27 (pp. 58-59). Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `avisos_generales.id` | BIGSERIAL PRIMARY KEY |
| `titulo` | `String` | `avisos_generales.titulo` | VARCHAR(250) NOT NULL |
| `mensaje` | `String` | `avisos_generales.mensaje` | VARCHAR(10000) NOT NULL |
| `fechaPublicacion` | `LocalDateTime` | `avisos_generales.fecha_publicacion` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `fechaExpiracion` | `LocalDateTime` | `avisos_generales.fecha_expiracion` | TIMESTAMP WITHOUT TIME ZONE |
| `estatus` | `boolean` | `avisos_generales.estatus` | BOOLEAN NOT NULL |
| `publico` | `boolean` | `avisos_generales.publico` | BOOLEAN NOT NULL |
| `fechaModificacion` | `LocalDateTime` | `avisos_generales.fecha_modificacion` | TIMESTAMP WITHOUT TIME ZONE |
| `responsable` | `CuentaUsuario` | `avisos_generales.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsableModificacion` | `CuentaUsuario` | `avisos_generales.responsable_modificacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `destinatarios` | `List<DestinatarioAviso>` | Inversa de `DestinatarioAviso.aviso` | OneToMany; LAZY; sin columna propia |

### CanalNotificacion

Archivo: [CanalNotificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/CanalNotificacion.java). Tabla: `canales_notificacion`.

**Fuente y alcance del Javadoc:** M07, pp. impresas 52/55-57 (visor 53/56-58), RF-M07-45, RF-M07-53, RF-M07-78, RF-M07-79; RN-M07-09, RN-M07-11, RN-M07-12. Modela configuracion de transporte; no implementa ni realiza envios. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `canales_notificacion.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `canales_notificacion.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `canales_notificacion.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `canales_notificacion.estatus` | BOOLEAN NOT NULL |
| `maxReintentos` | `Integer` | `canales_notificacion.max_reintentos` | INTEGER |
| `intervaloReintentoMinutos` | `Integer` | `canales_notificacion.intervalo_reintento_minutos` | INTEGER |
| `envios` | `List<EnvioNotificacion>` | Inversa de `EnvioNotificacion.canal` | OneToMany; LAZY; sin columna propia |
| `plantillas` | `List<PlantillaNotificacion>` | Inversa de `PlantillaNotificacion.canal` | OneToMany; LAZY; sin columna propia |

### DestinatarioAviso

Archivo: [DestinatarioAviso](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/DestinatarioAviso.java). Tabla: `destinatarios_aviso`.

**Fuente y alcance del Javadoc:** M07, p. impresa 57 (visor 58), RF-M07-70; RN-M07-24, RN-M07-25 (p. 59); atributos del diagrama. idReferencia conserva el atributo polimorfico del grafico y no es una FK generica. Los destinatarios por rol cuentan con FK concreta; su interpretacion restante esta pendiente. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `destinatarios_aviso.id` | BIGSERIAL PRIMARY KEY |
| `tipoUsuario` | `String` | `destinatarios_aviso.tipo_usuario` | VARCHAR(100) NOT NULL |
| `idReferencia` | `Long` | `destinatarios_aviso.id_referencia` | BIGINT |
| `aviso` | `AvisoGeneral` | `destinatarios_aviso.aviso_id` → `AvisoGeneral.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `rol` | `Rol` | `destinatarios_aviso.rol_id` → `Rol.id` | BIGINT; ManyToOne LAZY; opcional |

### EnvioNotificacion

Archivo: [EnvioNotificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/EnvioNotificacion.java). Tabla: `envios_notificacion`.

**Fuente y alcance del Javadoc:** M07, pp. impresas 55-56 (visor 56-57), RF-M07-47, RF-M07-53; RN-M07-10, RN-M07-11, RN-M07-12, RN-M07-13 (p. 58). Un registro describe un intento; aceptado por servidor no implica entregado. Ningun metodo envia mensajes. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `envios_notificacion.id` | BIGSERIAL PRIMARY KEY |
| `fechaEnvio` | `LocalDateTime` | `envios_notificacion.fecha_envio` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `estado` | `String` | `envios_notificacion.estado` | VARCHAR(60) NOT NULL |
| `error` | `String` | `envios_notificacion.error` | VARCHAR(3000) |
| `numeroIntento` | `Integer` | `envios_notificacion.numero_intento` | INTEGER NOT NULL |
| `notificacion` | `Notificacion` | `envios_notificacion.notificacion_id` → `Notificacion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `canal` | `CanalNotificacion` | `envios_notificacion.canal_id` → `CanalNotificacion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Notificacion

Archivo: [Notificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/Notificacion.java). Tabla: `notificaciones`.

**Fuente y alcance del Javadoc:** M07, pp. impresas 53/55/57-59 (visor 54/56/58-60), RF-M07-02, RF-M07-07, RF-M07-38, RF-M07-44; RN-M07-02, RN-M07-04, RN-M07-05, RN-M07-10, RN-M07-14, RN-M07-28. El destinatario es una cuenta registrada por RN-M07-14. Estado de lectura y resultado de envio son independientes; no hay envio automatico. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `notificaciones.id` | BIGSERIAL PRIMARY KEY |
| `asunto` | `String` | `notificaciones.asunto` | VARCHAR(250) NOT NULL |
| `mensaje` | `String` | `notificaciones.mensaje` | VARCHAR(10000) NOT NULL |
| `fechaCreacion` | `LocalDateTime` | `notificaciones.fecha_creacion` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `estado` | `String` | `notificaciones.estado` | VARCHAR(60) NOT NULL |
| `fechaLectura` | `LocalDateTime` | `notificaciones.fecha_lectura` | TIMESTAMP WITHOUT TIME ZONE |
| `archivada` | `boolean` | `notificaciones.archivada` | BOOLEAN NOT NULL |
| `eventoOrigen` | `String` | `notificaciones.evento_origen` | VARCHAR(150) |
| `destinatario` | `CuentaUsuario` | `notificaciones.destinatario_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `tipoNotificacion` | `TipoNotificacion` | `notificaciones.tipo_notificacion_id` → `TipoNotificacion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `plantilla` | `PlantillaNotificacion` | `notificaciones.plantilla_id` → `PlantillaNotificacion.id` | BIGINT; ManyToOne LAZY; opcional |
| `prestamo` | `Prestamo` | `notificaciones.prestamo_id` → `Prestamo.id` | BIGINT; ManyToOne LAZY; opcional |
| `devolucion` | `Devolucion` | `notificaciones.devolucion_id` → `Devolucion.id` | BIGINT; ManyToOne LAZY; opcional |
| `reservaLibro` | `ReservaLibro` | `notificaciones.reserva_libro_id` → `ReservaLibro.id` | BIGINT; ManyToOne LAZY; opcional |
| `reservaCubiculo` | `ReservaCubiculo` | `notificaciones.reserva_cubiculo_id` → `ReservaCubiculo.id` | BIGINT; ManyToOne LAZY; opcional |
| `sancion` | `Sancion` | `notificaciones.sancion_id` → `Sancion.id` | BIGINT; ManyToOne LAZY; opcional |
| `pagoMulta` | `PagoMulta` | `notificaciones.pago_multa_id` → `PagoMulta.id` | BIGINT; ManyToOne LAZY; opcional |
| `reposicionMaterial` | `ReposicionMaterial` | `notificaciones.reposicion_material_id` → `ReposicionMaterial.id` | BIGINT; ManyToOne LAZY; opcional |
| `envios` | `List<EnvioNotificacion>` | Inversa de `EnvioNotificacion.notificacion` | OneToMany; LAZY; sin columna propia |

### PlantillaNotificacion

Archivo: [PlantillaNotificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/PlantillaNotificacion.java). Tabla: `plantillas_notificacion`.

**Fuente y alcance del Javadoc:** M07, p. impresa 56 (visor 57), RF-M07-60, RF-M07-66; RN-M07-17, RN-M07-18, RN-M07-19, RN-M07-28 (pp. 58-59). Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `plantillas_notificacion.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `plantillas_notificacion.nombre` | VARCHAR(150) NOT NULL |
| `asunto` | `String` | `plantillas_notificacion.asunto` | VARCHAR(250) NOT NULL |
| `mensaje` | `String` | `plantillas_notificacion.mensaje` | VARCHAR(10000) NOT NULL |
| `variables` | `String` | `plantillas_notificacion.variables` | VARCHAR(3000) |
| `estatus` | `boolean` | `plantillas_notificacion.estatus` | BOOLEAN NOT NULL |
| `tipoNotificacion` | `TipoNotificacion` | `plantillas_notificacion.tipo_notificacion_id` → `TipoNotificacion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `canal` | `CanalNotificacion` | `plantillas_notificacion.canal_id` → `CanalNotificacion.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `notificaciones` | `List<Notificacion>` | Inversa de `Notificacion.plantilla` | OneToMany; LAZY; sin columna propia |

### PreferenciaNotificacion

Archivo: [PreferenciaNotificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/PreferenciaNotificacion.java). Tabla: `preferencias_notificacion`.

**Fuente y alcance del Javadoc:** M07, p. impresa 56 (visor 57), RF-M07-54, RF-M07-59; RN-M07-15, RN-M07-16 (p. 58). Las banderas no desactivan notificaciones obligatorias por si mismas; la regla se implementara en servicios futuros. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `preferencias_notificacion.id` | BIGSERIAL PRIMARY KEY |
| `permiteCorreo` | `boolean` | `preferencias_notificacion.permite_correo` | BOOLEAN NOT NULL |
| `permiteSistema` | `boolean` | `preferencias_notificacion.permite_sistema` | BOOLEAN NOT NULL |
| `permiteSMS` | `boolean` | `preferencias_notificacion.permite_sms` | BOOLEAN NOT NULL |
| `estatus` | `boolean` | `preferencias_notificacion.estatus` | BOOLEAN NOT NULL |
| `cuenta` | `CuentaUsuario` | `preferencias_notificacion.cuenta_id` → `CuentaUsuario.id` | BIGINT NOT NULL UNIQUE; OneToOne LAZY; obligatorio |

### TipoNotificacion

Archivo: [TipoNotificacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/notificaciones/TipoNotificacion.java). Tabla: `tipos_notificacion`.

**Fuente y alcance del Javadoc:** M07, p. impresa 52 (visor 53), elementos principales; RF-M07-06 (p. 53), RN-M07-04 (p. 58). Catalogo narrativo omitido en el diagrama; no contiene datos precargados. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `tipos_notificacion.id` | BIGSERIAL PRIMARY KEY |
| `codigo` | `String` | `tipos_notificacion.codigo` | VARCHAR(60) NOT NULL UNIQUE |
| `nombre` | `String` | `tipos_notificacion.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `tipos_notificacion.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `tipos_notificacion.estatus` | BOOLEAN NOT NULL |
| `obligatoria` | `boolean` | `tipos_notificacion.obligatoria` | BOOLEAN NOT NULL |
| `notificaciones` | `List<Notificacion>` | Inversa de `Notificacion.tipoNotificacion` | OneToMany; LAZY; sin columna propia |
| `plantillas` | `List<PlantillaNotificacion>` | Inversa de `PlantillaNotificacion.tipoNotificacion` | OneToMany; LAZY; sin columna propia |

## M08 — Reportes y estadísticas

Paquete modelo.reportes; PDF: impresas 60–67, físicas 61–68.

### ConfiguracionReporte

Archivo: [ConfiguracionReporte](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/ConfiguracionReporte.java). Tabla: `configuraciones_reporte`.

**Fuente y alcance del Javadoc:** M08, pp. impresas 60/64-65 (visor 61/65-66), RF-M08-70, RF-M08-71, RF-M08-78, RF-M08-81, RF-M08-89; RN-M08-05, RN-M08-07 (p. 66). Programado se conserva como atributo del grafico; no se crean tareas automaticas. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `configuraciones_reporte.id` | BIGSERIAL PRIMARY KEY |
| `formato` | `String` | `configuraciones_reporte.formato` | VARCHAR(60) NOT NULL |
| `filtros` | `String` | `configuraciones_reporte.filtros` | VARCHAR(5000) |
| `programado` | `boolean` | `configuraciones_reporte.programado` | BOOLEAN NOT NULL |
| `estatus` | `boolean` | `configuraciones_reporte.estatus` | BOOLEAN NOT NULL |
| `fechaInicio` | `LocalDate` | `configuraciones_reporte.fecha_inicio` | DATE |
| `fechaFin` | `LocalDate` | `configuraciones_reporte.fecha_fin` | DATE |
| `reporte` | `Reporte` | `configuraciones_reporte.reporte_id` → `Reporte.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### ExportacionReporte

Archivo: [ExportacionReporte](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/ExportacionReporte.java). Tabla: `exportaciones_reporte`.

**Fuente y alcance del Javadoc:** M08, p. impresa 65 (visor 66), RF-M08-78, RF-M08-84; RN-M08-24, RN-M08-25 (p. 67). Representa metadatos de un archivo; no exporta ni publica documentos. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `exportaciones_reporte.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `exportaciones_reporte.fecha` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `formato` | `String` | `exportaciones_reporte.formato` | VARCHAR(60) NOT NULL |
| `archivo` | `String` | `exportaciones_reporte.archivo` | VARCHAR(1000) |
| `estado` | `String` | `exportaciones_reporte.estado` | VARCHAR(60) NOT NULL |
| `reporte` | `Reporte` | `exportaciones_reporte.reporte_id` → `Reporte.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `exportaciones_reporte.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### HistorialReporte

Archivo: [HistorialReporte](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/HistorialReporte.java). Tabla: `historiales_reporte`.

**Fuente y alcance del Javadoc:** M08, p. impresa 65 (visor 66), RF-M08-85, RF-M08-91; RN-M08-27, RN-M08-29 (p. 67). Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `historiales_reporte.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `historiales_reporte.fecha` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `parametros` | `String` | `historiales_reporte.parametros` | VARCHAR(5000) |
| `estado` | `String` | `historiales_reporte.estado` | VARCHAR(60) NOT NULL |
| `fechaInicio` | `LocalDate` | `historiales_reporte.fecha_inicio` | DATE NOT NULL |
| `fechaFin` | `LocalDate` | `historiales_reporte.fecha_fin` | DATE NOT NULL |
| `filtros` | `String` | `historiales_reporte.filtros` | VARCHAR(5000) |
| `reporte` | `Reporte` | `historiales_reporte.reporte_id` → `Reporte.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `tipoReporte` | `TipoReporte` | `historiales_reporte.tipo_reporte_id` → `TipoReporte.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `historiales_reporte.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### Indicador

Archivo: [Indicador](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/Indicador.java). Tabla: `indicadores`.

**Fuente y alcance del Javadoc:** M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-10, RN-M08-11, RN-M08-15, RN-M08-16 (p. 66). Formula y valor son datos del modelo, no calculos ejecutables; vinculo opcional a panel es decision tecnica. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `indicadores.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `indicadores.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `indicadores.descripcion` | VARCHAR(2000) |
| `formula` | `String` | `indicadores.formula` | VARCHAR(3000) |
| `estatus` | `boolean` | `indicadores.estatus` | BOOLEAN NOT NULL |
| `valor` | `BigDecimal` | `indicadores.valor` | NUMERIC(12,2) |
| `unidadMedida` | `String` | `indicadores.unidad_medida` | VARCHAR(100) |
| `fechaActualizacion` | `LocalDateTime` | `indicadores.fecha_actualizacion` | TIMESTAMP WITHOUT TIME ZONE |
| `panel` | `PanelEstadistico` | `indicadores.panel_id` → `PanelEstadistico.id` | BIGINT; ManyToOne LAZY; opcional |

### PanelEstadistico

Archivo: [PanelEstadistico](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/PanelEstadistico.java). Tabla: `paneles_estadisticos`.

**Fuente y alcance del Javadoc:** M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-25 (pp. 66-67). Panel 1:N Indicador con pertenencia opcional es una decision tecnica declarada; la narrativa no fija la multiplicidad exacta. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `paneles_estadisticos.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `paneles_estadisticos.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `paneles_estadisticos.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `paneles_estadisticos.estatus` | BOOLEAN NOT NULL |
| `fechaActualizacion` | `LocalDateTime` | `paneles_estadisticos.fecha_actualizacion` | TIMESTAMP WITHOUT TIME ZONE |
| `indicadores` | `List<Indicador>` | Inversa de `Indicador.panel` | OneToMany; LAZY; sin columna propia |
| `reportes` | `List<Reporte>` | Inversa de `Reporte.panel` | OneToMany; LAZY; sin columna propia |

### Reporte

Archivo: [Reporte](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/Reporte.java). Tabla: `reportes`.

**Fuente y alcance del Javadoc:** M08, pp. impresas 60/65-67 (visor 61/66-68), RF-M08-85, RF-M08-87; RN-M08-04, RN-M08-05, RN-M08-06, RN-M08-25, RN-M08-29. Vinculo opcional a panel es decision tecnica explicita por cardinalidad grafica ambigua; generar reportes no esta implementado. Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `reportes.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `reportes.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `reportes.descripcion` | VARCHAR(2000) |
| `parametros` | `String` | `reportes.parametros` | VARCHAR(5000) |
| `estatus` | `boolean` | `reportes.estatus` | BOOLEAN NOT NULL |
| `fechaInicio` | `LocalDate` | `reportes.fecha_inicio` | DATE NOT NULL |
| `fechaFin` | `LocalDate` | `reportes.fecha_fin` | DATE NOT NULL |
| `fechaGeneracion` | `LocalDateTime` | `reportes.fecha_generacion` | TIMESTAMP WITHOUT TIME ZONE NOT NULL |
| `filtros` | `String` | `reportes.filtros` | VARCHAR(5000) |
| `totalRegistros` | `Long` | `reportes.total_registros` | BIGINT |
| `tipoReporte` | `TipoReporte` | `reportes.tipo_reporte_id` → `TipoReporte.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsable` | `CuentaUsuario` | `reportes.responsable_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `panel` | `PanelEstadistico` | `reportes.panel_id` → `PanelEstadistico.id` | BIGINT; ManyToOne LAZY; opcional |
| `configuraciones` | `List<ConfiguracionReporte>` | Inversa de `ConfiguracionReporte.reporte` | OneToMany; LAZY; sin columna propia |
| `exportaciones` | `List<ExportacionReporte>` | Inversa de `ExportacionReporte.reporte` | OneToMany; LAZY; sin columna propia |
| `historial` | `List<HistorialReporte>` | Inversa de `HistorialReporte.reporte` | OneToMany; LAZY; sin columna propia |

### TipoReporte

Archivo: [TipoReporte](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reportes/TipoReporte.java). Tabla: `tipos_reporte`.

**Fuente y alcance del Javadoc:** M08, p. impresa 60 (visor 61), elementos principales; RF-M08-90 (p. 65), RN-M08-05, RN-M08-21, RN-M08-30 (pp. 66-67). Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia. Sin logica de negocio ni eliminacion en cascada.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `tipos_reporte.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `tipos_reporte.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `tipos_reporte.descripcion` | VARCHAR(2000) |
| `estatus` | `boolean` | `tipos_reporte.estatus` | BOOLEAN NOT NULL |
| `reportes` | `List<Reporte>` | Inversa de `Reporte.tipoReporte` | OneToMany; LAZY; sin columna propia |
| `rolesPermitidos` | `Set<Rol>` | `tipos_reporte_roles` (`tipo_reporte_id`, `rol_id`) | ManyToMany; LAZY; unión hacia Rol |

## M09 / M10 — Configuración y auditoría

Paquete modelo.configuracion; PDF: impresas 68–76, físicas 69–77.

### BitacoraAuditoria

Archivo: [BitacoraAuditoria](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/BitacoraAuditoria.java). Tabla: `bitacoras_auditoria`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 70-71 y 74-76 (fisicas 71-72 y 75-77), RF-M09-26/27/28/29/30/31/32/33/34/37, RN-M09-05/06/07/08/09/10/11/14/29. Responsable opcional permite eventos automaticos o usuarios no identificados; operacionSistema distingue el origen. No se guardan contrasenas ni tokens en resultados, motivos o detalles. La relacion con entidad afectada usa nombre e ID porque puede abarcar cualquier modulo.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `bitacoras_auditoria.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `bitacoras_auditoria.fecha` | TIMESTAMP NOT NULL |
| `accion` | `String` | `bitacoras_auditoria.accion` | VARCHAR(100) NOT NULL |
| `modulo` | `String` | `bitacoras_auditoria.modulo` | VARCHAR(80) NOT NULL |
| `entidad` | `String` | `bitacoras_auditoria.entidad` | VARCHAR(150) |
| `idRegistro` | `Long` | `bitacoras_auditoria.id_registro` | BIGINT |
| `ip` | `String` | `bitacoras_auditoria.ip` | VARCHAR(45) |
| `resultado` | `String` | `bitacoras_auditoria.resultado` | VARCHAR(100) NOT NULL |
| `motivo` | `String` | `bitacoras_auditoria.motivo` | VARCHAR(1000) |
| `operacionSistema` | `Boolean` | `bitacoras_auditoria.operacion_sistema` | BOOLEAN NOT NULL DEFAULT FALSE |
| `responsable` | `CuentaUsuario` | `bitacoras_auditoria.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `detalles` | `Set<DetalleCambio>` | Inversa de `DetalleCambio.evento` | OneToMany; LAZY; sin columna propia |
| `incidentes` | `Set<IncidenteSeguridad>` | Inversa de `IncidenteSeguridad.eventosAuditoria` | ManyToMany; LAZY; sin columna propia |

### ConfiguracionSistema

Archivo: [ConfiguracionSistema](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/ConfiguracionSistema.java). Tabla: `configuraciones_sistema`.

**Fuente y alcance del Javadoc:** M09 del texto / M10 del diagrama, PDF pp. impresas 68-70, 74 y 76 (fisicas 69-71, 75 y 77), RF-M09-01/02/03/04/05/06/08/09/72/87/88/89, RN-M09-30/32. Se conserva la diferencia de numeracion. Atributos institucionales, contacto, zona horaria, formatos y version proceden del PDF; no contienen valores reales ni politicas operativas precargadas.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `configuraciones_sistema.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `configuraciones_sistema.nombre` | VARCHAR(150) NOT NULL |
| `nombreInstitucion` | `String` | `configuraciones_sistema.nombre_institucion` | VARCHAR(200) NOT NULL |
| `nombreBiblioteca` | `String` | `configuraciones_sistema.nombre_biblioteca` | VARCHAR(200) NOT NULL |
| `correoContacto` | `String` | `configuraciones_sistema.correo_contacto` | VARCHAR(254) |
| `telefonoContacto` | `String` | `configuraciones_sistema.telefono_contacto` | VARCHAR(30) |
| `direccionContacto` | `String` | `configuraciones_sistema.direccion_contacto` | VARCHAR(500) |
| `zonaHoraria` | `String` | `configuraciones_sistema.zona_horaria` | VARCHAR(60) NOT NULL |
| `formatoFecha` | `String` | `configuraciones_sistema.formato_fecha` | VARCHAR(80) |
| `formatoHora` | `String` | `configuraciones_sistema.formato_hora` | VARCHAR(80) |
| `descripcion` | `String` | `configuraciones_sistema.descripcion` | VARCHAR(2000) |
| `versionAplicacion` | `String` | `configuraciones_sistema.version_aplicacion` | VARCHAR(80) |
| `fechaUltimaActualizacion` | `LocalDateTime` | `configuraciones_sistema.fecha_ultima_actualizacion` | TIMESTAMP |
| `fechaModificacion` | `LocalDateTime` | `configuraciones_sistema.fecha_modificacion` | TIMESTAMP |
| `frecuenciaRespaldos` | `String` | `configuraciones_sistema.frecuencia_respaldos` | VARCHAR(100) |
| `estatus` | `Boolean` | `configuraciones_sistema.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `responsableModificacion` | `CuentaUsuario` | `configuraciones_sistema.responsable_modificacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `parametros` | `Set<ParametroSistema>` | Inversa de `ParametroSistema.configuracion` | OneToMany; LAZY; sin columna propia |

### DetalleCambio

Archivo: [DetalleCambio](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/DetalleCambio.java). Tabla: `detalles_cambio`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 71 y 75 (fisicas 72 y 76), RF-M09-40/41/42/43/44/45/46, RN-M09-13/14. Cada detalle pertenece obligatoriamente a un evento. Valores son opcionales para altas/bajas o informacion que no sea seguro conservar; no deben contener contrasenas, tokens ni secretos.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `detalles_cambio.id` | BIGSERIAL PRIMARY KEY |
| `campo` | `String` | `detalles_cambio.campo` | VARCHAR(150) NOT NULL |
| `valorAnterior` | `String` | `detalles_cambio.valor_anterior` | VARCHAR(4000) |
| `valorNuevo` | `String` | `detalles_cambio.valor_nuevo` | VARCHAR(4000) |
| `evento` | `BitacoraAuditoria` | `detalles_cambio.evento_id` → `BitacoraAuditoria.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |

### IncidenteSeguridad

Archivo: [IncidenteSeguridad](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/IncidenteSeguridad.java). Tabla: `incidentes_seguridad`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 68, 72 y 75 (fisicas 69, 73 y 76), RF-M09-59/60/61/62/63/64/65, RN-M09-21/22/23/29. Estado y gravedad permanecen como texto sin inventar niveles institucionales. Relacion N:M con eventos de auditoria permite registrar origen y acciones sucesivas del incidente; cuenta afectada y responsable son opcionales para incidentes aun no identificados/asignados.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `incidentes_seguridad.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `incidentes_seguridad.fecha` | TIMESTAMP NOT NULL |
| `descripcion` | `String` | `incidentes_seguridad.descripcion` | VARCHAR(2000) NOT NULL |
| `estado` | `String` | `incidentes_seguridad.estado` | VARCHAR(80) NOT NULL |
| `gravedad` | `String` | `incidentes_seguridad.gravedad` | VARCHAR(60) NOT NULL |
| `observaciones` | `String` | `incidentes_seguridad.observaciones` | VARCHAR(2000) |
| `accionesCorrectivas` | `String` | `incidentes_seguridad.acciones_correctivas` | VARCHAR(2000) |
| `fechaUltimaActualizacion` | `LocalDateTime` | `incidentes_seguridad.fecha_ultima_actualizacion` | TIMESTAMP |
| `cuentaAfectada` | `CuentaUsuario` | `incidentes_seguridad.cuenta_afectada_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `responsable` | `CuentaUsuario` | `incidentes_seguridad.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `eventosAuditoria` | `Set<BitacoraAuditoria>` | `incidentes_auditoria` (`incidente_id`, `evento_id`) | ManyToMany; LAZY; unión hacia BitacoraAuditoria |

### ParametroSistema

Archivo: [ParametroSistema](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/ParametroSistema.java). Tabla: `parametros_sistema`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 68-70 y 74-76 (fisicas 69-71 y 75-77), RF-M09-10/11/12/14/15/16/17, RN-M09-02/03/04/32. Cada parametro pertenece a una configuracion, con clave unica. Tipo de dato es texto explicito sin imponer un catalogo institucional inexistente. Obligatorio y esencial expresan RF-M09-15/16.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `parametros_sistema.id` | BIGSERIAL PRIMARY KEY |
| `clave` | `String` | `parametros_sistema.clave` | VARCHAR(100) NOT NULL |
| `valor` | `String` | `parametros_sistema.valor` | VARCHAR(2000) NOT NULL |
| `tipoDato` | `String` | `parametros_sistema.tipo_dato` | VARCHAR(60) NOT NULL |
| `descripcion` | `String` | `parametros_sistema.descripcion` | VARCHAR(500) |
| `obligatorio` | `Boolean` | `parametros_sistema.obligatorio` | BOOLEAN NOT NULL DEFAULT FALSE |
| `esencial` | `Boolean` | `parametros_sistema.esencial` | BOOLEAN NOT NULL DEFAULT FALSE |
| `estatus` | `Boolean` | `parametros_sistema.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `fechaModificacion` | `LocalDateTime` | `parametros_sistema.fecha_modificacion` | TIMESTAMP |
| `configuracion` | `ConfiguracionSistema` | `parametros_sistema.configuracion_id` → `ConfiguracionSistema.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `responsableModificacion` | `CuentaUsuario` | `parametros_sistema.responsable_modificacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

### PoliticaSeguridad

Archivo: [PoliticaSeguridad](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/PoliticaSeguridad.java). Tabla: `politicas_seguridad`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 68, 72 y 75 (fisicas 69, 73 y 76), RF-M09-50/56/57/58, RN-M09-17/18/19/20. Atributos de duracion e intentos representan condiciones configurables; no se asignan limites institucionales por defecto ni se implementan autenticacion o bloqueo de cuentas.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `politicas_seguridad.id` | BIGSERIAL PRIMARY KEY |
| `nombre` | `String` | `politicas_seguridad.nombre` | VARCHAR(150) NOT NULL |
| `descripcion` | `String` | `politicas_seguridad.descripcion` | VARCHAR(2000) |
| `reglas` | `String` | `politicas_seguridad.reglas` | VARCHAR(4000) |
| `estatus` | `Boolean` | `politicas_seguridad.estatus` | BOOLEAN NOT NULL DEFAULT TRUE |
| `inactividadMaximaMinutos` | `Integer` | `politicas_seguridad.inactividad_maxima_minutos` | INTEGER |
| `intentosFallidosMaximos` | `Integer` | `politicas_seguridad.intentos_fallidos_maximos` | INTEGER |
| `bloqueoTemporalMinutos` | `Integer` | `politicas_seguridad.bloqueo_temporal_minutos` | INTEGER |
| `fechaModificacion` | `LocalDateTime` | `politicas_seguridad.fecha_modificacion` | TIMESTAMP |
| `responsableModificacion` | `CuentaUsuario` | `politicas_seguridad.responsable_modificacion_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

### RespaldoSistema

Archivo: [RespaldoSistema](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/RespaldoSistema.java). Tabla: `respaldos_sistema`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 68, 73 y 75 (fisicas 69, 74 y 76), RF-M09-67/68/69/70/71/74/75/76, RN-M09-24/25/26/27. Registra metadatos de respaldo o restauracion, no el contenido ni credenciales del archivo. Tipo y estado mantienen vocabularios abiertos del diagrama; responsable puede faltar en ejecucion automatica.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `respaldos_sistema.id` | BIGSERIAL PRIMARY KEY |
| `fecha` | `LocalDateTime` | `respaldos_sistema.fecha` | TIMESTAMP NOT NULL |
| `archivo` | `String` | `respaldos_sistema.archivo` | VARCHAR(500) |
| `tipo` | `String` | `respaldos_sistema.tipo` | VARCHAR(80) NOT NULL |
| `estado` | `String` | `respaldos_sistema.estado` | VARCHAR(80) NOT NULL |
| `resultado` | `String` | `respaldos_sistema.resultado` | VARCHAR(2000) |
| `verificacionIntegridad` | `String` | `respaldos_sistema.verificacion_integridad` | VARCHAR(1000) |
| `responsable` | `CuentaUsuario` | `respaldos_sistema.responsable_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |
| `eventoAuditoria` | `BitacoraAuditoria` | `respaldos_sistema.evento_auditoria_id` → `BitacoraAuditoria.id` | BIGINT; ManyToOne LAZY; opcional |

### SesionUsuario

Archivo: [SesionUsuario](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/configuracion/SesionUsuario.java). Tabla: `sesiones_usuario`.

**Fuente y alcance del Javadoc:** M09 texto / M10 grafico, PDF pp. impresas 68, 72 y 75 (fisicas 69, 73 y 76), RF-M09-47/48/49/50/51/52/53/54/58, RN-M09-17/18/19. Sesion pertenece a CuentaUsuario. Ultima actividad soporta el dato necesario para controlar inactividad posteriormente; no almacena identificadores de sesion utilizables ni tokens. Estatus conserva el tipo string del diagrama sin inventar un catalogo institucional.

| Atributo Java | Tipo | Columna o asociación | SQL / contrato JPA |
|---|---|---|---|
| `id` | `Long` | `sesiones_usuario.id` | BIGSERIAL PRIMARY KEY |
| `fechaInicio` | `LocalDateTime` | `sesiones_usuario.fecha_inicio` | TIMESTAMP NOT NULL |
| `fechaFin` | `LocalDateTime` | `sesiones_usuario.fecha_fin` | TIMESTAMP |
| `ultimaActividad` | `LocalDateTime` | `sesiones_usuario.ultima_actividad` | TIMESTAMP |
| `ip` | `String` | `sesiones_usuario.ip` | VARCHAR(45) |
| `estatus` | `String` | `sesiones_usuario.estatus` | VARCHAR(60) NOT NULL |
| `motivoCierre` | `String` | `sesiones_usuario.motivo_cierre` | VARCHAR(500) |
| `cierreAdministrativo` | `Boolean` | `sesiones_usuario.cierre_administrativo` | BOOLEAN NOT NULL DEFAULT FALSE |
| `cuenta` | `CuentaUsuario` | `sesiones_usuario.cuenta_id` → `CuentaUsuario.id` | BIGINT NOT NULL; ManyToOne LAZY; obligatorio |
| `cerradaPor` | `CuentaUsuario` | `sesiones_usuario.cerrada_por_id` → `CuentaUsuario.id` | BIGINT; ManyToOne LAZY; opcional |

## Enums sin tabla propia

| Archivo | Valores |
|---|---|
| [EstadoConservacion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/EstadoConservacion.java) | `EXCELENTE`, `BUENO`, `REGULAR`, `DANADO` |
| [EstadoEjemplar](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/catalogo/EstadoEjemplar.java) | `DISPONIBLE`, `PRESTADO`, `RESERVADO`, `MANTENIMIENTO`, `EXTRAVIADO`, `BAJA` |
| [EstadoDetallePrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/EstadoDetallePrestamo.java) | `ACTIVO`, `VENCIDO`, `DEVUELTO` |
| [EstadoPrestamo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/prestamos/EstadoPrestamo.java) | `ACTIVO`, `FINALIZADO`, `CANCELADO` |
| [EstadoReservaLibro](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/reservas/EstadoReservaLibro.java) | `PENDIENTE`, `EN_ESPERA`, `LISTA_PARA_RECOGER`, `ATENDIDA`, `CANCELADA`, `VENCIDA` |
| [EstadoCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/EstadoCubiculo.java) | `HABILITADO`, `MANTENIMIENTO`, `FUERA_DE_SERVICIO` |
| [EstadoReservaCubiculo](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/cubiculos/EstadoReservaCubiculo.java) | `CONFIRMADA`, `EN_USO`, `FINALIZADA`, `CANCELADA`, `VENCIDA`, `NO_PRESENTADA` |
| [EstadoSancion](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/EstadoSancion.java) | `PENDIENTE`, `ACTIVA`, `CUMPLIDA`, `RESUELTA`, `CANCELADA` |
| [TipoIncidencia](../backend/src/main/java/mx/edu/itspa/biblioteca/modelo/sanciones/TipoIncidencia.java) | `RETRASO`, `DANO`, `EXTRAVIO`, `INCUMPLIMIENTO_RESERVACION`, `AUSENCIA_RESERVACION_CUBICULO`, `USO_INADECUADO_CUBICULO` |

## Tablas de unión sin entidad adicional

| Tabla | Columnas SQL |
|---|---|
| `cubiculos_equipamientos` | `cubiculo_id` BIGINT NOT NULL; `equipamiento_id` BIGINT NOT NULL |
| `cuentas_roles` | `cuenta_id` BIGINT NOT NULL; `rol_id` BIGINT NOT NULL |
| `incidentes_auditoria` | `incidente_id` BIGINT NOT NULL; `evento_id` BIGINT NOT NULL |
| `libros_autores` | `libro_id` BIGINT NOT NULL; `autor_id` BIGINT NOT NULL |
| `libros_categorias` | `libro_id` BIGINT NOT NULL; `categoria_id` BIGINT NOT NULL |
| `roles_permisos` | `rol_id` BIGINT NOT NULL; `permiso_id` BIGINT NOT NULL |
| `tipos_reporte_roles` | `tipo_reporte_id` BIGINT NOT NULL; `rol_id` BIGINT NOT NULL |
