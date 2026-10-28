package mx.edu.itspa.biblioteca;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.edu.itspa.biblioteca.services.impl.ApoyoSistemaServicioImpl;
import mx.edu.itspa.biblioteca.services.impl.IdentidadActual;
import mx.edu.itspa.biblioteca.services.impl.SesionServicioImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

/** M07–M09 contra PostgreSQL aislado; todas las filas se revierten. */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers=GuardiaBasePruebas.class)
@Transactional
class ApoyoSistemaIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @TempDir Path carpeta;
    IdentidadActual identidad;
    ApoyoSistemaServicioImpl apoyo;
    long persona;
    long cuenta;

    @BeforeEach
    void preparar() {
        persona=jdbc.queryForObject("insert into personas(nombre,apellidos,fecha_registro,estatus) values('Prueba de apoyo','Transaccional',current_timestamp,true) returning id",Long.class);
        cuenta=jdbc.queryForObject("insert into cuentas_usuario(usuario,contrasena_hash,fecha_creacion,estatus,persona_id) values(?,?,current_timestamp,true,?) returning id",Long.class,"test-apoyo-"+UUID.randomUUID(),"HASH_QUE_NO_DEBE_EXPORTARSE",persona);
        identidad=new IdentidadActual();
        autenticar(cuenta);
        apoyo=new ApoyoSistemaServicioImpl(jdbc,json,identidad,carpeta.toString());
    }

    private void autenticar(long id) {
        var principal=new SesionServicioImpl.CuentaPrincipal(id,persona,"prueba-apoyo","Prueba de apoyo","","VERIFICADA",true,List.of("ADMINISTRADOR"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
    }

    @AfterEach
    void limpiarIdentidad() { SecurityContextHolder.clearContext(); }

    @Test
    @SuppressWarnings("unchecked")
    void notificacionIdempotenteEsPropiaYLecturaIndependienteDeEntrega() {
        String evento="TEST:"+UUID.randomUUID();
        apoyo.notificar(persona,"TEST_APOYO",evento,"Prueba de entrega","Mensaje persistente",null,null);
        apoyo.notificar(persona,"TEST_APOYO",evento,"Prueba de entrega","Mensaje persistente",null,null);
        List<Map<String,Object>> items=(List<Map<String,Object>>)apoyo.notificaciones(null,null,false,null,null).get("items");
        assertThat(items).hasSize(1);
        long id=((Number)items.getFirst().get("id")).longValue();
        assertThat(apoyo.leerNotificacion(id)).containsEntry("estado","LEIDA");
        assertThat(jdbc.queryForObject("select estado from envios_notificacion where notificacion_id=?",String.class,id)).isEqualTo("DISPONIBLE_EN_SISTEMA");
        autenticar(cuenta+999999);
        assertThatThrownBy(()->apoyo.leerNotificacion(id)).isInstanceOf(ResponseStatusException.class);
        assertThat((List<?>)apoyo.notificaciones(null,null,false,null,null).get("items")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void archivadoConservaHistorialYNotificacionesObligatoriasNoSeDesactivan() {
        apoyo.guardarPreferencias(Map.of("permite_sistema",false));
        apoyo.notificar(persona,"TEST_OBLIGATORIO","TEST:"+UUID.randomUUID(),"Confirmación","Dato confirmado",null,null);
        List<Map<String,Object>> items=(List<Map<String,Object>>)apoyo.notificaciones(null,null,false,null,null).get("items");
        assertThat(items).hasSize(1);
        long id=((Number)items.getFirst().get("id")).longValue();
        apoyo.archivarNotificacion(id);
        assertThat((List<?>)apoyo.notificaciones(null,null,false,null,null).get("items")).isEmpty();
        assertThat((List<?>)apoyo.notificaciones(null,null,true,null,null).get("items")).hasSize(1);
        assertThatThrownBy(()->apoyo.guardarPreferencias(Map.of("permite_correo",true))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("proveedor");
    }

    @Test
    void avisoConPeriodoInvalidoNoPersisteYLaEdicionSeAudita() {
        assertThatThrownBy(()->apoyo.guardarAviso(null,Map.of("titulo","No guardar","mensaje","Prueba","fecha_publicacion","2026-10-20T12:00:00","fecha_expiracion","2026-10-19T12:00:00"))).isInstanceOf(ResponseStatusException.class);
        Map<String,Object> aviso=apoyo.guardarAviso(null,Map.of("titulo","Aviso de prueba","mensaje","Información confirmada","publico",true));
        long id=((Number)aviso.get("id")).longValue();
        apoyo.guardarAviso(id,Map.of("titulo","Aviso actualizado","mensaje","Información corregida","publico",true,"estatus",false));
        assertThat(jdbc.queryForObject("select count(*) from bitacoras_auditoria where entidad='avisos_generales' and id_registro=?",Integer.class,id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from detalles_cambio d join bitacoras_auditoria b on b.id=d.evento_id where b.entidad='avisos_generales' and b.id_registro=? and d.campo='titulo' and d.valor_anterior='Aviso de prueba' and d.valor_nuevo='Aviso actualizado'",Integer.class,id)).isEqualTo(1);
    }

    @Test
    void reporteTieneFilasRealesYHistorialDeExportacion() {
        var descarga=apoyo.exportar("catalogo",LocalDate.now().withDayOfMonth(1),LocalDate.now(),"xlsx");
        assertThat(descarga.tipoContenido()).contains("spreadsheetml");
        assertThat(descarga.contenido()).startsWith((byte)'P',(byte)'K');
        assertThat(jdbc.queryForObject("select count(*) from exportaciones_reporte where responsable_id=? and estado='DESCARGA_GENERADA'",Integer.class,cuenta)).isEqualTo(1);
        assertThatThrownBy(()->apoyo.reporte("prestamos",LocalDate.now(),LocalDate.now().minusDays(1))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void respaldoEsDescargableVerificaIntegridadYOmiteHashDeCuenta() {
        Map<String,Object> registro=apoyo.crearRespaldo();
        assertThat(registro).containsEntry("estado","COMPLETADO");
        byte[] bytes=apoyo.descargarRespaldo(((Number)registro.get("id")).longValue()).contenido();
        assertThat(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).contains("ITSPA_EXPORTACION_SEGURA_V1").doesNotContain("HASH_QUE_NO_DEBE_EXPORTARSE").doesNotContain("contrasena_hash");
        assertThat(String.valueOf(registro.get("verificacion_integridad"))).hasSize(64);
    }

    @Test
    void panelYTodosLosTiposDeReporteConsultanElEsquemaReal() {
        assertThat(apoyo.inicio()).containsKeys("institucion","estadisticas","avisos");
        assertThat(apoyo.panel()).containsKeys("indicadores","mis_indicadores","actividad_semanal");
        for(String tipo:List.of("prestamos","devoluciones","reservas","sanciones","visitas","cubiculos","usuarios","auditoria")) {
            assertThat(apoyo.reporte(tipo,LocalDate.now().withDayOfMonth(1),LocalDate.now())).containsEntry("tipo",tipo).containsKeys("filas","columnas","id");
        }
    }

    @Test
    void configuracionValidaTiposYConservaAuditoriaDeCambios() {
        Map<String,Object> institucion=Map.of("nombre_institucion","Institución de prueba","nombre_biblioteca","Biblioteca de prueba","zona_horaria","America/Mexico_City","correo_contacto","biblioteca@example.org");
        Map<String,Object> solicitud=Map.of("institucion",institucion,"parametros",List.of(Map.of("clave","prueba.apoyo","valor","42","tipo_dato","ENTERO","esencial",true)),"seguridad",Map.of("inactividad_maxima_minutos",30,"intentos_fallidos_maximos",5,"bloqueo_temporal_minutos",15));
        assertThat(apoyo.guardarConfiguracion(solicitud)).containsKeys("institucion","parametros","seguridad");
        assertThat(jdbc.queryForObject("select valor from parametros_sistema where clave='prueba.apoyo'",String.class)).isEqualTo("42");
        assertThat(jdbc.queryForObject("select count(*) from bitacoras_auditoria where responsable_id=? and modulo='M09'",Integer.class,cuenta)).isGreaterThanOrEqualTo(3);
        assertThatThrownBy(()->apoyo.guardarConfiguracion(Map.of("institucion",Map.of("nombre_institucion","Prueba","nombre_biblioteca","Biblioteca","zona_horaria","Zona/Inexistente")))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Zona horaria");
    }

    @Test
    void plantillaUsaSoloVariablesPermitidasYSeAplicaSinModificarMensajeAnterior() {
        apoyo.notificar(persona,"TEST_PLANTILLA","TEST-PLANTILLA-1","Asunto original","Mensaje original",null,null);
        long tipo=jdbc.queryForObject("select id from tipos_notificacion where codigo='TEST_PLANTILLA'",Long.class);
        long canal=jdbc.queryForObject("select id from canales_notificacion where nombre='SISTEMA' order by id limit 1",Long.class);
        Map<String,Object> plantilla=Map.of("nombre","Plantilla transaccional","asunto","Hola {{nombre}}","mensaje","{{asunto}}: {{mensaje}}","tipo_notificacion_id",tipo,"canal_id",canal,"estatus",true);
        apoyo.guardarPlantilla(null,plantilla);
        apoyo.notificar(persona,"TEST_PLANTILLA","TEST-PLANTILLA-2","Confirmación","Registro confirmado",null,null);
        assertThat(jdbc.queryForObject("select asunto from notificaciones where destinatario_id=? and evento_origen='TEST-PLANTILLA-2'",String.class,cuenta)).isEqualTo("Hola Prueba de apoyo Transaccional");
        assertThat(jdbc.queryForObject("select mensaje from notificaciones where destinatario_id=? and evento_origen='TEST-PLANTILLA-1'",String.class,cuenta)).isEqualTo("Mensaje original");
        assertThatThrownBy(()->apoyo.previsualizarPlantilla(Map.of("asunto","Prueba {{password}}","mensaje","No permitido"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Variable no autorizada");
    }

    @Test
    @SuppressWarnings("unchecked")
    void lectorNoPuedeConsultarAuditoriaYPanelNoEntregaActividadGlobal() {
        var lector=new SesionServicioImpl.CuentaPrincipal(cuenta,persona,"prueba-apoyo","Prueba de apoyo","","VERIFICADA",true,List.of("LECTOR"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(lector,null,lector.getAuthorities()));
        assertThatThrownBy(()->apoyo.auditoria(null,null,null,null,null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->apoyo.reporte("catalogo",null,null)).isInstanceOf(ResponseStatusException.class);
        Map<String,Object> panel=apoyo.panel();
        assertThat((Map<String,Object>)panel.get("indicadores")).doesNotContainKeys("usuarios_activos","visitas_hoy");
        assertThat((List<Map<String,Object>>)panel.get("actividad")).allSatisfy(a->assertThat(a.get("modulo")).isEqualTo("M07"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void avisoPorGrupoRespetaSubtipoYNoSePublicaAVIsitantes() {
        Map<String,Object> docentes=apoyo.guardarAviso(null,Map.of("titulo","Sólo docentes","mensaje","Información dirigida","publico",false,"grupos",List.of("DOCENTE")));
        Map<String,Object> personas=apoyo.guardarAviso(null,Map.of("titulo","Grupo persona","mensaje","Información propia","publico",false,"grupos",List.of("PERSONA")));
        assertThat(apoyo.avisos(false)).extracting(a->a.get("id")).contains(personas.get("id")).doesNotContain(docentes.get("id"));
        assertThat((List<Map<String,Object>>)apoyo.inicio().get("avisos")).extracting(a->a.get("id")).doesNotContain(personas.get("id"),docentes.get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void indicadoresYReporteExcluyenDanadosInactivosYLibrosDeBaja() {
        long disponibles=((Number)((Map<String,Object>)apoyo.inicio().get("estadisticas")).get("disponibles")).longValue();
        long libro=jdbc.queryForObject("insert into libros(titulo,estatus) values('Disponibilidad de prueba',true) returning id",Long.class);
        long baja=jdbc.queryForObject("insert into libros(titulo,estatus) values('Libro de baja de prueba',false) returning id",Long.class);
        for(int i=0;i<4;i++)jdbc.update("insert into ejemplares(codigo_barras,fecha_adquisicion,estado,estado_conservacion,estatus,libro_id) values(?,current_date,'DISPONIBLE',?,?,?)", "TEST-DISP-"+UUID.randomUUID(),i==1?"DANADO":"BUENO",i!=2,i==3?baja:libro);
        assertThat(((Number)((Map<String,Object>)apoyo.inicio().get("estadisticas")).get("disponibles")).longValue()).isEqualTo(disponibles+1);
        assertThat(((Number)((Map<String,Object>)apoyo.panel().get("indicadores")).get("disponibles")).longValue()).isEqualTo(disponibles+1);
        var filas=(List<Map<String,Object>>)apoyo.reporte("catalogo",null,null).get("filas");
        assertThat(((Number)filas.stream().filter(f->((Number)f.get("id")).longValue()==libro).findFirst().orElseThrow().get("disponibles")).longValue()).isEqualTo(1);
        assertThat(((Number)filas.stream().filter(f->((Number)f.get("id")).longValue()==baja).findFirst().orElseThrow().get("disponibles")).longValue()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void cubiculoConUsoAbiertoPermaneceOcupadoTrasLaHoraPrevista() {
        long disponibles=((Number)((Map<String,Object>)apoyo.panel().get("indicadores")).get("cubiculos_disponibles")).longValue();
        long cubiculo=jdbc.queryForObject("insert into cubiculos(codigo,nombre,capacidad,estado,estatus) values(?,'Uso abierto de prueba',4,'HABILITADO',true) returning id",Long.class,"TEST-USO-"+UUID.randomUUID());
        long reserva=jdbc.queryForObject("insert into reservas_cubiculo(fecha_reserva,fecha_inicio,fecha_fin,estado,persona_id,cubiculo_id) values(current_timestamp-interval '3 hour',current_timestamp-interval '2 hour',current_timestamp-interval '1 hour','EN_USO',?,?) returning id",Long.class,persona,cubiculo);
        jdbc.update("insert into usos_cubiculo(fecha_inicio,estado,asistencia_confirmada,reserva_id) values(current_timestamp-interval '2 hour','EN_USO',true,?)",reserva);
        assertThat(((Number)((Map<String,Object>)apoyo.panel().get("indicadores")).get("cubiculos_disponibles")).longValue()).isEqualTo(disponibles);
        jdbc.update("update usos_cubiculo set fecha_fin=current_timestamp,estado='FINALIZADO' where reserva_id=?",reserva);
        jdbc.update("update reservas_cubiculo set estado='FINALIZADA' where id=?",reserva);
        assertThat(((Number)((Map<String,Object>)apoyo.panel().get("indicadores")).get("cubiculos_disponibles")).longValue()).isEqualTo(disponibles+1);
    }
}
