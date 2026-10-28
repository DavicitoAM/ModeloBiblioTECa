package mx.edu.itspa.biblioteca.services.impl;

import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import mx.edu.itspa.biblioteca.services.PersonasServicio;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.*;

/** M01, PDF pp. impresas 8–14 / físicas 9–15; RF-M01-01..30/38..46,
 * RN-M01-01..12/17..22. Mantiene JOINED y las FK originales, sin borrados físicos. */
@Service
@Transactional
public class PersonasServicioImpl implements PersonasServicio {
    private final JdbcTemplate jdbc;
    private final IdentidadActual identidad;
    private final PasswordEncoder encoder;
    public PersonasServicioImpl(JdbcTemplate jdbc, IdentidadActual identidad, PasswordEncoder encoder) {
        this.jdbc=jdbc; this.identidad=identidad; this.encoder=encoder;
    }
    private static final String PERSONAS = """
        SELECT p.*, CASE WHEN e.id IS NOT NULL THEN 'ESTUDIANTE' WHEN d.id IS NOT NULL THEN 'DOCENTE'
        WHEN em.id IS NOT NULL THEN 'EMPLEADO' WHEN ex.id IS NOT NULL THEN 'EXTERNO' ELSE 'PERSONA' END tipo,
        e.matricula,e.semestre,e.estado_academico,e.carrera_id,c.nombre carrera,
        COALESCE(d.departamento_id,em.departamento_id) departamento_id,
        dep.nombre departamento,em.area_trabajo,em.puesto,ex.institucion,cu.id cuenta_id,
        EXISTS(SELECT 1 FROM registros_visita v WHERE v.persona_id=p.id AND v.fecha_salida IS NULL) dentro
        FROM personas p LEFT JOIN estudiantes e ON e.id=p.id LEFT JOIN carreras c ON c.id=e.carrera_id
        LEFT JOIN docentes d ON d.id=p.id LEFT JOIN empleados em ON em.id=p.id
        LEFT JOIN departamentos dep ON dep.id=COALESCE(d.departamento_id,em.departamento_id)
        LEFT JOIN usuarios_externos ex ON ex.id=p.id LEFT JOIN cuentas_usuario cu ON cu.persona_id=p.id
        """;
    @Override @Transactional(readOnly=true)
    public List<Map<String,Object>> personas(String busqueda) {
        String texto = "%" + (busqueda == null ? "" : busqueda.trim()) + "%";
        if (!identidad.esGestion()) return List.of(persona(identidad.personaId()));
        return jdbc.queryForList(PERSONAS + " WHERE COALESCE(p.tipo_identificacion,'')<>'IDENTIDAD_TECNICA' AND concat_ws(' ',p.nombre,p.apellidos,p.correo,p.numero_empleado,e.matricula) ILIKE ? ORDER BY p.apellidos,p.nombre", texto);
    }
    @Override @Transactional(readOnly=true)
    public Map<String,Object> persona(Long id) {
        identidad.exigirPropietario(id);
        return uno(PERSONAS + " WHERE p.id=?", id);
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> guardarPersona(Long id, PersonaSolicitud s) {
        String tipo = tipo(s.tipo());
        validarPersona(s,tipo);
        if(id != null) {
            Map<String,Object> previa=uno(PERSONAS+" WHERE p.id=?", id);
            if("IDENTIDAD_TECNICA".equals(previa.get("tipo_identificacion"))) conflicto("La identidad técnica de operación no es un registro de persona editable.");
            if(!previa.get("tipo").equals(tipo)) conflicto("El tipo de persona conserva su identidad; no puede cambiarse desde edición.");
            jdbc.queryForObject("SELECT id FROM personas WHERE id=? FOR UPDATE",Long.class,id);
        }
        if(s.numero_empleado()!=null && existe("SELECT count(*) FROM personas WHERE numero_empleado=? AND id<>?", s.numero_empleado().trim(), id==null?0L:id)) conflicto("Ese número de empleado ya pertenece a otra persona.");
        if(tipo.equals("ESTUDIANTE") && existe("SELECT count(*) FROM estudiantes WHERE matricula=? AND id<>?",s.matricula().trim(),id==null?0L:id)) conflicto("Esa matrícula ya está registrada.");
        boolean activa=s.estatus()==null || s.estatus();
        if(!activa && id!=null && id.equals(identidad.personaId())) conflicto("No puedes desactivar tu propio registro durante la sesión.");
        if(!activa && id!=null) protegerUltimoAdminPersona(id);
        if(id==null) {
            id=jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,correo,telefono,fecha_nacimiento,fecha_registro,tipo_identificacion,numero_identificacion,numero_empleado,estatus) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?,?,?,?) RETURNING id",Long.class,s.nombre().trim(),s.apellidos().trim(),vacio(s.correo()),vacio(s.telefono()),s.fecha_nacimiento(),vacio(s.tipo_identificacion()),vacio(s.numero_identificacion()),vacio(s.numero_empleado()),activa);
            switch(tipo) {
                case "ESTUDIANTE" -> jdbc.update("INSERT INTO estudiantes(id,matricula,semestre,estado_academico,carrera_id) VALUES(?,?,?,?,?)",id,s.matricula().trim(),s.semestre(),vacio(s.estado_academico()),s.carrera_id());
                case "DOCENTE" -> jdbc.update("INSERT INTO docentes(id,departamento_id) VALUES(?,?)",id,s.departamento_id());
                case "EMPLEADO" -> jdbc.update("INSERT INTO empleados(id,area_trabajo,puesto,departamento_id) VALUES(?,?,?,?)",id,s.area_trabajo().trim(),vacio(s.puesto()),s.departamento_id());
                case "EXTERNO" -> jdbc.update("INSERT INTO usuarios_externos(id,institucion) VALUES(?,?)",id,vacio(s.institucion()));
                default -> { }
            }
        } else {
            jdbc.update("UPDATE personas SET nombre=?,apellidos=?,correo=?,telefono=?,fecha_nacimiento=?,tipo_identificacion=?,numero_identificacion=?,numero_empleado=?,estatus=? WHERE id=?",s.nombre().trim(),s.apellidos().trim(),vacio(s.correo()),vacio(s.telefono()),s.fecha_nacimiento(),vacio(s.tipo_identificacion()),vacio(s.numero_identificacion()),vacio(s.numero_empleado()),activa,id);
            switch(tipo) {
                case "ESTUDIANTE" -> jdbc.update("UPDATE estudiantes SET matricula=?,semestre=?,estado_academico=?,carrera_id=? WHERE id=?",s.matricula().trim(),s.semestre(),vacio(s.estado_academico()),s.carrera_id(),id);
                case "DOCENTE" -> jdbc.update("UPDATE docentes SET departamento_id=? WHERE id=?",s.departamento_id(),id);
                case "EMPLEADO" -> jdbc.update("UPDATE empleados SET area_trabajo=?,puesto=?,departamento_id=? WHERE id=?",s.area_trabajo().trim(),vacio(s.puesto()),s.departamento_id(),id);
                case "EXTERNO" -> jdbc.update("UPDATE usuarios_externos SET institucion=? WHERE id=?",vacio(s.institucion()),id);
                default -> { }
            }
        }
        auditar("GUARDAR_PERSONA","Persona",id,"Registro " + tipo);
        return uno(PERSONAS+" WHERE p.id=?",id);
    }
    private void validarPersona(PersonaSolicitud s,String tipo) {
        if("IDENTIDAD_TECNICA".equalsIgnoreCase(s.tipo_identificacion())) invalido("Ese identificador está reservado para la operación técnica del sistema.");
        if(s.fecha_nacimiento()!=null && s.fecha_nacimiento().isAfter(LocalDate.now())) invalido("La fecha de nacimiento no puede estar en el futuro.");
        if(tipo.equals("ESTUDIANTE")) {
            requerido(s.matricula(),"Matrícula"); activo("carreras",s.carrera_id());
        }
        if(tipo.equals("DOCENTE") || tipo.equals("EMPLEADO")) requerido(s.numero_empleado(),"Número de empleado");
        if(tipo.equals("DOCENTE")) activo("departamentos",s.departamento_id());
        if(tipo.equals("EMPLEADO")) { requerido(s.area_trabajo(),"Área de trabajo"); if(s.departamento_id()!=null) activo("departamentos",s.departamento_id()); }
    }
    @Override @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Map<String,Object>> cuentas() {
        List<Map<String,Object>> filas=jdbc.queryForList("SELECT cu.id,cu.persona_id,cu.usuario,cu.fecha_creacion,cu.ultimo_acceso,cu.fecha_verificacion_correo,cu.estado_verificacion,cu.estatus,p.nombre,p.apellidos FROM cuentas_usuario cu JOIN personas p ON p.id=cu.persona_id WHERE cu.usuario<>'SISTEMA_LOCAL' ORDER BY cu.usuario");
        filas.forEach(c->c.put("roles",rolesCuenta(((Number)c.get("id")).longValue())));
        return filas;
    }
    @Override @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String,Object> guardarCuenta(Long id,CuentaSolicitud s) {
        jdbc.queryForList("SELECT id FROM cuentas_usuario ORDER BY id FOR UPDATE");
        if(id!=null&&existe("SELECT count(*) FROM cuentas_usuario WHERE id=? AND usuario='SISTEMA_LOCAL'",id)) conflicto("La cuenta técnica de operación no admite edición ni credenciales humanas.");
        activo("personas",s.persona_id());
        String usuario=usuario(s.usuario());
        if(existe("SELECT count(*) FROM cuentas_usuario WHERE lower(usuario)=lower(?) AND id<>?",usuario,id==null?0L:id)) conflicto("El nombre de usuario ya está registrado.");
        if(existe("SELECT count(*) FROM cuentas_usuario WHERE persona_id=? AND id<>?",s.persona_id(),id==null?0L:id)) conflicto("La persona ya tiene una cuenta.");
        List<String> roles=s.roles().stream().map(String::toUpperCase).distinct().toList();
        if(roles.isEmpty()) invalido("Asigna al menos un rol.");
        List<Long> idsRoles=new ArrayList<>();
        for(String rol:roles) idsRoles.add(unoId("SELECT id FROM roles WHERE nombre=? AND estatus",rol));
        boolean activa=s.estatus()==null||s.estatus();
        String verificacion=s.estado_verificacion()==null?"PENDIENTE":s.estado_verificacion().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("PENDIENTE","VERIFICADA","RECHAZADA").contains(verificacion)) invalido("Estado de verificación inválido.");
        if((roles.contains("ADMINISTRADOR")||roles.contains("BIBLIOTECARIO"))&&!verificacion.equals("VERIFICADA")) invalido("Completa la revisión institucional antes de habilitar un rol de administración o biblioteca.");
        if(id!=null) {
            Map<String,Object> previa=uno("SELECT id,persona_id FROM cuentas_usuario WHERE id=?",id);
            if(!Objects.equals(((Number)previa.get("persona_id")).longValue(),s.persona_id())) conflicto("Una cuenta no puede transferirse a otra persona.");
            if(rolesCuenta(id).contains("ADMINISTRADOR") && (!activa||!roles.contains("ADMINISTRADOR"))) protegerUltimoAdminCuenta(id);
            if(id.equals(identidad.cuentaId()) && (!activa||!roles.contains("ADMINISTRADOR"))) conflicto("No puedes quitarte el acceso administrativo durante tu sesión.");
            jdbc.update("UPDATE cuentas_usuario SET usuario=?,estatus=?,estado_verificacion=? WHERE id=?",usuario,activa,verificacion,id);
            if(s.password()!=null&&!s.password().isBlank()) { validarPassword(s.password()); jdbc.update("UPDATE cuentas_usuario SET contrasena_hash=? WHERE id=?",encoder.encode(s.password()),id); }
            jdbc.update("DELETE FROM cuentas_roles WHERE cuenta_id=?",id);
        } else {
            validarPassword(s.password());
            id=jdbc.queryForObject("INSERT INTO cuentas_usuario(persona_id,usuario,contrasena_hash,fecha_creacion,estatus,estado_verificacion) VALUES(?,?,?,CURRENT_TIMESTAMP,?,?) RETURNING id",Long.class,s.persona_id(),usuario,encoder.encode(s.password()),activa,verificacion);
        }
        for(Long rolId:idsRoles) jdbc.update("INSERT INTO cuentas_roles(cuenta_id,rol_id) VALUES(?,?)",id,rolId);
        auditar("GUARDAR_CUENTA","CuentaUsuario",id,"Roles: "+String.join(", ",roles));
        final Long cuentaId=id;
        return cuentas().stream().filter(c->((Number)c.get("id")).longValue()==cuentaId).findFirst().orElseThrow();
    }
    public List<String> rolesCuenta(Long id) { return jdbc.queryForList("SELECT r.nombre FROM roles r JOIN cuentas_roles cr ON cr.rol_id=r.id WHERE cr.cuenta_id=? AND r.estatus ORDER BY r.nombre",String.class,id); }
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public List<Map<String,Object>> roles() {
        var rows=jdbc.queryForList("SELECT id,nombre,descripcion,estatus FROM roles ORDER BY nombre");
        rows.forEach(r->r.put("permisos_ids",jdbc.queryForList("SELECT permiso_id FROM roles_permisos WHERE rol_id=? ORDER BY permiso_id",Long.class,r.get("id"))));
        return rows;
    }
    @Override @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String,Object> guardarRol(Long id,RolSolicitud s) {
        String nombre=s.nombre().trim().toUpperCase(Locale.ROOT);
        if(!nombre.matches("[A-Z][A-Z0-9_]{2,79}")) invalido("El rol requiere letras mayúsculas, números y guion bajo.");
        if(id!=null) {
            Map<String,Object> anterior=uno("SELECT * FROM roles WHERE id=? FOR UPDATE",id);
            if(Set.of("ADMINISTRADOR","BIBLIOTECARIO","LECTOR").contains(anterior.get("nombre")) && (!anterior.get("nombre").equals(nombre)||Boolean.FALSE.equals(s.estatus()))) conflicto("Los tres roles operativos no se pueden renombrar ni desactivar.");
        }
        if(existe("SELECT count(*) FROM roles WHERE nombre=? AND id<>?",nombre,id==null?0L:id)) conflicto("El rol ya existe.");
        if(id==null) id=jdbc.queryForObject("INSERT INTO roles(nombre,descripcion,estatus) VALUES(?,?,?) RETURNING id",Long.class,nombre,vacio(s.descripcion()),s.estatus()==null||s.estatus());
        else jdbc.update("UPDATE roles SET nombre=?,descripcion=?,estatus=? WHERE id=?",nombre,vacio(s.descripcion()),s.estatus()==null||s.estatus(),id);
        if(s.permisos_ids()!=null) {
            jdbc.update("DELETE FROM roles_permisos WHERE rol_id=?",id);
            for(Long permiso:new HashSet<>(s.permisos_ids())) {activo("permisos",permiso);jdbc.update("INSERT INTO roles_permisos(rol_id,permiso_id) VALUES(?,?)",id,permiso);}
        }
        auditar("GUARDAR_ROL","Rol",id,nombre);
        return uno("SELECT id,nombre,descripcion,estatus FROM roles WHERE id=?",id);
    }
    @Override @Transactional(readOnly=true)
    public Map<String,Object> catalogos() {
        Map<String,Object> resultado=new LinkedHashMap<>();
        for(String t:List.of("carreras","departamentos","motivos_visita")) resultado.put(t,jdbc.queryForList("SELECT * FROM "+t+" ORDER BY nombre"));
        if(identidad.tieneRol("ADMINISTRADOR")) resultado.put("permisos",jdbc.queryForList("SELECT id,nombre,descripcion,estatus FROM permisos ORDER BY nombre"));
        return resultado;
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> guardarCatalogo(String tipo,Long id,CatalogoSolicitud s) {
        if(!Set.of("carreras","departamentos","motivos_visita","permisos").contains(tipo)) invalido("Catálogo de personas no válido.");
        if(tipo.equals("permisos")) identidad.exigirAdministrador();
        if(tipo.equals("motivos_visita")&&s.nombre().length()>100) invalido("El motivo admite hasta 100 caracteres.");
        String nombre=s.nombre().trim();
        if(existe("SELECT count(*) FROM "+tipo+" WHERE lower(nombre)=lower(?) AND id<>?",nombre,id==null?0L:id)) conflicto("Ese nombre ya existe.");
        boolean descripcion=tipo.equals("motivos_visita")||tipo.equals("permisos");
        if(id==null) id=descripcion?jdbc.queryForObject("INSERT INTO "+tipo+"(nombre,descripcion,estatus) VALUES(?,?,?) RETURNING id",Long.class,nombre,vacio(s.descripcion()),s.estatus()==null||s.estatus()):jdbc.queryForObject("INSERT INTO "+tipo+"(nombre,estatus) VALUES(?,?) RETURNING id",Long.class,nombre,s.estatus()==null||s.estatus());
        else {uno("SELECT id FROM "+tipo+" WHERE id=?",id); if(descripcion) jdbc.update("UPDATE "+tipo+" SET nombre=?,descripcion=?,estatus=? WHERE id=?",nombre,vacio(s.descripcion()),s.estatus()==null||s.estatus(),id); else jdbc.update("UPDATE "+tipo+" SET nombre=?,estatus=? WHERE id=?",nombre,s.estatus()==null||s.estatus(),id);}
        auditar("GUARDAR_CATALOGO",tipo,id,nombre);
        return uno("SELECT * FROM "+tipo+" WHERE id=?",id);
    }
    private static final String VISITAS="SELECT v.*,p.nombre,p.apellidos,m.nombre motivo FROM registros_visita v JOIN personas p ON p.id=v.persona_id JOIN motivos_visita m ON m.id=v.motivo_id";
    @Override @Transactional(readOnly=true)
    public List<Map<String,Object>> visitas(boolean activas,Long personaId) {
        Long filtro=identidad.esGestion()?personaId:identidad.personaId();
        String sql=VISITAS+" WHERE 1=1"+(activas?" AND v.fecha_salida IS NULL":"")+(filtro==null?"":" AND v.persona_id=?")+" ORDER BY v.fecha_entrada DESC";
        return filtro==null?jdbc.queryForList(sql):jdbc.queryForList(sql,filtro);
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> registrarVisita(VisitaSolicitud s) {
        activo("personas",s.persona_id());activo("motivos_visita",s.motivo_id());
        jdbc.queryForObject("SELECT id FROM personas WHERE id=? FOR UPDATE",Long.class,s.persona_id());
        if(existe("SELECT count(*) FROM registros_visita WHERE persona_id=? AND fecha_salida IS NULL",s.persona_id())) conflicto("La persona ya tiene una visita abierta. Registra su salida primero.");
        Long id=jdbc.queryForObject("INSERT INTO registros_visita(persona_id,motivo_id,fecha_entrada,observaciones) VALUES(?,?,CURRENT_TIMESTAMP,?) RETURNING id",Long.class,s.persona_id(),s.motivo_id(),vacio(s.observaciones()));
        auditar("ENTRADA","RegistroVisita",id,"Entrada a biblioteca");
        return uno(VISITAS+" WHERE v.id=?",id);
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> salida(Long id) {
        uno("SELECT id FROM registros_visita WHERE id=? FOR UPDATE",id);
        if(jdbc.update("UPDATE registros_visita SET fecha_salida=CURRENT_TIMESTAMP WHERE id=? AND fecha_salida IS NULL",id)==0) conflicto("La visita ya tiene salida registrada.");
        auditar("SALIDA","RegistroVisita",id,"Salida de biblioteca");
        return uno(VISITAS+" WHERE v.id=?",id);
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> corregirVisita(Long id,CorreccionVisita s) {
        Map<String,Object> previa=uno("SELECT * FROM registros_visita WHERE id=? FOR UPDATE",id);
        Long persona=((Number)previa.get("persona_id")).longValue();
        jdbc.queryForObject("SELECT id FROM personas WHERE id=? FOR UPDATE",Long.class,persona);
        LocalDateTime ahora=LocalDateTime.now();
        if(s.fecha_entrada().isAfter(ahora)||s.fecha_salida()!=null&&s.fecha_salida().isAfter(ahora)) invalido("Una visita real no puede tener fechas futuras.");
        if(s.fecha_salida()!=null&&s.fecha_salida().isBefore(s.fecha_entrada())) invalido("La salida debe ser posterior a la entrada.");
        if(existe("SELECT count(*) FROM registros_visita WHERE persona_id=? AND id<>? AND fecha_entrada<COALESCE(?::timestamp,'infinity'::timestamp) AND COALESCE(fecha_salida,'infinity'::timestamp)>?",persona,id,s.fecha_salida()==null?null:Timestamp.valueOf(s.fecha_salida()),Timestamp.valueOf(s.fecha_entrada()))) conflicto("La corrección se superpone con otra visita de esta persona.");
        jdbc.update("UPDATE registros_visita SET fecha_entrada=?,fecha_salida=? WHERE id=?",Timestamp.valueOf(s.fecha_entrada()),s.fecha_salida()==null?null:Timestamp.valueOf(s.fecha_salida()),id);
        Long evento=jdbc.queryForObject("INSERT INTO bitacoras_auditoria(fecha,accion,modulo,entidad,id_registro,resultado,motivo,operacion_sistema,responsable_id) VALUES(CURRENT_TIMESTAMP,'CORREGIR_VISITA','M01','RegistroVisita',?,'CORRECTO',?,FALSE,?) RETURNING id",Long.class,id,s.motivo().trim(),identidad.cuentaId());
        jdbc.update("INSERT INTO detalles_cambio(evento_id,campo,valor_anterior,valor_nuevo) VALUES(?,'fecha_entrada',?,?)",evento,Objects.toString(previa.get("fecha_entrada"),null),s.fecha_entrada().toString());
        jdbc.update("INSERT INTO detalles_cambio(evento_id,campo,valor_anterior,valor_nuevo) VALUES(?,'fecha_salida',?,?)",evento,Objects.toString(previa.get("fecha_salida"),null),Objects.toString(s.fecha_salida(),null));
        return uno(VISITAS+" WHERE v.id=?",id);
    }
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public List<Map<String,Object>> verificaciones(Long cuentaId) {
        uno("SELECT id FROM cuentas_usuario WHERE id=?",cuentaId);
        return jdbc.queryForList("SELECT v.*,p.nombre||' '||p.apellidos responsable FROM verificaciones_institucionales v LEFT JOIN cuentas_usuario c ON c.id=v.responsable_id LEFT JOIN personas p ON p.id=c.persona_id WHERE v.cuenta_id=? ORDER BY v.fecha DESC,v.id DESC",cuentaId);
    }
    @Override @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String,Object> verificar(Long cuentaId,VerificacionSolicitud s) {
        uno("SELECT id FROM cuentas_usuario WHERE id=? FOR UPDATE",cuentaId);
        if(existe("SELECT count(*) FROM cuentas_usuario WHERE id=? AND usuario='SISTEMA_LOCAL'",cuentaId)) conflicto("La identidad técnica no utiliza verificación institucional de personas.");
        String resultado=s.resultado().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("VERIFICADA","RECHAZADA","PENDIENTE").contains(resultado)) invalido("Resultado de verificación inválido.");
        jdbc.update("UPDATE cuentas_usuario SET estado_verificacion=? WHERE id=?",resultado,cuentaId);
        jdbc.update("INSERT INTO verificaciones_institucionales(cuenta_id,responsable_id,tipo,resultado,fecha,observaciones) VALUES(?,?,'REVISION_INSTITUCIONAL',?,CURRENT_TIMESTAMP,?)",cuentaId,identidad.cuentaId(),resultado,s.observaciones().trim());
        auditar("VERIFICAR_CUENTA","CuentaUsuario",cuentaId,resultado);
        return uno("SELECT id,usuario,persona_id,estado_verificacion FROM cuentas_usuario WHERE id=?",cuentaId);
    }
    private void protegerUltimoAdminPersona(Long personaId) {
        var cuentas=jdbc.queryForList("SELECT id FROM cuentas_usuario WHERE persona_id=?",Long.class,personaId);
        for(Long id:cuentas) if(rolesCuenta(id).contains("ADMINISTRADOR")) protegerUltimoAdminCuenta(id);
    }
    private void protegerUltimoAdminCuenta(Long id) {
        jdbc.queryForList("SELECT id FROM cuentas_usuario ORDER BY id FOR UPDATE");
        if(!existe("SELECT count(*) FROM cuentas_usuario cu JOIN personas p ON p.id=cu.persona_id JOIN cuentas_roles cr ON cr.cuenta_id=cu.id JOIN roles r ON r.id=cr.rol_id WHERE cu.usuario<>'SISTEMA_LOCAL' AND cu.estatus AND p.estatus AND r.estatus AND r.nombre='ADMINISTRADOR' AND cu.id<>?",id)) conflicto("Debe permanecer al menos un administrador activo.");
    }
    void auditar(String accion,String entidad,Long id,String motivo) {
        jdbc.update("INSERT INTO bitacoras_auditoria(fecha,accion,modulo,entidad,id_registro,resultado,motivo,operacion_sistema,responsable_id) VALUES(CURRENT_TIMESTAMP,?,'M01',?,?,'CORRECTO',?,FALSE,?)",accion,entidad,id,motivo,identidad.cuentaId());
    }
    private void activo(String tabla,Long id) { if(id==null||!existe("SELECT count(*) FROM "+tabla+" WHERE id=? AND estatus",id)) invalido("Selecciona un registro activo de "+tabla+"."); }
    private boolean existe(String sql,Object... args) { Long n=jdbc.queryForObject(sql,Long.class,args);return n!=null&&n>0; }
    private Long unoId(String sql,Object... args) { var rows=jdbc.queryForList(sql,Long.class,args);if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El rol o registro no existe o está inactivo."); return rows.getFirst(); }
    private Map<String,Object> uno(String sql,Object... args) {var rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El registro no existe.");return rows.getFirst();}
    static String vacio(String s) {return s==null||s.isBlank()?null:s.trim();}
    static String usuario(String s) {String u=s==null?"":s.trim().toLowerCase(Locale.ROOT);if(u.equals("sistema_local"))invalido("El nombre solicitado está reservado para la operación técnica.");if(!u.matches("[a-z0-9][a-z0-9._@-]{2,99}"))invalido("El usuario debe tener de 3 a 100 letras, números, punto, guion o @.");return u;}
    static void validarPassword(String s) {if(s==null||s.isBlank()||s.length()<10||s.getBytes(StandardCharsets.UTF_8).length>72)invalido("La contraseña debe tener al menos 10 caracteres y un máximo de 72 bytes.");}
    private static String tipo(String s) {String t=s.trim().toUpperCase(Locale.ROOT);if(t.equals("USUARIO_EXTERNO"))t="EXTERNO";if(!Set.of("ESTUDIANTE","DOCENTE","EMPLEADO","EXTERNO","PERSONA").contains(t))invalido("Tipo de persona inválido.");return t;}
    private static void requerido(String s,String campo){if(s==null||s.isBlank())invalido(campo+" es obligatorio.");}
    private static void invalido(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
    private static void conflicto(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
