package mx.edu.itspa.biblioteca.controlador;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import mx.edu.itspa.biblioteca.services.SesionServicio;
import mx.edu.itspa.biblioteca.services.impl.SesionServicioImpl;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/app/auth")
public class SesionControlador {
    private final SesionServicio servicio;
    private final SesionServicioImpl sesiones;
    private final AuthenticationManager autenticador;
    private final HttpSessionSecurityContextRepository contextos;
    private final HttpSessionCsrfTokenRepository csrf;
    private final JdbcTemplate jdbc;
    public SesionControlador(SesionServicio servicio,SesionServicioImpl sesiones,AuthenticationManager autenticador,
        HttpSessionSecurityContextRepository contextos,HttpSessionCsrfTokenRepository csrf,JdbcTemplate jdbc){
        this.servicio=servicio;this.sesiones=sesiones;this.autenticador=autenticador;this.contextos=contextos;this.csrf=csrf;this.jdbc=jdbc;
    }
    @GetMapping("/estado") public ResponseEntity<Map<String,Object>> estado(HttpServletRequest req,HttpServletResponse res){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(datosEstado(req,res));}
    @PostMapping("/inicializar") public Map<String,Object> inicializar(@Valid @RequestBody Inicializacion s,HttpServletRequest req,HttpServletResponse res){exigirModoIntegracion();servicio.inicializar(s);return autenticar(new Login(s.usuario(),s.password()),req,res);}
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login s,HttpServletRequest req,HttpServletResponse res){exigirModoIntegracion();return autenticar(s,req,res);}
    private Map<String,Object> autenticar(Login s,HttpServletRequest req,HttpServletResponse res){
        String ip=req.getRemoteAddr();
        if(sesiones.bloqueado(s.usuario(),ip))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos fallidos. Espera unos minutos antes de volver a intentarlo.");
        org.springframework.security.core.Authentication autenticacion;
        try{autenticacion=autenticador.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(s.usuario().trim(),s.password()));}
        catch(AuthenticationException e){sesiones.falloLogin(s.usuario(),ip);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuario o contraseña incorrectos, o cuenta inactiva.");}
        HttpSession anterior=req.getSession(false);
        if(anterior!=null){sesiones.cerrarSesion((Long)anterior.getAttribute(SeguridadConfiguracion.ID_SESION),"Nuevo inicio de sesión");anterior.invalidate();}
        HttpSession session=req.getSession(true);
        session.setMaxInactiveInterval(sesiones.politica().get("inactividad_maxima_minutos")*60);
        var principal=(SesionServicioImpl.CuentaPrincipal)autenticacion.getPrincipal();
        session.setAttribute(SeguridadConfiguracion.ID_SESION,sesiones.abrirSesion(principal,ip));
        var contexto=SecurityContextHolder.createEmptyContext();contexto.setAuthentication(autenticacion);SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto,req,res);
        csrf.saveToken(csrf.generateToken(req),req,res);
        return datosEstado(req,res);
    }
    @PostMapping("/logout") public Map<String,Object> logout(HttpServletRequest req,HttpServletResponse res){
        if(servicio.accesoDirecto())return datosEstado(req,res);
        HttpSession session=req.getSession(false);if(session!=null){sesiones.cerrarSesion((Long)session.getAttribute(SeguridadConfiguracion.ID_SESION),"Cierre voluntario");session.invalidate();}
        SecurityContextHolder.clearContext();
        return datosEstado(req,res);
    }
    @GetMapping("/me") public Map<String,Object> yo(){return servicio.usuarioActual();}
    @PutMapping("/perfil") public Map<String,Object> perfil(@Valid @RequestBody Perfil s){exigirModoIntegracion();return servicio.perfil(s);}
    @PostMapping("/password") public Map<String,Object> password(@Valid @RequestBody CambioPassword s,HttpServletRequest req,HttpServletResponse res){exigirModoIntegracion();servicio.cambiarPassword(s);logout(req,res);return Map.of("mensaje","Contraseña actualizada. Vuelve a iniciar sesión.");}
    @PostMapping("/registro") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> registro(@Valid @RequestBody RegistroSolicitud s){exigirModoIntegracion();return servicio.registro(s);}
    @GetMapping("/catalogos-registro") public Map<String,Object> catalogosRegistro(){return Map.of("carreras",jdbc.queryForList("SELECT id,nombre FROM carreras WHERE estatus ORDER BY nombre"),"departamentos",jdbc.queryForList("SELECT id,nombre FROM departamentos WHERE estatus ORDER BY nombre"));}
    private Map<String,Object> datosEstado(HttpServletRequest req,HttpServletResponse res){
        CsrfToken token=csrf.loadToken(req);if(token==null){token=csrf.generateToken(req);csrf.saveToken(token,req,res);}
        Map<String,Object> result=new LinkedHashMap<>();result.put("inicializado",servicio.inicializado());result.put("accesoDirecto",servicio.accesoDirecto());result.put("usuario",servicio.usuarioActual());result.put("csrfToken",token.getToken());result.put("csrfHeader",token.getHeaderName());return result;
    }
    private void exigirModoIntegracion(){if(servicio.accesoDirecto())throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta versión opera directamente, sin cuentas de acceso humano. La autenticación se habilitará al integrar el sistema institucional.");}
}
