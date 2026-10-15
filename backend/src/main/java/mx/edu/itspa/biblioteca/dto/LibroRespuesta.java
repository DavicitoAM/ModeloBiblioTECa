package mx.edu.itspa.biblioteca.dto;

import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;

public record LibroRespuesta(
    Long id, String titulo, String descripcion, String edicion,
    Integer anioPublicacion, String isbn, Integer numPaginas
) {
    public static LibroRespuesta desde(Libro libro) {
        return new LibroRespuesta(libro.getId(), libro.getTitulo(), libro.getDescripcion(),
            libro.getEdicion(), libro.getAnioPublicacion(), libro.getIsbn(), libro.getNumPaginas());
    }
}
