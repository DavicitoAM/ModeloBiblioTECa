package mx.edu.itspa.biblioteca;

import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M02, PDF pp. impresas 16-20 / fisicas 17-21, RF-M02-01/02/26/27 y RN-M02-03/04.
 * RNF-MAN-07, p. impresa 81 / fisica 82: verifica invariantes de la relacion en memoria.
 * El helper debe mantener ambos extremos; estas pruebas no acreditan persistencia ni FK.
 */
class LibroEjemplarRelacionTest {
    @Test
    void agregarVariosEjemplaresMantieneAmbosExtremos() {
        Libro libro = new Libro();
        Ejemplar primero = new Ejemplar();
        Ejemplar segundo = new Ejemplar();

        libro.agregarEjemplar(primero);
        libro.agregarEjemplar(segundo);

        assertThat(libro.getEjemplares()).containsExactly(primero, segundo);
        assertThat(primero.getLibro()).isSameAs(libro);
        assertThat(segundo.getLibro()).isSameAs(libro);
    }

    @Test
    void agregarLaMismaInstanciaNoDuplicaLaAsociacion() {
        Libro libro = new Libro();
        Ejemplar ejemplar = new Ejemplar();

        libro.agregarEjemplar(ejemplar);
        libro.agregarEjemplar(ejemplar);

        assertThat(libro.getEjemplares()).containsExactly(ejemplar);
        assertThat(ejemplar.getLibro()).isSameAs(libro);
    }

    @Test
    void trasladarEjemplarActualizaLosDosLibrosYConservaLosDemas() {
        Libro origen = new Libro();
        Libro destino = new Libro();
        Ejemplar trasladado = new Ejemplar();
        Ejemplar permaneceEnOrigen = new Ejemplar();
        Ejemplar permaneceEnDestino = new Ejemplar();
        origen.agregarEjemplar(trasladado);
        origen.agregarEjemplar(permaneceEnOrigen);
        destino.agregarEjemplar(permaneceEnDestino);

        destino.agregarEjemplar(trasladado);

        assertThat(origen.getEjemplares()).containsExactly(permaneceEnOrigen);
        assertThat(destino.getEjemplares()).containsExactly(permaneceEnDestino, trasladado);
        assertThat(trasladado.getLibro()).isSameAs(destino);
        assertThat(permaneceEnOrigen.getLibro()).isSameAs(origen);
        assertThat(permaneceEnDestino.getLibro()).isSameAs(destino);
    }

    @Test
    void rechazaNuloSinAlterarLasRelacionesExistentes() {
        Libro libro = new Libro();
        Ejemplar existente = new Ejemplar();
        libro.agregarEjemplar(existente);

        assertThatThrownBy(() -> libro.agregarEjemplar(null)).isInstanceOf(NullPointerException.class);

        assertThat(libro.getEjemplares()).containsExactly(existente);
        assertThat(existente.getLibro()).isSameAs(libro);
    }
}
