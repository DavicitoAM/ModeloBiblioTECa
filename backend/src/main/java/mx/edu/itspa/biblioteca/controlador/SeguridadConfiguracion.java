package mx.edu.itspa.biblioteca.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mx.edu.itspa.biblioteca.services.impl.SesionServicioImpl;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

/** Adaptador de seguridad real para el navegador: cookies HttpOnly y protección CSRF.
 * RF-M01-12/13/16/21/23; RN-M01-07/08/11/12/14. */
@Configuration
@EnableMethodSecurity
public class SeguridadConfiguracion {
    public static final String ID_SESION="ITSPA_SESION_ID";
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean HttpSessionSecurityContextRepository contextoSesiones(){return new HttpSessionSecurityContextRepository();}
    @Bean HttpSessionCsrfTokenRepository tokensCsrf(){return new HttpSessionCsrfTokenRepository();}
    @Bean ServletListenerRegistrationBean<HttpSessionListener> cierreSesiones(SesionServicioImpl servicio){
        return new ServletListenerRegistrationBean<>(new HttpSessionListener(){
            @Override public void sessionDestroyed(HttpSessionEvent evento){
                servicio.cerrarSesion((Long)evento.getSession().getAttribute(ID_SESION),"Fin de sesión o tiempo de inactividad");
            }
        });
    }
    @Bean AuthenticationManager authenticationManager(SesionServicioImpl servicio,PasswordEncoder encoder){
        DaoAuthenticationProvider proveedor=new DaoAuthenticationProvider(servicio);
        proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }
    @Bean SecurityFilterChain cadenaSeguridad(HttpSecurity http,SesionServicioImpl servicio,
        HttpSessionCsrfTokenRepository csrf,HttpSessionSecurityContextRepository contexto,ObjectMapper json) throws Exception {
        http.securityContext(c->c.securityContextRepository(contexto))
            .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->error(res,json,401,"Inicia sesión para continuar."))
                .accessDeniedHandler((req,res,ex)->error(res,json,403,ex instanceof CsrfException?"La protección de sesión caducó. Actualiza la página e intenta de nuevo.":"No tienes permiso para esta operación.")))
            .authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.GET,"/api/app/auth/estado","/api/app/auth/catalogos-registro","/api/app/catalogo/**","/api/app/inicio","/api/v1/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/app/auth/inicializar","/api/app/auth/login","/api/app/auth/registro").permitAll()
                .requestMatchers("/api/v1/**").hasAnyRole("ADMINISTRADOR","BIBLIOTECARIO")
                .requestMatchers("/api/app/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(new SesionVigenteFilter(servicio,contexto,json),AuthorizationFilter.class);
        return http.build();
    }
    private static void error(HttpServletResponse response,ObjectMapper json,int estado,String mensaje) throws IOException {
        response.setStatus(estado);response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getWriter(),Map.of("error",mensaje));
    }
    static final class SesionVigenteFilter extends OncePerRequestFilter {
        private final SesionServicioImpl servicio;
        private final HttpSessionSecurityContextRepository contexto;
        private final ObjectMapper json;
        SesionVigenteFilter(SesionServicioImpl servicio,HttpSessionSecurityContextRepository contexto,ObjectMapper json){this.servicio=servicio;this.contexto=contexto;this.json=json;}
        @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
            if(servicio.accesoDirecto()&&req.getRequestURI().startsWith("/api/")){
                var principal=servicio.identidadDirecta();
                var local=SecurityContextHolder.createEmptyContext();
                local.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
                SecurityContextHolder.setContext(local);
                chain.doFilter(req,res);
                return;
            }
            var a=SecurityContextHolder.getContext().getAuthentication();
            if(a!=null&&a.getPrincipal() instanceof SesionServicioImpl.CuentaPrincipal previa){
                HttpSession session=req.getSession(false);
                Long id=session==null?null:(Long)session.getAttribute(ID_SESION);
                SesionServicioImpl.CuentaPrincipal actual=null;
                try {actual=servicio.loadUserByUsername(previa.usuario());}catch(UsernameNotFoundException ignored){ }
                if(actual==null||!actual.activa()||!actual.password().equals(previa.password())||!servicio.sesionVigente(id)){
                    servicio.cerrarSesion(id,"Acceso revocado o credencial modificada");
                    SecurityContextHolder.clearContext();if(session!=null)session.invalidate();
                }else{
                    var nuevo=UsernamePasswordAuthenticationToken.authenticated(actual,null,actual.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(nuevo);
                    contexto.saveContext(SecurityContextHolder.getContext(),req,res);
                    session.setMaxInactiveInterval(servicio.politica().get("inactividad_maxima_minutos")*60);
                    servicio.actividad(id);
                    boolean gestion=actual.roles().contains("ADMINISTRADOR")||actual.roles().contains("BIBLIOTECARIO");
                    if(!gestion&&!actual.verificacion().equals("VERIFICADA")&&!req.getMethod().equals("GET")&&!req.getMethod().equals("HEAD")&&!req.getRequestURI().startsWith("/api/app/auth/")){
                        error(res,json,403,"Tu cuenta está pendiente de verificación. Solicita al personal que revise tus datos para habilitar servicios.");return;
                    }
                }
            }
            chain.doFilter(req,res);
        }
    }
}
