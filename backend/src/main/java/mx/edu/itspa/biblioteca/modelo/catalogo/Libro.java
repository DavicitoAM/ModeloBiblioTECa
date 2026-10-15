package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** M02, PDF impresa 15–21 / visor 16–22, RF-M02-01/02/05/13/19/23, RN-M02-03/09/12.
 * Idioma opcional por decisión expresa del responsable; editorial principal 0..1. */
@Entity
@Table(name = "libros")
@Getter @Setter @NoArgsConstructor
public class Libro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;
    @Column(name = "descripcion", length = 2000)
    private String descripcion;
    @Column(name = "edicion", length = 100)
    private String edicion;
    @Column(name = "anio_publicacion")
    private Integer anioPublicacion;
    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;
    @Column(name = "num_paginas")
    private Integer numPaginas;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editorial_id", foreignKey = @ForeignKey(name = "fk_libros_editorial"))
    private Editorial editorial;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idioma_id", foreignKey = @ForeignKey(name = "fk_libros_idioma"))
    private Idioma idioma;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "libros_autores", joinColumns = @JoinColumn(name = "libro_id"),
        inverseJoinColumns = @JoinColumn(name = "autor_id"))
    private List<Autor> autores = new ArrayList<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "libros_categorias", joinColumns = @JoinColumn(name = "libro_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private List<Categoria> categorias = new ArrayList<>();
    @OneToMany(mappedBy = "libro", fetch = FetchType.LAZY)
    private List<Ejemplar> ejemplares = new ArrayList<>();

    /** Mantiene ambos extremos Java; la FK sigue siendo propiedad de Ejemplar. */
    public void agregarEjemplar(Ejemplar ejemplar) {
        Objects.requireNonNull(ejemplar, "El ejemplar es obligatorio");
        if (ejemplar.getLibro() != null && ejemplar.getLibro() != this) {
            ejemplar.getLibro().getEjemplares().remove(ejemplar);
        }
        if (!ejemplares.contains(ejemplar)) ejemplares.add(ejemplar);
        ejemplar.setLibro(this);
    }
}
