package mx.edu.itspa.biblioteca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import mx.edu.itspa.biblioteca.controlador.EjemplarControlador;
import mx.edu.itspa.biblioteca.controlador.LibroControlador;
import mx.edu.itspa.biblioteca.dto.CrearEjemplarSolicitud;
import mx.edu.itspa.biblioteca.dto.CrearLibroSolicitud;
import mx.edu.itspa.biblioteca.dto.EjemplarRespuesta;
import mx.edu.itspa.biblioteca.dto.LibroRespuesta;
import mx.edu.itspa.biblioteca.excepcion.ManejadorErrores;
import mx.edu.itspa.biblioteca.excepcion.RecursoNoEncontradoException;
import mx.edu.itspa.biblioteca.excepcion.ReglaNegocioException;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoConservacion;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoEjemplar;
import mx.edu.itspa.biblioteca.services.EjemplarServicio;
import mx.edu.itspa.biblioteca.services.LibroServicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M02, PDF pp. impresas 16/18 (visor 17/19), RF-M02-01, RF-M02-04,
 * RF-M02-26 y RF-M02-30; p. impresa 20 (visor 21), RN-M02-03 y RN-M02-04.
 * Verifica el contrato HTTP y los DTO del piloto con controladores/advice reales.
 * Los services son interfaces simuladas: no comprueba reglas internas, JPA,
 * migraciones, persistencia o transacciones. No carga el contexto de la aplicacion
 * ni crea conexiones a PostgreSQL.
 */
class ApiPilotoTest {
    private static final LibroRespuesta LIBRO = new LibroRespuesta(
        7L, "Libro API", "Demostracion academica", "Primera", 2025,
        "9780000000019", 320);
    private static final EjemplarRespuesta EJEMPLAR = new EjemplarRespuesta(
        11L, 7L, "ITSPA-API-001", LocalDate.of(2026, 10, 15),
        EstadoEjemplar.DISPONIBLE, EstadoConservacion.BUENO, true);
    private static final String ALTA_LIBRO = """
        {
          "titulo": "Libro API",
          "descripcion": "Demostracion academica",
          "edicion": "Primera",
          "anioPublicacion": 2025,
          "isbn": "9780000000019",
          "numPaginas": 320
        }
        """;
    private static final String ALTA_EJEMPLAR = """
        {
          "libroId": 7,
          "codigoBarras": "ITSPA-API-001",
          "fechaAdquisicion": "2026-10-15",
          "estadoConservacion": "BUENO"
        }
        """;

    private LibroServicio libros;
    private EjemplarServicio ejemplares;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void prepararApiSinBaseDeDatos() {
        // PROXY solo simula interfaces y evita requerir instrumentacion/agent attach.
        libros = mock(LibroServicio.class, withSettings().mockMaker(MockMakers.PROXY));
        ejemplares = mock(EjemplarServicio.class, withSettings().mockMaker(MockMakers.PROXY));
        ObjectMapper jackson = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(
                new LibroControlador(libros), new EjemplarControlador(ejemplares))
            .setControllerAdvice(new ManejadorErrores())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(jackson))
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void cerrarValidador() {
        if (validator != null) {
            validator.close();
        }
    }

    @Test
    void registrarLibroResponde201ConDtoJson() throws Exception {
        when(libros.registrar(any(CrearLibroSolicitud.class))).thenReturn(LIBRO);

        mvc.perform(post("/api/v1/libros")
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_LIBRO))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.titulo").isString())
            .andExpect(jsonPath("$.titulo").value("Libro API"))
            .andExpect(jsonPath("$.descripcion").isString())
            .andExpect(jsonPath("$.edicion").isString())
            .andExpect(jsonPath("$.anioPublicacion").isNumber())
            .andExpect(jsonPath("$.anioPublicacion").value(2025))
            .andExpect(jsonPath("$.isbn").isString())
            .andExpect(jsonPath("$.isbn").value("9780000000019"))
            .andExpect(jsonPath("$.numPaginas").isNumber())
            .andExpect(jsonPath("$.numPaginas").value(320));

