package mx.edu.itspa.biblioteca;

import jakarta.persistence.EntityManager;
import mx.edu.itspa.biblioteca.dto.*;
import mx.edu.itspa.biblioteca.excepcion.*;
import mx.edu.itspa.biblioteca.modelo.catalogo.*;
import mx.edu.itspa.biblioteca.repositorio.*;
import mx.edu.itspa.biblioteca.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** M02, impresa 16–21 / visor 17–22, RF-M02-01/07/26/30, RN-M02-01/04/11/12.
 * Valida todo el esquema; modifica solamente libros y ejemplares. */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = GuardiaBasePruebas.class)
@Transactional
class ModeloPersistenciaIntegracionTest {
    @Autowired LibroRepositorio libros;
    @Autowired EjemplarRepositorio ejemplares;
    @Autowired LibroServicio libroServicio;
    @Autowired EjemplarServicio ejemplarServicio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    private final List<Long> librosCreados = new ArrayList<>();

    @Test
    void esquemaCompletoYAmbasMigracionesEstanActivos() {
        assertThat(em.getMetamodel().getEntities()).hasSize(70);
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank", String.class))
            .containsExactly("1", "2");
    }

    @Test
    void libroPuedeExistirSinEjemplaresNiIdioma() {
        Libro libro = nuevoLibro(null);
        em.flush(); em.clear();
        Libro recargado = libros.findById(libro.getId()).orElseThrow();
        assertThat(recargado.getEjemplares()).isEmpty();
        assertThat(recargado.getIdioma()).isNull();
    }

    @Test
    void repositoriosRecarganVariosEjemplaresDeUnLibro() {
        Libro libro = nuevoLibro(null);
        for (int i = 0; i < 2; i++) {
            Ejemplar e = nuevoEjemplar(libro, "TEST-" + UUID.randomUUID());
            libro.agregarEjemplar(e);
            ejemplares.saveAndFlush(e);
        }
        assertThat(libro.getEjemplares()).hasSize(2);
        em.clear();
        Libro recargado = libros.findById(libro.getId()).orElseThrow();
        assertThat(recargado.getEjemplares()).hasSize(2)
            .allSatisfy(e -> assertThat(e.getLibro().getId()).isEqualTo(recargado.getId()));
        assertThat(ejemplares.findByLibroId(libro.getId())).hasSize(2);
    }

    @Test
    void servicesRegistranYConsultanConEstadoControlado() {
        LibroRespuesta libro = registrarLibro(null);
        var ejemplar = ejemplarServicio.registrar(new CrearEjemplarSolicitud(
            libro.id(), "TEST-SVC-" + UUID.randomUUID(), null, EstadoConservacion.EXCELENTE));
        em.flush(); em.clear();
        assertThat(libroServicio.buscar(libro.id()).titulo()).isEqualTo("Libro de prueba de service");
        assertThat(ejemplar.libroId()).isEqualTo(libro.id());
        assertThat(ejemplar.estado()).isEqualTo(EstadoEjemplar.DISPONIBLE);
        assertThat(ejemplar.fechaAdquisicion()).isEqualTo(LocalDate.now());
        assertThat(ejemplarServicio.listarPorLibro(libro.id())).hasSize(1);
        assertThat(ejemplares.findById(ejemplar.id()).orElseThrow().isEstatus()).isTrue();
    }

    @Test
    void isbnOpcionalYConsultaPorIsbn() {
        registrarLibro(null); registrarLibro("   ");
        String isbn = "TEST-" + UUID.randomUUID().toString().substring(0, 12);
        LibroRespuesta libro = registrarLibro(" " + isbn + " ");
        assertThat(libros.findByIsbn(isbn)).get().extracting(Libro::getId).isEqualTo(libro.id());
        assertThat(libros.existsByIsbn(isbn)).isTrue();
    }

    @Test
    void serviceRechazaIsbnDuplicado() {
        String isbn = "TEST-" + UUID.randomUUID().toString().substring(0, 12);
        registrarLibro(isbn);
        assertThatThrownBy(() -> registrarLibro(" " + isbn + " ")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void serviceRechazaCodigoDuplicado() {
        LibroRespuesta libro = registrarLibro(null);
        String codigo = "TEST-" + UUID.randomUUID();
        ejemplarServicio.registrar(new CrearEjemplarSolicitud(libro.id(), codigo, null, EstadoConservacion.BUENO));
        assertThatThrownBy(() -> ejemplarServicio.registrar(new CrearEjemplarSolicitud(
            libro.id(), " " + codigo + " ", null, EstadoConservacion.BUENO)))
            .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void serviceRechazaLibroInexistente() {
        assertThatThrownBy(() -> ejemplarServicio.registrar(new CrearEjemplarSolicitud(
            Long.MAX_VALUE, "TEST-" + UUID.randomUUID(), null, EstadoConservacion.BUENO)))
            .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void postgresProtegeLaFkSinPasarPorService() {
        Libro referencia = em.getReference(Libro.class, Long.MAX_VALUE);
        assertThatThrownBy(() -> ejemplares.saveAndFlush(nuevoEjemplar(referencia, "TEST-" + UUID.randomUUID())))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postgresProtegeLaUnicidadSinPasarPorService() {
        Libro libro = nuevoLibro(null);
        String codigo = "TEST-" + UUID.randomUUID();
        ejemplares.saveAndFlush(nuevoEjemplar(libro, codigo));
        assertThatThrownBy(() -> ejemplares.saveAndFlush(nuevoEjemplar(libro, codigo)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postgresProtegeElEstadoPermitido() {
        Libro libro = nuevoLibro(null);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ejemplares (codigo_barras, fecha_adquisicion, estado, estado_conservacion, estatus, libro_id) VALUES (?, CURRENT_DATE, 'INVALIDO', 'BUENO', TRUE, ?)",
            "TEST-" + UUID.randomUUID(), libro.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @AfterTransaction
    void losDatosDeCadaPruebaSeRevierten() {
        for (Long id : librosCreados) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM libros WHERE id = ?", Long.class, id)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ejemplares WHERE libro_id = ?", Long.class, id)).isZero();
        }
    }

    private Libro nuevoLibro(String isbn) {
        Libro libro = new Libro(); libro.setTitulo("Libro de prueba de repositorio"); libro.setIsbn(isbn);
        libro = libros.saveAndFlush(libro); librosCreados.add(libro.getId()); return libro;
    }

    private LibroRespuesta registrarLibro(String isbn) {
        LibroRespuesta libro = libroServicio.registrar(new CrearLibroSolicitud(
            "Libro de prueba de service", null, "1", 2025, isbn, 100));
        librosCreados.add(libro.id()); return libro;
    }

    private Ejemplar nuevoEjemplar(Libro libro, String codigo) {
        Ejemplar e = new Ejemplar(); e.setLibro(libro); e.setCodigoBarras(codigo);
        e.setFechaAdquisicion(LocalDate.now()); e.setEstado(EstadoEjemplar.DISPONIBLE);
        e.setEstadoConservacion(EstadoConservacion.BUENO); return e;
    }
}
