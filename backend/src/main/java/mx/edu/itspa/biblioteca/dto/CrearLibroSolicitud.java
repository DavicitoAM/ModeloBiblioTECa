package mx.edu.itspa.biblioteca.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrearLibroSolicitud(
    @NotBlank String titulo,
    String descripcion,
    String edicion,
    Integer anioPublicacion,
    String isbn,
    @Min(1) Integer numPaginas
) {}
