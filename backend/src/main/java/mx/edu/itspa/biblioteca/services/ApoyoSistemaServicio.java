package mx.edu.itspa.biblioteca.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Casos M07–M09: DTOs acotados y descargas; ninguna entidad JPA se expone. */
public interface ApoyoSistemaServicio {
    Map<String, Object> inicio();
    Map<String, Object> panel();
    Map<String, Object> notificaciones(String estado, String tipo, boolean archivadas, LocalDate desde, LocalDate hasta);
    Map<String, Object> notificaciones(String estado, String tipo, boolean archivadas, LocalDate desde, LocalDate hasta, boolean gestion, Long cuentaId);
    Map<String, Object> leerNotificacion(long id);
    Map<String, Object> leerTodas();
    Map<String, Object> archivarNotificacion(long id);
    Map<String, Object> preferencias();
    Map<String, Object> guardarPreferencias(Map<String, Object> solicitud);
    List<Map<String, Object>> avisos(boolean gestion);
    Map<String, Object> guardarAviso(Long id, Map<String, Object> solicitud);
    Map<String, Object> plantillas();
    Map<String, Object> guardarPlantilla(Long id, Map<String, Object> solicitud);
    Map<String, Object> previsualizarPlantilla(Map<String, Object> solicitud);
    Map<String, Object> guardarCanal(long id, Map<String, Object> solicitud);
    Map<String, Object> reporte(String tipo, LocalDate desde, LocalDate hasta);
    Descarga exportar(String tipo, LocalDate desde, LocalDate hasta, String formato);
    Map<String, Object> configuracion();
    Map<String, Object> guardarConfiguracion(Map<String, Object> solicitud);
    List<Map<String, Object>> auditoria(String modulo, String accion, String usuario, LocalDate desde, LocalDate hasta);
    List<Map<String, Object>> respaldos();
    Map<String, Object> crearRespaldo();
    Descarga descargarRespaldo(long id);
    List<Map<String, Object>> incidentes();
    Map<String, Object> guardarIncidente(Long id, Map<String, Object> solicitud);
    void notificar(Long personaId, String tipo, String evento, String asunto, String mensaje, String entidad, Long entidadId);

    record Descarga(String nombre, String tipoContenido, byte[] contenido) { }
}
