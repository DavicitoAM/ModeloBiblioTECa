# API operativa de apoyo M07–M09

Pedido del 20/10/2026: interfaz y funciones reales del sistema. Fuentes: PDF M07 pp. impresas 52–59/físicas 53–60; M08 pp. 60–67/61–68; M09 pp. 68–76/69–77. No cambia entidades, cardinalidades ni V1/V2. Los cuerpos son objetos JSON con `snake_case`.

| Método y ruta `/api/app` | Acceso | Entrada / resultado |
|---|---|---|
| GET `/inicio` | Público | `{institucion,estadisticas:{titulos,ejemplares,disponibles,categorias},avisos:[]}` |
| GET `/panel` | Cuenta | `{indicadores,mis_indicadores,actividad:[],prestamos_recientes:[],alertas:[],actividad_semanal:[],avisos:[]}`. Lector sólo datos propios; gestión agregado real. |
| GET `/notificaciones` | Propio | `?estado=NO_LEIDA&tipo=PRESTAMO&archivadas=false&desde=2026-10-01&hasta=2026-10-31`; `{items:[{id,asunto,mensaje,fecha_creacion,estado,fecha_lectura,archivada,tipo,nombre_tipo}],no_leidas,tipos:[{codigo,nombre}]}`. Hasta 500 mensajes por filtro. |
| POST `/notificaciones/{id}/leer` | Propio | Notificación actualizada |
| POST `/notificaciones/todas-leidas` | Propio | `{actualizadas:n}` |
| POST `/notificaciones/{id}/archivar` | Propio | `{archivada:true}` |
| GET/PUT `/preferencias` | Propio | `{permite_sistema,permite_correo,permite_sms,correo_disponible:false,sms_disponible:false}`. Canales externos no configurados no se pueden activar. Servicio obligatorio siempre interno. |
| GET `/avisos` | Cuenta | `?gestion=true` para personal; array `{id,titulo,mensaje,fecha_publicacion,fecha_expiracion,estatus,publico,roles:[]}`. Sólo vigentes dirigidos al lector; gestión incluye borradores/retirados. |
| POST `/avisos`; PUT `/avisos/{id}` | Gestión | `{titulo,mensaje,fecha_publicacion?,fecha_expiracion?,estatus:true,publico:true,roles:["LECTOR"],grupos:["ESTUDIANTE"]}`. Grupos admitidos: ESTUDIANTE, DOCENTE, EMPLEADO, EXTERNO, PERSONA; roles existentes. Sin roles/grupos y `publico:false` = toda cuenta. Avisos públicos requieren ambos vacíos. |
| GET `/plantillas` | Administrador | `{items:[],tipos:[],canales:[],variables:[nombre,asunto,mensaje,fecha,libro,vencimiento,cubiculo,monto]}` |
| POST `/plantillas`; PUT `/plantillas/{id}` | Administrador | `{nombre,asunto,mensaje,tipo_notificacion_id,canal_id,estatus}`. Variables `{{nombre}}` autorizadas; se aplica la última plantilla activa de canal SISTEMA al evento confirmado. |
| POST `/plantillas/previsualizar` | Administrador | `{asunto,mensaje}` → `{asunto,mensaje,vista_previa:true}` con datos rotulados de ejemplo, sin envío ni persistencia. |
| PUT `/canales/{id}` | Administrador | `{estatus,max_reintentos,intervalo_reintento_minutos}`; proveedor externo no configurado impide habilitarlo; canal interno obligatorio no se apaga. |
| GET `/reportes` | Gestión | `?tipo=catalogo|prestamos|reservas|sanciones|visitas|cubiculos|usuarios|devoluciones|auditoria&desde=YYYY-MM-DD&hasta=YYYY-MM-DD`; `{id,tipo,desde,hasta,generado_en,total_registros,columnas:[],filas:[]}` |
| GET `/reportes/exportar` | Gestión | Mismos filtros + `formato=csv|xlsx|pdf|json`; descarga nativa con nombre y Content-Type. Auditoría sólo administrador. |
| GET `/configuracion` | Administrador | `{institucion:{...},parametros:[],seguridad:{...},estado_servicios:{base_datos,version_aplicacion}}` |
| PUT `/configuracion` | Administrador | `{institucion:{nombre?,nombre_institucion,nombre_biblioteca,correo_contacto?,telefono_contacto?,direccion_contacto?,zona_horaria,formato_fecha?,formato_hora?,descripcion?},parametros:[{clave,valor,tipo_dato:TEXTO|ENTERO|DECIMAL|BOOLEANO,descripcion?,obligatorio?,esencial?}],seguridad?:{inactividad_maxima_minutos,intentos_fallidos_maximos,bloqueo_temporal_minutos},motivo?}` |
| GET `/auditoria` | Administrador | `?modulo=M09&accion=MODIFICAR&usuario=admin&desde&hasta`; array de últimos 500 eventos con `cambios:[]` |
| GET `/respaldos` | Administrador | Historial con archivo descargable, SHA-256 y resultado. |
| POST `/respaldos` | Administrador | Crea instantánea lógica JSON persistente de tablas de negocio sin contraseñas ni hashes; `{id,tipo,estado,resultado,verificacion_integridad,descarga}`. No sustituye respaldo completo autenticación. |
| GET `/respaldos/{id}/descargar` | Administrador | Descarga JSON sólo tras validar ruta y SHA-256. |
| GET/POST `/incidentes`; PUT `/incidentes/{id}` | Administrador | Gestión de incidentes reales `{descripcion,gravedad:BAJA|MEDIA|ALTA|CRITICA,estado:ABIERTO|EN_REVISION|RESUELTO,observaciones?,acciones_correctivas?,cuenta_afectada_id?}`; conserva trazabilidad. |

