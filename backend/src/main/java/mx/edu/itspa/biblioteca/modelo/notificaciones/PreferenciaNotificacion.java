package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M07, p. impresa 56 (visor 57), RF-M07-54, RF-M07-59; RN-M07-15, RN-M07-16 (p. 58).
 * Las banderas no desactivan notificaciones obligatorias por si mismas; la regla se implementara en servicios futuros.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "preferencias_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class PreferenciaNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "permite_correo", nullable = false)
    private boolean permiteCorreo;

    @Column(name = "permite_sistema", nullable = false)
    private boolean permiteSistema;

    @Column(name = "permite_sms", nullable = false)
    private boolean permiteSMS;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_preferencias_notificacion_cuenta"))
    private CuentaUsuario cuenta;
}

