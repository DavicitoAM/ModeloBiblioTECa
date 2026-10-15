package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 17/20, RF-M02-16/20 y RN-M02-09/10. */
@Entity @Table(name = "editoriales")
@Getter @Setter @NoArgsConstructor
public class Editorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
    @Column(name = "pais", length = 100)
    private String pais;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @OneToMany(mappedBy = "editorial", fetch = FetchType.LAZY)
    private List<Libro> libros = new ArrayList<>();
}
