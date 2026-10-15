package mx.edu.itspa.biblioteca.dto;

import mx.edu.itspa.biblioteca.modelo.catalogo.*;
import java.time.LocalDate;

public record EjemplarRespuesta(
    Long id, Long libroId, String codigoBarras, LocalDate fechaAdquisicion,
    EstadoEjemplar estado, EstadoConservacion estadoConservacion, boolean estatus
) {
    public static EjemplarRespuesta desde(Ejemplar ejemplar) {
        return new EjemplarRespuesta(ejemplar.getId(), ejemplar.getLibro().getId(),
            ejemplar.getCodigoBarras(), ejemplar.getFechaAdquisicion(),
            ejemplar.getEstado(), ejemplar.getEstadoConservacion(), ejemplar.isEstatus());
    }
}
