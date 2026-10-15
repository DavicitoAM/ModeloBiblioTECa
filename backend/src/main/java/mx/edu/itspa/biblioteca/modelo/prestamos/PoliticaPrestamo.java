package mx.edu.itspa.biblioteca.modelo.prestamos;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M03, PDF pp. impresas 22/26-28 (fisicas 23/27-29). RF-M03-57/58/59/60/61/62/63; RN-M03-08/09/21/28/29.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Condiciones configurables sin duraciones, limites ni autorizaciones predeterminadas; no se implementa motor de politicas.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "politicas_prestamo")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaPrestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo_usuario", nullable = false, length = 80)
    private String tipoUsuario;

    @Column(name = "max_libros", nullable = false)
    private Integer maxLibros;

    @Column(name = "dias_prestamo", nullable = false)
    private Integer diasPrestamo;

    @Column(name = "permite_renovacion", nullable = false)
    private Boolean permiteRenovacion;

    @Column(name = "renovaciones_max", nullable = false)
    private Integer renovacionesMax;

    @Column(name = "restricciones_material", length = 2000)
    private String restriccionesMaterial;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "politica", fetch = FetchType.LAZY)
    private List<Prestamo> prestamos = new ArrayList<>();
}
