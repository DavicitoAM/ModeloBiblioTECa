package mx.edu.itspa.biblioteca.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import mx.edu.itspa.biblioteca.services.ApoyoSistemaServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

/**
 * M07 RF-36..59/67..82 y RN-06/09/10/14..25; M08 RF-01/70/78/85 y RN-09/25;
 * M09 RF-01..17/26..46/59..76, RN-01..16/21..32. PDF pp.52–76, visor53–77.
 * Consultas y comandos parametrizados sobre el esquema aprobado; DTO Map sin entidades.
 */
@Service
@EnableScheduling
@Transactional(readOnly = true)
public class ApoyoSistemaServicioImpl implements ApoyoSistemaServicio {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final IdentidadActual identidad;
    private final Path directorioRespaldos;
    @Value("${biblioteca.acceso.directo:true}")
    private boolean accesoDirecto;
    private static final Set<String> COLUMNAS_EVENTO = Set.of("prestamos", "devoluciones", "reservas_libro", "reservas_cubiculo", "sanciones", "pagos_multa", "reposiciones_material");
    private static final Map<String, String> FK_EVENTO = Map.of("prestamos", "prestamo_id", "devoluciones", "devolucion_id", "reservas_libro", "reserva_libro_id", "reservas_cubiculo", "reserva_cubiculo_id", "sanciones", "sancion_id", "pagos_multa", "pago_multa_id", "reposiciones_material", "reposicion_material_id");
    private static final Set<String> VARIABLES_PLANTILLA = Set.of("nombre","asunto","mensaje","fecha","libro","vencimiento","cubiculo","monto");

    public ApoyoSistemaServicioImpl(JdbcTemplate jdbc, ObjectMapper json, IdentidadActual identidad,
            @Value("${biblioteca.respaldos.directorio:.itspa-tmp/respaldos}") String directorioRespaldos) {
        this.jdbc = jdbc;
        this.json = json;
        this.identidad = identidad;
        this.directorioRespaldos = Path.of(directorioRespaldos).toAbsolutePath().normalize();
    }

    @Override
    public Map<String, Object> inicio() {
        return mapa("institucion", institucionPublica(), "estadisticas", mapa(
            "titulos", contar("select count(*) from libros where estatus"),
            "ejemplares", contar("select count(*) from ejemplares e join libros l on l.id=e.libro_id where e.estatus and l.estatus"),
            "disponibles", contar("select count(*) from ejemplares e join libros l on l.id=e.libro_id where e.estatus and l.estatus and e.estado='DISPONIBLE' and e.estado_conservacion<>'DANADO'"),
            "categorias", contar("select count(*) from categorias where estatus")),
            "avisos", consultar("select id,titulo,mensaje,fecha_publicacion,fecha_expiracion from avisos_generales where estatus and publico and fecha_publicacion<=current_timestamp and (fecha_expiracion is null or fecha_expiracion>current_timestamp) order by fecha_publicacion desc limit 6"));
    }

    @Override
    public Map<String, Object> panel() {
        long persona = identidad.personaId();
        boolean gestion = identidad.esGestion();
        Map<String, Object> mios = mapa(
            "prestamos_activos", contar("select count(distinct p.id) from detalles_prestamo d join prestamos p on p.id=d.prestamo_id where p.persona_id=? and p.estado='ACTIVO' and d.estado<>'DEVUELTO'", persona),
            "reservas_activas", contar("select count(*) from reservas_libro where persona_id=? and estado in ('PENDIENTE','EN_ESPERA','LISTA_PARA_RECOGER')", persona),
            "sanciones_activas", contar("select count(*) from sanciones s join incidencias i on i.id=s.incidencia_id where i.persona_id=? and s.estado in ('ACTIVA','PENDIENTE')", persona));
        Map<String, Object> indicadores = mapa(
            "libros", contar("select count(*) from libros where estatus"),
            "ejemplares", contar("select count(*) from ejemplares where estatus"),
            "disponibles", contar("select count(*) from ejemplares e join libros l on l.id=e.libro_id where e.estatus and l.estatus and e.estado='DISPONIBLE' and e.estado_conservacion<>'DANADO'"),
            "cubiculos_disponibles", contar("select count(*) from cubiculos c where c.estatus and c.estado='HABILITADO' and not exists(select 1 from reservas_cubiculo r where r.cubiculo_id=c.id and r.estado in ('CONFIRMADA','EN_USO') and current_timestamp>=r.fecha_inicio and current_timestamp<r.fecha_fin) and not exists(select 1 from usos_cubiculo u join reservas_cubiculo r on r.id=u.reserva_id where r.cubiculo_id=c.id and u.fecha_fin is null) and not exists(select 1 from bloqueos_cubiculo b where b.cubiculo_id=c.id and b.estatus and current_timestamp>=b.fecha_inicio and current_timestamp<b.fecha_fin)"));
        if (gestion) {
            indicadores.putAll(mapa(
                "prestamos_activos", contar("select count(distinct p.id) from detalles_prestamo d join prestamos p on p.id=d.prestamo_id where p.estado='ACTIVO' and d.estado<>'DEVUELTO'"),
                "prestamos_vencidos", contar("select count(distinct p.id) from detalles_prestamo d join prestamos p on p.id=d.prestamo_id where p.estado='ACTIVO' and d.estado<>'DEVUELTO' and d.fecha_vencimiento<current_date"),
                "reservas_pendientes", contar("select count(*) from reservas_libro where estado in ('PENDIENTE','EN_ESPERA','LISTA_PARA_RECOGER')"),
                "usuarios_activos", contar("select count(*) from cuentas_usuario c join personas p on p.id=c.persona_id where c.estatus and coalesce(p.tipo_identificacion,'')<>'IDENTIDAD_TECNICA'"),
                "sanciones_activas", contar("select count(*) from sanciones where estado in ('ACTIVA','PENDIENTE')"),
                "visitas_hoy", contar("select count(*) from registros_visita where fecha_entrada>=current_date and fecha_entrada<current_date+interval '1 day'")));
        } else {
            indicadores.putAll(mapa("prestamos_activos", mios.get("prestamos_activos"), "prestamos_vencidos", contar("select count(distinct p.id) from detalles_prestamo d join prestamos p on p.id=d.prestamo_id where p.persona_id=? and p.estado='ACTIVO' and d.estado<>'DEVUELTO' and d.fecha_vencimiento<current_date",persona), "reservas_pendientes",mios.get("reservas_activas"), "sanciones_activas",mios.get("sanciones_activas")));
        }
        String filtro = gestion ? "" : " and p.persona_id=" + persona;
        List<Map<String, Object>> prestamos = consultar("select p.id,p.fecha_prestamo,p.estado,p.persona_id,pe.nombre||' '||pe.apellidos as persona,min(d.fecha_vencimiento) as fecha_vencimiento,count(d.id) as ejemplares,string_agg(l.titulo,', ' order by d.id) as titulos from prestamos p join personas pe on pe.id=p.persona_id join detalles_prestamo d on d.prestamo_id=p.id join ejemplares e on e.id=d.ejemplar_id join libros l on l.id=e.libro_id where true"+filtro+" group by p.id,pe.nombre,pe.apellidos order by p.fecha_prestamo desc limit 6");
        List<Map<String, Object>> actividad = gestion
            ? consultar("select b.id,b.fecha,b.accion,b.modulo,b.entidad,b.resultado,c.usuario as responsable from bitacoras_auditoria b left join cuentas_usuario c on c.id=b.responsable_id order by b.fecha desc limit 8")
            : consultar("select id,fecha_creacion as fecha,asunto as accion,'M07' as modulo,estado as resultado from notificaciones where destinatario_id=? order by fecha_creacion desc limit 8", identidad.cuentaId());
        List<Map<String, Object>> alertas = new ArrayList<>();
        agregarAlerta(alertas, "VENCIMIENTO", "Préstamos vencidos", "Revisar devoluciones pendientes", indicadores.get("prestamos_vencidos"));
        agregarAlerta(alertas, "RESERVA", "Reservaciones activas", "Consultar solicitudes y asignaciones", indicadores.get("reservas_pendientes"));
        agregarAlerta(alertas, "SANCION", "Sanciones por atender", "Consultar restricciones y saldos", indicadores.get("sanciones_activas"));
        String semanalFiltro = gestion ? "" : " and p.persona_id="+persona;
        List<Map<String, Object>> semanal = consultar("select d::date as fecha,(select count(*) from prestamos p where p.fecha_prestamo::date=d::date"+semanalFiltro+") as prestamos,(select count(*) from devoluciones v join detalles_prestamo dp on dp.id=v.detalle_prestamo_id join prestamos p on p.id=dp.prestamo_id where v.fecha_devolucion::date=d::date"+semanalFiltro+") as devoluciones,(select count(*) from registros_visita p where p.fecha_entrada::date=d::date"+semanalFiltro+") as visitas from generate_series(current_date-6,current_date,interval '1 day') d order by fecha");
        return mapa("indicadores", indicadores, "mis_indicadores", mios, "actividad", actividad, "prestamos_recientes", prestamos, "alertas", alertas, "actividad_semanal", semanal, "avisos", avisos(false));
    }

