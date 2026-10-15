package mx.edu.itspa.biblioteca;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.EjemplarRespuesta;
import mx.edu.itspa.biblioteca.excepcion.RecursoNoEncontradoException;
import mx.edu.itspa.biblioteca.excepcion.ReglaNegocioException;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoConservacion;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoEjemplar;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import mx.edu.itspa.biblioteca.repositorio.EjemplarRepositorio;
import mx.edu.itspa.biblioteca.repositorio.LibroRepositorio;
import mx.edu.itspa.biblioteca.services.impl.EjemplarServicioImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * M02, PDF pp. impresas 18-20 (fisicas 19-21), RF-M02-26/27/30 y RN-M02-03/04.
 * Comprueba alta y consulta de ejemplares con entidades reales y repositorios proxy Mockito.
 * No arranca Spring ni consulta PostgreSQL: no valida FK, UNIQUE, transacciones o mapeos JPA.
 * Las fechas automaticas se comprueban entre lecturas anterior y posterior para admitir medianoche.
 */
class EjemplarServicioTest {
    private EjemplarRepositorio ejemplares;
    private LibroRepositorio libros;
    private EjemplarServicioImpl servicio;

    @BeforeEach
    void prepararServicioSinSpring() {
        ejemplares = mock(EjemplarRepositorio.class, withSettings().mockMaker(MockMakers.PROXY));
        libros = mock(LibroRepositorio.class, withSettings().mockMaker(MockMakers.PROXY));
        servicio = new EjemplarServicioImpl(ejemplares, libros);
    }

    @Test
    void registrarRelacionaLibroRecortaCodigoYConservaFechaYCondicionFisica() {
        Libro libro = libro(41L);
        LocalDate fecha = LocalDate.of(2024, 3, 15);
        when(libros.findById(41L)).thenReturn(Optional.of(libro));
        simularIdAlGuardar(501L);

        EjemplarRespuesta respuesta = servicio.registrar(new CrearEjemplarSolicitud(
            41L, "  ITSPA-UNIT-001 \t", fecha, EstadoConservacion.REGULAR));

        assertThat(respuesta).isEqualTo(new EjemplarRespuesta(
            501L, 41L, "ITSPA-UNIT-001", fecha, EstadoEjemplar.DISPONIBLE,
            EstadoConservacion.REGULAR, true));
        assertThat(libro.getEjemplares()).singleElement().satisfies(guardado -> {
            assertThat(guardado.getId()).isEqualTo(respuesta.id());
            assertThat(guardado.getLibro()).isSameAs(libro);
            assertThat(guardado.getEstado()).isEqualTo(EstadoEjemplar.DISPONIBLE);
            assertThat(guardado.isEstatus()).isTrue();
        });
    }

    @Test
    void registrarSinFechaUtilizaElDiaDeLaOperacion() {
        when(libros.findById(42L)).thenReturn(Optional.of(libro(42L)));
        simularIdAlGuardar(502L);
        LocalDate antes = LocalDate.now();

        EjemplarRespuesta respuesta = servicio.registrar(new CrearEjemplarSolicitud(
            42L, "ITSPA-UNIT-002", null, EstadoConservacion.EXCELENTE));

        LocalDate despues = LocalDate.now();
        assertThat(respuesta.id()).isEqualTo(502L);
        assertThat(respuesta.libroId()).isEqualTo(42L);
        assertThat(respuesta.fechaAdquisicion()).isBetween(antes, despues);
        assertThat(respuesta.estadoConservacion()).isEqualTo(EstadoConservacion.EXCELENTE);
        assertThat(respuesta.estado()).isEqualTo(EstadoEjemplar.DISPONIBLE);
        assertThat(respuesta.estatus()).isTrue();
    }

    @Test
    void registrarRechazaLibroInexistenteSinGuardarEjemplar() {
        when(libros.findById(43L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(new CrearEjemplarSolicitud(
            43L, "ITSPA-UNIT-003", LocalDate.of(2025, 1, 10), EstadoConservacion.BUENO)))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("Libro");
        verify(ejemplares, never()).save(any(Ejemplar.class));
    }

    @Test
    void registrarRechazaCodigoDuplicadoTrasRecortarSinGuardar() {
        when(ejemplares.existsByCodigoBarras("ITSPA-UNIT-004")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new CrearEjemplarSolicitud(
            44L, " ITSPA-UNIT-004 ", null, EstadoConservacion.BUENO)))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("registrado");
        verify(ejemplares, never()).save(any(Ejemplar.class));
    }

    @Test
    void listarPorLibroExistenteSinCopiasDevuelveListaVacia() {
        when(libros.existsById(45L)).thenReturn(true);
        when(ejemplares.findByLibroId(45L)).thenReturn(List.of());

        assertThat(servicio.listarPorLibro(45L)).isEmpty();
    }

    @Test
    void listarPorLibroDevuelveVariosDtosConSuEstadoConsultado() {
        Libro libro = libro(46L);
        LocalDate primeraFecha = LocalDate.of(2024, 4, 2);
        LocalDate segundaFecha = LocalDate.of(2025, 5, 3);
        Ejemplar primero = ejemplar(601L, libro, "ITSPA-UNIT-005", primeraFecha,
            EstadoEjemplar.DISPONIBLE, EstadoConservacion.BUENO, true);
        Ejemplar segundo = ejemplar(602L, libro, "ITSPA-UNIT-006", segundaFecha,
            EstadoEjemplar.BAJA, EstadoConservacion.DANADO, false);
        when(libros.existsById(46L)).thenReturn(true);
        when(ejemplares.findByLibroId(46L)).thenReturn(List.of(primero, segundo));

        List<EjemplarRespuesta> respuesta = servicio.listarPorLibro(46L);

        assertThat(respuesta).containsExactly(
            new EjemplarRespuesta(601L, 46L, "ITSPA-UNIT-005", primeraFecha,
                EstadoEjemplar.DISPONIBLE, EstadoConservacion.BUENO, true),
            new EjemplarRespuesta(602L, 46L, "ITSPA-UNIT-006", segundaFecha,
                EstadoEjemplar.BAJA, EstadoConservacion.DANADO, false));
    }

    @Test
    void listarRechazaLibroInexistenteSinConsultarEjemplares() {
        when(libros.existsById(47L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.listarPorLibro(47L))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("Libro");
        verify(ejemplares, never()).findByLibroId(anyLong());
    }

    private void simularIdAlGuardar(Long id) {
        when(ejemplares.save(any(Ejemplar.class))).thenAnswer(invocacion -> {
            Ejemplar guardado = invocacion.getArgument(0);
            guardado.setId(id);
            return guardado;
        });
    }

    private static Libro libro(Long id) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitulo("Libro del circuito de ejemplares");
        return libro;
    }

    private static Ejemplar ejemplar(Long id, Libro libro, String codigo, LocalDate fecha,
            EstadoEjemplar estado, EstadoConservacion conservacion, boolean activo) {
        Ejemplar ejemplar = new Ejemplar();
        ejemplar.setId(id);
        ejemplar.setLibro(libro);
        ejemplar.setCodigoBarras(codigo);
        ejemplar.setFechaAdquisicion(fecha);
        ejemplar.setEstado(estado);
        ejemplar.setEstadoConservacion(conservacion);
        ejemplar.setEstatus(activo);
        return ejemplar;
    }
}
