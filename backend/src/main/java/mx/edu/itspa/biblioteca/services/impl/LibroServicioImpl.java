package mx.edu.itspa.biblioteca.services.impl;

import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.dto.LibroRespuesta;
import mx.edu.itspa.biblioteca.excepcion.RecursoNoEncontradoException;
import mx.edu.itspa.biblioteca.excepcion.ReglaNegocioException;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import mx.edu.itspa.biblioteca.repositorio.LibroRepositorio;
import mx.edu.itspa.biblioteca.services.LibroServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LibroServicioImpl implements LibroServicio {
    private final LibroRepositorio repositorio;

    public LibroServicioImpl(LibroRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public LibroRespuesta registrar(CrearLibroSolicitud solicitud) {
        String isbn = solicitud.isbn() == null || solicitud.isbn().isBlank()
            ? null : solicitud.isbn().trim();
        if (isbn != null && repositorio.existsByIsbn(isbn)) {
            throw new ReglaNegocioException("Ya existe un libro con ese ISBN");
        }
        Libro libro = new Libro();
        libro.setTitulo(solicitud.titulo().trim());
        libro.setDescripcion(solicitud.descripcion());
        libro.setEdicion(solicitud.edicion());
        libro.setAnioPublicacion(solicitud.anioPublicacion());
        libro.setIsbn(isbn);
        libro.setNumPaginas(solicitud.numPaginas());
        return LibroRespuesta.desde(repositorio.save(libro));
    }

    @Override
    @Transactional(readOnly = true)
    public LibroRespuesta buscar(Long id) {
        return LibroRespuesta.desde(repositorio.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibroRespuesta> listar() {
        return repositorio.findAll().stream().map(LibroRespuesta::desde).toList();
    }
}