    @Override
    public Map<String, Object> notificaciones(String estado, String tipo, boolean archivadas, LocalDate desde, LocalDate hasta) {
        return notificaciones(estado,tipo,archivadas,desde,hasta,false,null);
    }

    @Override
    public Map<String, Object> notificaciones(String estado, String tipo, boolean archivadas, LocalDate desde, LocalDate hasta, boolean gestion, Long cuentaId) {
        validarRango(desde, hasta);
        if(gestion) {
            if(!accesoDirecto)throw fallo(HttpStatus.FORBIDDEN,"La supervisión de comunicaciones requiere acceso directo local.");
            identidad.exigirGestion();
        }
        Long destinatario=gestion?cuentaId:identidad.cuentaId();
        if(destinatario!=null&&destinatario<=0)throw fallo(HttpStatus.BAD_REQUEST,"La cuenta destinataria no es válida.");
        List<Object> args = new ArrayList<>(List.of(archivadas));
        String sql = "select n.id,n.asunto,n.mensaje,n.fecha_creacion,n.estado,n.fecha_lectura,n.archivada,n.evento_origen,t.codigo as tipo,t.nombre as nombre_tipo,c.id as destinatario_id,c.usuario as destinatario_usuario,p.nombre||' '||p.apellidos as destinatario from notificaciones n join tipos_notificacion t on t.id=n.tipo_notificacion_id join cuentas_usuario c on c.id=n.destinatario_id join personas p on p.id=c.persona_id where n.archivada=?";
        if(destinatario!=null){sql+=" and n.destinatario_id=?";args.add(destinatario);}
        if (estado != null && !estado.isBlank()) { sql += " and n.estado=?"; args.add(estado); }
        if (tipo != null && !tipo.isBlank()) { sql += " and t.codigo=?"; args.add(tipo); }
        sql += filtroFechas("n.fecha_creacion", desde, hasta, args);
        Object[] filtroCuenta=destinatario==null?new Object[]{}:new Object[]{destinatario};
        String condicionCuenta=destinatario==null?"":" and n.destinatario_id=?";
        return mapa("items", consultar(sql + " order by n.fecha_creacion desc,n.id desc limit 500", args.toArray()),
            "no_leidas", contar("select count(*) from notificaciones n where n.fecha_lectura is null and not n.archivada"+condicionCuenta,filtroCuenta),
            "tipos",consultar("select distinct t.codigo,t.nombre from tipos_notificacion t join notificaciones n on n.tipo_notificacion_id=t.id where true"+condicionCuenta+" order by t.nombre",filtroCuenta),
            "destinatarios",gestion?consultar("select distinct c.id,c.usuario,p.nombre||' '||p.apellidos as nombre from cuentas_usuario c join personas p on p.id=c.persona_id join notificaciones n on n.destinatario_id=c.id order by nombre"):List.of(),
            "supervision",gestion);
    }

    @Override @Transactional
    public Map<String, Object> leerNotificacion(long id) {
        int cantidad = jdbc.update("update notificaciones set estado='LEIDA',fecha_lectura=coalesce(fecha_lectura,current_timestamp) where id=? and destinatario_id=?", id, identidad.cuentaId());
        encontrado(cantidad);
        return uno("select id,asunto,mensaje,estado,fecha_lectura,archivada from notificaciones where id=? and destinatario_id=?", id, identidad.cuentaId());
    }

    @Override @Transactional
    public Map<String, Object> leerTodas() {
        return mapa("actualizadas", jdbc.update("update notificaciones set estado='LEIDA',fecha_lectura=current_timestamp where destinatario_id=? and fecha_lectura is null and not archivada", identidad.cuentaId()));
    }

    @Override @Transactional
    public Map<String, Object> archivarNotificacion(long id) {
        encontrado(jdbc.update("update notificaciones set archivada=true where id=? and destinatario_id=?", id, identidad.cuentaId()));
        return mapa("archivada", true);
    }

    @Override
    public Map<String, Object> preferencias() {
        List<Map<String,Object>> lista = consultar("select permite_correo,permite_sistema,permite_sms from preferencias_notificacion where cuenta_id=?", identidad.cuentaId());
        Map<String,Object> resultado = lista.isEmpty() ? mapa("permite_correo",false,"permite_sistema",true,"permite_sms",false) : lista.getFirst();
        resultado.putAll(mapa("correo_disponible",false,"sms_disponible",false));
        return resultado;
    }

    @Override @Transactional
    public Map<String, Object> guardarPreferencias(Map<String, Object> solicitud) {
        boolean sistema=booleano(solicitud,"permite_sistema",true), correo=booleano(solicitud,"permite_correo",false), sms=booleano(solicitud,"permite_sms",false);
        if (correo || sms) throw fallo(HttpStatus.CONFLICT,"Correo y SMS requieren un proveedor institucional configurado; el canal interno permanece disponible.");
        jdbc.update("insert into preferencias_notificacion(permite_correo,permite_sistema,permite_sms,estatus,cuenta_id) values(false,?,false,true,?) on conflict(cuenta_id) do update set permite_correo=false,permite_sistema=excluded.permite_sistema,permite_sms=false,estatus=true", sistema,identidad.cuentaId());
        return preferencias();
    }

    @Override
    public List<Map<String, Object>> avisos(boolean gestion) {
        if (gestion) identidad.exigirGestion();
        List<Map<String,Object>> lista;
        if (gestion) lista=consultar("select * from avisos_generales order by fecha_publicacion desc,id desc limit 200");
        else lista=consultar("select a.id,a.titulo,a.mensaje,a.fecha_publicacion,a.fecha_expiracion,a.estatus,a.publico from avisos_generales a where a.estatus and a.fecha_publicacion<=current_timestamp and (a.fecha_expiracion is null or a.fecha_expiracion>current_timestamp) and (a.publico or not exists(select 1 from destinatarios_aviso d where d.aviso_id=a.id) or exists(select 1 from destinatarios_aviso d join cuentas_roles cr on cr.rol_id=d.rol_id where d.aviso_id=a.id and cr.cuenta_id=?) or exists(select 1 from destinatarios_aviso d where d.aviso_id=a.id and d.rol_id is null and d.id_referencia is null and d.tipo_usuario=(select case when exists(select 1 from estudiantes where id=c.persona_id) then 'ESTUDIANTE' when exists(select 1 from docentes where id=c.persona_id) then 'DOCENTE' when exists(select 1 from empleados where id=c.persona_id) then 'EMPLEADO' when exists(select 1 from usuarios_externos where id=c.persona_id) then 'EXTERNO' else 'PERSONA' end from cuentas_usuario c where c.id=?))) order by a.fecha_publicacion desc,a.id desc limit 100", identidad.cuentaId(),identidad.cuentaId());
        for (Map<String,Object> aviso:lista) {
            aviso.put("roles",jdbc.queryForList("select r.nombre from destinatarios_aviso d join roles r on r.id=d.rol_id where d.aviso_id=? order by r.nombre",String.class,aviso.get("id")));
            aviso.put("grupos",jdbc.queryForList("select tipo_usuario from destinatarios_aviso where aviso_id=? and rol_id is null and id_referencia is null order by tipo_usuario",String.class,aviso.get("id")));
        }
        return lista;
    }

