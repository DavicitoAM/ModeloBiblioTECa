package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 15/17/20, RF-M02-10/14 y RN-M02-05/06. */
@Entity @Table(name = "autores")
@Getter @Setter @NoArgsConstructor
public class Autor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
    @Column(name = "apellidos", length = 200)
    private String apellidos;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @ManyToMany(mappedBy = "autores", fetch = FetchType.LAZY)
    private List<Libro> libros = new ArrayList<>();
}
