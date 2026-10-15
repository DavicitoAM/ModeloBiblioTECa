package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M05, PDF pp. impresas 36/40-43 (fisicas 37/41-44). RF-M05-63/64/65/66/67/68; RN-M05-13/26/28.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * El responsable respalda los cambios administrativos; no se cancelan reservas ni se detectan conflictos automaticamente.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "bloqueos_cubiculo")
@Getter
@Setter
@NoArgsConstructor
public class BloqueoCubiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "motivo", nullable = false, length = 2000)
    private String motivo;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cubiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bloqueos_cubiculo_cubiculo"))
    private Cubiculo cubiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bloqueos_cubiculo_responsable"))
    private CuentaUsuario responsable;
}