    @Override @Transactional
    public Map<String, Object> guardarAviso(Long id, Map<String, Object> solicitud) {
        identidad.exigirGestion();
        String titulo=texto(solicitud,"titulo",250,true), mensaje=texto(solicitud,"mensaje",10000,true);
        LocalDateTime inicio=fechaHora(solicitud,"fecha_publicacion",jdbc.queryForObject("select localtimestamp",LocalDateTime.class)), fin=fechaHora(solicitud,"fecha_expiracion",null);
        if(fin!=null&&!fin.isAfter(inicio)) throw fallo(HttpStatus.BAD_REQUEST,"La expiración debe ser posterior a la publicación.");
        boolean activo=booleano(solicitud,"estatus",true), publico=booleano(solicitud,"publico",true);
        List<String> roles = listaTextos(solicitud.get("roles"));
        List<String> grupos = listaTextos(solicitud.get("grupos"));
        if(!Set.of("ESTUDIANTE","DOCENTE","EMPLEADO","EXTERNO","PERSONA").containsAll(grupos))throw fallo(HttpStatus.BAD_REQUEST,"Grupo de destinatarios no permitido.");
        if(publico&&(!roles.isEmpty()||!grupos.isEmpty())) throw fallo(HttpStatus.BAD_REQUEST,"Un aviso público no puede restringirse a grupos o roles.");
        for(String rol:roles) if(contar("select count(*) from roles where nombre=? and estatus",rol)==0) throw fallo(HttpStatus.BAD_REQUEST,"Rol destinatario no encontrado: "+rol);
        Map<String,Object> previo=id==null?Map.of():uno("select * from avisos_generales where id=? for update",id);
        if(id==null) id=jdbc.queryForObject("insert into avisos_generales(titulo,mensaje,fecha_publicacion,fecha_expiracion,estatus,publico,responsable_id) values(?,?,?,?,?,?,?) returning id",Long.class,titulo,mensaje,inicio,fin,activo,publico,identidad.cuentaId());
        else jdbc.update("update avisos_generales set titulo=?,mensaje=?,fecha_publicacion=?,fecha_expiracion=?,estatus=?,publico=?,fecha_modificacion=current_timestamp,responsable_modificacion_id=? where id=?",titulo,mensaje,inicio,fin,activo,publico,identidad.cuentaId(),id);
        jdbc.update("delete from destinatarios_aviso where aviso_id=?",id);
        for(String rol:roles) jdbc.update("insert into destinatarios_aviso(tipo_usuario,aviso_id,rol_id) select nombre,?,id from roles where nombre=? and estatus",id,rol);
        for(String grupo:grupos)jdbc.update("insert into destinatarios_aviso(tipo_usuario,aviso_id) values(?,?)",grupo,id);
        Map<String,Object> actual=uno("select * from avisos_generales where id=?",id); actual.put("roles",roles);actual.put("grupos",grupos);
        auditar(previo.isEmpty()?"CREAR":"MODIFICAR","M07","avisos_generales",id,"Publicación o modificación de aviso",previo,actual);
        return actual;
    }

    @Override
    public Map<String,Object> plantillas() {
        identidad.exigirAdministrador();
        return mapa("items",consultar("select p.*,t.codigo as tipo,c.nombre as canal from plantillas_notificacion p join tipos_notificacion t on t.id=p.tipo_notificacion_id join canales_notificacion c on c.id=p.canal_id order by p.nombre"),"tipos",consultar("select id,codigo,nombre,obligatoria,estatus from tipos_notificacion order by nombre"),"canales",consultar("select id,nombre,descripcion,estatus,max_reintentos,intervalo_reintento_minutos from canales_notificacion order by nombre"),"variables",VARIABLES_PLANTILLA.stream().sorted().toList());
    }

    @Override @Transactional
    public Map<String,Object> guardarPlantilla(Long id,Map<String,Object> solicitud) {
        identidad.exigirAdministrador();
        String nombre=texto(solicitud,"nombre",150,true),asunto=texto(solicitud,"asunto",250,true),mensaje=texto(solicitud,"mensaje",10000,true);
        validarVariables(asunto);validarVariables(mensaje);
        long tipo=numeroId(solicitud.get("tipo_notificacion_id")),canal=numeroId(solicitud.get("canal_id"));
        if(contar("select count(*) from tipos_notificacion where id=? and estatus",tipo)==0||contar("select count(*) from canales_notificacion where id=?",canal)==0)throw fallo(HttpStatus.BAD_REQUEST,"Tipo o canal no disponible.");
        boolean activo=booleano(solicitud,"estatus",true);
        Map<String,Object> anterior=id==null?Map.of():uno("select * from plantillas_notificacion where id=? for update",id);
        if(id==null)id=jdbc.queryForObject("insert into plantillas_notificacion(nombre,asunto,mensaje,variables,estatus,tipo_notificacion_id,canal_id) values(?,?,?,?,?,?,?) returning id",Long.class,nombre,asunto,mensaje,String.join(",",VARIABLES_PLANTILLA.stream().sorted().toList()),activo,tipo,canal);
        else jdbc.update("update plantillas_notificacion set nombre=?,asunto=?,mensaje=?,estatus=?,tipo_notificacion_id=?,canal_id=? where id=?",nombre,asunto,mensaje,activo,tipo,canal,id);
        Map<String,Object> actual=uno("select * from plantillas_notificacion where id=?",id);
        auditar(anterior.isEmpty()?"CREAR":"MODIFICAR","M07","plantillas_notificacion",id,"Gestión de plantilla",anterior,actual);
        return actual;
    }

    @Override
    public Map<String,Object> previsualizarPlantilla(Map<String,Object> solicitud) {
        identidad.exigirAdministrador();
        String asunto=texto(solicitud,"asunto",250,true),mensaje=texto(solicitud,"mensaje",10000,true);
        validarVariables(asunto);validarVariables(mensaje);
        Map<String,String> valores=Map.of("nombre","Usuario de vista previa","asunto","Confirmación de servicio","mensaje","Contenido del evento confirmado","fecha",LocalDate.now().toString(),"libro","Título del libro","vencimiento",LocalDate.now().plusDays(1).toString(),"cubiculo","Cubículo de ejemplo","monto","0.00");
        return mapa("asunto",reemplazar(asunto,valores),"mensaje",reemplazar(mensaje,valores),"vista_previa",true);
    }

    @Override @Transactional
    public Map<String,Object> guardarCanal(long id,Map<String,Object> solicitud) {
        identidad.exigirAdministrador();
        Map<String,Object> anterior=uno("select * from canales_notificacion where id=? for update",id);
        boolean activo=booleano(solicitud,"estatus",false);
        if(activo&&!"SISTEMA".equals(anterior.get("nombre")))throw fallo(HttpStatus.CONFLICT,"El canal externo requiere un proveedor institucional configurado; no se habilitó el envío.");
        int intentos=entero(solicitud,"max_reintentos",0,10),intervalo=entero(solicitud,"intervalo_reintento_minutos",0,1440);
        if("SISTEMA".equals(anterior.get("nombre"))&&!activo)throw fallo(HttpStatus.CONFLICT,"El canal interno obligatorio debe permanecer habilitado.");
        jdbc.update("update canales_notificacion set estatus=?,max_reintentos=?,intervalo_reintento_minutos=? where id=?",activo,intentos,intervalo,id);
        Map<String,Object> actual=uno("select * from canales_notificacion where id=?",id);
        auditar("MODIFICAR","M07","canales_notificacion",id,"Configuración de canal",anterior,actual);
        return actual;
    }

    private void validarVariables(String contenido) {
        var expresion=java.util.regex.Pattern.compile("\\{\\{([^{}]+)}}").matcher(contenido);
        while(expresion.find())if(!VARIABLES_PLANTILLA.contains(expresion.group(1).trim()))throw fallo(HttpStatus.BAD_REQUEST,"Variable no autorizada: "+expresion.group(1));
        if(contenido.replaceAll("\\{\\{[^{}]+}}","").contains("{{"))throw fallo(HttpStatus.BAD_REQUEST,"Variable de plantilla sin cierre.");
    }

