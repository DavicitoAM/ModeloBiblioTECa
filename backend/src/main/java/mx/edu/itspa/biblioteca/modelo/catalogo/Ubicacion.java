package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 18/21, RF-M02-38/42, RN-M02-16/22. Cardinalidad acordada 1 a 0..N ejemplares. */
@Entity @Table(name = "ubicaciones")
@Getter @Setter @NoArgsConstructor
public class Ubicacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
    @Column(name = "seccion", nullable = false, length = 100)
    private String seccion;
    @Column(name = "estante", nullable = false, length = 100)
    private String estante;
    @Column(name = "nivel", length = 100)
    private String nivel;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @OneToMany(mappedBy = "ubicacion", fetch = FetchType.LAZY)
    private List<Ejemplar> ejemplares = new ArrayList<>();
}
