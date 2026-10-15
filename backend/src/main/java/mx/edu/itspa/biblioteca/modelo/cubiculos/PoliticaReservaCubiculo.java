package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M05, PDF pp. impresas 36/41-43 (fisicas 37/42-44). RF-M05-69/70/71/72/73/74/75/76; RN-M05-11/16/17/23/27.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Parametros configurables sin tiempos, cantidades ni tipos de usuario predeterminados.
 * Las unidades de duracion/anticipacion/tolerancia deberan concretarse antes de implementar reglas; no se infieren del tipo int del PNG.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "politicas_reserva_cubiculo")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaReservaCubiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tiempo_minimo")
    private Integer tiempoMinimo;

    @Column(name = "tiempo_maximo", nullable = false)
    private Integer tiempoMaximo;

    @Column(name = "anticipacion_max", nullable = false)
    private Integer anticipacionMax;

    @Column(name = "max_reservas", nullable = false)
    private Integer maxReservas;

    @Column(name = "tolerancia_llegada")
    private Integer toleranciaLlegada;

    @Column(name = "tipos_usuario_permitidos", length = 1000)
    private String tiposUsuarioPermitidos;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "politica", fetch = FetchType.LAZY)
    private List<ReservaCubiculo> reservas = new ArrayList<>();
}