    private String reemplazar(String texto,Map<String,String> valores) {
        var matcher=java.util.regex.Pattern.compile("\\{\\{\\s*([a-z_]+)\\s*}}").matcher(texto);
        StringBuilder salida=new StringBuilder();
        while(matcher.find())matcher.appendReplacement(salida,java.util.regex.Matcher.quoteReplacement(valores.getOrDefault(matcher.group(1),"")));
        matcher.appendTail(salida);return salida.toString();
    }

    @Override @Transactional
    public void notificar(Long personaId, String tipo, String evento, String asunto, String mensaje, String entidad, Long entidadId) {
        if(personaId==null||tipo==null||!tipo.matches("[A-Z_]{2,60}")||evento==null||evento.length()>150||asunto==null||asunto.length()>250||mensaje==null||mensaje.length()>10000) throw fallo(HttpStatus.BAD_REQUEST,"Los datos de notificación no son válidos.");
        if(entidad!=null&&!COLUMNAS_EVENTO.contains(entidad)) throw fallo(HttpStatus.BAD_REQUEST,"Origen de notificación no permitido.");
        List<Long> cuentas=jdbc.queryForList("select id from cuentas_usuario where persona_id=? and estatus for update",Long.class,personaId);
        if(cuentas.isEmpty()) return;
        long cuenta=cuentas.getFirst();
        long tipoId=jdbc.queryForObject("insert into tipos_notificacion(codigo,nombre,descripcion,estatus,obligatoria) values(?,?,?,true,true) on conflict(codigo) do update set codigo=excluded.codigo returning id",Long.class,tipo,tipo.replace('_',' '),"Eventos confirmados del servicio bibliotecario");
        if(contar("select count(*) from notificaciones where destinatario_id=? and tipo_notificacion_id=? and evento_origen=?",cuenta,tipoId,evento)>0) return;
        Map<String,Object> tipoDato=uno("select obligatoria,estatus from tipos_notificacion where id=?",tipoId);
        if(!Boolean.TRUE.equals(tipoDato.get("estatus"))) return;
        if(!Boolean.TRUE.equals(tipoDato.get("obligatoria"))&&contar("select count(*) from preferencias_notificacion where cuenta_id=? and not permite_sistema",cuenta)>0) return;
        List<Map<String,Object>> plantillas=consultar("select p.id,p.asunto,p.mensaje from plantillas_notificacion p join canales_notificacion c on c.id=p.canal_id where p.tipo_notificacion_id=? and p.estatus and c.estatus and c.nombre='SISTEMA' order by p.id desc limit 1",tipoId);
        Long plantilla=null;
        if(!plantillas.isEmpty()) {
            Map<String,Object> p=plantillas.getFirst();plantilla=((Number)p.get("id")).longValue();
            Map<String,String> valores=new HashMap<>();
            valores.put("nombre",jdbc.queryForObject("select nombre||' '||apellidos from personas where id=?",String.class,personaId));
            valores.put("asunto",asunto);valores.put("mensaje",mensaje);valores.put("fecha",LocalDate.now().toString());
            if("prestamos".equals(entidad)) {
                List<Map<String,Object>> datos=consultar("select string_agg(l.titulo,', ') as libro,min(d.fecha_vencimiento) as vencimiento from detalles_prestamo d join ejemplares e on e.id=d.ejemplar_id join libros l on l.id=e.libro_id where d.prestamo_id=?",entidadId);
                if(!datos.isEmpty())datos.getFirst().forEach((k,v)->valores.put(k,Objects.toString(v,"")));
            } else if("reservas_libro".equals(entidad)) {
                List<Map<String,Object>> datos=consultar("select l.titulo as libro,r.fecha_limite as vencimiento from reservas_libro r join libros l on l.id=r.libro_id where r.id=?",entidadId);
                if(!datos.isEmpty())datos.getFirst().forEach((k,v)->valores.put(k,Objects.toString(v,"")));
            } else if("reservas_cubiculo".equals(entidad)) {
                List<Map<String,Object>> datos=consultar("select c.nombre as cubiculo,r.fecha_inicio as fecha from reservas_cubiculo r join cubiculos c on c.id=r.cubiculo_id where r.id=?",entidadId);
                if(!datos.isEmpty())datos.getFirst().forEach((k,v)->valores.put(k,Objects.toString(v,"")));
            }
            asunto=reemplazar(String.valueOf(p.get("asunto")),valores);mensaje=reemplazar(String.valueOf(p.get("mensaje")),valores);
            if(asunto.length()>250||mensaje.length()>10000)throw fallo(HttpStatus.CONFLICT,"La plantilla genera un mensaje demasiado largo. El administrador debe corregirla.");
        }
        String columna=entidad==null?null:FK_EVENTO.get(entidad);
        String columnas=columna==null?"":","+columna;
        String valores=columna==null?"":",?";
        List<Object> args=new ArrayList<>(List.of(asunto,mensaje,evento,cuenta,tipoId));args.add(plantilla);
        if(columna!=null) args.add(entidadId);
        long notificacion=jdbc.queryForObject("insert into notificaciones(asunto,mensaje,fecha_creacion,estado,archivada,evento_origen,destinatario_id,tipo_notificacion_id,plantilla_id"+columnas+") values(?,?,current_timestamp,'NO_LEIDA',false,?,?,?,?"+valores+") returning id",Long.class,args.toArray());
        jdbc.execute("select pg_advisory_xact_lock(hashtext('itspa_canal_sistema'))");
        List<Long> canales=jdbc.queryForList("select id from canales_notificacion where nombre='SISTEMA' order by id limit 1",Long.class);
        long canal=canales.isEmpty()?jdbc.queryForObject("insert into canales_notificacion(nombre,descripcion,estatus,max_reintentos,intervalo_reintento_minutos) values('SISTEMA','Bandeja interna persistente',true,0,0) returning id",Long.class):canales.getFirst();
        jdbc.update("insert into envios_notificacion(fecha_envio,estado,numero_intento,notificacion_id,canal_id) values(current_timestamp,'DISPONIBLE_EN_SISTEMA',1,?,?)",notificacion,canal);
    }

    /** El recordatorio usa sólo préstamos aún abiertos; evento diario evita duplicados. */
    @Scheduled(fixedDelayString="${biblioteca.notificaciones.intervalo-ms:60000}")
    @Transactional
    public void recordarVencimientos() {
        List<Map<String,Object>> pendientes=consultar("select d.id,d.fecha_vencimiento,p.id as prestamo_id,p.persona_id,l.titulo from detalles_prestamo d join prestamos p on p.id=d.prestamo_id join ejemplares e on e.id=d.ejemplar_id join libros l on l.id=e.libro_id where p.estado='ACTIVO' and d.estado<>'DEVUELTO' and d.fecha_vencimiento<current_date");
        for(Map<String,Object> p:pendientes) notificar(((Number)p.get("persona_id")).longValue(),"PRESTAMO_VENCIDO","VENCIDO:"+p.get("id")+":"+LocalDate.now(),"Devolución pendiente", "El ejemplar de "+p.get("titulo")+" venció el "+p.get("fecha_vencimiento")+". Consulta tus préstamos o acude a biblioteca.","prestamos",((Number)p.get("prestamo_id")).longValue());
    }

