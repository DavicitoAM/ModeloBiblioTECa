package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M07, pp. impresas 55-56 (visor 56-57), RF-M07-47, RF-M07-53; RN-M07-10, RN-M07-11, RN-M07-12, RN-M07-13 (p. 58).
 * Un registro describe un intento; aceptado por servidor no implica entregado. Ningun metodo envia mensajes.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "envios_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class EnvioNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "error", nullable = true, length = 3000)
    private String error;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notificacion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_envios_notificacion_notificacion"))
    private Notificacion notificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canal_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_envios_notificacion_canal"))
    private CanalNotificacion canal;
}

