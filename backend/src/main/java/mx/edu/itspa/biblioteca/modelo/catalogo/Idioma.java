package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 15/16, RF-M02-02/49. Catálogo; ningún idioma se impone por defecto. */
@Entity @Table(name = "idiomas")
@Getter @Setter @NoArgsConstructor
public class Idioma {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @OneToMany(mappedBy = "idioma", fetch = FetchType.LAZY)
    private List<Libro> libros = new ArrayList<>();
}