    @Override @Transactional
    public Map<String, Object> reporte(String tipo, LocalDate desde, LocalDate hasta) {
        identidad.exigirGestion();
        String seleccionado=tipo==null?"catalogo":tipo.toLowerCase(Locale.ROOT);
        LocalDate inicio=desde==null?LocalDate.now().withDayOfMonth(1):desde;
        LocalDate fin=hasta==null?LocalDate.now():hasta;
        validarRango(inicio,fin);
        if(seleccionado.equals("auditoria")) identidad.exigirAdministrador();
        List<Object> args=new ArrayList<>();
        String sql=switch(seleccionado) {
            case "catalogo" -> "select l.id,l.titulo,l.isbn,l.edicion,l.anio_publicacion,l.estatus,count(e.id) as ejemplares,count(e.id) filter(where l.estatus and e.estatus and e.estado='DISPONIBLE' and e.estado_conservacion<>'DANADO') as disponibles,ed.nombre as editorial from libros l left join ejemplares e on e.libro_id=l.id left join editoriales ed on ed.id=l.editorial_id group by l.id,ed.nombre order by l.titulo";
            case "prestamos" -> "select p.id,p.fecha_prestamo,p.estado,pe.id as persona_id,pe.nombre||' '||pe.apellidos as persona,l.titulo,e.codigo_barras,d.fecha_vencimiento,d.estado as estado_ejemplar from prestamos p join personas pe on pe.id=p.persona_id join detalles_prestamo d on d.prestamo_id=p.id join ejemplares e on e.id=d.ejemplar_id join libros l on l.id=e.libro_id where true"+filtroFechas("p.fecha_prestamo",inicio,fin,args)+" order by p.fecha_prestamo desc,p.id,d.id";
            case "devoluciones" -> "select v.id,v.fecha_devolucion,l.titulo,e.codigo_barras,v.estado_conservacion,v.observaciones,p.persona_id from devoluciones v join detalles_prestamo d on d.id=v.detalle_prestamo_id join prestamos p on p.id=d.prestamo_id join ejemplares e on e.id=d.ejemplar_id join libros l on l.id=e.libro_id where true"+filtroFechas("v.fecha_devolucion",inicio,fin,args)+" order by v.fecha_devolucion desc";
            case "reservas" -> "select r.id,r.fecha_reserva,r.fecha_limite,r.estado,r.posicion_espera,r.persona_id,pe.nombre||' '||pe.apellidos as persona,l.titulo from reservas_libro r join personas pe on pe.id=r.persona_id join libros l on l.id=r.libro_id where true"+filtroFechas("r.fecha_reserva",inicio,fin,args)+" order by r.fecha_reserva desc";
            case "sanciones" -> "select s.id,s.fecha,s.estado,s.descripcion,s.motivo,s.fecha_fin,t.nombre as tipo,i.persona_id,pe.nombre||' '||pe.apellidos as persona,m.monto,m.saldo_pendiente from sanciones s join tipos_sancion t on t.id=s.tipo_sancion_id join incidencias i on i.id=s.incidencia_id join personas pe on pe.id=i.persona_id left join multas m on m.sancion_id=s.id where true"+filtroFechas("s.fecha",inicio,fin,args)+" order by s.fecha desc";
            case "visitas" -> "select r.id,r.fecha_entrada,r.fecha_salida,r.persona_id,p.nombre||' '||p.apellidos as persona,m.nombre as motivo,r.observaciones from registros_visita r join personas p on p.id=r.persona_id join motivos_visita m on m.id=r.motivo_id where true"+filtroFechas("r.fecha_entrada",inicio,fin,args)+" order by r.fecha_entrada desc";
            case "cubiculos" -> "select r.id,r.fecha_reserva,r.fecha_inicio,r.fecha_fin,r.estado,r.persona_id,p.nombre||' '||p.apellidos as persona,c.codigo,c.nombre as cubiculo,c.capacidad from reservas_cubiculo r join personas p on p.id=r.persona_id join cubiculos c on c.id=r.cubiculo_id where true"+filtroFechas("r.fecha_inicio",inicio,fin,args)+" order by r.fecha_inicio desc";
            case "usuarios" -> "select c.id,c.usuario,c.fecha_creacion,c.ultimo_acceso,c.estatus,p.nombre,p.apellidos,coalesce(string_agg(r.nombre,', ' order by r.nombre),'') as roles from cuentas_usuario c join personas p on p.id=c.persona_id left join cuentas_roles cr on cr.cuenta_id=c.id left join roles r on r.id=cr.rol_id where coalesce(p.tipo_identificacion,'')<>'IDENTIDAD_TECNICA'"+filtroFechas("c.fecha_creacion",inicio,fin,args)+" group by c.id,p.nombre,p.apellidos order by c.fecha_creacion desc";
            case "auditoria" -> "select b.id,b.fecha,b.accion,b.modulo,b.entidad,b.id_registro,b.resultado,b.motivo,c.usuario as responsable from bitacoras_auditoria b left join cuentas_usuario c on c.id=b.responsable_id where true"+filtroFechas("b.fecha",inicio,fin,args)+" order by b.fecha desc";
            default -> throw fallo(HttpStatus.BAD_REQUEST,"Tipo de reporte no permitido.");
        };
        List<Map<String,Object>> filas=consultar(sql+" limit 10001",args.toArray());
        if(filas.size()>10000) throw fallo(HttpStatus.UNPROCESSABLE_ENTITY,"El reporte supera 10 000 filas; acota el periodo.");
        long tipoId;
        jdbc.execute("select pg_advisory_xact_lock(hashtext('itspa_tipo_reporte'))");
        List<Long> tipos=jdbc.queryForList("select id from tipos_reporte where nombre=? order by id limit 1",Long.class,seleccionado);
        tipoId=tipos.isEmpty()?jdbc.queryForObject("insert into tipos_reporte(nombre,descripcion,estatus) values(?,?,true) returning id",Long.class,seleccionado,"Consulta verificable de "+seleccionado):tipos.getFirst();
        long id=jdbc.queryForObject("insert into reportes(nombre,estatus,fecha_inicio,fecha_fin,fecha_generacion,total_registros,tipo_reporte_id,responsable_id,parametros) values(?,true,?,?,current_timestamp,?,?,?,?) returning id",Long.class,"Reporte de "+seleccionado,inicio,fin,filas.size(),tipoId,identidad.cuentaId(),"tipo="+seleccionado);
        jdbc.update("insert into historiales_reporte(fecha,estado,fecha_inicio,fecha_fin,reporte_id,tipo_reporte_id,responsable_id) values(current_timestamp,'GENERADO',?,?,?,?,?)",inicio,fin,id,tipoId,identidad.cuentaId());
        auditar("GENERAR","M08","reportes",id,"Reporte "+seleccionado+": "+filas.size()+" filas",Map.of(),Map.of());
        List<String> columnas=filas.isEmpty()?columnasReporteVacio(seleccionado):new ArrayList<>(filas.getFirst().keySet());
        return mapa("id",id,"tipo",seleccionado,"desde",inicio.toString(),"hasta",fin.toString(),"generado_en",LocalDateTime.now().toString(),"total_registros",filas.size(),"columnas",columnas,"filas",filas);
    }

