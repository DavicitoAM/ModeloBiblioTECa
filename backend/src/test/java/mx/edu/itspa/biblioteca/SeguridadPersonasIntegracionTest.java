package mx.edu.itspa.biblioteca;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** M01. HTTP real de Spring Security + PostgreSQL aislado; rollback de todos los datos. */
@SpringBootTest(properties="biblioteca.acceso.directo=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers=GuardiaBasePruebas.class)
@Transactional
class SeguridadPersonasIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    private static final String PASSWORD="Prueba-segura-2026!";
    private record Cliente(MockHttpSession sesion,String token,String header,String usuario,Long personaId,Long cuentaId) { }

    @Test void estadoPublicoEntregaCsrfPeroNoSecretos() throws Exception {
        var respuesta=mvc.perform(get("/api/app/auth/estado")).andExpect(status().isOk()).andExpect(jsonPath("$.usuario").isEmpty()).andReturn();
        JsonNode estado=json.readTree(respuesta.getResponse().getContentAsString());
        assertThat(estado.get("csrfToken").asText()).isNotBlank();
        assertThat(respuesta.getResponse().getContentAsString()).doesNotContain("contrasena_hash");
    }
    @Test void csrfImpideLoginSinTokenYCredencialIncorrectaNoAccede() throws Exception {
        mvc.perform(post("/api/app/auth/login").contentType("application/json").content("{\"usuario\":\"inexistente\",\"password\":\"incorrecta\"}")).andExpect(status().isForbidden());
        Cliente anonimo=anonimo();
        mvc.perform(post("/api/app/auth/login").session(anonimo.sesion()).header(anonimo.header(),anonimo.token()).contentType("application/json").content(json.writeValueAsString(Map.of("usuario","inexistente-"+UUID.randomUUID(),"password",PASSWORD)))).andExpect(status().isUnauthorized());
    }
    @Test void autenticacionRotaSesionYGuardaAccesoSinExponerHash() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");
        mvc.perform(get("/api/app/auth/me").session(admin.sesion())).andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value(admin.usuario())).andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR")).andExpect(jsonPath("$.password").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT ultimo_acceso IS NOT NULL FROM cuentas_usuario WHERE id=?",Boolean.class,admin.cuentaId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sesiones_usuario WHERE cuenta_id=? AND estatus='ACTIVA'",Integer.class,admin.cuentaId())).isEqualTo(1);
        mvc.perform(get("/api/app/cuentas").session(admin.sesion())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("contrasena_hash"))));
    }
    @Test void lectorSoloConsultaSuPersonaYNoAdministraCuentas() throws Exception {
        Cliente lector=cliente("LECTOR","VERIFICADA");
        Long ajena=persona("Otra");
        mvc.perform(get("/api/app/personas/"+lector.personaId()).session(lector.sesion())).andExpect(status().isOk());
        mvc.perform(get("/api/app/personas/"+ajena).session(lector.sesion())).andExpect(status().isForbidden());
        mvc.perform(get("/api/app/cuentas").session(lector.sesion())).andExpect(status().isForbidden());
        mvc.perform(get("/api/app/personas").session(lector.sesion())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void desactivarCuentaYRevocarRolSeAplicanASesionYaAbierta() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");
        jdbc.update("DELETE FROM cuentas_roles WHERE cuenta_id=?",admin.cuentaId());
        mvc.perform(get("/api/app/cuentas").session(admin.sesion())).andExpect(status().isForbidden());
        jdbc.update("UPDATE cuentas_usuario SET estatus=FALSE WHERE id=?",admin.cuentaId());
        mvc.perform(get("/api/app/auth/me").session(admin.sesion())).andExpect(status().isUnauthorized());
    }
    @Test void visitaNoPermiteDobleEntradaNiDobleSalida() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");Long persona=persona("Visita");
        Long motivo=jdbc.queryForObject("INSERT INTO motivos_visita(nombre,estatus) VALUES(?,TRUE) RETURNING id",Long.class,"Motivo test "+UUID.randomUUID());
        String body=json.writeValueAsString(Map.of("persona_id",persona,"motivo_id",motivo));
        var alta=mvc.perform(post("/api/app/visitas").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn();
        Long id=json.readTree(alta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/app/visitas").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/app/visitas/"+id+"/salida").session(admin.sesion()).header(admin.header(),admin.token())).andExpect(status().isOk()).andExpect(jsonPath("$.fecha_salida").isNotEmpty());
        mvc.perform(post("/api/app/visitas/"+id+"/salida").session(admin.sesion()).header(admin.header(),admin.token())).andExpect(status().isConflict());
    }
    @Test void personasConservanJoinedYUnicidadTransversalDeNumeroEmpleado() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");
        Long departamento=jdbc.queryForObject("INSERT INTO departamentos(nombre,estatus) VALUES(?,TRUE) RETURNING id",Long.class,"Departamento test "+UUID.randomUUID());
        String numero="TEST-"+UUID.randomUUID().toString().substring(0,20);
        Map<String,Object> datos=new HashMap<>(Map.of("tipo","DOCENTE","nombre","Docente","apellidos","Prueba","numero_empleado",numero,"departamento_id",departamento));
        var alta=mvc.perform(post("/api/app/personas").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(json.writeValueAsString(datos))).andExpect(status().isCreated()).andReturn();
        Long id=json.readTree(alta.getResponse().getContentAsString()).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM docentes d JOIN personas p ON p.id=d.id WHERE d.id=?",Integer.class,id)).isEqualTo(1);
        datos.put("tipo","EMPLEADO");datos.put("area_trabajo","Pruebas");
        mvc.perform(post("/api/app/personas").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(json.writeValueAsString(datos))).andExpect(status().isConflict());
    }
    @Test void cuentasGuardanHashYProhibenAbandonarAdministracionPropia() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");Long p=persona("Cuenta");
        String usuario="test-"+UUID.randomUUID();
        Map<String,Object> body=new HashMap<>(Map.of("persona_id",p,"usuario",usuario,"password",PASSWORD,"roles",List.of("LECTOR"),"estado_verificacion","VERIFICADA"));
        var alta=mvc.perform(post("/api/app/cuentas").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn();
        assertThat(alta.getResponse().getContentAsString()).doesNotContain(PASSWORD,"contrasena_hash");
        String hash=jdbc.queryForObject("SELECT contrasena_hash FROM cuentas_usuario WHERE usuario=?",String.class,usuario);
        assertThat(hash).isNotEqualTo(PASSWORD);assertThat(passwords.matches(PASSWORD,hash)).isTrue();
        body.put("persona_id",admin.personaId());body.put("usuario",admin.usuario());body.remove("password");
        mvc.perform(put("/api/app/cuentas/"+admin.cuentaId()).session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isConflict());
    }
    @Test void cambiarPasswordExigeActualYCierraSesion() throws Exception {
        Cliente lector=cliente("LECTOR","VERIFICADA");
        mvc.perform(post("/api/app/auth/password").session(lector.sesion()).header(lector.header(),lector.token()).contentType("application/json").content(json.writeValueAsString(Map.of("actual","incorrecta","nueva","Nueva-prueba-2026!")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/app/auth/password").session(lector.sesion()).header(lector.header(),lector.token()).contentType("application/json").content(json.writeValueAsString(Map.of("actual",PASSWORD,"nueva","Nueva-prueba-2026!")))).andExpect(status().isOk());
        String hash=jdbc.queryForObject("SELECT contrasena_hash FROM cuentas_usuario WHERE id=?",String.class,lector.cuentaId());
        assertThat(passwords.matches("Nueva-prueba-2026!",hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sesiones_usuario WHERE cuenta_id=? AND estatus='ACTIVA'",Integer.class,lector.cuentaId())).isZero();
    }
    @Test void pendientesNoModificanServiciosYBootstrapNoSePuedeRepetir() throws Exception {
        Cliente lector=cliente("LECTOR","PENDIENTE");
        mvc.perform(post("/api/app/visitas").session(lector.sesion()).header(lector.header(),lector.token()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/app/auth/inicializar").session(lector.sesion()).header(lector.header(),lector.token()).contentType("application/json").content(json.writeValueAsString(Map.of("nombre","Prueba","apellidos","Prueba","usuario","bootstrap-test","password",PASSWORD)))).andExpect(status().isConflict());
    }
    @Test void corregirVisitaConservaFechasPreviasYMotivoEnAuditoria() throws Exception {
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");Long persona=persona("Corrección");
        Long motivo=jdbc.queryForObject("INSERT INTO motivos_visita(nombre,estatus) VALUES(?,TRUE) RETURNING id",Long.class,"Corrección test "+UUID.randomUUID());
        Long visita=jdbc.queryForObject("INSERT INTO registros_visita(persona_id,motivo_id,fecha_entrada,fecha_salida) VALUES(?,?,CURRENT_TIMESTAMP-interval '2 hour',CURRENT_TIMESTAMP-interval '1 hour') RETURNING id",Long.class,persona,motivo);
        String entrada=java.time.LocalDateTime.now().minusHours(3).withNano(0).toString(),salida=java.time.LocalDateTime.now().minusHours(2).withNano(0).toString();
        mvc.perform(put("/api/app/visitas/"+visita).session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(json.writeValueAsString(Map.of("fecha_entrada",entrada,"fecha_salida",salida,"motivo","Corrección administrativa de prueba")))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM detalles_cambio d JOIN bitacoras_auditoria b ON b.id=d.evento_id WHERE b.accion='CORREGIR_VISITA' AND b.id_registro=? AND b.motivo='Corrección administrativa de prueba'",Integer.class,visita)).isEqualTo(2);
    }
    @Test void primeraCuentaEsAdministradorRealYSoloPuedeInicializarseUnaVez() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cuentas_usuario",Integer.class)).isZero();
        Cliente anonimo=anonimo();
        var r=mvc.perform(post("/api/app/auth/inicializar").session(anonimo.sesion()).header(anonimo.header(),anonimo.token()).contentType("application/json").content(json.writeValueAsString(Map.of("nombre","Responsable","apellidos","Prueba","usuario","inicio-"+UUID.randomUUID(),"password",PASSWORD)))).andExpect(status().isOk()).andExpect(jsonPath("$.inicializado").value(true)).andExpect(jsonPath("$.usuario.roles[0]").value("ADMINISTRADOR")).andReturn();
        var estado=json.readTree(r.getResponse().getContentAsString());
        mvc.perform(post("/api/app/auth/inicializar").session((MockHttpSession)r.getRequest().getSession()).header(estado.get("csrfHeader").asText(),estado.get("csrfToken").asText()).contentType("application/json").content(json.writeValueAsString(Map.of("nombre","Otro","apellidos","Intento","usuario","otro-test","password",PASSWORD)))).andExpect(status().isConflict());
    }
    @Test void autorregistroSoloCreaLectorPendienteYRechazaPrivilegiosDeEntrada() throws Exception {
        cliente("ADMINISTRADOR","VERIFICADA");Cliente anonimo=anonimo();String usuario="registro-"+UUID.randomUUID();
        var body=Map.of("persona",Map.of("tipo","EXTERNO","nombre","Visitante","apellidos","Prueba"),"usuario",usuario,"password",PASSWORD,"roles",List.of("ADMINISTRADOR"));
        mvc.perform(post("/api/app/auth/registro").session(anonimo.sesion()).header(anonimo.header(),anonimo.token()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE"));
        assertThat(jdbc.queryForList("SELECT r.nombre FROM roles r JOIN cuentas_roles cr ON cr.rol_id=r.id JOIN cuentas_usuario c ON c.id=cr.cuenta_id WHERE c.usuario=?",String.class,usuario)).containsExactly("LECTOR");
    }
    @Test void apiPilotoTambienProtegeEscriturasConRolesYCsrf() throws Exception {
        Cliente anonimo=anonimo();String libro="{\"titulo\":\"Prueba de autorización del piloto\"}";
        mvc.perform(post("/api/v1/libros").session(anonimo.sesion()).header(anonimo.header(),anonimo.token()).contentType("application/json").content(libro)).andExpect(status().isUnauthorized());
        Cliente lector=cliente("LECTOR","VERIFICADA");
        mvc.perform(post("/api/v1/libros").session(lector.sesion()).header(lector.header(),lector.token()).contentType("application/json").content(libro)).andExpect(status().isForbidden());
        Cliente admin=cliente("ADMINISTRADOR","VERIFICADA");
        mvc.perform(post("/api/v1/libros").session(admin.sesion()).contentType("application/json").content(libro)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/libros").session(admin.sesion()).header(admin.header(),admin.token()).contentType("application/json").content(libro)).andExpect(status().isCreated());
    }
    private Cliente anonimo() throws Exception {
        var r=mvc.perform(get("/api/app/auth/estado")).andExpect(status().isOk()).andReturn();
        var estado=json.readTree(r.getResponse().getContentAsString());
        return new Cliente((MockHttpSession)r.getRequest().getSession(),estado.get("csrfToken").asText(),estado.get("csrfHeader").asText(),null,null,null);
    }
    private Cliente cliente(String rol,String verificacion) throws Exception {
        Long persona=persona("Sesión");String usuario="test-"+UUID.randomUUID();
        for(String nombreRol:List.of("ADMINISTRADOR","BIBLIOTECARIO","LECTOR")) jdbc.update("INSERT INTO roles(nombre,estatus) VALUES(?,TRUE) ON CONFLICT(nombre) DO NOTHING",nombreRol);
        Long cuenta=jdbc.queryForObject("INSERT INTO cuentas_usuario(usuario,contrasena_hash,fecha_creacion,estado_verificacion,estatus,persona_id) VALUES(?,?,CURRENT_TIMESTAMP,?,TRUE,?) RETURNING id",Long.class,usuario,passwords.encode(PASSWORD),verificacion,persona);
        jdbc.update("INSERT INTO cuentas_roles(cuenta_id,rol_id) SELECT ?,id FROM roles WHERE nombre=?",cuenta,rol);
        Cliente anonimo=anonimo();
        var r=mvc.perform(post("/api/app/auth/login").session(anonimo.sesion()).header(anonimo.header(),anonimo.token()).contentType("application/json").content(json.writeValueAsString(Map.of("usuario",usuario,"password",PASSWORD)))).andExpect(status().isOk()).andReturn();
        var estado=json.readTree(r.getResponse().getContentAsString());
        return new Cliente((MockHttpSession)r.getRequest().getSession(),estado.get("csrfToken").asText(),estado.get("csrfHeader").asText(),usuario,persona,cuenta);
    }
    private Long persona(String nombre){return jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,fecha_registro,estatus) VALUES(?,'Prueba automatizada',CURRENT_TIMESTAMP,TRUE) RETURNING id",Long.class,nombre);}
}
