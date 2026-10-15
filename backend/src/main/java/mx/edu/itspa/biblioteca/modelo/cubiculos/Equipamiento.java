package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M05, PDF pp. impresas 36-38 (fisicas 37-39) y PNG. RF-M05-04/15.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Catalogo compartido por cubiculos mediante muchos a muchos; no se inventan cantidades ni asignaciones exclusivas.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "equipamientos")
@Getter
@Setter
@NoArgsConstructor
public class Equipamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @ManyToMany(mappedBy = "equipamientos", fetch = FetchType.LAZY)
    private Set<Cubiculo> cubiculos = new HashSet<>();
}
