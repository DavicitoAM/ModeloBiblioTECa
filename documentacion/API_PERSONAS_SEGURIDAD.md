# API de personas y seguridad de la aplicación

Contrato del 20 de octubre de 2026. Fuente: M01, PDF pp. impresas 8–14 / físicas 9–15, RF-M01-01..30/36..46 y RN-M01-01..22. El pedido actual autoriza interfaz y operación real; se conservan las entidades, cardinalidades y migraciones V1/V2.

## Versión vigente: acceso directo

El responsable solicitó omitir completamente el login para esta versión e integrar después la identidad institucional. La propiedad `biblioteca.acceso.directo` vale `true` por defecto. `GET /auth/estado` devuelve `accesoDirecto:true` y la identidad **técnica**, no humana, `SISTEMA_LOCAL` / «Operación biblioteca». El índice abre los servicios directamente. CSRF y su cookie HttpOnly siguen vigentes; las mutaciones requieren el token entregado por el estado.

La primera petición API crea, de forma idempotente, el registro técnico exigido por las FK de responsables del modelo, un hash aleatorio sin credencial humana y el rol operativo administrativo. No crea un login ni una fila `sesiones_usuario`, ni guarda autenticación en la sesión HTTP. Las peticiones reciben el contexto técnico solo durante su ejecución. No se puede iniciar sesión con `SISTEMA_LOCAL` en ningún modo; se excluye de los directorios humanos y no admite edición.

En modo directo, `/auth/login`, `/auth/inicializar`, `/auth/registro`, `/auth/perfil` y `/auth/password` responden 409. `/auth/logout` devuelve el mismo estado de acceso directo. La UI no muestra esos flujos. Las operaciones de catálogo, personas, visitas y servicios quedan disponibles en la instalación local, vinculadas al responsable técnico para auditoría. Este modo debe permanecer ligado a `127.0.0.1`; el responsable integrará el acceso institucional antes de exponerlo a una red.

`biblioteca.acceso.directo=false` conserva el contrato de seguridad descrito a continuación para la integración futura y sus pruebas. Los tests de seguridad declaran `false`; los tres tests de acceso directo declaran `true` y revierten sus registros en `biblioteca_modelo_test`.

La sesión usa cookie de servidor HttpOnly. El cliente empieza con `GET /api/app/auth/estado`, conserva `csrfToken` y envía en cada POST/PUT el encabezado cuyo nombre devuelve `csrfHeader` (por defecto `X-CSRF-TOKEN`). El navegador conserva la cookie. Login, inicialización y logout devuelven un token CSRF nuevo; sustituir siempre el anterior. No usar almacenamiento local de credenciales ni tokens inventados. Todas las respuestas de error usan `{error: "mensaje"}`.

| Método y ruta bajo `/api/app` | Entrada / respuesta |
|---|---|
| GET `/auth/estado` | Público: `{inicializado:boolean,usuario:null\|Usuario,csrfToken,csrfHeader}` |
| POST `/auth/inicializar` | Solo primera cuenta. `{nombre,apellidos,usuario,password}`. Crea persona, administrador y roles operativos y autentica; devuelve estado |
| POST `/auth/login` | `{usuario,password}`. Devuelve estado |
| POST `/auth/logout` | Cierra sesión y devuelve estado anónimo con nuevo CSRF |
| GET `/auth/me` | `Usuario` autenticado |
| PUT `/auth/perfil` | `{nombre,apellidos,correo?,telefono?}`. Solo perfil propio; devuelve esos campos más id |
| POST `/auth/password` | `{actual,nueva}`. Verifica contraseña actual, actualiza y cierra sesión |
| GET `/auth/catalogos-registro` | Público: `{carreras:[{id,nombre}],departamentos:[{id,nombre}]}` |
| POST `/auth/registro` | Público con CSRF: `{persona:PersonaSolicitud,usuario,password}`. Crea exclusivamente rol LECTOR y estado PENDIENTE. No autentica |
| GET `/personas?q=...` | Gestión: lista completa filtrable; lector: solo persona propia |
| GET `/personas/{id}` | Persona con datos de subtipo; exige propiedad o gestión |
| POST `/personas` y PUT `/personas/{id}` | Gestión. `PersonaSolicitud`; devuelve fila guardada |
| GET `/cuentas` | Administrador. Lista sin contraseñas/hashes con `roles:string[]` |
| POST `/cuentas` y PUT `/cuentas/{id}` | Administrador. `{persona_id,usuario,password,roles:string[],estatus,estado_verificacion}`; password obligatorio al crear y opcional al editar |
| POST `/cuentas/{id}/verificacion` | Gestión. `{resultado:"VERIFICADA"\|"PENDIENTE"\|"RECHAZADA",observaciones}`. Revisión manual con evidencia en historial |
| GET `/cuentas/{id}/verificaciones` | Gestión. Historial de resultado, observaciones, fecha y responsable de cada revisión |
| GET `/roles` | Gestión: `[{id,nombre,descripcion,estatus,permisos_ids}]` |
| POST `/roles` y PUT `/roles/{id}` | Administrador: `{nombre,descripcion?,estatus?,permisos_ids?:number[]}` |
| GET `/catalogos-personas` | `{carreras,departamentos,motivos_visita}`. Administrador obtiene además `permisos` |
| POST `/catalogos-personas/{tipo}` y PUT `/catalogos-personas/{tipo}/{id}` | Gestión, tipo `carreras`, `departamentos`, `motivos_visita`; `permisos` requiere administrador. `{nombre,descripcion?,estatus?}` |
| GET `/visitas?activas=false&persona_id=...` | Gestión ve todas; lector solo propias. Filas con persona, motivo y fechas |
| POST `/visitas` | Gestión. `{persona_id,motivo_id,observaciones?}`. Registra entrada con hora del servidor |
| POST `/visitas/{id}/salida` | Gestión. Registra salida con hora del servidor; rechaza doble cierre |
| PUT `/visitas/{id}` | Gestión. `{fecha_entrada,fecha_salida:null\|fecha,motivo}`. Corrección administrativa de fechas con valores anteriores/nuevos en bitácora y sin solapes |

