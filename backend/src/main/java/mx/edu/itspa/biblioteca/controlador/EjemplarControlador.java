package mx.edu.itspa.biblioteca.controlador;

import jakarta.validation.Valid;
import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.EjemplarRespuesta;
import mx.edu.itspa.biblioteca.services.EjemplarServicio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ejemplares")
public class EjemplarControlador {
    private final EjemplarServicio servicio;

    public EjemplarControlador(EjemplarServicio servicio) { this.servicio = servicio; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EjemplarRespuesta registrar(@Valid @RequestBody CrearEjemplarSolicitud solicitud) {
        return servicio.registrar(solicitud);
    }

    @GetMapping
    public List<EjemplarRespuesta> listarPorLibro(@RequestParam Long libroId) {
        return servicio.listarPorLibro(libroId);
    }
}
