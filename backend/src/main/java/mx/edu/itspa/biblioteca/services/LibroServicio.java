package mx.edu.itspa.biblioteca.services;

import java.util.List;
import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.dto.LibroRespuesta;

public interface LibroServicio {
    LibroRespuesta registrar(CrearLibroSolicitud solicitud);
    LibroRespuesta buscar(Long id);
    List<LibroRespuesta> listar();
}
