package mx.edu.itspa.biblioteca;

import java.util.List;
import java.util.Optional;
import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.dto.LibroRespuesta;
import mx.edu.itspa.biblioteca.excepcion.RecursoNoEncontradoException;
import mx.edu.itspa.biblioteca.excepcion.ReglaNegocioException;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import mx.edu.itspa.biblioteca.repositorio.LibroRepositorio;
import mx.edu.itspa.biblioteca.services.impl.LibroServicioImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * M02, PDF pp. impresas 16-20 (fisicas 17-21), RF-M02-01/06/07 y RN-M02-03/11/12.
 * Comprueba el comportamiento existente del servicio con DTOs y entidades reales.
 * Solo el repositorio es un proxy Mockito; no arranca Spring ni conecta a una BD.
 * La normalizacion de ISBN probada se limita a trim y vacios, sin reglas bibliograficas nuevas.
 * Estas pruebas no validan transacciones, mapeos JPA ni restricciones PostgreSQL.
 */
class LibroServicioTest {
    private LibroRepositorio repositorio;
    private LibroServicioImpl servicio;

    @BeforeEach
    void prepararServicioSinSpring() {
        repositorio = mock(LibroRepositorio.class, withSettings().mockMaker(MockMakers.PROXY));
        servicio = new LibroServicioImpl(repositorio);
    }

    @Test
    void registrarDevuelveTodosLosCamposYRecortaElTitulo() {
        simularIdAlGuardar(101L);
        var solicitud = new CrearLibroSolicitud(
            "  Estructuras de datos  ", "Texto de la practica", "Segunda", 2024,
            "9780000000019", 320);

        LibroRespuesta respuesta = servicio.registrar(solicitud);

        assertThat(respuesta).isEqualTo(new LibroRespuesta(
            101L, "Estructuras de datos", "Texto de la practica", "Segunda", 2024,
            "9780000000019", 320));
    }

    @Test
    void registrarRecortaLosEspaciosExterioresDelIsbn() {
        simularIdAlGuardar(102L);

        LibroRespuesta respuesta = servicio.registrar(solicitudConIsbn("  9780000000019 \t"));

        assertThat(respuesta.id()).isEqualTo(102L);
        assertThat(respuesta.isbn()).isEqualTo("9780000000019");
    }

    @Test
    void registrarAdmiteIsbnNulo() {
        simularIdAlGuardar(103L);

        LibroRespuesta respuesta = servicio.registrar(solicitudConIsbn(null));

        assertThat(respuesta.id()).isEqualTo(103L);
        assertThat(respuesta.isbn()).isNull();
    }

    @Test
    void registrarConvierteIsbnVacioEnNulo() {
        simularIdAlGuardar(104L);

        LibroRespuesta respuesta = servicio.registrar(solicitudConIsbn(""));

        assertThat(respuesta.id()).isEqualTo(104L);
        assertThat(respuesta.isbn()).isNull();
    }

    @Test
    void registrarConvierteIsbnDeEspaciosEnNulo() {
        simularIdAlGuardar(105L);

        LibroRespuesta respuesta = servicio.registrar(solicitudConIsbn(" \t  "));

        assertThat(respuesta.id()).isEqualTo(105L);
        assertThat(respuesta.isbn()).isNull();
    }

    @Test
    void registrarRechazaIsbnDuplicadoTrasRecortarSinGuardar() {
        when(repositorio.existsByIsbn("9780000000019")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(solicitudConIsbn(" 9780000000019 ")))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("ISBN");
        verify(repositorio, never()).save(any(Libro.class));
    }

    @Test
    void buscarDevuelveLosDatosDelLibroExistente() {
        Libro libro = libro(21L, "Redes", "Primera", 2023, "9780000000026", 180);
        when(repositorio.findById(21L)).thenReturn(Optional.of(libro));

        LibroRespuesta respuesta = servicio.buscar(21L);

        assertThat(respuesta).isEqualTo(new LibroRespuesta(
            21L, "Redes", "Descripcion de Redes", "Primera", 2023, "9780000000026", 180));
    }

    @Test
    void buscarRechazaUnLibroInexistente() {
        when(repositorio.findById(22L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscar(22L))
            .isInstanceOf(RecursoNoEncontradoException.class)
            .hasMessageContaining("Libro");
    }

    @Test
    void listarDevuelveUnaListaVaciaCuandoNoHayLibros() {
        when(repositorio.findAll()).thenReturn(List.of());

        assertThat(servicio.listar()).isEmpty();
    }

    @Test
    void listarDevuelveVariosLibrosComoDtosConSusDatos() {
        Libro primero = libro(31L, "Bases de datos", "Segunda", 2024, "9780000000033", 250);
        Libro segundo = libro(32L, "Algoritmos", "Tercera", 2025, null, 410);
        when(repositorio.findAll()).thenReturn(List.of(primero, segundo));

        List<LibroRespuesta> respuesta = servicio.listar();

        assertThat(respuesta).containsExactly(
            new LibroRespuesta(31L, "Bases de datos", "Descripcion de Bases de datos",
                "Segunda", 2024, "9780000000033", 250),
            new LibroRespuesta(32L, "Algoritmos", "Descripcion de Algoritmos",
                "Tercera", 2025, null, 410));
    }

    private void simularIdAlGuardar(Long id) {
        when(repositorio.save(any(Libro.class))).thenAnswer(invocacion -> {
            Libro guardado = invocacion.getArgument(0);
            guardado.setId(id);
            return guardado;
        });
    }

    private static CrearLibroSolicitud solicitudConIsbn(String isbn) {
        return new CrearLibroSolicitud("Libro de prueba", "Descripcion", "Primera", 2025, isbn, 100);
    }

    private static Libro libro(Long id, String titulo, String edicion, Integer anio, String isbn, Integer paginas) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitulo(titulo);
        libro.setDescripcion("Descripcion de " + titulo);
        libro.setEdicion(edicion);
        libro.setAnioPublicacion(anio);
        libro.setIsbn(isbn);
        libro.setNumPaginas(paginas);
        return libro;
    }
}
