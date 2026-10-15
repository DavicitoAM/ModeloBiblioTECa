package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M06, p. impresa 47 (visor 48), RF-M06-33, RF-M06-42; RN-M06-05, RN-M06-21, RN-M06-22, RN-M06-23 (pp. 49-50).
 * La fecha final puede faltar para restricciones permanentes; no se bloquea ningun servicio en esta entrega.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "restricciones_servicio")
@Getter
@Setter
@NoArgsConstructor
public class RestriccionServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = true)
    private LocalDate fechaFin;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "servicio_afectado", nullable = false, length = 150)
    private String servicioAfectado;

    @Column(name = "condiciones_levantamiento", nullable = true, length = 3000)
    private String condicionesLevantamiento;

    @Column(name = "temporal", nullable = false)
    private boolean temporal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sancion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_restricciones_servicio_sancion"))
    private Sancion sancion;
}