    @Override @Transactional
    @SuppressWarnings("unchecked")
    public Descarga exportar(String tipo, LocalDate desde, LocalDate hasta, String formato) {
        String extension=formato==null?"csv":formato.toLowerCase(Locale.ROOT);
        if(!Set.of("csv","xlsx","pdf","json").contains(extension)) throw fallo(HttpStatus.BAD_REQUEST,"Formato permitido: csv, xlsx, pdf o json.");
        Map<String,Object> resultado=reporte(tipo,desde,hasta);
        List<String> columnas=(List<String>)resultado.get("columnas");
        List<Map<String,Object>> filas=(List<Map<String,Object>>)resultado.get("filas");
        byte[] contenido=switch(extension) {
            case "csv" -> ReportesExportador.csv(columnas,filas);
            case "xlsx" -> ReportesExportador.xlsx(columnas,filas);
            case "pdf" -> ReportesExportador.pdf("BiblioTECa ITSPA · Reporte de "+resultado.get("tipo"),columnas,filas);
            default -> serializar(resultado);
        };
        String nombre="itspa-"+resultado.get("tipo")+"-"+resultado.get("desde")+"-"+resultado.get("hasta")+"."+extension;
        jdbc.update("insert into exportaciones_reporte(fecha,formato,archivo,estado,reporte_id,responsable_id) values(current_timestamp,?,?,'DESCARGA_GENERADA',?,?)",extension.toUpperCase(Locale.ROOT),nombre,resultado.get("id"),identidad.cuentaId());
        String mime=switch(extension){case "csv"->"text/csv;charset=UTF-8";case "xlsx"->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";case "pdf"->"application/pdf";default->"application/json";};
        return new Descarga(nombre,mime,contenido);
    }

    @Override
    public Map<String, Object> configuracion() {
        identidad.exigirAdministrador();
        List<Map<String,Object>> seg=consultar("select id,nombre,descripcion,inactividad_maxima_minutos,intentos_fallidos_maximos,bloqueo_temporal_minutos from politicas_seguridad where estatus order by id desc limit 1");
        return mapa("institucion",institucionCompleta(),"parametros",consultar("select id,clave,valor,tipo_dato,descripcion,obligatorio,esencial,estatus,fecha_modificacion from parametros_sistema order by clave"),"seguridad",seg.isEmpty()?Map.of():seg.getFirst(),"estado_servicios",mapa("base_datos",jdbc.queryForObject("select case when 1=1 then 'CONECTADA' end",String.class),"version_aplicacion","0.1.0-SNAPSHOT"));
    }

    @Override @Transactional
    public Map<String, Object> guardarConfiguracion(Map<String, Object> solicitud) {
        identidad.exigirAdministrador();
        Map<String,Object> datos=objeto(solicitud.get("institucion"));
        String institucion=texto(datos,"nombre_institucion",200,true), biblioteca=texto(datos,"nombre_biblioteca",200,true), zona=texto(datos,"zona_horaria",60,true);
        try { ZoneId.of(zona); } catch(Exception ex) { throw fallo(HttpStatus.BAD_REQUEST,"Zona horaria no válida."); }
        String formatoFecha=texto(datos,"formato_fecha",80,false),formatoHora=texto(datos,"formato_hora",80,false);
        try { if(formatoFecha!=null)DateTimeFormatter.ofPattern(formatoFecha);if(formatoHora!=null)DateTimeFormatter.ofPattern(formatoHora); } catch(IllegalArgumentException ex) { throw fallo(HttpStatus.BAD_REQUEST,"Formato de fecha u hora no válido."); }
        String correo=texto(datos,"correo_contacto",254,false);
        if(correo!=null&&!correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw fallo(HttpStatus.BAD_REQUEST,"Correo de contacto no válido.");
        List<Map<String,Object>> existentes=consultar("select * from configuraciones_sistema where estatus order by id desc limit 1 for update");
        Map<String,Object> anterior=existentes.isEmpty()?Map.of():existentes.getFirst();
        Long id=existentes.isEmpty()?null:((Number)anterior.get("id")).longValue();
        String nombre=Optional.ofNullable(texto(datos,"nombre",150,false)).orElse("Configuración institucional");
        String telefono=texto(datos,"telefono_contacto",30,false),direccion=texto(datos,"direccion_contacto",500,false),descripcion=texto(datos,"descripcion",2000,false);
        if(id==null) id=jdbc.queryForObject("insert into configuraciones_sistema(nombre,nombre_institucion,nombre_biblioteca,correo_contacto,telefono_contacto,direccion_contacto,zona_horaria,formato_fecha,formato_hora,descripcion,version_aplicacion,fecha_modificacion,estatus,responsable_modificacion_id) values(?,?,?,?,?,?,?,?,?,?,'0.1.0-SNAPSHOT',current_timestamp,true,?) returning id",Long.class,nombre,institucion,biblioteca,correo,telefono,direccion,zona,formatoFecha,formatoHora,descripcion,identidad.cuentaId());
        else jdbc.update("update configuraciones_sistema set nombre=?,nombre_institucion=?,nombre_biblioteca=?,correo_contacto=?,telefono_contacto=?,direccion_contacto=?,zona_horaria=?,formato_fecha=?,formato_hora=?,descripcion=?,fecha_modificacion=current_timestamp,responsable_modificacion_id=? where id=?",nombre,institucion,biblioteca,correo,telefono,direccion,zona,formatoFecha,formatoHora,descripcion,identidad.cuentaId(),id);
        auditar("MODIFICAR","M09","configuraciones_sistema",id,texto(solicitud,"motivo",1000,false),anterior,uno("select * from configuraciones_sistema where id=?",id));
        if(solicitud.get("parametros") instanceof List<?> parametros) for(Object p:parametros) guardarParametro(id,objeto(p));
        if(solicitud.get("seguridad")!=null) guardarSeguridad(objeto(solicitud.get("seguridad")));
        return configuracion();
    }

    private void guardarParametro(long configuracion, Map<String,Object> p) {
        String clave=texto(p,"clave",100,true),valor=texto(p,"valor",2000,true),tipo=texto(p,"tipo_dato",60,true).toUpperCase(Locale.ROOT);
        if(!clave.matches("[a-zA-Z][a-zA-Z0-9_.-]{1,99}")||esSecreto(clave)) throw fallo(HttpStatus.BAD_REQUEST,"Clave de parámetro no válida o reservada para secretos.");
        try { switch(tipo){case "ENTERO"->Long.parseLong(valor);case "DECIMAL"->new BigDecimal(valor);case "BOOLEANO"->{if(!Set.of("true","false").contains(valor)) throw new IllegalArgumentException();}case "TEXTO"->{ }default->throw new IllegalArgumentException();} }
        catch(IllegalArgumentException ex){throw fallo(HttpStatus.BAD_REQUEST,"El valor de "+clave+" no corresponde a "+tipo+".");}
        List<Map<String,Object>> previos=consultar("select * from parametros_sistema where clave=? for update",clave);
        boolean obligatorio=booleano(p,"obligatorio",false),esencial=booleano(p,"esencial",false);
        if(!previos.isEmpty()&&Boolean.TRUE.equals(previos.getFirst().get("esencial"))) esencial=true;
        long id=jdbc.queryForObject("insert into parametros_sistema(clave,valor,tipo_dato,descripcion,obligatorio,esencial,estatus,fecha_modificacion,configuracion_id,responsable_modificacion_id) values(?,?,?,?,?,?,true,current_timestamp,?,?) on conflict(clave) do update set valor=excluded.valor,tipo_dato=excluded.tipo_dato,descripcion=excluded.descripcion,obligatorio=excluded.obligatorio,esencial=excluded.esencial,fecha_modificacion=current_timestamp,responsable_modificacion_id=excluded.responsable_modificacion_id returning id",Long.class,clave,valor,tipo,texto(p,"descripcion",500,false),obligatorio,esencial,configuracion,identidad.cuentaId());
        auditar("MODIFICAR","M09","parametros_sistema",id,"Actualización de parámetro "+clave,previos.isEmpty()?Map.of():previos.getFirst(),uno("select * from parametros_sistema where id=?",id));
    }

    private void guardarSeguridad(Map<String,Object> p) {
        int minutos=entero(p,"inactividad_maxima_minutos",1,1440),intentos=entero(p,"intentos_fallidos_maximos",1,100),bloqueo=entero(p,"bloqueo_temporal_minutos",1,1440);
        List<Map<String,Object>> previos=consultar("select * from politicas_seguridad where estatus order by id desc limit 1 for update");
        long id;
        if(previos.isEmpty()) id=jdbc.queryForObject("insert into politicas_seguridad(nombre,descripcion,estatus,inactividad_maxima_minutos,intentos_fallidos_maximos,bloqueo_temporal_minutos,fecha_modificacion,responsable_modificacion_id) values('Configuración de acceso','Política de autenticación y sesiones',true,?,?,?,current_timestamp,?) returning id",Long.class,minutos,intentos,bloqueo,identidad.cuentaId());
        else { id=((Number)previos.getFirst().get("id")).longValue();jdbc.update("update politicas_seguridad set inactividad_maxima_minutos=?,intentos_fallidos_maximos=?,bloqueo_temporal_minutos=?,fecha_modificacion=current_timestamp,responsable_modificacion_id=? where id=?",minutos,intentos,bloqueo,identidad.cuentaId(),id); }
        auditar("MODIFICAR","M09","politicas_seguridad",id,"Cambios de protección de acceso",previos.isEmpty()?Map.of():previos.getFirst(),uno("select * from politicas_seguridad where id=?",id));
    }

    @Override
    public List<Map<String, Object>> auditoria(String modulo,String accion,String usuario,LocalDate desde,LocalDate hasta) {
        identidad.exigirAdministrador();validarRango(desde,hasta);
        String sql="select b.*,c.usuario as responsable from bitacoras_auditoria b left join cuentas_usuario c on c.id=b.responsable_id where true";
        List<Object> args=new ArrayList<>();
        if(modulo!=null&&!modulo.isBlank()){sql+=" and b.modulo=?";args.add(modulo);}
        if(accion!=null&&!accion.isBlank()){sql+=" and b.accion=?";args.add(accion);}
        if(usuario!=null&&!usuario.isBlank()){sql+=" and c.usuario=?";args.add(usuario);}
        sql+=filtroFechas("b.fecha",desde,hasta,args);
        List<Map<String,Object>> eventos=consultar(sql+" order by b.fecha desc,b.id desc limit 500",args.toArray());
        for(Map<String,Object> evento:eventos) evento.put("cambios",consultar("select campo,valor_anterior,valor_nuevo from detalles_cambio where evento_id=? order by id",evento.get("id")));
        return eventos;
    }

    @Override
    public List<Map<String, Object>> respaldos() {
        identidad.exigirAdministrador();
        List<Map<String,Object>> lista=consultar("select r.id,r.fecha,r.tipo,r.estado,r.resultado,r.verificacion_integridad,c.usuario as responsable from respaldos_sistema r left join cuentas_usuario c on c.id=r.responsable_id order by r.fecha desc limit 100");
        for(Map<String,Object> r:lista) if("COMPLETADO".equals(r.get("estado"))) r.put("descarga","/api/app/respaldos/"+r.get("id")+"/descargar");
        return lista;
    }

    @Override @Transactional(isolation=Isolation.REPEATABLE_READ,noRollbackFor=ResponseStatusException.class)
    public Map<String, Object> crearRespaldo() {
        identidad.exigirAdministrador();
        long id=jdbc.queryForObject("insert into respaldos_sistema(fecha,tipo,estado,responsable_id) values(current_timestamp,'EXPORTACION_LOGICA_SEGURA','EN_PROCESO',?) returning id",Long.class,identidad.cuentaId());
        try {
            Map<String,Object> tablas=new LinkedHashMap<>();
            List<String> nombres=jdbc.queryForList("select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE' and table_name<>'flyway_schema_history' order by table_name",String.class);
            for(String nombre:nombres) {
                if(!nombre.matches("[a-z_]+"))throw new IOException("Identificador no previsto");
                List<String> columnas=jdbc.queryForList("select column_name from information_schema.columns where table_schema='public' and table_name=? order by ordinal_position",String.class,nombre).stream().filter(c->!esSecreto(c)).toList();
                if(columnas.isEmpty())continue;
                String seleccion=String.join(",",columnas.stream().map(c->"\""+c+"\"").toList());
                List<Map<String,Object>> filas=consultar("select "+seleccion+" from \""+nombre+"\"");
                tablas.put(nombre,filas);
            }
            Map<String,Object> respaldo=mapa("formato","ITSPA_EXPORTACION_SEGURA_V1","generado_en",LocalDateTime.now().toString(),"advertencia","Instantánea lógica sin credenciales ni hashes. No restituye contraseñas ni sesiones autenticadas.","tablas",tablas);
            byte[] bytes=serializar(respaldo);
            Files.createDirectories(directorioRespaldos);
            String nombre="itspa-respaldo-"+id+"-"+UUID.randomUUID()+".json";
            Path archivo=directorioRespaldos.resolve(nombre).normalize();
            if(!archivo.startsWith(directorioRespaldos))throw new IOException("Ruta no válida");
            Files.write(archivo,bytes,StandardOpenOption.CREATE_NEW);
            String hash=sha256(bytes);
            if(!hash.equals(sha256(Files.readAllBytes(archivo))))throw new IOException("Integridad no verificada");
            long evento=auditar("RESPALDAR","M09","respaldos_sistema",id,"Instantánea lógica segura de "+tablas.size()+" tablas",Map.of(),Map.of());
            jdbc.update("update respaldos_sistema set archivo=?,estado='COMPLETADO',resultado=?,verificacion_integridad=?,evento_auditoria_id=? where id=?",nombre,tablas.size()+" tablas exportadas; "+bytes.length+" bytes. Sin contraseñas, hashes ni tokens.",hash,evento,id);
            return mapa("id",id,"tipo","EXPORTACION_LOGICA_SEGURA","estado","COMPLETADO","resultado",tablas.size()+" tablas exportadas","verificacion_integridad",hash,"descarga","/api/app/respaldos/"+id+"/descargar");
        } catch(IOException ex) {
            jdbc.update("update respaldos_sistema set estado='FALLIDO',resultado='No fue posible escribir o verificar la instantánea segura' where id=?",id);
            throw fallo(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo crear o verificar el archivo de respaldo. Consulta el historial.");
        }
    }

    @Override
    public Descarga descargarRespaldo(long id) {
        identidad.exigirAdministrador();
        Map<String,Object> registro=uno("select archivo,estado,verificacion_integridad from respaldos_sistema where id=?",id);
        if(!"COMPLETADO".equals(registro.get("estado")))throw fallo(HttpStatus.CONFLICT,"El respaldo no está disponible.");
        String nombre=String.valueOf(registro.get("archivo"));
        if(!nombre.matches("itspa-respaldo-[0-9]+-[a-f0-9-]+\\.json"))throw fallo(HttpStatus.CONFLICT,"Archivo de respaldo no válido.");
        Path ruta=directorioRespaldos.resolve(nombre).normalize();
        try {
            if(!ruta.startsWith(directorioRespaldos)||Files.isSymbolicLink(ruta)||!ruta.toRealPath().startsWith(directorioRespaldos.toRealPath()))throw new IOException();
            byte[] bytes=Files.readAllBytes(ruta);
            if(!sha256(bytes).equals(registro.get("verificacion_integridad")))throw fallo(HttpStatus.CONFLICT,"Falló la verificación de integridad del respaldo.");
            return new Descarga(nombre,"application/json",bytes);
        }catch(IOException ex){throw fallo(HttpStatus.NOT_FOUND,"El archivo de respaldo no está disponible.");}
    }

    @Override
    public List<Map<String, Object>> incidentes() {
        identidad.exigirAdministrador();
        return consultar("select i.*,c.usuario as responsable from incidentes_seguridad i left join cuentas_usuario c on c.id=i.responsable_id order by i.fecha desc limit 200");
    }

    @Override @Transactional
    public Map<String, Object> guardarIncidente(Long id, Map<String,Object> solicitud) {
        identidad.exigirAdministrador();
        String descripcion=texto(solicitud,"descripcion",2000,true),gravedad=texto(solicitud,"gravedad",60,true),estado=texto(solicitud,"estado",80,true);
        if(!Set.of("BAJA","MEDIA","ALTA","CRITICA").contains(gravedad)||!Set.of("ABIERTO","EN_REVISION","RESUELTO").contains(estado))throw fallo(HttpStatus.BAD_REQUEST,"Gravedad o estado de incidente no válidos.");
        String observaciones=texto(solicitud,"observaciones",2000,false),acciones=texto(solicitud,"acciones_correctivas",2000,false);
        Long cuenta=solicitud.get("cuenta_afectada_id")==null?null:numeroId(solicitud.get("cuenta_afectada_id"));
        if(cuenta!=null&&contar("select count(*) from cuentas_usuario where id=?",cuenta)==0)throw fallo(HttpStatus.BAD_REQUEST,"La cuenta afectada no existe.");
        Map<String,Object> anterior=id==null?Map.of():uno("select * from incidentes_seguridad where id=? for update",id);
        if(id==null)id=jdbc.queryForObject("insert into incidentes_seguridad(fecha,descripcion,estado,gravedad,observaciones,acciones_correctivas,fecha_ultima_actualizacion,cuenta_afectada_id,responsable_id) values(current_timestamp,?,?,?,?,?,current_timestamp,?,?) returning id",Long.class,descripcion,estado,gravedad,observaciones,acciones,cuenta,identidad.cuentaId());
        else jdbc.update("update incidentes_seguridad set descripcion=?,estado=?,gravedad=?,observaciones=?,acciones_correctivas=?,fecha_ultima_actualizacion=current_timestamp,responsable_id=?,cuenta_afectada_id=? where id=?",descripcion,estado,gravedad,observaciones,acciones,identidad.cuentaId(),cuenta,id);
        Map<String,Object> actual=uno("select * from incidentes_seguridad where id=?",id);
        long evento=auditar(anterior.isEmpty()?"CREAR":"MODIFICAR","M09","incidentes_seguridad",id,"Gestión de incidente de seguridad",anterior,actual);
        jdbc.update("insert into incidentes_auditoria(incidente_id,evento_id) values(?,?)",id,evento);
        return actual;
    }

    private Map<String,Object> institucionPublica() {
        Map<String,Object> completa=institucionCompleta();
        return mapa("nombre_institucion",completa.get("nombre_institucion"),"nombre_biblioteca",completa.get("nombre_biblioteca"),"correo_contacto",completa.get("correo_contacto"),"telefono_contacto",completa.get("telefono_contacto"),"zona_horaria",completa.get("zona_horaria"));
    }

    private Map<String,Object> institucionCompleta() {
        List<Map<String,Object>> configuraciones=consultar("select * from configuraciones_sistema where estatus order by id desc limit 1");
        return configuraciones.isEmpty()?mapa("nombre","Configuración institucional","nombre_institucion","Instituto Tecnológico Superior de Pátzcuaro","nombre_biblioteca","BiblioTECa ITSPA","zona_horaria","America/Mexico_City","formato_fecha","dd/MM/yyyy","formato_hora","HH:mm"):configuraciones.getFirst();
    }

    private long auditar(String accion,String modulo,String entidad,Long id,String motivo,Map<String,Object> anterior,Map<String,Object> actual) {
        long evento=jdbc.queryForObject("insert into bitacoras_auditoria(fecha,accion,modulo,entidad,id_registro,resultado,motivo,operacion_sistema,responsable_id) values(current_timestamp,?,?,?,?,'EXITOSO',?,false,?) returning id",Long.class,accion,modulo,entidad,id,motivo,identidad.cuentaId());
        for(String campo:actual.keySet()) if(!esSecreto(campo)&&!Objects.equals(anterior.get(campo),actual.get(campo)))jdbc.update("insert into detalles_cambio(campo,valor_anterior,valor_nuevo,evento_id) values(?,?,?,?)",campo,limitar(anterior.get(campo)),limitar(actual.get(campo)),evento);
        return evento;
    }

    private String limitar(Object v){if(v==null)return null;String s=String.valueOf(v);return s.length()>4000?s.substring(0,4000):s;}
    private static boolean esSecreto(String campo){String c=campo.toLowerCase(Locale.ROOT);return c.contains("contrasena")||c.contains("password")||c.contains("token")||c.contains("secret")||c.contains("credencial")||c.endsWith("_hash");}
    private void agregarAlerta(List<Map<String,Object>> lista,String tipo,String titulo,String descripcion,Object cantidad){if(cantidad instanceof Number n&&n.longValue()>0)lista.add(mapa("tipo",tipo,"titulo",titulo,"descripcion",descripcion,"cantidad",cantidad));}
    private List<String> columnasReporteVacio(String tipo){return switch(tipo){case "catalogo"->List.of("id","titulo","isbn","edicion","anio_publicacion","estatus","ejemplares","disponibles","editorial");case "prestamos"->List.of("id","fecha_prestamo","estado","persona_id","persona","titulo","codigo_barras","fecha_vencimiento","estado_ejemplar");case "devoluciones"->List.of("id","fecha_devolucion","titulo","codigo_barras","estado_conservacion","observaciones","persona_id");case "reservas"->List.of("id","fecha_reserva","fecha_limite","estado","posicion_espera","persona_id","persona","titulo");case "sanciones"->List.of("id","fecha","estado","descripcion","motivo","fecha_fin","tipo","persona_id","persona","monto","saldo_pendiente");case "visitas"->List.of("id","fecha_entrada","fecha_salida","persona_id","persona","motivo","observaciones");case "cubiculos"->List.of("id","fecha_reserva","fecha_inicio","fecha_fin","estado","persona_id","persona","codigo","cubiculo","capacidad");case "usuarios"->List.of("id","usuario","fecha_creacion","ultimo_acceso","estatus","nombre","apellidos","roles");default->List.of("id","fecha","accion","modulo","entidad","id_registro","resultado","motivo","responsable");};}
    private long contar(String sql,Object...args){Long n=jdbc.queryForObject(sql,Long.class,args);return n==null?0:n;}
    private Map<String,Object> uno(String sql,Object...args){List<Map<String,Object>> resultados=consultar(sql,args);if(resultados.isEmpty())throw fallo(HttpStatus.NOT_FOUND,"Registro no encontrado.");return resultados.getFirst();}
    private List<Map<String,Object>> consultar(String sql,Object...args){List<Map<String,Object>> filas=jdbc.queryForList(sql,args);for(Map<String,Object> fila:filas)fila.replaceAll((k,v)->v instanceof Timestamp t?t.toLocalDateTime().toString():v instanceof java.sql.Date d?d.toLocalDate().toString():v instanceof java.sql.Time t?t.toLocalTime().toString():v);return filas;}
    private static Map<String,Object> mapa(Object...pares){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pares.length;i+=2)m.put(String.valueOf(pares[i]),pares[i+1]);return m;}
    private void encontrado(int n){if(n==0)throw fallo(HttpStatus.NOT_FOUND,"Registro no encontrado.");}
    private ResponseStatusException fallo(HttpStatus estado,String detalle){return new ResponseStatusException(estado,detalle);}
    private String texto(Map<String,Object> solicitud,String campo,int max,boolean requerido){Object valor=solicitud.get(campo);if(valor!=null&&!(valor instanceof String))throw fallo(HttpStatus.BAD_REQUEST,campo+" debe ser texto.");String s=valor==null?null:((String)valor).trim();if(s!=null&&s.isEmpty())s=null;if((requerido&&s==null)||(s!=null&&s.length()>max))throw fallo(HttpStatus.BAD_REQUEST,"Revisa el campo "+campo+" (máximo "+max+" caracteres).");return s;}
    private boolean booleano(Map<String,Object> m,String campo,boolean predeterminado){if(!m.containsKey(campo))return predeterminado;Object v=m.get(campo);if(!(v instanceof Boolean))throw fallo(HttpStatus.BAD_REQUEST,campo+" debe ser booleano.");return (Boolean)v;}
    private int entero(Map<String,Object> m,String campo,int min,int max){try{BigDecimal n=new BigDecimal(String.valueOf(m.get(campo)));int v=n.intValueExact();if(v<min||v>max)throw new ArithmeticException();return v;}catch(Exception ex){throw fallo(HttpStatus.BAD_REQUEST,campo+" debe ser un entero entre "+min+" y "+max+".");}}
    private long numeroId(Object o){try{long n=new BigDecimal(String.valueOf(o)).longValueExact();if(n<=0)throw new ArithmeticException();return n;}catch(Exception ex){throw fallo(HttpStatus.BAD_REQUEST,"Identificador no válido.");}}
    private LocalDateTime fechaHora(Map<String,Object> m,String campo,LocalDateTime predeterminado){String v=texto(m,campo,80,false);try{return v==null?predeterminado:LocalDateTime.parse(v);}catch(Exception ex){throw fallo(HttpStatus.BAD_REQUEST,campo+" requiere fecha y hora ISO local.");}}
    @SuppressWarnings("unchecked") private Map<String,Object> objeto(Object o){if(!(o instanceof Map<?,?>))throw fallo(HttpStatus.BAD_REQUEST,"Se requiere un objeto JSON.");return (Map<String,Object>)o;}
    private List<String> listaTextos(Object o){if(o==null)return List.of();if(!(o instanceof List<?> l)||l.size()>30)throw fallo(HttpStatus.BAD_REQUEST,"Lista de roles no válida.");List<String> r=new ArrayList<>();for(Object v:(List<?>)o){if(!(v instanceof String s)||s.length()>80)throw fallo(HttpStatus.BAD_REQUEST,"Rol no válido.");if(!r.contains(s))r.add(s);}return r;}
    private void validarRango(LocalDate desde,LocalDate hasta){if(desde!=null&&hasta!=null&&hasta.isBefore(desde))throw fallo(HttpStatus.BAD_REQUEST,"La fecha final no puede ser anterior a la inicial.");}
    private String filtroFechas(String columna,LocalDate desde,LocalDate hasta,List<Object> args){String s="";if(desde!=null){s+=" and "+columna+">=?";args.add(desde.atStartOfDay());}if(hasta!=null){s+=" and "+columna+"<?";args.add(hasta.plusDays(1).atStartOfDay());}return s;}
    private byte[] serializar(Object o){try{return json.writerWithDefaultPrettyPrinter().writeValueAsBytes(o);}catch(JsonProcessingException ex){throw fallo(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo generar el documento JSON.");}}
    private String sha256(byte[] b){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 no disponible",ex);}}
}
