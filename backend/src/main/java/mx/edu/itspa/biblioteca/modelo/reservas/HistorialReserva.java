package mx.edu.itspa.biblioteca.modelo.reservas;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M04, PDF pp. impresas 31/34-35 (fisicas 32/35-36) y PNG. RF-M04-22/26; RN-M04-26/27.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Las posiciones permiten conservar movimientos de la lista (RF-M04-26); no asignan prioridad.
 * Estado anterior opcional para el alta; responsable opcional para transiciones automaticas.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "historiales_reserva")
@Getter
@Setter
@NoArgsConstructor
public class HistorialReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoReservaLibro estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoReservaLibro estadoNuevo;

    @Column(name = "posicion_anterior")
    private Integer posicionAnterior;

    @Column(name = "posicion_nueva")
    private Integer posicionNueva;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, foreignKey = @ForeignKey(name = "fk_historiales_reserva_reserva"))
    private ReservaLibro reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_id", foreignKey = @ForeignKey(name = "fk_historiales_reserva_responsable"))
    private CuentaUsuario responsable;
}