`Usuario` (camelCase): `{id,personaId,usuario,nombre,roles:["ADMINISTRADOR"|"BIBLIOTECARIO"|"LECTOR"],estadoVerificacion}`. Los datos de tablas usan snake_case y `id` numérico.

`PersonaSolicitud`: `{tipo,nombre,apellidos,correo?,telefono?,fecha_nacimiento?,tipo_identificacion?,numero_identificacion?,numero_empleado?,estatus?,matricula?,semestre?,estado_academico?,carrera_id?,departamento_id?,area_trabajo?,puesto?,institucion?}`. Tipos: `ESTUDIANTE`, `DOCENTE`, `EMPLEADO`, `EXTERNO`, `PERSONA` (este último solo gestión). Crear estudiante requiere matrícula/carrera; docente número de empleado/departamento; empleado número de empleado/área de trabajo. Fechas `YYYY-MM-DD`. El tipo no cambia al editar. La respuesta añade `carrera`, `departamento`, `cuenta_id` y `dentro`.

Contraseñas: al menos diez caracteres y máximo 72 bytes UTF-8 por BCrypt. Usuario: 3–100 caracteres `a-z0-9._@-`, se normaliza a minúsculas. Cuentas inactivas y personas desactivadas no acceden. Cambios de contraseña invalidan sesiones existentes. Revocaciones de roles se aplican en la siguiente petición. No se permite quitar el último administrador ni perder el acceso administrativo propio desde su sesión.

La primera cuenta de administración se crea con datos elegidos por el responsable; no hay usuario/contraseña prefijados. Bootstrap serializado con advisory lock PostgreSQL para impedir dos primeras cuentas simultáneas. Los autorregistros permanecen pendientes de revisión, con acceso de lectura personal y catálogo, sin mutaciones de servicios restringidos hasta verificación. La revisión manual no se presenta como verificación automática de correo ni de sistemas escolares externos.

Helper para los servicios: `mx.edu.itspa.biblioteca.services.impl.IdentidadActual`: `cuentaId()`, `personaId()`, `esGestion()`, `tieneRol(String)`, `exigirPropietario(Long)`, `exigirGestion()`, `exigirAdministrador()`. La identidad proviene de la sesión real del servidor. Para gestión: `@PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")`; administración: `hasRole('ADMINISTRADOR')`.

Sesiones y accesos quedan registrados en `sesiones_usuario`/`bitacoras_auditoria`; entradas usan bloqueo por persona para impedir dos visitas abiertas concurrentes. Parámetros de seguridad leen la última política activa; hasta configuración se utilizan límites técnicos iniciales de 30 minutos de inactividad, cinco intentos fallidos y bloqueo de 15 minutos. Los parámetros no son una afirmación de políticas institucionales aprobadas.

No hay envío automático de recuperación/verificación por correo sin transporte configurado. Un administrador puede restablecer una contraseña desde cuentas después de verificar la identidad mediante el procedimiento institucional; el usuario cambia su contraseña en perfil. La cookie segura HTTPS debe activarse al desplegar con TLS; el desarrollo local usa HTTP y SameSite Strict.
