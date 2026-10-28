package mx.edu.itspa.biblioteca.services.impl;

import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import mx.edu.itspa.biblioteca.services.SesionServicio;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/** RF-M01-11..23/36..41, RN-M01-05..16/22, pp. impresas 10–14.
 * Sesiones de servidor; BCrypt. Ninguna contraseña/hash se devuelve por HTTP. */
@Service
@Transactional
public class SesionServicioImpl implements SesionServicio, UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final IdentidadActual identidad;
    private final boolean accesoDirecto;
    public static final String USUARIO_TECNICO="SISTEMA_LOCAL";
    public SesionServicioImpl(JdbcTemplate jdbc,PasswordEncoder encoder,IdentidadActual identidad,
        @Value("${biblioteca.acceso.directo:true}") boolean accesoDirecto) {this.jdbc=jdbc;this.encoder=encoder;this.identidad=identidad;this.accesoDirecto=accesoDirecto;}
    @Override public boolean accesoDirecto(){return accesoDirecto;}

    public record CuentaPrincipal(Long cuentaId,Long personaId,String usuario,String nombre,
        String password,String verificacion,boolean activa,List<String> roles) implements UserDetails,Serializable {
        @Override public Collection<? extends GrantedAuthority> getAuthorities(){return roles.stream().map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList();}
        @Override public String getPassword(){return password;}
        @Override public String getUsername(){return usuario;}
        @Override public boolean isEnabled(){return activa;}
        public Map<String,Object> respuesta(){return Map.of("id",cuentaId,"personaId",personaId,"usuario",usuario,"nombre",nombre,"roles",roles,"estadoVerificacion",verificacion);}
    }
    @Override @Transactional(readOnly=true)
    public CuentaPrincipal loadUserByUsername(String usuario) throws UsernameNotFoundException {
        if(USUARIO_TECNICO.equalsIgnoreCase(usuario))throw new UsernameNotFoundException("La identidad técnica no admite inicio de sesión.");
        return cargarCuenta(usuario);
    }
    private CuentaPrincipal cargarCuenta(String usuario) {
        var filas=jdbc.queryForList("SELECT cu.id,cu.persona_id,cu.usuario,cu.contrasena_hash,cu.estado_verificacion,cu.estatus AND p.estatus activa,concat_ws(' ',p.nombre,p.apellidos) nombre FROM cuentas_usuario cu JOIN personas p ON p.id=cu.persona_id WHERE lower(cu.usuario)=lower(?)",usuario);
        if(filas.size()!=1) throw new UsernameNotFoundException("Credenciales incorrectas.");
        var f=filas.getFirst();Long id=((Number)f.get("id")).longValue();
        List<String> roles=jdbc.queryForList("SELECT r.nombre FROM roles r JOIN cuentas_roles cr ON cr.rol_id=r.id WHERE cr.cuenta_id=? AND r.estatus ORDER BY r.nombre",String.class,id);
        return new CuentaPrincipal(id,((Number)f.get("persona_id")).longValue(),(String)f.get("usuario"),(String)f.get("nombre"),(String)f.get("contrasena_hash"),Objects.toString(f.get("estado_verificacion"),"PENDIENTE"),(Boolean)f.get("activa"),List.copyOf(roles));
    }
    /** Solo modo local expresamente solicitado. No inicia sesión ni representa a una persona humana. */
    public CuentaPrincipal identidadDirecta() {
        if(!accesoDirecto)throw new IllegalStateException("El acceso directo no está habilitado.");
        jdbc.execute("SELECT pg_advisory_xact_lock(68170103)");
        if(!existe("SELECT count(*) FROM cuentas_usuario WHERE usuario=?",USUARIO_TECNICO)){
            for(String rol:List.of("ADMINISTRADOR","BIBLIOTECARIO","LECTOR"))jdbc.update("INSERT INTO roles(nombre,descripcion,estatus) VALUES(?,?,TRUE) ON CONFLICT(nombre) DO NOTHING",rol,"Rol operativo de biblioteca: "+rol);
            Long persona=jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,fecha_registro,tipo_identificacion,numero_identificacion,estatus) VALUES('Operación','biblioteca',CURRENT_TIMESTAMP,'IDENTIDAD_TECNICA',?,TRUE) RETURNING id",Long.class,USUARIO_TECNICO);
            Long cuenta=jdbc.queryForObject("INSERT INTO cuentas_usuario(persona_id,usuario,contrasena_hash,fecha_creacion,estado_verificacion,estatus) VALUES(?,?,?,CURRENT_TIMESTAMP,'VERIFICADA',TRUE) RETURNING id",Long.class,persona,USUARIO_TECNICO,encoder.encode(UUID.randomUUID().toString()+UUID.randomUUID()));
            jdbc.update("INSERT INTO cuentas_roles(cuenta_id,rol_id) SELECT ?,id FROM roles WHERE nombre='ADMINISTRADOR'",cuenta);
            jdbc.update("INSERT INTO bitacoras_auditoria(fecha,accion,modulo,entidad,id_registro,resultado,motivo,operacion_sistema,responsable_id) VALUES(CURRENT_TIMESTAMP,'ACTIVAR_OPERACION_LOCAL','M09','CuentaUsuario',?,'CORRECTO','Identidad técnica para operación local sin inicio de sesión humano',TRUE,?)",cuenta,cuenta);
        }
        return cargarCuenta(USUARIO_TECNICO);
    }
    @Override @Transactional(readOnly=true)
    public boolean inicializado(){return accesoDirecto||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM cuentas_usuario WHERE usuario<>?)",Boolean.class,USUARIO_TECNICO));}
    @Override
    public void inicializar(Inicializacion s) {
        jdbc.execute("SELECT pg_advisory_xact_lock(68170101)");
        if(inicializado()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El sistema ya tiene cuentas. Inicia sesión con una cuenta existente.");
        PersonasServicioImpl.validarPassword(s.password());String usuario=PersonasServicioImpl.usuario(s.usuario());
        for(String rol:List.of("ADMINISTRADOR","BIBLIOTECARIO","LECTOR")) jdbc.update("INSERT INTO roles(nombre,descripcion,estatus) VALUES(?,?,TRUE) ON CONFLICT(nombre) DO NOTHING",rol,switch(rol){case "ADMINISTRADOR"->"Administración de cuentas, configuración y operación bibliotecaria";case "BIBLIOTECARIO"->"Atención y operación de servicios bibliotecarios";default->"Consulta personal y servicios bibliotecarios autorizados";});
        Long persona=jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,fecha_registro,estatus) VALUES(?,?,CURRENT_TIMESTAMP,TRUE) RETURNING id",Long.class,s.nombre().trim(),s.apellidos().trim());
        Long cuenta=jdbc.queryForObject("INSERT INTO cuentas_usuario(persona_id,usuario,contrasena_hash,fecha_creacion,estado_verificacion,estatus) VALUES(?,?,?,CURRENT_TIMESTAMP,'VERIFICADA',TRUE) RETURNING id",Long.class,persona,usuario,encoder.encode(s.password()));
        jdbc.update("INSERT INTO cuentas_roles(cuenta_id,rol_id) SELECT ?,id FROM roles WHERE nombre='ADMINISTRADOR'",cuenta);
        evento("INICIALIZAR",cuenta,null,"Primera cuenta de administración","CORRECTO");
    }
    @Override @Transactional(readOnly=true)
    public Map<String,Object> usuarioActual() {
        var a=SecurityContextHolder.getContext().getAuthentication();
        return a!=null&&a.getPrincipal() instanceof CuentaPrincipal p?p.respuesta():null;
    }
    @Override
    public Map<String,Object> registro(RegistroSolicitud s) {
        if(!inicializado()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Primero configura la cuenta administradora.");
        PersonasServicioImpl.validarPassword(s.password());String usuario=PersonasServicioImpl.usuario(s.usuario());
        var p=s.persona();String tipo=p.tipo().trim().toUpperCase(Locale.ROOT);
        if("IDENTIDAD_TECNICA".equalsIgnoreCase(p.tipo_identificacion()))invalido("El identificador técnico está reservado para el sistema.");
        if(tipo.equals("USUARIO_EXTERNO"))tipo="EXTERNO";
        if(!Set.of("ESTUDIANTE","DOCENTE","EMPLEADO","EXTERNO").contains(tipo)) invalido("Selecciona estudiante, docente, empleado o externo.");
        if(p.fecha_nacimiento()!=null&&p.fecha_nacimiento().isAfter(LocalDate.now())) invalido("La fecha de nacimiento no puede estar en el futuro.");
        jdbc.execute("SELECT pg_advisory_xact_lock(68170102)");
        if(existe("SELECT count(*) FROM cuentas_usuario WHERE lower(usuario)=lower(?)",usuario)) conflicto("El nombre de usuario ya existe.");
        if(PersonasServicioImpl.vacio(p.numero_empleado())!=null&&existe("SELECT count(*) FROM personas WHERE numero_empleado=?",p.numero_empleado().trim())) conflicto("El número de empleado ya está registrado. Solicita al personal la creación de tu cuenta.");
        if(tipo.equals("ESTUDIANTE")) {
            requerido(p.matricula(),"Matrícula");activo("carreras",p.carrera_id());
            if(existe("SELECT count(*) FROM estudiantes WHERE matricula=?",p.matricula().trim())) conflicto("La matrícula ya está registrada. Solicita al personal la creación de tu cuenta.");
        }
        if(tipo.equals("DOCENTE")||tipo.equals("EMPLEADO")) requerido(p.numero_empleado(),"Número de empleado");
        if(tipo.equals("DOCENTE"))activo("departamentos",p.departamento_id());
        if(tipo.equals("EMPLEADO")){requerido(p.area_trabajo(),"Área de trabajo");if(p.departamento_id()!=null)activo("departamentos",p.departamento_id());}
        Long persona=jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,correo,telefono,fecha_nacimiento,fecha_registro,tipo_identificacion,numero_identificacion,numero_empleado,estatus) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?,?,?,TRUE) RETURNING id",Long.class,p.nombre().trim(),p.apellidos().trim(),PersonasServicioImpl.vacio(p.correo()),PersonasServicioImpl.vacio(p.telefono()),p.fecha_nacimiento(),PersonasServicioImpl.vacio(p.tipo_identificacion()),PersonasServicioImpl.vacio(p.numero_identificacion()),tipo.equals("DOCENTE")||tipo.equals("EMPLEADO")?p.numero_empleado().trim():null);
        switch(tipo){
            case "ESTUDIANTE"->jdbc.update("INSERT INTO estudiantes(id,matricula,semestre,estado_academico,carrera_id) VALUES(?,?,?,?,?)",persona,p.matricula().trim(),p.semestre(),PersonasServicioImpl.vacio(p.estado_academico()),p.carrera_id());
            case "DOCENTE"->jdbc.update("INSERT INTO docentes(id,departamento_id) VALUES(?,?)",persona,p.departamento_id());
            case "EMPLEADO"->jdbc.update("INSERT INTO empleados(id,area_trabajo,puesto,departamento_id) VALUES(?,?,?,?)",persona,p.area_trabajo().trim(),PersonasServicioImpl.vacio(p.puesto()),p.departamento_id());
            default->jdbc.update("INSERT INTO usuarios_externos(id,institucion) VALUES(?,?)",persona,PersonasServicioImpl.vacio(p.institucion()));
        }
        Long cuenta=jdbc.queryForObject("INSERT INTO cuentas_usuario(persona_id,usuario,contrasena_hash,fecha_creacion,estatus,estado_verificacion) VALUES(?,?,?,CURRENT_TIMESTAMP,TRUE,'PENDIENTE') RETURNING id",Long.class,persona,usuario,encoder.encode(s.password()));
        jdbc.update("INSERT INTO cuentas_roles(cuenta_id,rol_id) SELECT ?,id FROM roles WHERE nombre='LECTOR' AND estatus",cuenta);
        evento("AUTORREGISTRO",cuenta,null,"Cuenta pendiente de revisión institucional; sin privilegios administrativos","CORRECTO");
        return Map.of("id",cuenta,"usuario",usuario,"personaId",persona,"estadoVerificacion","PENDIENTE","mensaje","Registro recibido. Puedes iniciar sesión y consultar el catálogo. El personal debe revisar tus datos antes de habilitar servicios restringidos.");
    }
    @Override
    public Map<String,Object> perfil(Perfil s) {
        Long id=identidad.personaId();
        jdbc.update("UPDATE personas SET nombre=?,apellidos=?,correo=?,telefono=? WHERE id=?",s.nombre().trim(),s.apellidos().trim(),PersonasServicioImpl.vacio(s.correo()),PersonasServicioImpl.vacio(s.telefono()),id);
        evento("ACTUALIZAR_PERFIL",identidad.cuentaId(),null,"Datos de contacto actualizados","CORRECTO");
        return jdbc.queryForMap("SELECT id,nombre,apellidos,correo,telefono FROM personas WHERE id=?",id);
    }
    @Override
    public void cambiarPassword(CambioPassword s) {
        PersonasServicioImpl.validarPassword(s.nueva());
        String hash=jdbc.queryForObject("SELECT contrasena_hash FROM cuentas_usuario WHERE id=? FOR UPDATE",String.class,identidad.cuentaId());
        if(!encoder.matches(s.actual(),hash)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña actual no es correcta.");
        if(encoder.matches(s.nueva(),hash)) invalido("La nueva contraseña debe ser diferente.");
        jdbc.update("UPDATE cuentas_usuario SET contrasena_hash=? WHERE id=?",encoder.encode(s.nueva()),identidad.cuentaId());
        evento("CAMBIAR_PASSWORD",identidad.cuentaId(),null,"Contraseña actualizada; requiere nuevo inicio de sesión","CORRECTO");
    }
    public Long abrirSesion(CuentaPrincipal p,String ip) {
        jdbc.update("UPDATE cuentas_usuario SET ultimo_acceso=CURRENT_TIMESTAMP WHERE id=?",p.cuentaId());
        Long id=jdbc.queryForObject("INSERT INTO sesiones_usuario(cuenta_id,fecha_inicio,ultima_actividad,ip,estatus,cierre_administrativo) VALUES(?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,'ACTIVA',FALSE) RETURNING id",Long.class,p.cuentaId(),ip);
        evento("LOGIN",p.cuentaId(),ip,"Inicio de sesión","CORRECTO");return id;
    }
    public void cerrarSesion(Long id,String motivo) {if(id!=null) jdbc.update("UPDATE sesiones_usuario SET fecha_fin=CURRENT_TIMESTAMP,estatus='CERRADA',motivo_cierre=? WHERE id=? AND fecha_fin IS NULL",motivo,id);}
    public void actividad(Long id) {if(id!=null)jdbc.update("UPDATE sesiones_usuario SET ultima_actividad=CURRENT_TIMESTAMP WHERE id=? AND fecha_fin IS NULL",id);}
    @Transactional(readOnly=true)
    public boolean sesionVigente(Long id) {return id!=null&&existe("SELECT count(*) FROM sesiones_usuario WHERE id=? AND estatus='ACTIVA' AND fecha_fin IS NULL",id);}
    public void falloLogin(String usuario,String ip) {evento("LOGIN_FALLIDO",null,ip,usuario==null?"":usuario.trim().toLowerCase(Locale.ROOT),"DENEGADO");}
    @Transactional(readOnly=true)
    public boolean bloqueado(String usuario,String ip) {
        var p=politica();
        Long intentos=jdbc.queryForObject("SELECT count(*) FROM bitacoras_auditoria WHERE accion='LOGIN_FALLIDO' AND (motivo=? OR ip=?) AND fecha>CURRENT_TIMESTAMP-(? * interval '1 minute')",Long.class,usuario.trim().toLowerCase(Locale.ROOT),ip,p.get("bloqueo_temporal_minutos"));
        return intentos!=null&&intentos>=p.get("intentos_fallidos_maximos");
    }
    @Transactional(readOnly=true)
    public Map<String,Integer> politica() {
        var filas=jdbc.queryForList("SELECT inactividad_maxima_minutos,intentos_fallidos_maximos,bloqueo_temporal_minutos FROM politicas_seguridad WHERE estatus ORDER BY id DESC LIMIT 1");
        Map<String,Integer> valores=new HashMap<>(Map.of("inactividad_maxima_minutos",30,"intentos_fallidos_maximos",5,"bloqueo_temporal_minutos",15));
        if(!filas.isEmpty())filas.getFirst().forEach((k,v)->{if(v instanceof Number n&&n.intValue()>0)valores.put(k,n.intValue());});
        return valores;
    }
    private void evento(String accion,Long cuenta,String ip,String motivo,String resultado) {jdbc.update("INSERT INTO bitacoras_auditoria(fecha,accion,modulo,entidad,id_registro,ip,resultado,motivo,operacion_sistema,responsable_id) VALUES(CURRENT_TIMESTAMP,?,'M01','CuentaUsuario',?,?,?,?,FALSE,?)",accion,cuenta,ip,resultado,motivo,cuenta);}
    private boolean existe(String sql,Object...args){Long n=jdbc.queryForObject(sql,Long.class,args);return n!=null&&n>0;}
    private void activo(String tabla,Long id){if(id==null||!existe("SELECT count(*) FROM "+tabla+" WHERE id=? AND estatus",id))invalido("Selecciona un registro activo de "+tabla+".");}
    private static void requerido(String s,String nombre){if(s==null||s.isBlank())invalido(nombre+" es obligatorio.");}
    private static void invalido(String mensaje){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private static void conflicto(String mensaje){throw new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