        verify(libros).registrar(new CrearLibroSolicitud(
            "Libro API", "Demostracion academica", "Primera", 2025,
            "9780000000019", 320));
    }

    @Test
    void registrarEjemplarResponde201ConFechaIsoEnumsYBooleano() throws Exception {
        when(ejemplares.registrar(any(CrearEjemplarSolicitud.class))).thenReturn(EJEMPLAR);

        mvc.perform(post("/api/v1/ejemplares")
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_EJEMPLAR))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.id").value(11))
            .andExpect(jsonPath("$.libroId").isNumber())
            .andExpect(jsonPath("$.libroId").value(7))
            .andExpect(jsonPath("$.codigoBarras").isString())
            .andExpect(jsonPath("$.codigoBarras").value("ITSPA-API-001"))
            .andExpect(jsonPath("$.fechaAdquisicion").isString())
            .andExpect(jsonPath("$.fechaAdquisicion").value("2026-10-15"))
            .andExpect(jsonPath("$.estado").isString())
            .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.estadoConservacion").isString())
            .andExpect(jsonPath("$.estadoConservacion").value("BUENO"))
            .andExpect(jsonPath("$.estatus").isBoolean())
            .andExpect(jsonPath("$.estatus").value(true));

        verify(ejemplares).registrar(new CrearEjemplarSolicitud(
            7L, "ITSPA-API-001", LocalDate.of(2026, 10, 15), EstadoConservacion.BUENO));
    }

    @Test
    void consultarLibroPorIdResponde200() throws Exception {
        when(libros.buscar(7L)).thenReturn(LIBRO);

        mvc.perform(get("/api/v1/libros/7"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.titulo").value("Libro API"));
    }

    @Test
    void listarLibrosResponde200ConArregloDeDtos() throws Exception {
        when(libros.listar()).thenReturn(List.of(LIBRO));

        mvc.perform(get("/api/v1/libros"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].isbn").value("9780000000019"));
    }

    @Test
    void listarEjemplaresPorLibroResponde200() throws Exception {
        when(ejemplares.listarPorLibro(7L)).thenReturn(List.of(EJEMPLAR));

        mvc.perform(get("/api/v1/ejemplares").param("libroId", "7"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].libroId").value(7))
            .andExpect(jsonPath("$[0].codigoBarras").value("ITSPA-API-001"));
    }

    @ParameterizedTest(name = "libro invalido [{index}] devuelve 400 sin delegar")
    @ValueSource(strings = {
        "{\"titulo\": \" \", \"numPaginas\": 320}",
        "{\"titulo\": \"Libro API\", \"numPaginas\": 0}"
    })
    void validarLibroAntesDeDelegar(String json) throws Exception {
        mvc.perform(post("/api/v1/libros")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libros, ejemplares);
    }

    @ParameterizedTest(name = "ejemplar invalido [{index}] devuelve 400 sin delegar")
    @ValueSource(strings = {
        "{\"codigoBarras\": \"ITSPA-API-001\", \"estadoConservacion\": \"BUENO\"}",
        "{\"libroId\": 0, \"codigoBarras\": \"ITSPA-API-001\", \"estadoConservacion\": \"BUENO\"}",
        "{\"libroId\": 7, \"codigoBarras\": \" \", \"estadoConservacion\": \"BUENO\"}",
        "{\"libroId\": 7, \"codigoBarras\": \"ITSPA-API-001\"}"
    })
    void validarEjemplarAntesDeDelegar(String json) throws Exception {
        mvc.perform(post("/api/v1/ejemplares")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libros, ejemplares);
    }

    @Test
    void traducirLibroNoEncontradoA404ConErrorJson() throws Exception {
        when(libros.buscar(99L))
            .thenThrow(new RecursoNoEncontradoException("Libro no encontrado"));

        mvc.perform(get("/api/v1/libros/99"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").isString())
            .andExpect(jsonPath("$.error").value("Libro no encontrado"));
    }

    @Test
    void traducirLibroInexistenteEnAltaDeEjemplarA404() throws Exception {
        when(ejemplares.registrar(any(CrearEjemplarSolicitud.class)))
            .thenThrow(new RecursoNoEncontradoException("Libro no encontrado"));

        mvc.perform(post("/api/v1/ejemplares")
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_EJEMPLAR))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Libro no encontrado"));
    }

    @Test
    void traducirIsbnDuplicadoA409ConErrorJson() throws Exception {
        when(libros.registrar(any(CrearLibroSolicitud.class)))
            .thenThrow(new ReglaNegocioException("Ya existe un libro con ese ISBN"));

        mvc.perform(post("/api/v1/libros")
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_LIBRO))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").isString())
            .andExpect(jsonPath("$.error").value("Ya existe un libro con ese ISBN"));
    }

    @Test
    void traducirCodigoDuplicadoA409ConErrorJson() throws Exception {
        when(ejemplares.registrar(any(CrearEjemplarSolicitud.class)))
            .thenThrow(new ReglaNegocioException("El codigo de barras ya esta registrado"));

        mvc.perform(post("/api/v1/ejemplares")
                .contentType(MediaType.APPLICATION_JSON).content(ALTA_EJEMPLAR))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("El codigo de barras ya esta registrado"));
    }

    @Test
    void rechazarListadoSinParametroLibroIdAntesDeDelegar() throws Exception {
        mvc.perform(get("/api/v1/ejemplares"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libros, ejemplares);
    }

    @Test
    void rechazarParametroLibroIdNoNumericoAntesDeDelegar() throws Exception {
        mvc.perform(get("/api/v1/ejemplares").param("libroId", "no-numero"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libros, ejemplares);
    }

    @Test
    void rechazarIdDeRutaNoNumericoAntesDeDelegar() throws Exception {
        mvc.perform(get("/api/v1/libros/no-numero"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libros, ejemplares);
    }
}
