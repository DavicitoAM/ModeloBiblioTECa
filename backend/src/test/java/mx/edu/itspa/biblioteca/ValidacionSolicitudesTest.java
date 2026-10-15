package mx.edu.itspa.biblioteca;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoConservacion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * M02, PDF pp. impresas 16-20 / fisicas 17-21, RF-M02-01/02/26/27.
 * RNF-MAN-07, p. impresa 81 / fisica 82: pruebas mantenibles de las solicitudes piloto.
 * Ejecuta las restricciones Jakarta existentes, sin Spring, JDBC ni reglas nuevas de ISBN.
 * La aceptacion de valores opcionales sigue las anotaciones actuales de los DTO.
 */
class ValidacionSolicitudesTest {
    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void prepararValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        if (fabrica != null) fabrica.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rechazaTituloNuloVacioOBlanco(String titulo) {
        var solicitud = new CrearLibroSolicitud(titulo, null, null, null, null, null);

        soloInfraccionEn(solicitud, "titulo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rechazaCodigoNuloVacioOBlanco(String codigo) {
        var solicitud = new CrearEjemplarSolicitud(1L, codigo, null, EstadoConservacion.BUENO);

        soloInfraccionEn(solicitud, "codigoBarras");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void rechazaIdentificadorDeLibroNuloCeroONegativo(Long libroId) {
        var solicitud = new CrearEjemplarSolicitud(libroId, "ITSPA-001", null, EstadoConservacion.BUENO);

        soloInfraccionEn(solicitud, "libroId");
    }

    @Test
    void rechazaConservacionNula() {
        var solicitud = new CrearEjemplarSolicitud(1L, "ITSPA-001", null, null);

        soloInfraccionEn(solicitud, "estadoConservacion");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rechazaNumeroDePaginasCeroONegativo(int paginas) {
        var solicitud = new CrearLibroSolicitud("Catalogo de prueba", null, null, null, null, paginas);

        soloInfraccionEn(solicitud, "numPaginas");
    }

    @Test
    void aceptaLibroConIsbnYPaginasOpcionalesNulos() {
        var solicitud = new CrearLibroSolicitud("Catalogo de prueba", null, null, null, null, null);

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    @Test
    void aceptaLibroConDatosBibliograficosYPaginaMinima() {
        var solicitud = new CrearLibroSolicitud(
            "Catalogo de prueba", "Datos de demostracion", "Primera", 2025, "9780000000019", 1);

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    @Test
    void aceptaEjemplarConFechaDeAdquisicionOpcionalNula() {
        var solicitud = new CrearEjemplarSolicitud(1L, "ITSPA-001", null, EstadoConservacion.BUENO);

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    @Test
    void aceptaEjemplarConFechaDeAdquisicionIndicada() {
        var solicitud = new CrearEjemplarSolicitud(
            1L, "ITSPA-001", LocalDate.of(2024, 2, 29), EstadoConservacion.EXCELENTE);

        assertThat(validador.validate(solicitud)).isEmpty();
    }

    private void soloInfraccionEn(Object solicitud, String propiedad) {
        assertThat(validador.validate(solicitud))
            .extracting(infraccion -> infraccion.getPropertyPath().toString())
            .containsExactly(propiedad);
    }
}
