package mx.edu.itspa.biblioteca.services;

import java.util.List;
import java.util.Map;

/** M02–M06: contratos DTO de la aplicación; nunca expone entidades JPA. */
public interface OperacionBibliotecaServicio {
    List<Map<String,Object>> catalogo(String q,String categoria,String disponibilidad);
    Map<String,Object> libro(long id);
    Map<String,Object> guardarLibro(Long id,Map<String,Object> datos);
    List<Map<String,Object>> ejemplares(Long libroId);
    Map<String,Object> guardarEjemplar(Long id,Map<String,Object> datos);
    Map<String,Object> catalogos();
    Map<String,Object> crearCatalogo(String tipo,Map<String,Object> datos);
    Map<String,Object> editarCatalogo(String tipo,long id,Map<String,Object> datos);
    List<Map<String,Object>> politicas(String tipo);
    Map<String,Object> guardarPolitica(String tipo,Long id,Map<String,Object> datos);
    List<Map<String,Object>> prestamos();
    Map<String,Object> prestar(Map<String,Object> datos);
    Map<String,Object> devolver(long id,Map<String,Object> datos);
    Map<String,Object> renovar(long id,Map<String,Object> datos);
    List<Map<String,Object>> reservas();
    Map<String,Object> reservar(Map<String,Object> datos);
    Map<String,Object> accionReserva(long id,String accion,Map<String,Object> datos);
    List<Map<String,Object>> cubiculos();
    Map<String,Object> guardarCubiculo(Long id,Map<String,Object> datos);
    List<Map<String,Object>> horarios();
    Map<String,Object> guardarHorario(Long id,Map<String,Object> datos);
    List<Map<String,Object>> bloqueos();
    Map<String,Object> guardarBloqueo(Map<String,Object> datos);
    Map<String,Object> cancelarBloqueo(long id,Map<String,Object> datos);
    List<Map<String,Object>> reservasCubiculo();
    Map<String,Object> reservarCubiculo(Map<String,Object> datos);
    Map<String,Object> accionCubiculo(long id,String accion,Map<String,Object> datos);
    List<Map<String,Object>> sanciones();
    List<Map<String,Object>> incidencias();
    Map<String,Object> registrarIncidencia(Map<String,Object> datos);
    Map<String,Object> sancionar(Map<String,Object> datos);
    Map<String,Object> accionSancion(long id,String accion,Map<String,Object> datos);
}
