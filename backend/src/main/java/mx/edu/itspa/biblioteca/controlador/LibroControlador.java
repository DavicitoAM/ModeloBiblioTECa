package mx.edu.itspa.biblioteca.controlador;

import jakarta.validation.Valid;
import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.dto.LibroRespuesta;
import mx.edu.itspa.biblioteca.services.LibroServicio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/libros")
public class LibroControlador {
    private final LibroServicio servicio;

    public LibroControlador(LibroServicio servicio) { this.servicio = servicio; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibroRespuesta registrar(@Valid @RequestBody CrearLibroSolicitud solicitud) {
        return servicio.registrar(solicitud);
    }

    @GetMapping("/{id}")
    public LibroRespuesta buscar(@PathVariable Long id) { return servicio.buscar(id); }

    @GetMapping
    public List<LibroRespuesta> listar() { return servicio.listar(); }
}
