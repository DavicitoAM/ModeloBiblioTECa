package mx.edu.itspa.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoConservacion;
import java.time.LocalDate;

public record CrearEjemplarSolicitud(
    @NotNull @Positive Long libroId,
    @NotBlank String codigoBarras,
    LocalDate fechaAdquisicion,
    @NotNull EstadoConservacion estadoConservacion
) {}
