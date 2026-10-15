package mx.edu.itspa.biblioteca.modelo.prestamos;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;

/**
 * M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-07/11/24/35/39; RN-M03-03/04/10/13/14/15/19.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * La devolucion definitiva es opcional y unica por detalle, segun RN-M03-13/14 y decision expresa del responsable.
 * No existe una relacion directa Libro-Prestamo: la circulacion identifica al Ejemplar.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "detalles_prestamo")
@Getter
@Setter
@NoArgsConstructor
public class DetallePrestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDetallePrestamo estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalles_prestamo_prestamo"))
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalles_prestamo_ejemplar"))
    private Ejemplar ejemplar;

    @OneToOne(mappedBy = "detallePrestamo", fetch = FetchType.LAZY)
    private Devolucion devolucion;

    @OneToMany(mappedBy = "detallePrestamo", fetch = FetchType.LAZY)
    private List<Renovacion> renovaciones = new ArrayList<>();
}
