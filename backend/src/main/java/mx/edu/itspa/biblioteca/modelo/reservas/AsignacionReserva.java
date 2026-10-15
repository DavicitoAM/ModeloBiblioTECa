package mx.edu.itspa.biblioteca.modelo.reservas;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;

/**
 * M04, PDF pp. impresas 29/31-35 (fisicas 30/32-36). RF-M04-25/27/28/29/32; RN-M04-05/12/13/15/27.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Cada asignacion identifica el ejemplar concreto; pueden conservarse asignaciones historicas de una reserva.
 * La exclusividad de asignaciones vigentes requiere futuras reglas transaccionales; no se afirma implementada por esta FK.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "asignaciones_reserva")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asignaciones_reserva_reserva"))
    private ReservaLibro reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false, foreignKey = @ForeignKey(name = "fk_asignaciones_reserva_ejemplar"))
    private Ejemplar ejemplar;
}
