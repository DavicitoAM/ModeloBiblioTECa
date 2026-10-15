package mx.edu.itspa.biblioteca.services;

import java.util.List;
import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.EjemplarRespuesta;

public interface EjemplarServicio {
    EjemplarRespuesta registrar(CrearEjemplarSolicitud solicitud);
    List<EjemplarRespuesta> listarPorLibro(Long libroId);
}
