package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M07, p. impresa 56 (visor 57), RF-M07-60, RF-M07-66; RN-M07-17, RN-M07-18, RN-M07-19, RN-M07-28 (pp. 58-59).
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "plantillas_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class PlantillaNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "asunto", nullable = false, length = 250)
    private String asunto;

    @Column(name = "mensaje", nullable = false, length = 10000)
    private String mensaje;

    @Column(name = "variables", nullable = true, length = 3000)
    private String variables;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_notificacion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_plantillas_notificacion_tipo_notificacion"))
    private TipoNotificacion tipoNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canal_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_plantillas_notificacion_canal"))
    private CanalNotificacion canal;

    @OneToMany(mappedBy = "plantilla", fetch = FetchType.LAZY)
    private List<Notificacion> notificaciones = new ArrayList<>();
}