`indicadores`: `libros`, `ejemplares`, `disponibles`, `prestamos_activos`, `prestamos_vencidos`, `reservas_pendientes`, `cubiculos_disponibles`, `usuarios_activos`, `sanciones_activas`, `visitas_hoy`. `mis_indicadores`: `prestamos_activos`, `reservas_activas`, `sanciones_activas`.

Las notificaciones internas tienen lectura separada de entrega y se conservan al archivar. Los avisos y modificaciones de configuración guardan auditoría y detalles. Exportaciones neutralizan fórmulas en celdas de texto CSV/XLSX. No se envían mensajes externos ni se afirma entregabilidad sin proveedor.

Los rangos de reportes son inclusivos, con límite explícito de 10 000 filas: si se supera, se pide acotar el periodo en lugar de truncar silenciosamente. Catálogo representa inventario actual; rango se conserva como contexto, sin atribuir una fecha de alta inexistente al Libro.

## Decisiones operativas y evidencias

- El pedido actual autoriza las operaciones de M07–M09 y sus vistas; no altera las relaciones del modelo. Para avisos por grupo se consulta el subtipo persistente de Persona en las tablas JOINED aprobadas. `id_referencia` no se utiliza como una FK polimórfica inventada. Roles y grupos seleccionados se combinan por inclusión (pertenecer a cualquiera habilita el aviso).
- Las instantáneas seguras se escriben fuera de los recursos públicos, en `.itspa-tmp/respaldos` bajo el directorio de ejecución o en `biblioteca.respaldos.directorio` configurado por el operador. Su transacción usa lectura repetible para conservar una vista consistente. No se implementa restauración automática destructiva.
- Verificación real inicial 20/10/2026: conexión a `biblioteca_modelo_test`, 78 tablas incluidas Flyway; `mvn -DskipTests compile` terminó BUILD SUCCESS (114 fuentes presentes en ese momento). `mvn -Dtest=ApoyoSistemaIntegracionTest,ReportesExportadorTest test` terminó BUILD SUCCESS con 8 casos, cero fallos/errores. Las verificaciones añadidas después se incluyen en la siguiente corrida integrada del coordinador; consultar su bitácora final.
- Capturas que debe conservar la persona: notificación propia antes/después de lectura y su fila `envios_notificacion` independiente; aviso dirigido frente a visitante público; reporte con fechas y archivos PDF/XLSX descargados; cambio institucional junto a su evento/detalles de auditoría; instantánea y su SHA-256 en historial. No se afirma que estas capturas ya existan.
