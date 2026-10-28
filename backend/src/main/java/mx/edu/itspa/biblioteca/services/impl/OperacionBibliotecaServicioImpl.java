package mx.edu.itspa.biblioteca.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import mx.edu.itspa.biblioteca.repositorio.OperacionBibliotecaRepositorio;
import mx.edu.itspa.biblioteca.services.OperacionBibliotecaServicio;
import mx.edu.itspa.biblioteca.services.ApoyoSistemaServicio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas M02–M06 del PDF, pp. impresas 15–51 (físicas 16–52).
 * El pedido del 20/10 amplía el piloto a operaciones reales. No cambia el modelo.
 * Inventario, préstamo, reserva, devolución y pago comparten una transacción.
 */
@Service
@Transactional(readOnly=true)
public class OperacionBibliotecaServicioImpl implements OperacionBibliotecaServicio {
    private final OperacionBibliotecaRepositorio db;
    private final IdentidadActual identidad;
    private final ApoyoSistemaServicio apoyo;
    private final ObjectMapper json;
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final String ACTIVAS_LIBRO = "('PENDIENTE','EN_ESPERA','LISTA_PARA_RECOGER')";
    private static final Set<String> TIPOS = Set.of("ESTUDIANTE","DOCENTE","EMPLEADO","EXTERNO");
    private static final Set<String> CONSERVACION = Set.of("EXCELENTE","BUENO","REGULAR","DANADO");
    private static final String SELECT_LIBRO = """
        SELECT l.*,ed.nombre editorial,i.nombre idioma,
          COALESCE((SELECT string_agg(trim(a.nombre||' '||COALESCE(a.apellidos,'')),', ' ORDER BY a.nombre)
            FROM libros_autores la JOIN autores a ON a.id=la.autor_id WHERE la.libro_id=l.id),'') autores,
          COALESCE((SELECT string_agg(c.nombre,', ' ORDER BY c.nombre) FROM libros_categorias lc
            JOIN categorias c ON c.id=lc.categoria_id WHERE lc.libro_id=l.id),'') categorias,
          (SELECT count(*) FROM ejemplares e WHERE e.libro_id=l.id AND e.estatus) total_ejemplares,
          (SELECT count(*) FROM ejemplares e WHERE e.libro_id=l.id AND e.estatus AND e.estado='DISPONIBLE' AND e.estado_conservacion<>'DANADO') disponibles
        FROM libros l LEFT JOIN editoriales ed ON ed.id=l.editorial_id LEFT JOIN idiomas i ON i.id=l.idioma_id
        """;
    public OperacionBibliotecaServicioImpl(OperacionBibliotecaRepositorio db,IdentidadActual identidad,
        ApoyoSistemaServicio apoyo,ObjectMapper json) { this.db=db; this.identidad=identidad; this.apoyo=apoyo; this.json=json; }

