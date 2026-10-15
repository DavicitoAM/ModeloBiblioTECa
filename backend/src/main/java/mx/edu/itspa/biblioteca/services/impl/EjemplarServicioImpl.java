package mx.edu.itspa.biblioteca.services.impl;

import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.EjemplarRespuesta;
import mx.edu.itspa.biblioteca.excepcion.RecursoNoEncontradoException;
import mx.edu.itspa.biblioteca.excepcion.ReglaNegocioException;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoEjemplar;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import mx.edu.itspa.biblioteca.repositorio.EjemplarRepositorio;
import mx.edu.itspa.biblioteca.repositorio.LibroRepositorio;
import mx.edu.itspa.biblioteca.services.EjemplarServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class EjemplarServicioImpl implements EjemplarServicio {
    private final EjemplarRepositorio ejemplares;
    private final LibroRepositorio libros;

    public EjemplarServicioImpl(EjemplarRepositorio ejemplares, LibroRepositorio libros) {
        this.ejemplares = ejemplares;
        this.libros = libros;
    }

    @Override
    @Transactional
    public EjemplarRespuesta registrar(CrearEjemplarSolicitud solicitud) {
        String codigo = solicitud.codigoBarras().trim();
        if (ejemplares.existsByCodigoBarras(codigo)) {
            throw new ReglaNegocioException("El código de barras ya está registrado");
        }
        Libro libro = libros.findById(solicitud.libroId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));
        Ejemplar ejemplar = new Ejemplar();
        libro.agregarEjemplar(ejemplar);
        ejemplar.setCodigoBarras(codigo);
        ejemplar.setFechaAdquisicion(solicitud.fechaAdquisicion() == null
            ? LocalDate.now() : solicitud.fechaAdquisicion());
        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplar.setEstadoConservacion(solicitud.estadoConservacion());
        ejemplar.setEstatus(true);
        return EjemplarRespuesta.desde(ejemplares.save(ejemplar));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EjemplarRespuesta> listarPorLibro(Long libroId) {
        if (!libros.existsById(libroId)) {
            throw new RecursoNoEncontradoException("Libro no encontrado");
        }
        return ejemplares.findByLibroId(libroId).stream().map(EjemplarRespuesta::desde).toList();
    }
}
