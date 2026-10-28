package mx.edu.itspa.biblioteca;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import mx.edu.itspa.biblioteca.services.OperacionBibliotecaServicio;
import mx.edu.itspa.biblioteca.services.impl.SesionServicioImpl.CuentaPrincipal;
import org.junit.jupiter.api.*;
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

/** M02–M06: transiciones completas con PostgreSQL aislado y reversión por prueba. */
@SpringBootTest(properties="biblioteca.acceso.directo=false")
@ActiveProfiles("test")
@ContextConfiguration(initializers=GuardiaBasePruebas.class)
@Transactional
class OperacionesBibliotecaIntegracionTest {
    @Autowired OperacionBibliotecaServicio servicio;
    @Autowired JdbcTemplate jdbc;
    long admin,cuentaAdmin,lector1,lector2,cuenta1,cuenta2,ubicacion;
    final String sufijo=UUID.randomUUID().toString().substring(0,8);
    final ZoneId zona=ZoneId.of("America/Mexico_City");
    @BeforeEach void preparar() {
        admin=persona("Administración prueba");cuentaAdmin=cuenta(admin,"admin");lector1=persona("Lector A prueba");cuenta1=cuenta(lector1,"lectorA");lector2=persona("Lector B prueba");cuenta2=cuenta(lector2,"lectorB");
        autenticar(cuentaAdmin,admin,"ADMINISTRADOR");
        ubicacion=id(servicio.crearCatalogo("ubicaciones",m("nombre","Prueba "+sufijo,"seccion","Test","estante","T1")));
        servicio.guardarPolitica("prestamo",null,m("tipo_usuario","EXTERNO","max_libros",3,"dias_prestamo",7,"permite_renovacion",true,"renovaciones_max",1));
        servicio.guardarPolitica("reserva-libro",null,m("tiempo_limite",2,"max_reservas",3,"reglas_prioridad","FIFO","tipos_usuario_permitidos","EXTERNO"));
    }
    @AfterEach void limpiarSesion(){SecurityContextHolder.clearContext();}
    @Test void prestamoMultipleDevolucionParcialYFinalConservanHistoria() {
        long libro=libro(),e1=ejemplar(libro,"1"),e2=ejemplar(libro,"2");long prestamo=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e1,e2))));
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e1)).isEqualTo("PRESTADO");
        long detalle=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=? AND ejemplar_id=?",prestamo,e1);
        servicio.devolver(prestamo,m("devoluciones",List.of(m("detalle_id",detalle,"estado_conservacion","BUENO"))));
        assertThat(texto("SELECT estado FROM prestamos WHERE id=?",prestamo)).isEqualTo("ACTIVO");
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e1)).isEqualTo("DISPONIBLE");
        long segundo=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=? AND ejemplar_id=?",prestamo,e2);
        servicio.devolver(prestamo,m("devoluciones",List.of(m("detalle_id",segundo,"estado_conservacion","DANADO","observaciones","Lomo dañado en revisión"))));
        assertThat(texto("SELECT estado FROM prestamos WHERE id=?",prestamo)).isEqualTo("FINALIZADO");
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e2)).isEqualTo("MANTENIMIENTO");
        assertThat(numero("SELECT count(*) FROM devoluciones WHERE detalle_prestamo_id IN (?,?)",detalle,segundo)).isEqualTo(2);
        assertThat(numero("SELECT count(*) FROM incidencias WHERE detalle_prestamo_id=? AND tipo='DANO'",segundo)).isEqualTo(1);
    }
    @Test void noPuedePrestarseDosVecesElMismoEjemplar() {
        long e=ejemplar(libro(),"DUP");servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e)));
        assertThatThrownBy(()->servicio.prestar(m("persona_id",lector2,"ejemplares_ids",List.of(e)))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("no está disponible");
        assertThat(numero("SELECT count(*) FROM detalles_prestamo WHERE ejemplar_id=? AND estado='ACTIVO'",e)).isEqualTo(1);
    }
    @Test void colaAsignaAlDevolverYCancelaLiberandoAlSiguiente() {
        long libro=libro(),e=ejemplar(libro,"COLA");long prestamo=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e))));
        long r2=id(servicio.reservar(m("persona_id",lector2,"libro_id",libro)));
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",r2)).isEqualTo("EN_ESPERA");
        long detalle=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",prestamo);
        servicio.devolver(prestamo,m("devoluciones",List.of(m("detalle_id",detalle,"estado_conservacion","BUENO"))));
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",r2)).isEqualTo("LISTA_PARA_RECOGER");
        long r1=id(servicio.reservar(m("persona_id",lector1,"libro_id",libro)));
        servicio.accionReserva(r2,"cancelar",m("motivo","La persona cancela la solicitud"));
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",r1)).isEqualTo("LISTA_PARA_RECOGER");
        servicio.accionReserva(r1,"atender",Map.of());
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",r1)).isEqualTo("ATENDIDA");
        assertThat(numero("SELECT detalle_prestamo_id FROM reservas_libro WHERE id=?",r1)).isPositive();
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e)).isEqualTo("PRESTADO");
    }
    @Test void renovacionConservaFechasYRespetaLimite() {
        long e=ejemplar(libro(),"REN");long p=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e))));long d=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p);
        LocalDate anterior=jdbc.queryForObject("SELECT fecha_vencimiento FROM detalles_prestamo WHERE id=?",LocalDate.class,d);
        servicio.renovar(p,m("detalle_id",d));
        assertThat(jdbc.queryForObject("SELECT fecha_vencimiento_anterior FROM renovaciones WHERE detalle_prestamo_id=?",LocalDate.class,d)).isEqualTo(anterior);
        assertThat(jdbc.queryForObject("SELECT fecha_vencimiento FROM detalles_prestamo WHERE id=?",LocalDate.class,d)).isEqualTo(anterior.plusDays(7));
        assertThatThrownBy(()->servicio.renovar(p,m("detalle_id",d))).hasMessageContaining("límite de renovaciones");
    }
    @Test void reservaDeOtroLectorImpideRenovar() {
        long libro=libro(),e=ejemplar(libro,"PRIOR");long p=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e))));long d=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p);
        servicio.reservar(m("persona_id",lector2,"libro_id",libro));
        assertThatThrownBy(()->servicio.renovar(p,m("detalle_id",d))).hasMessageContaining("Otra persona espera");
        assertThat(numero("SELECT count(*) FROM renovaciones WHERE detalle_prestamo_id=?",d)).isZero();
    }
    @Test void noSePermitenSolapesDeCubiculoNiDePersona() {
        configurarCubiculos();long c1=cubiculo("C1"),c2=cubiculo("C2");LocalDateTime inicio=LocalDate.now(zona).plusDays(1).atTime(10,0),fin=inicio.plusHours(1);
        servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c1,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString()));
        assertThatThrownBy(()->servicio.reservarCubiculo(m("persona_id",lector2,"cubiculo_id",c1,"fecha_inicio",inicio.plusMinutes(30).toString(),"fecha_fin",fin.plusMinutes(30).toString()))).hasMessageContaining("ya tienen una reserva");
        assertThatThrownBy(()->servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c2,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString()))).hasMessageContaining("ya tienen una reserva");
    }
    @Test void horarioYBloqueosSeAplicanYNoBorranReservas() {
        configurarCubiculos();long c=cubiculo("BLOQ");LocalDateTime inicio=LocalDate.now(zona).plusDays(1).atTime(12,0),fin=inicio.plusHours(1);
        long bloqueo=id(servicio.guardarBloqueo(m("cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString(),"motivo","Revisión del mobiliario")));
        assertThatThrownBy(()->servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString()))).hasMessageContaining("bloqueo administrativo");
        servicio.cancelarBloqueo(bloqueo,m("motivo","Revisión terminada"));long reserva=id(servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString())));
        assertThatThrownBy(()->servicio.guardarBloqueo(m("cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",fin.toString(),"motivo","Nuevo bloqueo"))).hasMessageContaining("reservaciones afectadas");
        assertThat(texto("SELECT estado FROM reservas_cubiculo WHERE id=?",reserva)).isEqualTo("CONFIRMADA");
    }
    @Test void multaConPagosParcialesNoAdmiteSobrepagoYConservaCondiciones() {
        long tipo=id(servicio.crearCatalogo("tipos_sancion",m("codigo","PRUEBA_"+sufijo,"nombre","Sanción de prueba")));
        Map<String,Object> politica=m("tipo_incidencia","DANO","tipo_sancion_id",tipo,"reglas","Cubrir la obligación documentada","multas_autorizadas",true,"permite_pagos_parciales",true,"monto_multa",new BigDecimal("100.00"),"tipo_usuario","EXTERNO","servicios_restringidos","PRESTAMO");
        long pol=id(servicio.guardarPolitica("sancion",null,politica));long sancion=id(servicio.sancionar(m("persona_id",lector1,"politica_id",pol,"tipo","DANO","descripcion","Daño documentado de prueba","motivo","Daño por revisión")));
        politica.put("permite_pagos_parciales",false);servicio.guardarPolitica("sancion",pol,politica);
        servicio.accionSancion(sancion,"pago",m("monto",new BigDecimal("40.00"),"metodo","EFECTIVO"));
        assertThat(jdbc.queryForObject("SELECT saldo_pendiente FROM multas WHERE sancion_id=?",BigDecimal.class,sancion)).isEqualByComparingTo("60.00");
        assertThatThrownBy(()->servicio.accionSancion(sancion,"pago",m("monto",new BigDecimal("61.00"),"metodo","EFECTIVO"))).hasMessageContaining("supera el saldo");
        servicio.accionSancion(sancion,"pago",m("monto",new BigDecimal("60.00"),"metodo","EFECTIVO"));
        assertThat(texto("SELECT estado FROM sanciones WHERE id=?",sancion)).isEqualTo("CUMPLIDA");
        servicio.accionSancion(sancion,"resolver",m("motivo","Obligación cubierta"));
        assertThat(texto("SELECT estado FROM sanciones WHERE id=?",sancion)).isEqualTo("RESUELTA");
        assertThat(numero("SELECT count(*) FROM pagos_multa WHERE multa_id=(SELECT id FROM multas WHERE sancion_id=?)",sancion)).isEqualTo(2);
    }
    @Test void lectorVeSoloSusOperacionesYNoPuedeModificarLasDeOtraPersona() {
        long e=ejemplar(libro(),"PRIV");long p=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e))));long d=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p);
        autenticar(cuenta2,lector2,"LECTOR");assertThat(servicio.prestamos()).noneMatch(x->id(x)==p);
        assertThatThrownBy(()->servicio.renovar(p,m("detalle_id",d))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("permiso");
    }
    @Test void reservaDuplicadaYCuentaNoVerificadaNoSeAutorizan() {
        long libro=libro();ejemplar(libro,"DRES");servicio.reservar(m("persona_id",lector1,"libro_id",libro));
        assertThatThrownBy(()->servicio.reservar(m("persona_id",lector1,"libro_id",libro))).hasMessageContaining("Ya existe una reserva activa");
        jdbc.update("UPDATE cuentas_usuario SET estado_verificacion='PENDIENTE' WHERE id=?",cuenta2);
        assertThatThrownBy(()->servicio.reservar(m("persona_id",lector2,"libro_id",libro))).hasMessageContaining("activa y verificada");
    }
    @Test void copiaEnCirculacionNoSePuedeDarDeBajaDesdeInventario() {
        long libro=libro(),e=ejemplar(libro,"INV");servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e)));
        assertThatThrownBy(()->servicio.guardarEjemplar(e,m("libro_id",libro,"codigo_barras","TEST-OPS-"+sufijo+"INV","estado","BAJA","ubicacion_id",ubicacion,"estado_conservacion","BUENO"))).hasMessageContaining("circulación activa");
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e)).isEqualTo("PRESTADO");
    }
    @Test void plazoVencidoLiberaLaCopiaYNotificaASiguienteReserva() {
        long libro=libro(),e=ejemplar(libro,"EXP");long primera=id(servicio.reservar(m("persona_id",lector1,"libro_id",libro)));long segunda=id(servicio.reservar(m("persona_id",lector2,"libro_id",libro)));
        jdbc.update("UPDATE reservas_libro SET fecha_limite=? WHERE id=?",LocalDate.now(zona).minusDays(1),primera);
        servicio.reservas();
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",primera)).isEqualTo("VENCIDA");
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",segunda)).isEqualTo("LISTA_PARA_RECOGER");
        assertThat(texto("SELECT estado FROM ejemplares WHERE id=?",e)).isEqualTo("RESERVADO");
    }
    @Test void vencidoNoSeRenuevaYGeneraIncidenciaAlDevolver() {
        long e=ejemplar(libro(),"VENC");long p=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e))));long detalle=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p);
        jdbc.update("UPDATE detalles_prestamo SET fecha_vencimiento=? WHERE id=?",LocalDate.now(zona).minusDays(2),detalle);
        assertThatThrownBy(()->servicio.renovar(p,m("detalle_id",detalle))).hasMessageContaining("no vencidos");
        servicio.devolver(p,m("devoluciones",List.of(m("detalle_id",detalle,"estado_conservacion","BUENO"))));
        assertThat(numero("SELECT count(*) FROM incidencias WHERE detalle_prestamo_id=? AND tipo='RETRASO'",detalle)).isEqualTo(1);
    }
    @Test void prioridadConfiguradaOrdenaTiposYConservaOrdenDeLlegadaEnEmpates() {
        long docente=persona("Docente de prueba");cuenta(docente,"docente");jdbc.update("DELETE FROM usuarios_externos WHERE id=?",docente);long departamento=jdbc.queryForObject("INSERT INTO departamentos(nombre,estatus) VALUES (?,true) RETURNING id",Long.class,"Departamento prueba "+sufijo);jdbc.update("INSERT INTO docentes(id,departamento_id) VALUES (?,?)",docente,departamento);
        servicio.guardarPolitica("reserva-libro",null,m("tiempo_limite",2,"max_reservas",3,"reglas_prioridad","DOCENTE,ESTUDIANTE,EMPLEADO,EXTERNO","tipos_usuario_permitidos","EXTERNO,DOCENTE"));
        long libro=libro();long externa=id(servicio.reservar(m("persona_id",lector1,"libro_id",libro)));long prioritaria=id(servicio.reservar(m("persona_id",docente,"libro_id",libro)));
        assertThat(numero("SELECT posicion_espera FROM reservas_libro WHERE id=?",prioritaria)).isEqualTo(1);
        assertThat(numero("SELECT posicion_espera FROM reservas_libro WHERE id=?",externa)).isEqualTo(2);
        ejemplar(libro,"PRIORIDAD");
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",prioritaria)).isEqualTo("LISTA_PARA_RECOGER");
        assertThat(texto("SELECT estado FROM reservas_libro WHERE id=?",externa)).isEqualTo("EN_ESPERA");
    }
    @Test void ubicacionConCopiasActivasNoSeDesactivaNiSeAsignaUnaInactiva() {
        long libro=libro();ejemplar(libro,"UBIC");
        assertThatThrownBy(()->servicio.editarCatalogo("ubicaciones",ubicacion,m("nombre","Ubicación prueba","seccion","Test","estante","T1","estatus",false))).hasMessageContaining("Reasigne los ejemplares activos");
        assertThat(jdbc.queryForObject("SELECT estatus FROM ubicaciones WHERE id=?",Boolean.class,ubicacion)).isTrue();
        long inactiva=id(servicio.crearCatalogo("ubicaciones",m("nombre","Inactiva prueba","seccion","Test","estante","T2","estatus",false)));
        assertThatThrownBy(()->servicio.guardarEjemplar(null,m("libro_id",libro,"codigo_barras","TEST-OPS-"+sufijo+"INACT","ubicacion_id",inactiva,"estado_conservacion","BUENO"))).hasMessageContaining("ubicación seleccionada está inactiva");
    }
    @Test void registroManualDeInasistenciaNoSeReviertePorAutoexpiracion() {
        configurarCubiculos();long c=cubiculo("AUSENCIA");LocalDateTime inicio=LocalDate.now(zona).plusDays(1).atTime(10,0);long r=id(servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",inicio.plusHours(1).toString())));
        LocalDateTime pasado=LocalDateTime.now(zona).minusHours(2);jdbc.update("UPDATE reservas_cubiculo SET fecha_inicio=?,fecha_fin=? WHERE id=?",pasado,pasado.plusHours(1),r);
        servicio.accionCubiculo(r,"no-presentada",Map.of());
        assertThat(texto("SELECT estado FROM reservas_cubiculo WHERE id=?",r)).isEqualTo("NO_PRESENTADA");
        assertThat(numero("SELECT count(*) FROM incidencias WHERE reserva_cubiculo_id=? AND tipo='AUSENCIA_RESERVACION_CUBICULO'",r)).isEqualTo(1);
    }
    @Test void usoRealActivoImpideDeshabilitarAunqueHayaPasadoElFinPrevisto() {
        configurarCubiculos();long c=cubiculo("OCUPADO");LocalDateTime inicio=LocalDate.now(zona).plusDays(1).atTime(10,0);long r=id(servicio.reservarCubiculo(m("persona_id",lector1,"cubiculo_id",c,"fecha_inicio",inicio.toString(),"fecha_fin",inicio.plusHours(1).toString())));
        LocalDateTime pasado=LocalDateTime.now(zona).minusHours(2);jdbc.update("UPDATE reservas_cubiculo SET estado='EN_USO',fecha_inicio=?,fecha_fin=? WHERE id=?",pasado,pasado.plusHours(1),r);jdbc.update("INSERT INTO usos_cubiculo(fecha_inicio,estado,asistencia_confirmada,reserva_id) VALUES (?,'EN_USO',true,?)",pasado,r);
        assertThatThrownBy(()->servicio.guardarCubiculo(c,m("codigo","OCUPADO"+sufijo,"nombre","Cubículo de prueba","capacidad",4,"estado","MANTENIMIENTO"))).hasMessageContaining("uso activo");
        assertThat(texto("SELECT estado FROM cubiculos WHERE id=?",c)).isEqualTo("HABILITADO");
    }
    @Test void restriccionSoloRenovacionNoImpidePrestamoNuevoPeroSiRenovar() {
        long libro=libro(),e1=ejemplar(libro,"SR1"),e2=ejemplar(libro,"SR2");long p=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e1))));long detalle=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p);
        long tipo=id(servicio.crearCatalogo("tipos_sancion",m("codigo","RENOVACION_TEST_"+sufijo,"nombre","Restricción de prueba")));long pol=id(servicio.guardarPolitica("sancion",null,m("tipo_incidencia","DANO","tipo_sancion_id",tipo,"reglas","Restricción de renovación de prueba","servicios_restringidos","RENOVACION")));
        servicio.sancionar(m("persona_id",lector1,"politica_id",pol,"tipo","DANO","descripcion","Incidencia para validar la restricción específica","motivo","Validación de regla RENOVACION"));
        assertThat(id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e2))))).isPositive();
        assertThatThrownBy(()->servicio.renovar(p,m("detalle_id",detalle))).hasMessageContaining("restricción activa para RENOVACION");
    }
    @Test void reposicionNoPuedeConfigurarseParaInasistencias() {
        long tipo=id(servicio.crearCatalogo("tipos_sancion",m("codigo","REPOSICION_TEST_"+sufijo,"nombre","Reposición de prueba")));
        assertThatThrownBy(()->servicio.guardarPolitica("sancion",null,m("tipo_incidencia","AUSENCIA_RESERVACION_CUBICULO","tipo_sancion_id",tipo,"reglas","Política incompatible"))).hasMessageContaining("solo corresponde a daño o extravío");
    }
    @Test void condicionesOriginalesDeSancionNoSeTruncanYSeValidaSuCapacidad() {
        long tipo=id(servicio.crearCatalogo("tipos_sancion",m("codigo","CONDICIONES_TEST_"+sufijo,"nombre","Condiciones de prueba")));String reglas="Regla completa "+"x".repeat(1900);Map<String,Object> datos=m("tipo_incidencia","DANO","tipo_sancion_id",tipo,"reglas",reglas);
        long pol=id(servicio.guardarPolitica("sancion",null,datos));long sancion=id(servicio.sancionar(m("persona_id",lector1,"politica_id",pol,"tipo","DANO","descripcion","Conservación de condiciones","motivo","Verificar condiciones originales")));
        assertThat(texto("SELECT condiciones_cumplimiento FROM sanciones WHERE id=?",sancion)).contains(reglas);
        datos.put("reglas","x".repeat(3000));assertThatThrownBy(()->servicio.guardarPolitica("sancion",null,datos)).hasMessageContaining("límite de 3000 caracteres");
    }
    @Test void disponibilidadNoAnunciaMaterialDanadoAunqueUnRegistroLegadoDigaDisponible() {
        long libro=libro(),e=ejemplar(libro,"ESTADO");jdbc.update("UPDATE ejemplares SET estado_conservacion='DANADO' WHERE id=?",e);
        assertThat(((Number)servicio.libro(libro).get("disponibles")).longValue()).isZero();
        assertThat(servicio.catalogo(null,null,"DISPONIBLE")).noneMatch(l->id(l)==libro);
    }
    @Test void incidenciasConEventoNoSeDuplicanOtroDiaNiMezclanDetallesDePrestamos() {
        long libro=libro(),e1=ejemplar(libro,"INC1"),e2=ejemplar(libro,"INC2");long p1=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e1)))),p2=id(servicio.prestar(m("persona_id",lector1,"ejemplares_ids",List.of(e2))));long detalle=numero("SELECT id FROM detalles_prestamo WHERE prestamo_id=?",p1);
        Map<String,Object> datos=m("persona_id",lector1,"tipo","DANO","descripcion","Evento de daño identificado","motivo","Validar evento único","prestamo_id",p1,"detalle_prestamo_id",detalle,"ejemplar_id",e1);long incidencia=id(servicio.registrarIncidencia(datos));
        jdbc.update("UPDATE incidencias SET fecha=? WHERE id=?",LocalDateTime.now(zona).minusDays(1),incidencia);
        assertThatThrownBy(()->servicio.registrarIncidencia(datos)).hasMessageContaining("mismo evento y motivo");
        datos.put("prestamo_id",p2);assertThatThrownBy(()->servicio.registrarIncidencia(datos)).hasMessageContaining("detalle y el préstamo no corresponden");
    }
    private void configurarCubiculos(){servicio.guardarPolitica("cubiculo",null,m("tiempo_minimo",30,"tiempo_maximo",180,"anticipacion_max",10,"max_reservas",3,"tolerancia_llegada",10,"tipos_usuario_permitidos","EXTERNO"));LocalDate fecha=LocalDate.now(zona).plusDays(1);servicio.guardarHorario(null,m("fecha_especial",fecha.toString(),"hora_apertura","08:00","hora_cierre","20:00","cerrado",false));}
    private long cubiculo(String codigo){return id(servicio.guardarCubiculo(null,m("codigo",codigo+sufijo,"nombre","Cubículo de prueba","capacidad",4,"estado","HABILITADO")));}
    private long libro(){return id(servicio.guardarLibro(null,m("titulo","Libro de prueba "+sufijo)));}
    private long ejemplar(long libro,String codigo){return id(servicio.guardarEjemplar(null,m("libro_id",libro,"codigo_barras","TEST-OPS-"+sufijo+codigo,"ubicacion_id",ubicacion,"estado_conservacion","BUENO")));}
    private long persona(String nombre){long id=jdbc.queryForObject("INSERT INTO personas(nombre,apellidos,fecha_registro,estatus) VALUES (?, 'Operaciones',CURRENT_TIMESTAMP,true) RETURNING id",Long.class,nombre);jdbc.update("INSERT INTO usuarios_externos(id,institucion) VALUES (?,'Prueba aislada')",id);return id;}
    private long cuenta(long persona,String usuario){return jdbc.queryForObject("INSERT INTO cuentas_usuario(persona_id,usuario,contrasena_hash,fecha_creacion,estado_verificacion,estatus) VALUES (?,?, 'TEST_ONLY_NO_AUTHENTICATION',CURRENT_TIMESTAMP,'VERIFICADA',true) RETURNING id",Long.class,persona,usuario+sufijo);}
    private void autenticar(long cuenta,long persona,String rol){CuentaPrincipal p=new CuentaPrincipal(cuenta,persona,"test","Prueba",null,"VERIFICADA",true,List.of(rol));SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p,null,p.getAuthorities()));}
    private long numero(String sql,Object...args){return Objects.requireNonNull(jdbc.queryForObject(sql,Long.class,args));}
    private String texto(String sql,Object...args){return jdbc.queryForObject(sql,String.class,args);}
    private static long id(Map<String,Object>r){return((Number)r.get("id")).longValue();}
    private static Map<String,Object> m(Object...p){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<p.length;i+=2)m.put(p[i].toString(),p[i+1]);return m;}
}
