package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M05, PDF pp. impresas 36/39/42-43 (fisicas 37/40/43-44). RF-M05-42/43/44/46/47/50; RN-M05-18/19/20/21/22.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * La fecha de fin es opcional mientras el uso este activo; no se calcula ocupacion automaticamente.
 * FK de horario opcional aprobada como referencia al horario aplicado para la dependencia del PNG; no valida horarios.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "usos_cubiculo")
@Getter
@Setter
@NoArgsConstructor
public class UsoCubiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "asistencia_confirmada")
    private Boolean asistenciaConfirmada;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_usos_cubiculo_reserva"))
    private ReservaCubiculo reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "horario_biblioteca_id", foreignKey = @ForeignKey(name = "fk_usos_cubiculo_horario_biblioteca"))
    private HorarioBiblioteca horarioBiblioteca;
}
