package mx.edu.itspa.biblioteca.services;

import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import java.util.Map;

public interface SesionServicio {
    boolean accesoDirecto();
    boolean inicializado();
    void inicializar(Inicializacion solicitud);
    Map<String,Object> usuarioActual();
    Map<String,Object> registro(RegistroSolicitud solicitud);
    Map<String,Object> perfil(Perfil solicitud);
    void cambiarPassword(CambioPassword solicitud);
}
