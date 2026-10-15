package mx.edu.itspa.biblioteca.modelo.reservas;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M04, PDF pp. impresas 29/33-35 (fisicas 30/34-36). RF-M04-50/51/52/53/54/55/56; RN-M04-09/10/11/17/25.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * diasAnticipacion se conserva del PNG; tiempos y limites se configuran sin valores predeterminados.
 * Las reglas de prioridad se almacenan como configuracion descriptiva; no se implementa un algoritmo ni vocabulario de usuarios.
 * La unidad de tiempoLimite debera concretarse antes de implementar el motor de politicas.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "politicas_reserva_libro")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaReservaLibro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dias_anticipacion")
    private Integer diasAnticipacion;

    @Column(name = "tiempo_limite", nullable = false)
    private Integer tiempoLimite;

    @Column(name = "max_reservas", nullable = false)
    private Integer maxReservas;

    @Column(name = "reglas_prioridad", length = 2000)
    private String reglasPrioridad;

    @Column(name = "tipos_usuario_permitidos", length = 1000)
    private String tiposUsuarioPermitidos;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "politica", fetch = FetchType.LAZY)
    private List<ReservaLibro> reservas = new ArrayList<>();
}
