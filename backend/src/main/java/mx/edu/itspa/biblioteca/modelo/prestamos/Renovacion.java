package mx.edu.itspa.biblioteca.modelo.prestamos;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M03, PDF pp. impresas 22/25-28 (fisicas 23/26-29). RF-M03-40/45/46/47/48; RN-M03-19/21/22/24.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * fechaVencimiento es la nueva fecha del diagrama; se conserva tambien la anterior por RN-M03-22.
 * Responsable opcional admite solicitudes automatizadas; no implementa aprobacion ni modifica el detalle por si sola.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "renovaciones")
@Getter
@Setter
@NoArgsConstructor
public class Renovacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_renovacion", nullable = false)
    private LocalDateTime fechaRenovacion;

    @Column(name = "fecha_vencimiento_anterior", nullable = false)
    private LocalDate fechaVencimientoAnterior;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "estado", length = 30)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_prestamo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_renovaciones_detalle_prestamo"))
    private DetallePrestamo detallePrestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_id", foreignKey = @ForeignKey(name = "fk_renovaciones_responsable"))
    private CuentaUsuario responsable;
}
