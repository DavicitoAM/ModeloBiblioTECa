package mx.edu.itspa.biblioteca.services;

import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import java.util.List;
import java.util.Map;

public interface PersonasServicio {
    List<Map<String,Object>> personas(String busqueda);
    Map<String,Object> persona(Long id);
    Map<String,Object> guardarPersona(Long id, PersonaSolicitud solicitud);
    List<Map<String,Object>> cuentas();
    Map<String,Object> guardarCuenta(Long id, CuentaSolicitud solicitud);
    List<Map<String,Object>> roles();
    Map<String,Object> guardarRol(Long id, RolSolicitud solicitud);
    Map<String,Object> catalogos();
    Map<String,Object> guardarCatalogo(String tipo, Long id, CatalogoSolicitud solicitud);
    List<Map<String,Object>> visitas(boolean activas, Long personaId);
    Map<String,Object> registrarVisita(VisitaSolicitud solicitud);
    Map<String,Object> salida(Long id);
    Map<String,Object> corregirVisita(Long id, CorreccionVisita solicitud);
    List<Map<String,Object>> verificaciones(Long cuentaId);
    Map<String,Object> verificar(Long cuentaId, VerificacionSolicitud solicitud);
}