    @Override public List<Map<String,Object>> catalogo(String q,String categoria,String disponibilidad) {
        StringBuilder sql=new StringBuilder(SELECT_LIBRO + " WHERE 1=1"); List<Object> args=new ArrayList<>();
        if(!identidad.esGestion()) sql.append(" AND l.estatus");
        if(q!=null && !q.isBlank()) {
            sql.append(" AND (l.titulo ILIKE ? OR COALESCE(l.isbn,'') ILIKE ? OR COALESCE(ed.nombre,'') ILIKE ? OR EXISTS (SELECT 1 FROM libros_autores la JOIN autores a ON a.id=la.autor_id WHERE la.libro_id=l.id AND concat(a.nombre,' ',a.apellidos) ILIKE ?) OR EXISTS (SELECT 1 FROM libros_categorias lc JOIN categorias c ON c.id=lc.categoria_id WHERE lc.libro_id=l.id AND c.nombre ILIKE ?))");
            for(int n=0;n<5;n++) args.add("%"+q.strip()+"%");
        }
        if(categoria!=null && !categoria.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM libros_categorias lc JOIN categorias c ON c.id=lc.categoria_id WHERE lc.libro_id=l.id AND (c.id::text=? OR c.nombre ILIKE ?))");
            args.add(categoria);args.add(categoria);
        }
        if("DISPONIBLE".equalsIgnoreCase(disponibilidad)) sql.append(" AND EXISTS (SELECT 1 FROM ejemplares e WHERE e.libro_id=l.id AND e.estatus AND e.estado='DISPONIBLE' AND e.estado_conservacion<>'DANADO')");
        sql.append(" ORDER BY l.titulo,l.id");return db.filas(sql.toString(),args.toArray());
    }
    @Override public Map<String,Object> libro(long id) {
        Map<String,Object> row=uno(SELECT_LIBRO+" WHERE l.id=?",id);
        if(!identidad.esGestion()&&!bool(row,"estatus",true)) throw no("Libro no disponible");
        row.put("autores_ids",db.filas("SELECT autor_id id FROM libros_autores WHERE libro_id=? ORDER BY autor_id",id).stream().map(x->x.get("id")).toList());
        row.put("categorias_ids",db.filas("SELECT categoria_id id FROM libros_categorias WHERE libro_id=? ORDER BY categoria_id",id).stream().map(x->x.get("id")).toList());
        row.put("ejemplares",ejemplaresDeLibro(id).stream().filter(e->identidad.esGestion()||bool(e,"estatus",false)).toList());return row;
    }
    @Override @Transactional public Map<String,Object> guardarLibro(Long id,Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();if(id!=null) registro("libros",id,true);
        Map<String,Object> v=m("titulo",requerido(d,"titulo",250),"isbn",texto(d,"isbn",20),"edicion",texto(d,"edicion",100),
            "anio_publicacion",enteroOpcional(d,"anio_publicacion",1,Year.now(ZONA).getValue()+1),"num_paginas",enteroOpcional(d,"num_paginas",1,100000),
            "descripcion",texto(d,"descripcion",2000),"editorial_id",referencia(d,"editorial_id","editoriales"),"idioma_id",referencia(d,"idioma_id","idiomas"),"estatus",bool(d,"estatus",true));
        if(id==null) id=db.insertar("libros",v);else db.actualizar("libros",id,v);
        vincular("libros_autores","libro_id",id,"autor_id","autores",ids(d,"autores_ids"));
        vincular("libros_categorias","libro_id",id,"categoria_id","categorias",ids(d,"categorias_ids"));
        auditar("GUARDAR_LIBRO","M02","libros",id,"Datos bibliográficos y asociaciones actualizados");return libro(id);
    }
    private List<Map<String,Object>> ejemplaresDeLibro(long id) {return db.filas("SELECT e.*,u.nombre ubicacion,u.seccion,u.estante,u.nivel FROM ejemplares e LEFT JOIN ubicaciones u ON u.id=e.ubicacion_id WHERE e.libro_id=? ORDER BY e.codigo_barras",id);}
    @Override public List<Map<String,Object>> ejemplares(Long libroId) {
        identidad.exigirGestion();if(libroId!=null)return ejemplaresDeLibro(libroId);
        return db.filas("SELECT e.*,l.titulo,u.nombre ubicacion,u.seccion,u.estante FROM ejemplares e JOIN libros l ON l.id=e.libro_id LEFT JOIN ubicaciones u ON u.id=e.ubicacion_id ORDER BY l.titulo,e.codigo_barras");
    }
    @Override @Transactional public Map<String,Object> guardarEjemplar(Long id,Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();Map<String,Object> previo=id==null?null:registro("ejemplares",id,true);
        long libro=id==null?positivo(d,"libro_id"):num(previo,"libro_id");registro("libros",libro,true);
        if(id!=null&&d.containsKey("libro_id")&&positivo(d,"libro_id")!=libro) throw conflicto("Un ejemplar histórico no puede cambiar de libro");
        String estado=opcion(d,"estado",Set.of("DISPONIBLE","MANTENIMIENTO","EXTRAVIADO","BAJA"),previo==null?"DISPONIBLE":str(previo,"estado"));
        boolean activo=bool(d,"estatus",true);
        if(previo!=null && Set.of("PRESTADO","RESERVADO").contains(str(previo,"estado"))) {
            if(!estado.equals(str(previo,"estado"))||!activo) throw conflicto("La circulación activa debe resolverse desde préstamo o reserva antes de cambiar el ejemplar");
        }
        Long ubicacion=referencia(d,"ubicacion_id","ubicaciones");
        if(ubicacion!=null&&!bool(registro("ubicaciones",ubicacion,true),"estatus",false))throw conflicto("La ubicación seleccionada está inactiva; elija una ubicación vigente");
        if(activo&&Set.of("DISPONIBLE","RESERVADO").contains(estado)&&ubicacion==null) throw conflicto("Indique la ubicación del ejemplar activo");
        String conservacion=opcion(d,"estado_conservacion",CONSERVACION,"BUENO");
        if("DANADO".equals(conservacion)&&"DISPONIBLE".equals(estado))throw conflicto("El material dañado debe pasar a mantenimiento antes de estar disponible");
        Map<String,Object> v=m("libro_id",libro,"codigo_barras",requerido(d,"codigo_barras",80),"fecha_adquisicion",fecha(d,"fecha_adquisicion",hoy()),
            "estado",estado,"estado_conservacion",conservacion,"estatus",activo,"ubicacion_id",ubicacion,"observaciones",texto(d,"observaciones",2000));
        if(id==null)id=db.insertar("ejemplares",v);else db.actualizar("ejemplares",id,v);
        historialEjemplar(id,previo==null?null:str(previo,"estado"),estado,texto(d,"observaciones",2000));
        auditar("GUARDAR_EJEMPLAR","M02","ejemplares",id,"Actualización de inventario");asignarSiguiente(libro);return registro("ejemplares",id,false);
    }
    @Override public Map<String,Object> catalogos() {
        Map<String,Object> r=new LinkedHashMap<>();for(String t:List.of("autores","categorias","editoriales","idiomas","ubicaciones","equipamientos","tipos_sancion"))
            r.put(t,db.filas("SELECT * FROM "+t+" ORDER BY nombre,id"));return r;
    }
    @Override @Transactional public Map<String,Object> crearCatalogo(String tipo,Map<String,Object> d) {
        return guardarCatalogo(tipo,null,d);
    }
    @Override @Transactional public Map<String,Object> editarCatalogo(String tipo,long id,Map<String,Object> d) {
        return guardarCatalogo(tipo,id,d);
    }
    private Map<String,Object> guardarCatalogo(String tipo,Long id,Map<String,Object>d) {
        identidad.exigirGestion();db.bloquearOperacion();exigir(Set.of("autores","categorias","editoriales","idiomas","ubicaciones","equipamientos","tipos_sancion").contains(tipo),"Catálogo desconocido");
        if(id!=null)registro(tipo,id,true);
        if(id!=null&&tipo.equals("ubicaciones")&&!bool(d,"estatus",true)&&db.numero("SELECT count(*) FROM ejemplares WHERE ubicacion_id=? AND estatus",id)>0)throw conflicto("Reasigne los ejemplares activos antes de desactivar esta ubicación");
        Map<String,Object> v=m("nombre",requerido(d,"nombre",tipo.equals("idiomas")?100:tipo.equals("editoriales")?200:150),"estatus",bool(d,"estatus",true));
        switch(tipo) {
            case "autores" -> v.put("apellidos",texto(d,"apellidos",200));
            case "editoriales" -> v.put("pais",texto(d,"pais",100));
            case "ubicaciones" -> {v.put("seccion",requerido(d,"seccion",100));v.put("estante",requerido(d,"estante",100));v.put("nivel",texto(d,"nivel",100));}
            case "tipos_sancion" -> {v.put("codigo",requerido(d,"codigo",60));v.put("descripcion",texto(d,"descripcion",2000));}
            case "categorias","equipamientos" -> v.put("descripcion",texto(d,"descripcion",2000));default -> { }
        }
        if(id!=null&&tipo.equals("tipos_sancion")&&esReposicion(v)&&db.numero("SELECT count(*) FROM politicas_sancion WHERE tipo_sancion_id=? AND estatus AND tipo_incidencia NOT IN ('DANO','EXTRAVIO')",id)>0)throw conflicto("No cambie este tipo a reposición mientras existan políticas activas de incidencias distintas a daño o extravío");
        if(id==null)id=db.insertar(tipo,v);else db.actualizar(tipo,id,v);auditar("GUARDAR_CATALOGO","M02",tipo,id,"Registro de catálogo");return registro(tipo,id,false);
    }
    private String tablaPolitica(String tipo) {return switch(tipo) {case "prestamo"->"politicas_prestamo";case "reserva-libro"->"politicas_reserva_libro";case "cubiculo"->"politicas_reserva_cubiculo";case "sancion"->"politicas_sancion";default->throw error("Tipo de política desconocido");};}
    @Override public List<Map<String,Object>> politicas(String tipo) {identidad.exigirGestion();return db.filas("SELECT * FROM "+tablaPolitica(tipo)+" ORDER BY id DESC");}
    @Override @Transactional public Map<String,Object> guardarPolitica(String tipo,Long id,Map<String,Object> d) {
        identidad.exigirAdministrador();db.bloquearOperacion();String tabla=tablaPolitica(tipo);if(id!=null)registro(tabla,id,true);
        Map<String,Object> v=m("estatus",bool(d,"estatus",true));
        switch(tipo) {
            case "prestamo" -> {v.put("tipo_usuario",opcion(d,"tipo_usuario",TIPOS,null));v.put("max_libros",entero(d,"max_libros",1,1000));v.put("dias_prestamo",entero(d,"dias_prestamo",1,3650));v.put("permite_renovacion",bool(d,"permite_renovacion",false));v.put("renovaciones_max",entero(d,"renovaciones_max",0,100));v.put("restricciones_material",texto(d,"restricciones_material",2000));}
            case "reserva-libro" -> {v.put("dias_anticipacion",enteroOpcional(d,"dias_anticipacion",0,3650));v.put("tiempo_limite",entero(d,"tiempo_limite",1,3650));v.put("max_reservas",entero(d,"max_reservas",1,1000));String regla=str(d,"reglas_prioridad").isBlank()?"FIFO":str(d,"reglas_prioridad").strip().toUpperCase(Locale.ROOT);if(!regla.equals("FIFO"))exigir(csv(regla).size()==4&&TIPOS.containsAll(csv(regla)),"La prioridad debe ser FIFO o los cuatro tipos de usuario separados por comas en orden de atención");v.put("reglas_prioridad",regla);v.put("tipos_usuario_permitidos",tiposPermitidos(d));}
            case "cubiculo" -> {v.put("tiempo_minimo",enteroOpcional(d,"tiempo_minimo",1,1440));v.put("tiempo_maximo",entero(d,"tiempo_maximo",1,1440));v.put("anticipacion_max",entero(d,"anticipacion_max",0,3650));v.put("max_reservas",entero(d,"max_reservas",1,1000));v.put("tolerancia_llegada",enteroOpcional(d,"tolerancia_llegada",0,1440));v.put("tipos_usuario_permitidos",tiposPermitidos(d));if(v.get("tiempo_minimo")!=null)exigir(((Number)v.get("tiempo_minimo")).intValue()<=((Number)v.get("tiempo_maximo")).intValue(),"Duración mínima mayor que máxima");}
            case "sancion" -> {v.put("tipo_incidencia",opcion(d,"tipo_incidencia",tiposIncidencia(),null));v.put("tipo_sancion_id",referenciaObligatoria(d,"tipo_sancion_id","tipos_sancion"));v.put("reglas",requerido(d,"reglas",5000));v.put("tipo_usuario",str(d,"tipo_usuario").isBlank()?null:opcion(d,"tipo_usuario",TIPOS,null));v.put("dias_suspension",enteroOpcional(d,"dias_suspension",0,3650));v.put("monto_multa",dineroOpcional(d,"monto_multa"));v.put("multas_autorizadas",bool(d,"multas_autorizadas",false));v.put("permite_pagos_parciales",bool(d,"permite_pagos_parciales",false));String servicios=texto(d,"servicios_restringidos",2000);if(servicios!=null)for(String s:csv(servicios))exigir(Set.of("PRESTAMO","RENOVACION","RESERVA_LIBRO","CUBICULO").contains(s),"Servicio restringido desconocido: "+s);v.put("servicios_restringidos",servicios==null?null:String.join(",",csv(servicios)));if(!bool(v,"multas_autorizadas",false)&&v.get("monto_multa")!=null&&decimal(v.get("monto_multa")).signum()>0)throw error("Una multa requiere autorización económica expresa");Map<String,Object>tipoS=registro("tipos_sancion",num(v,"tipo_sancion_id"),false);if(esReposicion(tipoS))exigir(Set.of("DANO","EXTRAVIO").contains(str(v,"tipo_incidencia")),"La reposición de material solo corresponde a daño o extravío");condicionesSancion(v);}
        }
        if(id==null)id=db.insertar(tabla,v);else db.actualizar(tabla,id,v);
        auditar("GUARDAR_POLITICA","M03-M06",tabla,id,"Unidades explícitas; las operaciones históricas conservan condiciones aplicadas");return registro(tabla,id,false);
    }

    /** RF-M03-01/07/10/27/40/63; RN-M03-03/05/07/08/13/15/20/22/27/30. */
    @Override @Transactional public List<Map<String,Object>> prestamos() {
        db.bloquearOperacion();identificarVencidos();
        String filtro=identidad.esGestion()?"":" WHERE p.persona_id="+identidad.personaId();
        List<Map<String,Object>> filas=db.filas("SELECT p.*,pe.nombre||' '||pe.apellidos persona_nombre FROM prestamos p JOIN personas pe ON pe.id=p.persona_id"+filtro+" ORDER BY p.id DESC");
        filas.forEach(this::detallarPrestamo);return filas;
    }
    private void identificarVencidos() {
        String propio=identidad.esGestion()?"":" AND p.persona_id="+identidad.personaId();
        for(Map<String,Object>d:db.filas("SELECT d.*,p.persona_id FROM detalles_prestamo d JOIN prestamos p ON p.id=d.prestamo_id WHERE d.estado='ACTIVO' AND d.fecha_vencimiento<?"+propio+" FOR UPDATE OF d",hoy())){
            db.actualizar("detalles_prestamo",num(d,"id"),m("estado","VENCIDO"));Map<String,Object>e=registro("ejemplares",num(d,"ejemplar_id"),false);
            registrarIncidenciaAutomatica(num(d,"persona_id"),"RETRASO",num(d,"id"),null,num(e,"id"),num(d,"prestamo_id"),"Préstamo vencido desde "+str(d,"fecha_vencimiento"));
        }
    }
    private Map<String,Object> prestamo(long id) {
        Map<String,Object> r=uno("SELECT p.*,pe.nombre||' '||pe.apellidos persona_nombre FROM prestamos p JOIN personas pe ON pe.id=p.persona_id WHERE p.id=?",id);
        identidad.exigirPropietario(num(r,"persona_id"));detallarPrestamo(r);return r;
    }
    private void detallarPrestamo(Map<String,Object> r) {
        List<Map<String,Object>> detalles=db.filas("""
            SELECT d.*,e.codigo_barras,e.estado_conservacion,e.libro_id,l.titulo,
              CASE WHEN d.estado<>'DEVUELTO' AND d.fecha_vencimiento<(CURRENT_TIMESTAMP AT TIME ZONE 'America/Mexico_City')::date THEN 'VENCIDO' ELSE d.estado END estado_actual,
              CASE WHEN d.estado<>'DEVUELTO' THEN GREATEST(0,(CURRENT_TIMESTAMP AT TIME ZONE 'America/Mexico_City')::date-d.fecha_vencimiento) ELSE 0 END dias_retraso
            FROM detalles_prestamo d JOIN ejemplares e ON e.id=d.ejemplar_id JOIN libros l ON l.id=e.libro_id
            WHERE d.prestamo_id=? ORDER BY d.id
            """,num(r,"id"));
        for(Map<String,Object> d:detalles) {
            long id=num(d,"id");d.put("devolucion",db.primero("SELECT * FROM devoluciones WHERE detalle_prestamo_id=?",id).orElse(null));
            d.put("renovaciones",db.filas("SELECT * FROM renovaciones WHERE detalle_prestamo_id=? ORDER BY id",id));
            if(d.get("devolucion") instanceof Map<?,?> devolucion)d.put("dias_retraso",Math.max(0,ChronoUnit.DAYS.between(fechaValor(d.get("fecha_vencimiento")),horaValor(devolucion.get("fecha_devolucion")).toLocalDate())));
        }
        r.put("detalles",detalles);
    }
    private Map<String,Object> persona(long id) {
        Map<String,Object> p=registro("personas",id,true);if(!bool(p,"estatus",false))throw conflicto("La persona está inactiva");
        if(db.numero("SELECT count(*) FROM cuentas_usuario WHERE persona_id=? AND (NOT estatus OR COALESCE(estado_verificacion,'PENDIENTE')<>'VERIFICADA')",id)>0)throw conflicto("La cuenta debe estar activa y verificada por el personal antes de usar servicios bibliotecarios");
        p.put("tipo_usuario",tipoPersona(id));return p;
    }
    private String tipoPersona(long id) {
        if(db.numero("SELECT count(*) FROM estudiantes WHERE id=?",id)>0)return "ESTUDIANTE";
        if(db.numero("SELECT count(*) FROM docentes WHERE id=?",id)>0)return "DOCENTE";
        if(db.numero("SELECT count(*) FROM empleados WHERE id=?",id)>0)return "EMPLEADO";
        if(db.numero("SELECT count(*) FROM usuarios_externos WHERE id=?",id)>0)return "EXTERNO";
        throw conflicto("Complete el tipo de persona antes de realizar operaciones");
    }
    private void sinRestricciones(long persona,String servicio) {
        long n=db.numero("""
            SELECT count(*) FROM restricciones_servicio r JOIN sanciones s ON s.id=r.sancion_id
            JOIN incidencias i ON i.id=s.incidencia_id WHERE i.persona_id=? AND s.estado IN ('ACTIVA','PENDIENTE')
            AND r.estado='ACTIVA' AND r.fecha_inicio<=? AND (r.fecha_fin IS NULL OR r.fecha_fin>=?)
            AND r.servicio_afectado IN (?, 'TODOS')
            """,persona,hoy(),hoy(),servicio);
        if(n>0)throw conflicto("La persona tiene una restricción activa para "+servicio+". Revise sus sanciones");
    }
    private boolean elegibleReserva(long persona) {
        if(db.numero("SELECT count(*) FROM personas p JOIN cuentas_usuario c ON c.persona_id=p.id WHERE p.id=? AND p.estatus AND c.estatus AND c.estado_verificacion='VERIFICADA'",persona)==0)return false;
        return db.numero("SELECT count(*) FROM restricciones_servicio r JOIN sanciones s ON s.id=r.sancion_id JOIN incidencias i ON i.id=s.incidencia_id WHERE i.persona_id=? AND s.estado IN ('ACTIVA','PENDIENTE') AND r.estado='ACTIVA' AND r.fecha_inicio<=? AND (r.fecha_fin IS NULL OR r.fecha_fin>=?) AND r.servicio_afectado IN ('RESERVA_LIBRO','TODOS')",persona,hoy(),hoy())==0;
    }
    private void exigirCuentaHabilitada(long persona) {if(db.numero("SELECT count(*) FROM cuentas_usuario WHERE persona_id=? AND estatus AND estado_verificacion='VERIFICADA'",persona)==0)throw conflicto("La reservación requiere una cuenta activa y verificada");}
    private Map<String,Object> politicaPrestamo(long persona) {
        String tipo=str(persona(persona),"tipo_usuario");
        return db.primero("SELECT * FROM politicas_prestamo WHERE estatus AND tipo_usuario=? ORDER BY id DESC LIMIT 1",tipo)
            .orElseThrow(()->conflicto("Configure una política de préstamo activa para "+tipo+" en Administración"));
    }
    @Override @Transactional public Map<String,Object> prestar(Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();expirarReservas();return crearPrestamo(positivo(d,"persona_id"),ids(d,"ejemplares_ids"),texto(d,"observaciones",2000),null);
    }
    private Map<String,Object> crearPrestamo(long persona,List<Long> ejemplares,String observaciones,Long reservaId) {
        exigir(!ejemplares.isEmpty(),"Seleccione al menos un ejemplar");Map<String,Object> pol=politicaPrestamo(persona);sinRestricciones(persona,"PRESTAMO");
        long activos=db.numero("SELECT count(*) FROM detalles_prestamo d JOIN prestamos p ON p.id=d.prestamo_id WHERE p.persona_id=? AND d.estado IN ('ACTIVO','VENCIDO')",persona);
        if(activos+ejemplares.size()>num(pol,"max_libros"))throw conflicto("Se supera el límite de "+num(pol,"max_libros")+" ejemplares de la política");
        List<Map<String,Object>> copias=new ArrayList<>();
        for(long id:ejemplares.stream().sorted().toList()) {
            Map<String,Object> e=registro("ejemplares",id,true);Map<String,Object> l=registro("libros",num(e,"libro_id"),true);
            if(!bool(e,"estatus",false)||!bool(l,"estatus",false))throw conflicto("Libro o ejemplar inactivo");
            if("DANADO".equals(str(e,"estado_conservacion")))throw conflicto("El ejemplar dañado requiere revisión antes del préstamo");
            boolean apartado=reservaId!=null&&db.numero("SELECT count(*) FROM asignaciones_reserva WHERE reserva_id=? AND ejemplar_id=? AND estado='ACTIVA'",reservaId,id)>0;
            if(!"DISPONIBLE".equals(str(e,"estado"))&&!(apartado&&"RESERVADO".equals(str(e,"estado"))))throw conflicto("El ejemplar "+str(e,"codigo_barras")+" no está disponible");
            if(db.numero("SELECT count(*) FROM detalles_prestamo WHERE ejemplar_id=? AND estado IN ('ACTIVO','VENCIDO')",id)>0)throw conflicto("El ejemplar ya participa en un préstamo activo");
            if(!apartado&&db.filas("SELECT persona_id FROM reservas_libro WHERE libro_id=? AND estado IN ('PENDIENTE','EN_ESPERA')",num(e,"libro_id")).stream().anyMatch(r->elegibleReserva(num(r,"persona_id"))))throw conflicto("Hay reservas pendientes: asigne el ejemplar al primero elegible de la cola");
            List<String> excluidas=csv(str(pol,"restricciones_material"));
            if(!excluidas.isEmpty()) {
                List<Map<String,Object>> cats=db.filas("SELECT upper(c.nombre) nombre FROM categorias c JOIN libros_categorias lc ON lc.categoria_id=c.id WHERE lc.libro_id=?",num(e,"libro_id"));
                if(cats.stream().anyMatch(c->excluidas.contains(str(c,"nombre"))))throw conflicto("La política excluye la categoría de este material del préstamo");
            }
            copias.add(e);
        }
        LocalDate vence=hoy().plusDays(num(pol,"dias_prestamo"));
        long id=db.insertar("prestamos",m("fecha_prestamo",ahora(),"fecha_vencimiento",vence,"estado","ACTIVO","observaciones",observaciones,
            "fecha_confirmacion_entrega",ahora(),"condiciones_aplicadas",snapshot(pol),"persona_id",persona,"bibliotecario_id",identidad.cuentaId(),"politica_id",num(pol,"id")));
        for(Map<String,Object> e:copias) {
            long detalle=db.insertar("detalles_prestamo",m("fecha_vencimiento",vence,"estado","ACTIVO","prestamo_id",id,"ejemplar_id",num(e,"id")));
            estadoEjemplar(num(e,"id"),"PRESTADO","Entrega confirmada en préstamo #"+id);
            if(reservaId!=null) {
                Map<String,Object> r=registro("reservas_libro",reservaId,true);String anterior=str(r,"estado");
                db.actualizar("reservas_libro",reservaId,m("estado","ATENDIDA","fecha_atencion",ahora(),"detalle_prestamo_id",detalle,"posicion_espera",null));
                db.ejecutar("UPDATE asignaciones_reserva SET estado='ATENDIDA' WHERE reserva_id=? AND estado='ACTIVA'",reservaId);
                historialReserva(reservaId,anterior,"ATENDIDA",r.get("posicion_espera"),null,"Entrega mediante préstamo #"+id);
            }
        }
        auditar("CONFIRMAR_PRESTAMO","M03","prestamos",id,"Entrega de "+copias.size()+" ejemplares");
        avisar(persona,"NOTIFICACION_PRESTAMO","PRESTAMO:"+id,"Préstamo registrado","Tu préstamo #"+id+" vence el "+vence,"prestamos",id);return prestamo(id);
    }
    @Override @Transactional public Map<String,Object> devolver(long id,Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();Map<String,Object> p=registro("prestamos",id,true);
        if(!"ACTIVO".equals(str(p,"estado")))throw conflicto("El préstamo ya está finalizado");
        Object items=d.get("devoluciones");if(!(items instanceof List<?> lista)||lista.isEmpty())throw error("Indique devoluciones con detalle_id y estado_conservacion");
        Set<Long> procesados=new HashSet<>();Set<Long> libros=new HashSet<>();
        for(Object item:(List<?>)items) {
            if(!(item instanceof Map<?,?>))throw error("Devolución inválida");Map<String,Object> entrada=new LinkedHashMap<>();((Map<?,?>)item).forEach((k,v)->entrada.put(k.toString(),v));
            long detalle=positivo(entrada,"detalle_id");exigir(procesados.add(detalle),"Detalle repetido");Map<String,Object> dp=registro("detalles_prestamo",detalle,true);
            exigir(num(dp,"prestamo_id")==id,"El detalle no pertenece al préstamo");if("DEVUELTO".equals(str(dp,"estado")))throw conflicto("El detalle ya fue devuelto");
            String conservacion=opcion(entrada,"estado_conservacion",CONSERVACION,null);
            String estado=opcion(entrada,"estado_ejemplar",Set.of("DISPONIBLE","MANTENIMIENTO","EXTRAVIADO","BAJA"),"DANADO".equals(conservacion)?"MANTENIMIENTO":"DISPONIBLE");
            if("DANADO".equals(conservacion)&&"DISPONIBLE".equals(estado))throw error("Un material dañado debe revisarse en mantenimiento");
            Map<String,Object> e=registro("ejemplares",num(dp,"ejemplar_id"),true);libros.add(num(e,"libro_id"));
            long devolucion=db.insertar("devoluciones",m("fecha_devolucion",ahora(),"estado","CONFIRMADA","estado_conservacion",conservacion,"observaciones",texto(entrada,"observaciones",2000),"detalle_prestamo_id",detalle,"bibliotecario_id",identidad.cuentaId()));
            db.actualizar("detalles_prestamo",detalle,m("estado","DEVUELTO"));db.actualizar("ejemplares",num(e,"id"),m("estado_conservacion",conservacion));
            estadoEjemplar(num(e,"id"),estado,"Devolución #"+devolucion);
            if(fechaValor(dp.get("fecha_vencimiento")).isBefore(hoy()))registrarIncidenciaAutomatica(num(p,"persona_id"),"RETRASO",detalle,devolucion,num(e,"id"),id,"Devolución con "+ChronoUnit.DAYS.between(fechaValor(dp.get("fecha_vencimiento")),hoy())+" días de retraso");
            if("DANADO".equals(conservacion))registrarIncidenciaAutomatica(num(p,"persona_id"),"DANO",detalle,devolucion,num(e,"id"),id,"Daño documentado en devolución: "+str(entrada,"observaciones"));
            if("EXTRAVIADO".equals(estado))registrarIncidenciaAutomatica(num(p,"persona_id"),"EXTRAVIO",detalle,devolucion,num(e,"id"),id,"Ejemplar reportado como extraviado");
            avisar(num(p,"persona_id"),"NOTIFICACION_DEVOLUCION","DEVOLUCION:"+devolucion,"Devolución registrada","Recibimos el ejemplar "+str(e,"codigo_barras"),"devoluciones",devolucion);
        }
        if(db.numero("SELECT count(*) FROM detalles_prestamo WHERE prestamo_id=? AND estado<>'DEVUELTO'",id)==0)db.actualizar("prestamos",id,m("estado","FINALIZADO"));
        libros.forEach(this::asignarSiguiente);auditar("DEVOLVER_PRESTAMO","M03","prestamos",id,"Recepción parcial o total de "+procesados.size()+" ejemplares");return prestamo(id);
    }
    @Override @Transactional public Map<String,Object> renovar(long id,Map<String,Object> d) {
        db.bloquearOperacion();Map<String,Object> p=registro("prestamos",id,true);identidad.exigirPropietario(num(p,"persona_id"));sinRestricciones(num(p,"persona_id"),"PRESTAMO");sinRestricciones(num(p,"persona_id"),"RENOVACION");
        long detalle=positivo(d,"detalle_id");Map<String,Object> dp=registro("detalles_prestamo",detalle,true);exigir(num(dp,"prestamo_id")==id,"Detalle ajeno al préstamo");
        LocalDate anterior=fechaValor(dp.get("fecha_vencimiento"));if(!"ACTIVO".equals(str(dp,"estado"))||anterior.isBefore(hoy()))throw conflicto("Solo se renuevan préstamos activos y no vencidos");
        Map<String,Object> pol=condiciones(p,"politicas_prestamo");if(!bool(pol,"permite_renovacion",false))throw conflicto("La política no permite renovaciones");
        if(db.numero("SELECT count(*) FROM renovaciones WHERE detalle_prestamo_id=?",detalle)>=num(pol,"renovaciones_max"))throw conflicto("Se alcanzó el límite de renovaciones");
        Map<String,Object> e=registro("ejemplares",num(dp,"ejemplar_id"),true);
        if(db.numero("SELECT count(*) FROM reservas_libro WHERE libro_id=? AND persona_id<>? AND estado IN "+ACTIVAS_LIBRO,num(e,"libro_id"),num(p,"persona_id"))>0)throw conflicto("Otra persona espera este libro; no puede renovarse");
        LocalDate nueva=anterior.plusDays(num(pol,"dias_prestamo"));long renovacion=db.insertar("renovaciones",m("fecha_renovacion",ahora(),"fecha_vencimiento_anterior",anterior,"fecha_vencimiento",nueva,"estado","APROBADA","detalle_prestamo_id",detalle,"responsable_id",identidad.cuentaId()));
        db.actualizar("detalles_prestamo",detalle,m("fecha_vencimiento",nueva));db.ejecutar("UPDATE prestamos SET fecha_vencimiento=(SELECT max(fecha_vencimiento) FROM detalles_prestamo WHERE prestamo_id=?) WHERE id=?",id,id);
        auditar("RENOVAR_PRESTAMO","M03","prestamos",id,"Detalle #"+detalle+": "+anterior+" → "+nueva);
        avisar(num(p,"persona_id"),"NOTIFICACION_RENOVACION","RENOVACION:"+renovacion,"Renovación aprobada","La nueva fecha de devolución es "+nueva,"prestamos",id);return prestamo(id);
    }

    /** RF-M04-16/26/29/39/44/56; RN-M04-08/10/11/12/18/20/22/23/28. */
    @Override @Transactional public List<Map<String,Object>> reservas() {
        db.bloquearOperacion();expirarReservas();String filtro=identidad.esGestion()?"":" WHERE r.persona_id="+identidad.personaId();
        List<Map<String,Object>> r=db.filas("SELECT r.*,l.titulo,p.nombre||' '||p.apellidos persona_nombre FROM reservas_libro r JOIN libros l ON l.id=r.libro_id JOIN personas p ON p.id=r.persona_id"+filtro+" ORDER BY r.id DESC");r.forEach(this::detallarReserva);return r;
    }
    private Map<String,Object> reserva(long id) {Map<String,Object> r=uno("SELECT r.*,l.titulo,p.nombre||' '||p.apellidos persona_nombre FROM reservas_libro r JOIN libros l ON l.id=r.libro_id JOIN personas p ON p.id=r.persona_id WHERE r.id=?",id);identidad.exigirPropietario(num(r,"persona_id"));detallarReserva(r);return r;}
    private void detallarReserva(Map<String,Object> r) {r.put("asignaciones",db.filas("SELECT a.*,e.codigo_barras FROM asignaciones_reserva a JOIN ejemplares e ON e.id=a.ejemplar_id WHERE a.reserva_id=? ORDER BY a.id DESC",num(r,"id")));r.put("historial",db.filas("SELECT * FROM historiales_reserva WHERE reserva_id=? ORDER BY id",num(r,"id")));r.put("elegible",elegibleReserva(num(r,"persona_id")));}
    private Map<String,Object> politicaReserva(String tabla,long persona) {
        String tipo=str(persona(persona),"tipo_usuario");return db.filas("SELECT * FROM "+tabla+" WHERE estatus ORDER BY id DESC").stream()
            .filter(p->csv(str(p,"tipos_usuario_permitidos")).contains(tipo)).findFirst().orElseThrow(()->conflicto("Configure una política activa para "+tipo+" en "+tabla));
    }
    @Override @Transactional public Map<String,Object> reservar(Map<String,Object> d) {
        db.bloquearOperacion();expirarReservas();long persona=personaSolicitada(d);exigirCuentaHabilitada(persona);long libro=positivo(d,"libro_id");Map<String,Object> l=registro("libros",libro,true);
        if(!bool(l,"estatus",false))throw conflicto("El libro está inactivo");sinRestricciones(persona,"RESERVA_LIBRO");Map<String,Object> pol=politicaReserva("politicas_reserva_libro",persona);
        String prioridad=str(pol,"reglas_prioridad");if(!"FIFO".equals(prioridad)&&!(csv(prioridad).size()==4&&TIPOS.containsAll(csv(prioridad))))throw conflicto("Configure una prioridad estructurada: FIFO o los cuatro tipos de usuario ordenados");
        pol.put("prioridad_aplicada",prioridad.equals("FIFO")?0:csv(prioridad).indexOf(str(persona(persona),"tipo_usuario")));
        if(db.numero("SELECT count(*) FROM reservas_libro WHERE persona_id=? AND libro_id=? AND estado IN "+ACTIVAS_LIBRO,persona,libro)>0)throw conflicto("Ya existe una reserva activa de esta persona para el libro");
        if(db.numero("SELECT count(*) FROM reservas_libro WHERE persona_id=? AND estado IN "+ACTIVAS_LIBRO,persona)>=num(pol,"max_reservas"))throw conflicto("Se alcanzó el límite de reservas activas");
        db.ejecutar("INSERT INTO listas_espera(libro_id) VALUES (?) ON CONFLICT (libro_id) DO NOTHING",libro);long lista=num(uno("SELECT * FROM listas_espera WHERE libro_id=? FOR UPDATE",libro),"id");
        long posicion=db.numero("SELECT COALESCE(max(posicion_espera),0)+1 FROM reservas_libro WHERE lista_espera_id=?",lista);
        long id=db.insertar("reservas_libro",m("fecha_reserva",ahora(),"estado","EN_ESPERA","observaciones",texto(d,"observaciones",2000),"posicion_espera",posicion,"condiciones_aplicadas",snapshot(pol),"persona_id",persona,"libro_id",libro,"politica_id",num(pol,"id"),"lista_espera_id",lista));
        historialReserva(id,null,"EN_ESPERA",null,posicion,"Reserva registrada en cola FIFO");asignarSiguiente(libro);
        auditar("CREAR_RESERVA","M04","reservas_libro",id,"Libro #"+libro);
        avisar(persona,"NOTIFICACION_RESERVA","RESERVA:"+id,"Reserva registrada","Tu reserva #"+id+" está registrada. Consulta su estado y plazo en Mis reservas.","reservas_libro",id);return reserva(id);
    }
    private long personaSolicitada(Map<String,Object>d) {long id=identidad.esGestion()&&d.get("persona_id")!=null?positivo(d,"persona_id"):identidad.personaId();identidad.exigirPropietario(id);persona(id);return id;}
    @Override @Transactional public Map<String,Object> accionReserva(long id,String accion,Map<String,Object> d) {
        db.bloquearOperacion();expirarReservas();Map<String,Object> r=registro("reservas_libro",id,true);identidad.exigirPropietario(num(r,"persona_id"));
        switch(accion) {
            case "cancelar" -> {String motivo=requerido(d,"motivo",2000);if(!Set.of("PENDIENTE","EN_ESPERA","LISTA_PARA_RECOGER").contains(str(r,"estado")))throw conflicto("La reserva ya no puede cancelarse");cerrarReserva(r,"CANCELADA",motivo);asignarSiguiente(num(r,"libro_id"));}
            case "asignar" -> {identidad.exigirGestion();if(!"LISTA_PARA_RECOGER".equals(str(r,"estado"))){if(!Set.of("PENDIENTE","EN_ESPERA").contains(str(r,"estado")))throw conflicto("La reserva no está en espera");asignarSiguiente(num(r,"libro_id"));if(!"LISTA_PARA_RECOGER".equals(str(registro("reservas_libro",id,false),"estado")))throw conflicto("No hay ejemplares disponibles o existen reservas anteriores");}}
            case "atender" -> {identidad.exigirGestion();if(!"LISTA_PARA_RECOGER".equals(str(r,"estado")))throw conflicto("Primero debe asignarse un ejemplar a la reserva");Map<String,Object> a=uno("SELECT * FROM asignaciones_reserva WHERE reserva_id=? AND estado='ACTIVA' FOR UPDATE",id);crearPrestamo(num(r,"persona_id"),List.of(num(a,"ejemplar_id")),"Entrega de reserva #"+id,id);asignarSiguiente(num(r,"libro_id"));}
            default -> throw error("Acción de reserva desconocida");
        }
        auditar("RESERVA_"+accion.toUpperCase(Locale.ROOT),"M04","reservas_libro",id,str(d,"motivo"));return reserva(id);
    }
    private void historialReserva(long id,String anterior,String nuevo,Object antes,Object despues,String motivo) {db.insertar("historiales_reserva",m("fecha",ahora(),"estado_anterior",anterior,"estado_nuevo",nuevo,"posicion_anterior",antes,"posicion_nueva",despues,"observaciones",motivo,"reserva_id",id,"responsable_id",identidad.cuentaId()));}
    private void cerrarReserva(Map<String,Object> r,String estado,String motivo) {
        long id=num(r,"id");Map<String,Object> valores=m("estado",estado,"posicion_espera",null);
        if("CANCELADA".equals(estado)){valores.put("fecha_cancelacion",ahora());valores.put("motivo_cancelacion",motivo);valores.put("responsable_cancelacion_id",identidad.cuentaId());}
        db.actualizar("reservas_libro",id,valores);historialReserva(id,str(r,"estado"),estado,r.get("posicion_espera"),null,motivo);
        for(Map<String,Object>a:db.filas("SELECT * FROM asignaciones_reserva WHERE reserva_id=? AND estado='ACTIVA' FOR UPDATE",id)) {Map<String,Object> e=registro("ejemplares",num(a,"ejemplar_id"),true);estadoEjemplar(num(a,"ejemplar_id"),"DANADO".equals(str(e,"estado_conservacion"))?"MANTENIMIENTO":"DISPONIBLE","Reserva "+estado.toLowerCase(Locale.ROOT));db.actualizar("asignaciones_reserva",num(a,"id"),m("estado","LIBERADA"));}
        if("VENCIDA".equals(estado)&&db.numero("SELECT count(*) FROM incidencias WHERE reserva_libro_id=? AND tipo='INCUMPLIMIENTO_RESERVACION'",id)==0){long incidencia=crearIncidencia(m("tipo","INCUMPLIMIENTO_RESERVACION","descripcion","No se recogió el libro dentro del plazo de la reserva #"+id,"motivo","PLAZO_RECOGIDA_VENCIDO","reserva_libro_id",id),num(r,"persona_id"));aplicarPoliticaAutomatica(incidencia);}
        avisar(num(r,"persona_id"),"NOTIFICACION_RESERVA","RESERVA:"+id+":"+estado,"Reserva "+estado.toLowerCase(Locale.ROOT),motivo,"reservas_libro",id);
    }
    private void expirarReservas() {
        Set<Long> libros=new HashSet<>();for(Map<String,Object>r:db.filas("SELECT * FROM reservas_libro WHERE estado='LISTA_PARA_RECOGER' AND fecha_limite<? FOR UPDATE",hoy())) {cerrarReserva(r,"VENCIDA","Venció el plazo de recogida");libros.add(num(r,"libro_id"));}libros.forEach(this::asignarSiguiente);
    }
    private void asignarSiguiente(long libro) {
        if(!bool(registro("libros",libro,true),"estatus",false))return;
        List<Map<String,Object>> cola=db.filas("SELECT * FROM reservas_libro WHERE libro_id=? AND estado IN ('PENDIENTE','EN_ESPERA') ORDER BY fecha_reserva,id FOR UPDATE",libro);ordenarCola(cola);
        for(Map<String,Object> r:cola) {
            if(!elegibleReserva(num(r,"persona_id")))continue;
            Optional<Map<String,Object>> copia=db.primero("SELECT * FROM ejemplares WHERE libro_id=? AND estatus AND estado='DISPONIBLE' AND estado_conservacion<>'DANADO' ORDER BY id LIMIT 1 FOR UPDATE",libro);if(copia.isEmpty())break;
            Map<String,Object> pol=condiciones(r,"politicas_reserva_libro");long id=num(r,"id");LocalDate limite=hoy().plusDays(num(pol,"tiempo_limite"));
            db.insertar("asignaciones_reserva",m("fecha_asignacion",ahora(),"estado","ACTIVA","reserva_id",id,"ejemplar_id",num(copia.get(),"id")));
            db.actualizar("reservas_libro",id,m("estado","LISTA_PARA_RECOGER","fecha_limite",limite,"posicion_espera",null));
            estadoEjemplar(num(copia.get(),"id"),"RESERVADO","Asignado a reserva #"+id);historialReserva(id,str(r,"estado"),"LISTA_PARA_RECOGER",r.get("posicion_espera"),null,"Ejemplar apartado hasta "+limite);
            avisar(num(r,"persona_id"),"NOTIFICACION_RESERVA","RESERVA:"+id+":LISTA","Tu libro está listo","Recoge tu ejemplar antes del "+limite,"reservas_libro",id);
        }
        List<Map<String,Object>> pendientes=db.filas("SELECT * FROM reservas_libro WHERE libro_id=? AND estado IN ('PENDIENTE','EN_ESPERA') ORDER BY fecha_reserva,id",libro);ordenarCola(pendientes);
        // Evita colisión temporal del UNIQUE(lista,posicion) al desplazar varias filas.
        db.ejecutar("UPDATE reservas_libro SET posicion_espera=NULL WHERE libro_id=? AND estado IN ('PENDIENTE','EN_ESPERA')",libro);
        for(int i=0;i<pendientes.size();i++){Map<String,Object> r=pendientes.get(i);int posicion=i+1;db.actualizar("reservas_libro",num(r,"id"),m("posicion_espera",posicion));if(r.get("posicion_espera")==null||num(r,"posicion_espera")!=posicion)historialReserva(num(r,"id"),str(r,"estado"),str(r,"estado"),r.get("posicion_espera"),posicion,"Actualización de posición FIFO");}
    }
    private void ordenarCola(List<Map<String,Object>> filas) {filas.sort(Comparator.<Map<String,Object>>comparingLong(r->{Map<String,Object> p=condiciones(r,"politicas_reserva_libro");return p.get("prioridad_aplicada")==null?0:num(p,"prioridad_aplicada");}).thenComparing(r->horaValor(r.get("fecha_reserva"))).thenComparingLong(r->num(r,"id")));}

    /** RF-M05-19/29/42/50/60/63/69; RN-M05-06/07/10/11/13/17/19/21/23/29. */
    @Override public List<Map<String,Object>> cubiculos() {
        List<Map<String,Object>> filas=db.filas("""
            SELECT c.*,
              (SELECT count(*) FROM reservas_cubiculo r WHERE r.cubiculo_id=c.id AND r.estado IN ('CONFIRMADA','EN_USO') AND r.fecha_inicio::date=(CURRENT_TIMESTAMP AT TIME ZONE 'America/Mexico_City')::date) reservas_hoy,
              EXISTS(SELECT 1 FROM usos_cubiculo u JOIN reservas_cubiculo r ON r.id=u.reserva_id WHERE r.cubiculo_id=c.id AND u.fecha_fin IS NULL) en_uso,
              COALESCE((SELECT string_agg(e.nombre,', ' ORDER BY e.nombre) FROM equipamientos e JOIN cubiculos_equipamientos ce ON ce.equipamiento_id=e.id WHERE ce.cubiculo_id=c.id),'') equipamientos
            FROM cubiculos c ORDER BY c.codigo
            """);
        filas.forEach(c->c.put("equipamientos_ids",db.filas("SELECT equipamiento_id id FROM cubiculos_equipamientos WHERE cubiculo_id=?",num(c,"id")).stream().map(e->e.get("id")).toList()));return filas;
    }
    @Override @Transactional public Map<String,Object> guardarCubiculo(Long id,Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();if(id!=null)registro("cubiculos",id,true);
        String estado=opcion(d,"estado",Set.of("HABILITADO","MANTENIMIENTO","FUERA_DE_SERVICIO"),"HABILITADO");boolean activo=bool(d,"estatus",true);
        if(id!=null&&(!activo||!"HABILITADO".equals(estado))&&db.numero("SELECT count(*) FROM reservas_cubiculo WHERE cubiculo_id=? AND (estado='EN_USO' OR (estado='CONFIRMADA' AND fecha_fin>?))",id,ahora())>0)throw conflicto("Resuelva las reservas vigentes y el uso activo antes de deshabilitar el cubículo");
        Map<String,Object> v=m("codigo",requerido(d,"codigo",80),"nombre",requerido(d,"nombre",150),"ubicacion",texto(d,"ubicacion",250),"capacidad",entero(d,"capacidad",1,1000),"estado",estado,"descripcion",texto(d,"descripcion",2000),"estatus",activo);
        if(id==null)id=db.insertar("cubiculos",v);else db.actualizar("cubiculos",id,v);
        vincular("cubiculos_equipamientos","cubiculo_id",id,"equipamiento_id","equipamientos",ids(d,"equipamientos_ids"));auditar("GUARDAR_CUBICULO","M05","cubiculos",id,"Datos y equipamiento actualizados");return registro("cubiculos",id,false);
    }
    @Override public List<Map<String,Object>> horarios() {return db.filas("SELECT * FROM horarios_biblioteca ORDER BY fecha_especial DESC NULLS LAST,dia_semana,hora_apertura");}
    @Override @Transactional public Map<String,Object> guardarHorario(Long id,Map<String,Object> d) {
        identidad.exigirGestion();db.bloquearOperacion();if(id!=null)registro("horarios_biblioteca",id,true);
        LocalDate especial=fecha(d,"fecha_especial",null);String dia=texto(d,"dia_semana",20);boolean cerrado=bool(d,"cerrado",false);
        if(especial==null){exigir(dia!=null,"Indique día de semana o fecha especial");try{dia=DayOfWeek.valueOf(dia.toUpperCase(Locale.ROOT)).name();}catch(RuntimeException e){throw error("Use MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY o SUNDAY");}}
        else dia=null;
        LocalTime apertura=null,cierre=null;if(!cerrado){try{apertura=LocalTime.parse(requerido(d,"hora_apertura",20));cierre=LocalTime.parse(requerido(d,"hora_cierre",20));}catch(RuntimeException e){throw error("Indique horas válidas HH:mm");}exigir(cierre.isAfter(apertura),"El cierre debe ser posterior a la apertura");}
        Map<String,Object> v=m("dia_semana",dia,"fecha_especial",especial,"hora_apertura",apertura,"hora_cierre",cierre,"cerrado",cerrado,"estatus",bool(d,"estatus",true));
        if(id==null)id=db.insertar("horarios_biblioteca",v);else db.actualizar("horarios_biblioteca",id,v);auditar("GUARDAR_HORARIO","M05","horarios_biblioteca",id,"Horario explícito; fecha especial prevalece sobre semana");return registro("horarios_biblioteca",id,false);
    }
    @Override public List<Map<String,Object>> bloqueos() {return db.filas("SELECT b.*,c.nombre cubiculo FROM bloqueos_cubiculo b JOIN cubiculos c ON c.id=b.cubiculo_id ORDER BY b.fecha_inicio DESC");}
    @Override @Transactional public Map<String,Object> guardarBloqueo(Map<String,Object>d) {
        identidad.exigirGestion();db.bloquearOperacion();long cubiculo=referenciaObligatoria(d,"cubiculo_id","cubiculos");registro("cubiculos",cubiculo,true);
        LocalDateTime inicio=fechaHora(d,"fecha_inicio"),fin=fechaHora(d,"fecha_fin");exigir(fin.isAfter(inicio),"Fin debe ser posterior al inicio");
        if(db.numero("SELECT count(*) FROM reservas_cubiculo WHERE cubiculo_id=? AND estado IN ('CONFIRMADA','EN_USO') AND fecha_inicio<? AND fecha_fin>?",cubiculo,fin,inicio)>0)throw conflicto("Hay reservaciones afectadas. Resuélvalas antes de crear el bloqueo; no se eliminan automáticamente");
        long id=db.insertar("bloqueos_cubiculo",m("fecha_inicio",inicio,"fecha_fin",fin,"motivo",requerido(d,"motivo",2000),"estatus",true,"cubiculo_id",cubiculo,"responsable_id",identidad.cuentaId()));
        auditar("BLOQUEAR_CUBICULO","M05","bloqueos_cubiculo",id,str(d,"motivo"));return registro("bloqueos_cubiculo",id,false);
    }
    @Override @Transactional public Map<String,Object> cancelarBloqueo(long id,Map<String,Object>d) {identidad.exigirGestion();db.bloquearOperacion();registro("bloqueos_cubiculo",id,true);String motivo=requerido(d,"motivo",1000);db.actualizar("bloqueos_cubiculo",id,m("estatus",false));auditar("CANCELAR_BLOQUEO","M05","bloqueos_cubiculo",id,motivo);return registro("bloqueos_cubiculo",id,false);}
    @Override @Transactional public List<Map<String,Object>> reservasCubiculo() {
        db.bloquearOperacion();expirarCubiculos();String filtro=identidad.esGestion()?"":" WHERE r.persona_id="+identidad.personaId();
        List<Map<String,Object>> r=db.filas("SELECT r.*,c.nombre cubiculo,c.codigo,p.nombre||' '||p.apellidos persona_nombre FROM reservas_cubiculo r JOIN cubiculos c ON c.id=r.cubiculo_id JOIN personas p ON p.id=r.persona_id"+filtro+" ORDER BY r.fecha_inicio DESC");
        r.forEach(x->x.put("uso",db.primero("SELECT * FROM usos_cubiculo WHERE reserva_id=?",num(x,"id")).orElse(null)));return r;
    }
    private Map<String,Object> reservaCubiculo(long id) {Map<String,Object> r=uno("SELECT r.*,c.nombre cubiculo,c.codigo,p.nombre||' '||p.apellidos persona_nombre FROM reservas_cubiculo r JOIN cubiculos c ON c.id=r.cubiculo_id JOIN personas p ON p.id=r.persona_id WHERE r.id=?",id);identidad.exigirPropietario(num(r,"persona_id"));r.put("uso",db.primero("SELECT * FROM usos_cubiculo WHERE reserva_id=?",id).orElse(null));return r;}
    private Map<String,Object> horarioPara(LocalDateTime inicio,LocalDateTime fin) {
        if(!inicio.toLocalDate().equals(fin.toLocalDate()))throw conflicto("La reserva debe comenzar y terminar el mismo día");
        List<Map<String,Object>> h=db.filas("SELECT * FROM horarios_biblioteca WHERE estatus AND fecha_especial=? ORDER BY id DESC",inicio.toLocalDate());
        if(h.isEmpty())h=db.filas("SELECT * FROM horarios_biblioteca WHERE estatus AND fecha_especial IS NULL AND dia_semana=? ORDER BY id DESC",inicio.getDayOfWeek().name());
        if(h.isEmpty())throw conflicto("Configure el horario de biblioteca para ese día antes de reservar");
        if(h.stream().anyMatch(x->bool(x,"cerrado",false)))throw conflicto("La biblioteca está cerrada en la fecha seleccionada");
        return h.stream().filter(x->{LocalTime a=LocalTime.parse(str(x,"hora_apertura")),c=LocalTime.parse(str(x,"hora_cierre"));return !inicio.toLocalTime().isBefore(a)&&!fin.toLocalTime().isAfter(c);})
            .findFirst().orElseThrow(()->conflicto("El periodo está fuera del horario de biblioteca"));
    }
    @Override @Transactional public Map<String,Object> reservarCubiculo(Map<String,Object>d) {
        db.bloquearOperacion();expirarCubiculos();long persona=personaSolicitada(d);exigirCuentaHabilitada(persona);sinRestricciones(persona,"CUBICULO");long cubiculo=positivo(d,"cubiculo_id");Map<String,Object> c=registro("cubiculos",cubiculo,true);
        if(!bool(c,"estatus",false)||!"HABILITADO".equals(str(c,"estado")))throw conflicto("El cubículo no está habilitado");
        LocalDateTime inicio=fechaHora(d,"fecha_inicio"),fin=fechaHora(d,"fecha_fin");exigir(fin.isAfter(inicio),"Fin debe ser posterior al inicio");if(inicio.isBefore(ahora().minusSeconds(30)))throw conflicto("No se permiten reservas en el pasado");
        Map<String,Object> pol=politicaReserva("politicas_reserva_cubiculo",persona);long minutos=ChronoUnit.MINUTES.between(inicio,fin);
        if(minutos>num(pol,"tiempo_maximo")||(pol.get("tiempo_minimo")!=null&&minutos<num(pol,"tiempo_minimo")))throw conflicto("La duración no cumple los límites de la política (minutos)");
        if(ChronoUnit.DAYS.between(hoy(),inicio.toLocalDate())>num(pol,"anticipacion_max"))throw conflicto("La fecha supera la anticipación permitida");
        if(db.numero("SELECT count(*) FROM reservas_cubiculo WHERE persona_id=? AND estado IN ('CONFIRMADA','EN_USO') AND fecha_fin>?",persona,ahora())>=num(pol,"max_reservas"))throw conflicto("Se alcanzó el máximo de reservas activas");
        if(db.numero("SELECT count(*) FROM reservas_cubiculo WHERE (cubiculo_id=? OR persona_id=?) AND estado IN ('CONFIRMADA','EN_USO') AND fecha_inicio<? AND fecha_fin>?",cubiculo,persona,fin,inicio)>0)throw conflicto("El cubículo o la persona ya tienen una reserva en ese horario");
        if(db.numero("SELECT count(*) FROM bloqueos_cubiculo WHERE cubiculo_id=? AND estatus AND fecha_inicio<? AND fecha_fin>?",cubiculo,fin,inicio)>0)throw conflicto("El periodo tiene un bloqueo administrativo");horarioPara(inicio,fin);
        long id=db.insertar("reservas_cubiculo",m("fecha_reserva",ahora(),"fecha_inicio",inicio,"fecha_fin",fin,"estado","CONFIRMADA","observaciones",texto(d,"observaciones",2000),"condiciones_aplicadas",snapshot(pol),"persona_id",persona,"cubiculo_id",cubiculo,"politica_id",num(pol,"id")));
        auditar("RESERVAR_CUBICULO","M05","reservas_cubiculo",id,"Horario "+inicio+" → "+fin);avisar(persona,"NOTIFICACION_CUBICULO","CUBICULO:"+id,"Cubículo reservado",str(c,"nombre")+", "+inicio+" a "+fin,"reservas_cubiculo",id);return reservaCubiculo(id);
    }
    @Override @Transactional public Map<String,Object> accionCubiculo(long id,String accion,Map<String,Object>d) {
        db.bloquearOperacion();if(!"no-presentada".equals(accion))expirarCubiculos();Map<String,Object> r=registro("reservas_cubiculo",id,true);identidad.exigirPropietario(num(r,"persona_id"));String estado=str(r,"estado");
        switch(accion) {
            case "cancelar" -> {if(!"CONFIRMADA".equals(estado))throw conflicto("Solo se cancelan reservas confirmadas");String motivo=requerido(d,"motivo",2000);db.actualizar("reservas_cubiculo",id,m("estado","CANCELADA","fecha_cancelacion",ahora(),"motivo_cancelacion",motivo,"responsable_cancelacion_id",identidad.cuentaId()));}
            case "iniciar" -> {identidad.exigirGestion();if(!"CONFIRMADA".equals(estado))throw conflicto("Solo puede iniciar una reserva confirmada");LocalDateTime inicio=horaValor(r.get("fecha_inicio")),fin=horaValor(r.get("fecha_fin"));if(ahora().isBefore(inicio)||!ahora().isBefore(fin))throw conflicto("La asistencia debe registrarse dentro del periodo reservado");
                Map<String,Object> c=registro("cubiculos",num(r,"cubiculo_id"),true);if(!bool(c,"estatus",false)||!"HABILITADO".equals(str(c,"estado")))throw conflicto("El cubículo no está habilitado");
                if(db.numero("SELECT count(*) FROM usos_cubiculo u JOIN reservas_cubiculo rc ON rc.id=u.reserva_id WHERE rc.cubiculo_id=? AND u.fecha_fin IS NULL",num(r,"cubiculo_id"))>0)throw conflicto("El cubículo tiene un uso activo; primero registre su finalización");
                Map<String,Object> horario=horarioPara(inicio,fin);db.insertar("usos_cubiculo",m("fecha_inicio",ahora(),"estado","EN_USO","observaciones",texto(d,"observaciones",2000),"asistencia_confirmada",true,"reserva_id",id,"horario_biblioteca_id",num(horario,"id")));db.actualizar("reservas_cubiculo",id,m("estado","EN_USO"));}
            case "finalizar" -> {identidad.exigirGestion();if(!"EN_USO".equals(estado))throw conflicto("La reserva no está en uso");Map<String,Object> uso=uno("SELECT * FROM usos_cubiculo WHERE reserva_id=? FOR UPDATE",id);db.actualizar("usos_cubiculo",num(uso,"id"),m("fecha_fin",ahora(),"estado","FINALIZADO","observaciones",texto(d,"observaciones",2000)));db.actualizar("reservas_cubiculo",id,m("estado","FINALIZADA"));}
            case "no-presentada" -> {identidad.exigirGestion();if(!"CONFIRMADA".equals(estado))throw conflicto("La reserva ya no está confirmada");if(ahora().isBefore(limiteLlegada(r)))throw conflicto("Todavía no vence la tolerancia de llegada");noPresentada(r);}
            default -> throw error("Acción de cubículo desconocida");
        }
        auditar("CUBICULO_"+accion.toUpperCase(Locale.ROOT),"M05","reservas_cubiculo",id,str(d,"motivo"));avisar(num(r,"persona_id"),"NOTIFICACION_CUBICULO","CUBICULO:"+id+":"+accion,"Actualización de reserva de cubículo","Acción registrada: "+accion,"reservas_cubiculo",id);return reservaCubiculo(id);
    }
    private LocalDateTime limiteLlegada(Map<String,Object> r) {Map<String,Object> pol=condiciones(r,"politicas_reserva_cubiculo");return pol.get("tolerancia_llegada")==null?horaValor(r.get("fecha_fin")):horaValor(r.get("fecha_inicio")).plusMinutes(num(pol,"tolerancia_llegada"));}
    private void noPresentada(Map<String,Object> r) {db.actualizar("reservas_cubiculo",num(r,"id"),m("estado","NO_PRESENTADA"));long incidencia=db.insertar("incidencias",m("fecha",ahora(),"tipo","AUSENCIA_RESERVACION_CUBICULO","descripcion","No se registró asistencia a la reserva #"+num(r,"id"),"estado","PENDIENTE","motivo","Inasistencia después del plazo configurado","persona_id",num(r,"persona_id"),"responsable_id",identidad.cuentaId(),"reserva_cubiculo_id",num(r,"id")));aplicarPoliticaAutomatica(incidencia);avisar(num(r,"persona_id"),"NOTIFICACION_CUBICULO","CUBICULO:"+num(r,"id")+":NO_PRESENTADA","Reserva sin asistencia","La reserva quedó como no presentada al vencer el plazo de llegada.","reservas_cubiculo",num(r,"id"));}
    private void expirarCubiculos() {for(Map<String,Object> r:db.filas("SELECT * FROM reservas_cubiculo WHERE estado='CONFIRMADA' AND fecha_inicio<? FOR UPDATE",ahora()))if(ahora().isAfter(limiteLlegada(r)))noPresentada(r);}

    /** RF-M06-01/21/33/43/51/61/67/80; RN-M06-04/06/09/10/14/17/19/23/27/29/32. */
    @Override public List<Map<String,Object>> incidencias() {
        String filtro=identidad.esGestion()?"":" WHERE i.persona_id="+identidad.personaId();return db.filas("SELECT i.*,p.nombre||' '||p.apellidos persona_nombre FROM incidencias i JOIN personas p ON p.id=i.persona_id"+filtro+" ORDER BY i.id DESC");
    }
    @Override @Transactional public Map<String,Object> registrarIncidencia(Map<String,Object>d) {
        identidad.exigirGestion();db.bloquearOperacion();long persona=referenciaObligatoria(d,"persona_id","personas");long id=crearIncidencia(d,persona);auditar("REGISTRAR_INCIDENCIA","M06","incidencias",id,str(d,"motivo"));return registro("incidencias",id,false);
    }
    private long crearIncidencia(Map<String,Object>d,long persona) {
        String tipo=opcion(d,"tipo",tiposIncidencia(),null),motivo=requerido(d,"motivo",1000);Map<String,Object> v=m("fecha",ahora(),"tipo",tipo,"descripcion",requerido(d,"descripcion",2000),"estado","PENDIENTE","motivo",motivo,"observaciones",texto(d,"observaciones",3000),"evidencias",texto(d,"evidencias",5000),"persona_id",persona,"responsable_id",identidad.cuentaId());
        for(String[] referencia:List.of(new String[]{"ejemplar_id","ejemplares"},new String[]{"prestamo_id","prestamos"},new String[]{"detalle_prestamo_id","detalles_prestamo"},new String[]{"devolucion_id","devoluciones"},new String[]{"reserva_libro_id","reservas_libro"},new String[]{"reserva_cubiculo_id","reservas_cubiculo"},new String[]{"uso_cubiculo_id","usos_cubiculo"}))v.put(referencia[0],referencia(d,referencia[0],referencia[1]));
        if(v.get("prestamo_id")!=null)exigir(num(registro("prestamos",num(v,"prestamo_id"),false),"persona_id")==persona,"El préstamo corresponde a otra persona");
        if(v.get("detalle_prestamo_id")!=null){Map<String,Object> dp=registro("detalles_prestamo",num(v,"detalle_prestamo_id"),false);exigir(num(registro("prestamos",num(dp,"prestamo_id"),false),"persona_id")==persona,"El detalle corresponde a otra persona");if(v.get("ejemplar_id")!=null)exigir(num(dp,"ejemplar_id")==num(v,"ejemplar_id"),"El detalle y el ejemplar no corresponden");if(v.get("prestamo_id")!=null)exigir(num(dp,"prestamo_id")==num(v,"prestamo_id"),"El detalle y el préstamo no corresponden");}
        for(String tabla:List.of("reservas_libro","reservas_cubiculo")){String k=tabla.equals("reservas_libro")?"reserva_libro_id":"reserva_cubiculo_id";if(v.get(k)!=null)exigir(num(registro(tabla,num(v,k),false),"persona_id")==persona,"La reservación corresponde a otra persona");}
        if(v.get("devolucion_id")!=null){Map<String,Object> evento=uno("SELECT p.persona_id,dp.id detalle_id,dp.prestamo_id,dp.ejemplar_id FROM devoluciones d JOIN detalles_prestamo dp ON dp.id=d.detalle_prestamo_id JOIN prestamos p ON p.id=dp.prestamo_id WHERE d.id=?",num(v,"devolucion_id"));exigir(num(evento,"persona_id")==persona,"La devolución corresponde a otra persona");if(v.get("detalle_prestamo_id")!=null)exigir(num(v,"detalle_prestamo_id")==num(evento,"detalle_id"),"La devolución y el detalle no corresponden");if(v.get("prestamo_id")!=null)exigir(num(v,"prestamo_id")==num(evento,"prestamo_id"),"La devolución y el préstamo no corresponden");if(v.get("ejemplar_id")!=null)exigir(num(v,"ejemplar_id")==num(evento,"ejemplar_id"),"La devolución y el ejemplar no corresponden");}
        if(v.get("uso_cubiculo_id")!=null){Map<String,Object> evento=uno("SELECT r.persona_id,r.id reserva_id FROM usos_cubiculo u JOIN reservas_cubiculo r ON r.id=u.reserva_id WHERE u.id=?",num(v,"uso_cubiculo_id"));exigir(num(evento,"persona_id")==persona,"El uso de cubículo corresponde a otra persona");if(v.get("reserva_cubiculo_id")!=null)exigir(num(v,"reserva_cubiculo_id")==num(evento,"reserva_id"),"El uso y la reserva de cubículo no corresponden");}
        if(v.get("prestamo_id")!=null&&v.get("ejemplar_id")!=null)exigir(db.numero("SELECT count(*) FROM detalles_prestamo WHERE prestamo_id=? AND ejemplar_id=?",num(v,"prestamo_id"),num(v,"ejemplar_id"))>0,"El ejemplar no pertenece al préstamo seleccionado");
        boolean eventoIdentificado=List.of("ejemplar_id","prestamo_id","detalle_prestamo_id","devolucion_id","reserva_libro_id","reserva_cubiculo_id","uso_cubiculo_id").stream().anyMatch(k->v.get(k)!=null);
        long duplicados=db.numero("""
            SELECT count(*) FROM incidencias WHERE persona_id=? AND tipo=? AND motivo=?
            AND detalle_prestamo_id IS NOT DISTINCT FROM ?::bigint AND reserva_libro_id IS NOT DISTINCT FROM ?::bigint
            AND reserva_cubiculo_id IS NOT DISTINCT FROM ?::bigint AND ejemplar_id IS NOT DISTINCT FROM ?::bigint
            AND prestamo_id IS NOT DISTINCT FROM ?::bigint AND devolucion_id IS NOT DISTINCT FROM ?::bigint
            AND uso_cubiculo_id IS NOT DISTINCT FROM ?::bigint AND (?::boolean OR fecha::date=?)
            """,persona,tipo,motivo,v.get("detalle_prestamo_id"),v.get("reserva_libro_id"),v.get("reserva_cubiculo_id"),v.get("ejemplar_id"),v.get("prestamo_id"),v.get("devolucion_id"),v.get("uso_cubiculo_id"),eventoIdentificado,hoy());
        if(duplicados>0)throw conflicto("Ya se registró una incidencia del mismo evento y motivo");return db.insertar("incidencias",v);
    }
    private void registrarIncidenciaAutomatica(long persona,String tipo,long detalle,Long devolucion,long ejemplar,long prestamo,String descripcion) {
        if(db.numero("SELECT count(*) FROM incidencias WHERE tipo=? AND detalle_prestamo_id=?",tipo,detalle)>0){if(devolucion!=null)db.ejecutar("UPDATE incidencias SET devolucion_id=? WHERE tipo=? AND detalle_prestamo_id=? AND devolucion_id IS NULL",devolucion,tipo,detalle);return;}
        long id=crearIncidencia(m("tipo",tipo,"motivo",tipo+"_DEVOLUCION","descripcion",descripcion,"detalle_prestamo_id",detalle,"devolucion_id",devolucion,"ejemplar_id",ejemplar,"prestamo_id",prestamo),persona);aplicarPoliticaAutomatica(id);
    }
    private void aplicarPoliticaAutomatica(long incidencia) {
        Map<String,Object> i=registro("incidencias",incidencia,true);String tipo=tipoPersona(num(i,"persona_id"));
        List<Map<String,Object>> pols=db.filas("SELECT * FROM politicas_sancion WHERE estatus AND tipo_incidencia=? AND (tipo_usuario IS NULL OR tipo_usuario='' OR tipo_usuario=?) ORDER BY CASE WHEN tipo_usuario=? THEN 0 ELSE 1 END,id DESC",str(i,"tipo"),tipo,tipo);
        if(!pols.isEmpty())aplicarSancion(i,pols.getFirst());
    }
    @Override public List<Map<String,Object>> sanciones() {
        String filtro=identidad.esGestion()?"":" WHERE i.persona_id="+identidad.personaId();
        List<Map<String,Object>> filas=db.filas("SELECT s.*,i.persona_id,i.tipo,i.descripcion incidencia_descripcion,p.nombre||' '||p.apellidos persona_nombre,t.nombre tipo_sancion,t.codigo tipo_sancion_codigo FROM sanciones s JOIN incidencias i ON i.id=s.incidencia_id JOIN personas p ON p.id=i.persona_id JOIN tipos_sancion t ON t.id=s.tipo_sancion_id"+filtro+" ORDER BY s.id DESC");filas.forEach(this::detallarSancion);return filas;
    }
    private Map<String,Object> sancion(long id) {Map<String,Object> s=uno("SELECT s.*,i.persona_id,i.tipo,i.descripcion incidencia_descripcion,p.nombre||' '||p.apellidos persona_nombre,t.nombre tipo_sancion,t.codigo tipo_sancion_codigo FROM sanciones s JOIN incidencias i ON i.id=s.incidencia_id JOIN personas p ON p.id=i.persona_id JOIN tipos_sancion t ON t.id=s.tipo_sancion_id WHERE s.id=?",id);identidad.exigirPropietario(num(s,"persona_id"));detallarSancion(s);return s;}
    private void detallarSancion(Map<String,Object>s) {long id=num(s,"id");Map<String,Object> multa=db.primero("SELECT * FROM multas WHERE sancion_id=?",id).orElse(null);if(multa!=null)multa.put("pagos",db.filas("SELECT * FROM pagos_multa WHERE multa_id=? ORDER BY id",num(multa,"id")));s.put("multa",multa);s.put("saldo_pendiente",multa==null?BigDecimal.ZERO:multa.get("saldo_pendiente"));s.put("restricciones",db.filas("SELECT * FROM restricciones_servicio WHERE sancion_id=? ORDER BY id",id));s.put("reposiciones",db.filas("SELECT r.*,e.codigo_barras FROM reposiciones_material r JOIN ejemplares e ON e.id=r.ejemplar_id WHERE r.sancion_id=? ORDER BY r.id",id));s.put("historial",db.filas("SELECT * FROM historiales_sancion WHERE sancion_id=? ORDER BY id",id));}
    @Override @Transactional public Map<String,Object> sancionar(Map<String,Object>d) {
        identidad.exigirGestion();db.bloquearOperacion();long politica=positivo(d,"politica_id");Map<String,Object> pol=registro("politicas_sancion",politica,true);long incidencia;
        if(d.get("incidencia_id")!=null)incidencia=positivo(d,"incidencia_id");else {long persona=referenciaObligatoria(d,"persona_id","personas");incidencia=crearIncidencia(d,persona);}
        return sancion(aplicarSancion(registro("incidencias",incidencia,true),pol));
    }
    private long aplicarSancion(Map<String,Object> i,Map<String,Object> pol) {
        long persona=num(i,"persona_id");String tipo=tipoPersona(persona);if(!bool(pol,"estatus",false))throw conflicto("La política de sanción está inactiva");
        exigir(str(pol,"tipo_incidencia").equals(str(i,"tipo")),"La política no corresponde al tipo de incidencia");String tipoPol=str(pol,"tipo_usuario");exigir(tipoPol.isBlank()||tipoPol.equals(tipo),"La política no corresponde al tipo de usuario");
        if(db.numero("SELECT count(*) FROM sanciones WHERE incidencia_id=? AND politica_id=? AND estado<>'CANCELADA'",num(i,"id"),num(pol,"id"))>0)throw conflicto("Esta política ya fue aplicada a la incidencia");
        long dias=pol.get("dias_suspension")==null?0:num(pol,"dias_suspension");LocalDate fin=dias>0?hoy().plusDays(dias):null;
        String condiciones=condicionesSancion(pol);
        long id=db.insertar("sanciones",m("fecha",ahora(),"descripcion",str(i,"descripcion"),"estado","ACTIVA","motivo",str(i,"motivo"),"condiciones_cumplimiento",condiciones,"fecha_fin",fin,"incidencia_id",num(i,"id"),"tipo_sancion_id",num(pol,"tipo_sancion_id"),"politica_id",num(pol,"id"),"responsable_id",identidad.cuentaId()));
        BigDecimal monto=pol.get("monto_multa")==null?BigDecimal.ZERO:decimal(pol.get("monto_multa"));
        if(monto.signum()>0){if(!bool(pol,"multas_autorizadas",false))throw conflicto("La política no autoriza multas económicas");db.insertar("multas",m("monto",monto,"estado","PENDIENTE","fecha_generacion",ahora(),"saldo_pendiente",monto,"sancion_id",id));}
        for(String servicio:csv(str(pol,"servicios_restringidos")))db.insertar("restricciones_servicio",m("fecha_inicio",hoy(),"fecha_fin",fin,"descripcion",str(i,"descripcion"),"estado","ACTIVA","servicio_afectado",servicio,"condiciones_levantamiento",str(pol,"reglas"),"temporal",fin!=null,"sancion_id",id));
        Map<String,Object> tipoS=registro("tipos_sancion",num(pol,"tipo_sancion_id"),false);
        if(esReposicion(tipoS)){
            if(i.get("ejemplar_id")==null)throw conflicto("La reposición requiere identificar el ejemplar afectado");db.insertar("reposiciones_material",m("descripcion",str(i,"descripcion"),"estado","PENDIENTE","condiciones",str(pol,"reglas"),"sancion_id",id,"ejemplar_id",num(i,"ejemplar_id")));}
        db.actualizar("incidencias",num(i,"id"),m("estado","SANCIONADA"));historialSancion(id,null,"ACTIVA","Aplicación de política #"+num(pol,"id"));
        auditar("APLICAR_SANCION","M06","sanciones",id,"Incidencia #"+num(i,"id"));avisar(persona,"NOTIFICACION_SANCION","SANCION:"+id,"Sanción registrada","Consulta los motivos, condiciones y obligaciones de la sanción #"+id,"sanciones",id);return id;
    }
    @Override @Transactional public Map<String,Object> accionSancion(long id,String accion,Map<String,Object>d) {
        identidad.exigirGestion();db.bloquearOperacion();Map<String,Object> s=registro("sanciones",id,true);String anterior=str(s,"estado");if(Set.of("RESUELTA","CANCELADA").contains(anterior))throw conflicto("La sanción ya está cerrada");
        Map<String,Object> i=registro("incidencias",num(s,"incidencia_id"),false);
        switch(accion) {
            case "pago" -> {Map<String,Object> multa=uno("SELECT * FROM multas WHERE sancion_id=? FOR UPDATE",id);BigDecimal monto=dineroOpcional(d,"monto");exigir(monto!=null&&monto.signum()>0,"El pago debe ser mayor que cero");BigDecimal saldo=decimal(multa.get("saldo_pendiente"));if(monto.compareTo(saldo)>0)throw conflicto("El pago supera el saldo pendiente de "+saldo);
                Map<String,Object> condiciones;try{condiciones=json.readValue(str(s,"condiciones_cumplimiento"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}catch(JsonProcessingException e){condiciones=registro("politicas_sancion",num(s,"politica_id"),false);}
                if(monto.compareTo(saldo)<0&&!bool(condiciones,"permite_pagos_parciales",false))throw conflicto("Las condiciones originales no permiten pagos parciales");
                long pago=db.insertar("pagos_multa",m("fecha_pago",ahora(),"monto",monto,"metodo",requerido(d,"metodo",100),"comprobante",texto(d,"comprobante",1000),"referencia",texto(d,"referencia",250),"multa_id",num(multa,"id"),"responsable_id",identidad.cuentaId()));BigDecimal restante=saldo.subtract(monto);db.actualizar("multas",num(multa,"id"),m("saldo_pendiente",restante,"estado",restante.signum()==0?"PAGADA":"PARCIAL"));marcarCumplidaSiProcede(s,"Pago #"+pago);
                avisar(num(i,"persona_id"),"NOTIFICACION_PAGO","PAGO:"+pago,"Pago registrado","Se registró un pago de $"+monto+". Saldo: $"+restante,"pagos_multa",pago);}
            case "reposicion" -> {Map<String,Object> r=uno("SELECT * FROM reposiciones_material WHERE sancion_id=? AND estado='PENDIENTE' ORDER BY id LIMIT 1 FOR UPDATE",id);long reemplazo=referenciaObligatoria(d,"ejemplar_reposicion_id","ejemplares");Map<String,Object> original=registro("ejemplares",num(r,"ejemplar_id"),true),nuevo=registro("ejemplares",reemplazo,true);exigir(reemplazo!=num(original,"id"),"Indique un ejemplar de reposición distinto al original");exigir(num(original,"libro_id")==num(nuevo,"libro_id"),"La reposición debe corresponder a la misma edición bibliográfica");if(!"DISPONIBLE".equals(str(nuevo,"estado"))||!bool(nuevo,"estatus",false))throw conflicto("La reposición debe ser un ejemplar disponible y activo");db.actualizar("reposiciones_material",num(r,"id"),m("estado","VALIDADA","fecha_cumplimiento",hoy(),"fecha_validacion",ahora(),"ejemplar_reposicion_id",reemplazo,"validador_id",identidad.cuentaId(),"material_reemplazo",str(nuevo,"codigo_barras")+" "+str(d,"observaciones")));marcarCumplidaSiProcede(s,"Reposición validada");}
            case "resolver" -> {String motivo=requerido(d,"motivo",2000);verificarCumplimiento(s);db.actualizar("sanciones",id,m("estado","RESUELTA","fecha_resolucion",ahora()));db.ejecutar("UPDATE restricciones_servicio SET estado='LEVANTADA' WHERE sancion_id=?",id);historialSancion(id,anterior,"RESUELTA",motivo);}
            case "cancelar" -> {String motivo=requerido(d,"motivo",2000);db.actualizar("sanciones",id,m("estado","CANCELADA","fecha_resolucion",ahora(),"motivo_cancelacion",motivo,"responsable_cancelacion_id",identidad.cuentaId()));db.ejecutar("UPDATE restricciones_servicio SET estado='LEVANTADA' WHERE sancion_id=?",id);db.ejecutar("UPDATE multas SET estado='CANCELADA',saldo_pendiente=0 WHERE sancion_id=?",id);db.ejecutar("UPDATE reposiciones_material SET estado='CANCELADA' WHERE sancion_id=? AND estado='PENDIENTE'",id);historialSancion(id,anterior,"CANCELADA",motivo);}
            default -> throw error("Acción de sanción desconocida");
        }
        auditar("SANCION_"+accion.toUpperCase(Locale.ROOT),"M06","sanciones",id,str(d,"motivo"));return sancion(id);
    }
    private void verificarCumplimiento(Map<String,Object>s) {long id=num(s,"id");if(db.numero("SELECT count(*) FROM multas WHERE sancion_id=? AND saldo_pendiente>0",id)>0)throw conflicto("La multa aún tiene saldo pendiente");if(db.numero("SELECT count(*) FROM reposiciones_material WHERE sancion_id=? AND estado='PENDIENTE'",id)>0)throw conflicto("La reposición aún no ha sido validada");if(s.get("fecha_fin")!=null&&hoy().isBefore(fechaValor(s.get("fecha_fin"))))throw conflicto("Todavía no termina el periodo de suspensión");}
    private void marcarCumplidaSiProcede(Map<String,Object>s,String motivo) {try{verificarCumplimiento(s);}catch(ResponseStatusException ex){return;}if("ACTIVA".equals(str(s,"estado"))){db.actualizar("sanciones",num(s,"id"),m("estado","CUMPLIDA"));db.ejecutar("UPDATE restricciones_servicio SET estado='LEVANTADA' WHERE sancion_id=?",num(s,"id"));historialSancion(num(s,"id"),"ACTIVA","CUMPLIDA",motivo);}}
    private void historialSancion(long id,String anterior,String nuevo,String obs) {db.insertar("historiales_sancion",m("fecha",ahora(),"estado_anterior",anterior,"estado_nuevo",nuevo,"observaciones",obs,"sancion_id",id,"responsable_id",identidad.cuentaId()));}
    private boolean esReposicion(Map<String,Object> tipo){return str(tipo,"codigo").toUpperCase(Locale.ROOT).contains("REPOSICION");}
    private String condicionesSancion(Map<String,Object> pol) {String s=snapshot(m("reglas",str(pol,"reglas"),"permite_pagos_parciales",bool(pol,"permite_pagos_parciales",false),"multas_autorizadas",bool(pol,"multas_autorizadas",false),"monto_multa",pol.get("monto_multa"),"dias_suspension",pol.get("dias_suspension"),"servicios_restringidos",pol.get("servicios_restringidos")));exigir(s.length()<=3000,"Simplifique las reglas: las condiciones completas deben caber en el límite de 3000 caracteres del modelo de sanción");return s;}

    private Map<String,Object> uno(String sql,Object... args) {return db.primero(sql,args).orElseThrow(()->no("Registro no encontrado"));}
    private Map<String,Object> registro(String tabla,long id,boolean lock) {return uno("SELECT * FROM "+tabla+" WHERE id=?"+(lock?" FOR UPDATE":""),id);}
    private void vincular(String tabla,String padre,long id,String hijo,String destino,List<Long> ids) {
        for(long x:ids)registro(destino,x,false);db.ejecutar("DELETE FROM "+tabla+" WHERE "+padre+"=?",id);
        for(long x:ids)db.ejecutar("INSERT INTO "+tabla+" ("+padre+","+hijo+") VALUES (?,?)",id,x);
    }
    private void auditar(String accion,String modulo,String entidad,long id,String motivo) {db.insertar("bitacoras_auditoria",m("fecha",ahora(),"accion",accion,"modulo",modulo,"entidad",entidad,"id_registro",id,"resultado","CORRECTO","motivo",motivo,"operacion_sistema",false,"responsable_id",identidad.cuentaId()));}
    private void avisar(long persona,String tipo,String evento,String asunto,String mensaje,String entidad,long id) {apoyo.notificar(persona,tipo,evento,asunto,mensaje,entidad,id);}
    private void historialEjemplar(long id,String anterior,String nuevo,String obs) {if(Objects.equals(anterior,nuevo))return;db.insertar("historiales_estado_ejemplar",m("ejemplar_id",id,"fecha",ahora(),"estado_anterior",anterior,"estado_nuevo",nuevo,"observaciones",obs,"responsable_id",identidad.cuentaId()));}
    private void estadoEjemplar(long id,String nuevo,String motivo) {Map<String,Object> e=registro("ejemplares",id,true);db.actualizar("ejemplares",id,m("estado",nuevo));historialEjemplar(id,str(e,"estado"),nuevo,motivo);}
    private String snapshot(Map<String,Object> politica) {try{return json.writeValueAsString(politica);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private Map<String,Object> condiciones(Map<String,Object> registro,String tabla) {
        String s=str(registro,"condiciones_aplicadas");if(!s.isBlank())try{return json.readValue(s,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}catch(JsonProcessingException ignored){ }
        return registro(tabla,num(registro,"politica_id"),false);
    }
    private LocalDate hoy(){return LocalDate.now(ZONA);} private LocalDateTime ahora(){return LocalDateTime.now(ZONA);}
    private static Map<String,Object> m(Object... pares){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<pares.length;i+=2)r.put(pares[i].toString(),pares[i+1]);return r;}
    private static String str(Map<String,Object>d,String k){Object v=d.get(k);return v==null?"":v.toString();}
    private static long num(Map<String,Object>d,String k){Object v=d.get(k);try{return v instanceof Number?new BigDecimal(v.toString()).longValueExact():Long.parseLong(str(d,k));}catch(RuntimeException e){throw error("Falta o es inválido "+k+"; debe ser un entero");}}
    private static long positivo(Map<String,Object>d,String k){long v=num(d,k);exigir(v>0,k+" debe ser positivo");return v;}
    private static String texto(Map<String,Object>d,String k,int max){String s=str(d,k).strip();exigir(s.length()<=max,k+" supera "+max+" caracteres");return s.isEmpty()?null:s;}
    private static String requerido(Map<String,Object>d,String k,int max){String s=texto(d,k,max);exigir(s!=null,"Indique "+k);return s;}
    private static boolean bool(Map<String,Object>d,String k,boolean defecto){Object v=d.get(k);if(v==null)return defecto;if(v instanceof Boolean b)return b;if("true".equalsIgnoreCase(v.toString()))return true;if("false".equalsIgnoreCase(v.toString()))return false;throw error(k+" debe ser booleano");}
    private static int entero(Map<String,Object>d,String k,int min,int max){long n=num(d,k);exigir(n>=min&&n<=max,k+" debe estar entre "+min+" y "+max);return(int)n;}
    private static Integer enteroOpcional(Map<String,Object>d,String k,int min,int max){return d.get(k)==null||str(d,k).isBlank()?null:entero(d,k,min,max);}
    private static List<Long> ids(Map<String,Object>d,String k){Object v=d.get(k);if(v==null)return List.of();if(!(v instanceof List<?> list))throw error(k+" debe ser una lista");List<Long> r=new ArrayList<>();for(Object x:list){long n=num(m("x",x),"x");exigir(n>0,"Identificador inválido");if(r.contains(n))throw error("No repita identificadores en "+k);r.add(n);}return r;}
    private static String opcion(Map<String,Object>d,String k,Set<String> permitidos,String defecto){String s=str(d,k).strip().toUpperCase(Locale.ROOT);if(s.isEmpty())s=defecto;exigir(s!=null&&(permitidos.contains(s)||Objects.equals(s,defecto)),k+" inválido");return s;}
    private static LocalDate fecha(Map<String,Object>d,String k,LocalDate defecto){if(str(d,k).isBlank())return defecto;try{return LocalDate.parse(str(d,k));}catch(RuntimeException e){throw error(k+" debe ser una fecha ISO (AAAA-MM-DD)");}}
    private static LocalDateTime fechaHora(Map<String,Object>d,String k){try{return LocalDateTime.parse(requerido(d,k,40));}catch(RuntimeException e){throw error(k+" debe ser fecha y hora ISO local");}}
    private static LocalDate fechaValor(Object v){if(v instanceof LocalDate d)return d;return LocalDate.parse(v.toString());}
    private static LocalDateTime horaValor(Object v){if(v instanceof LocalDateTime d)return d;return LocalDateTime.parse(v.toString());}
    private static BigDecimal decimal(Object v){try{return new BigDecimal(v.toString());}catch(RuntimeException e){throw error("Importe inválido");}}
    private static BigDecimal dineroOpcional(Map<String,Object>d,String k){if(d.get(k)==null||str(d,k).isBlank())return null;BigDecimal n=decimal(d.get(k));exigir(n.signum()>=0&&n.scale()<=2&&n.compareTo(new BigDecimal("9999999999.99"))<=0,k+" debe ser importe positivo con dos decimales dentro del límite de almacenamiento");return n;}
    private static List<String> csv(String s){return Arrays.stream(s.toUpperCase(Locale.ROOT).split(",")).map(String::strip).filter(x->!x.isBlank()).distinct().toList();}
    private static String tiposPermitidos(Map<String,Object>d){String s=requerido(d,"tipos_usuario_permitidos",1000);List<String> valores=csv(s);exigir(!valores.isEmpty()&&TIPOS.containsAll(valores),"Tipos de usuario no válidos");return String.join(",",valores);}
    private Long referencia(Map<String,Object>d,String k,String tabla){if(d.get(k)==null||str(d,k).isBlank())return null;long id=positivo(d,k);registro(tabla,id,false);return id;}
    private long referenciaObligatoria(Map<String,Object>d,String k,String tabla){long id=positivo(d,k);registro(tabla,id,false);return id;}
    private static void exigir(boolean ok,String mensaje){if(!ok)throw error(mensaje);}
    private static ResponseStatusException error(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static ResponseStatusException conflicto(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private static ResponseStatusException no(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    private static Set<String> tiposIncidencia(){return Set.of("RETRASO","DANO","EXTRAVIO","INCUMPLIMIENTO_RESERVACION","AUSENCIA_RESERVACION_CUBICULO","USO_INADECUADO_CUBICULO");}
}
