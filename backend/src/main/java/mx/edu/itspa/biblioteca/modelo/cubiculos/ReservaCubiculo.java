package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;
import mx.edu.itspa.biblioteca.modelo.personas.Persona;

/**
 * M05, PDF pp. impresas 36-43 (fisicas 37-44). RF-M05-19/20/21/22/29/30/31/32/54/76; RN-M05-03/04/06/14/15.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Los timestamps incluyen fecha y hora del periodo; fechaReserva registra la solicitud.
 * El CHECK de fin posterior al inicio cumple RN-M05-15; detectar solapamientos (RN-M05-06/07) requiere reglas posteriores.
 * Politica opcional mientras se evalua la solicitud; condicionesAplicadas es soporte para la instantanea exigida por RF-M05-76.
 * El responsable de cancelacion es opcional para solicitudes canceladas por la propia Persona.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "reservas_cubiculo")
@Getter
@Setter
@NoArgsConstructor
public class ReservaCubiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoReservaCubiculo estado;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 2000)
    private String motivoCancelacion;

    @Column(name = "condiciones_aplicadas", length = 4000)
    private String condicionesAplicadas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_cubiculo_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cubiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_cubiculo_cubiculo"))
    private Cubiculo cubiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "politica_id", foreignKey = @ForeignKey(name = "fk_reservas_cubiculo_politica"))
    private PoliticaReservaCubiculo politica;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_cancelacion_id", foreignKey = @ForeignKey(name = "fk_reservas_cubiculo_responsable_cancelacion"))
    private CuentaUsuario responsableCancelacion;

    @OneToOne(mappedBy = "reserva", fetch = FetchType.LAZY)
    private UsoCubiculo uso;
}
