package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 17/20, RF-M02-21/25 y RN-M02-07/08. */
@Entity @Table(name = "categorias")
@Getter @Setter @NoArgsConstructor
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
    @Column(name = "descripcion", length = 2000)
    private String descripcion;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @ManyToMany(mappedBy = "categorias", fetch = FetchType.LAZY)
    private List<Libro> libros = new ArrayList<>();
}
