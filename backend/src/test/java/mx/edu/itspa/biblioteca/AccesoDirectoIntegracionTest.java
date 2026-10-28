package mx.edu.itspa.biblioteca;

import com.fasterxml.jackson.databind.*;
import mx.edu.itspa.biblioteca.services.impl.SesionServicioImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pedido expreso: edición local sin login. Identidad técnica, CSRF y rollback PostgreSQL. */
@SpringBootTest(properties="biblioteca.acceso.directo=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers=GuardiaBasePruebas.class)
@Transactional
class AccesoDirectoIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired SesionServicioImpl sesiones;

    @Test void abreDirectamenteConIdentidadTecnicaSinLoginNiSesionAutenticada() throws Exception {
        var r=estado();var datos=json.readTree(r.getResponse().getContentAsString());
        assertThat(datos.get("accesoDirecto").asBoolean()).isTrue();
        assertThat(datos.get("inicializado").asBoolean()).isTrue();
        assertThat(datos.get("usuario").get("usuario").asText()).isEqualTo("SISTEMA_LOCAL");
        assertThat(datos.get("usuario").get("nombre").asText()).isEqualTo("Operación biblioteca");
        MockHttpSession session=(MockHttpSession)r.getRequest().getSession();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
        mvc.perform(get("/api/app/auth/estado").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.id").value(datos.get("usuario").get("id").asLong()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cuentas_usuario WHERE usuario='SISTEMA_LOCAL'",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sesiones_usuario s JOIN cuentas_usuario c ON c.id=s.cuenta_id WHERE c.usuario='SISTEMA_LOCAL'",Integer.class)).isZero();
    }
    @Test void escribeSinLoginConCsrfYAuditaComoOperacionTecnica() throws Exception {
        var r=estado();var datos=json.readTree(r.getResponse().getContentAsString());MockHttpSession session=(MockHttpSession)r.getRequest().getSession();
        String body=json.writeValueAsString(Map.of("nombre","Motivo técnico de prueba "+UUID.randomUUID()));
        mvc.perform(post("/api/app/catalogos-personas/motivos_visita").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
        var alta=mvc.perform(post("/api/app/catalogos-personas/motivos_visita").session(session).header(datos.get("csrfHeader").asText(),datos.get("csrfToken").asText()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(alta.getResponse().getContentAsString()).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bitacoras_auditoria b JOIN cuentas_usuario c ON c.id=b.responsable_id WHERE b.accion='GUARDAR_CATALOGO' AND b.id_registro=? AND c.usuario='SISTEMA_LOCAL'",Integer.class,id)).isEqualTo(1);
        mvc.perform(get("/api/app/personas").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("IDENTIDAD_TECNICA"))));
    }
    @Test void endpointsCredencialesNoAplicanYCuentaTecnicaNuncaAdmiteLogin() throws Exception {
        var r=estado();var datos=json.readTree(r.getResponse().getContentAsString());MockHttpSession session=(MockHttpSession)r.getRequest().getSession();String header=datos.get("csrfHeader").asText(),token=datos.get("csrfToken").asText();
        mvc.perform(post("/api/app/auth/login").session(session).header(header,token).contentType("application/json").content("{\"usuario\":\"SISTEMA_LOCAL\",\"password\":\"no-hay-credencial-humana\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/app/auth/password").session(session).header(header,token).contentType("application/json").content("{\"actual\":\"cualquier-actual\",\"nueva\":\"Nueva-prueba-2026!\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/app/auth/logout").session(session).header(header,token)).andExpect(status().isOk()).andExpect(jsonPath("$.accesoDirecto").value(true)).andExpect(jsonPath("$.usuario.usuario").value("SISTEMA_LOCAL"));
        assertThatThrownBy(()->sesiones.loadUserByUsername("SISTEMA_LOCAL")).isInstanceOf(UsernameNotFoundException.class);
    }
    private MvcResult estado() throws Exception {return mvc.perform(get("/api/app/auth/estado")).andExpect(status().isOk()).andReturn();}
}
