package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M07, pp. impresas 52/55-57 (visor 53/56-58), RF-M07-45, RF-M07-53, RF-M07-78, RF-M07-79; RN-M07-09, RN-M07-11, RN-M07-12.
 * Modela configuracion de transporte; no implementa ni realiza envios.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "canales_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class CanalNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "max_reintentos", nullable = true)
    private Integer maxReintentos;

    @Column(name = "intervalo_reintento_minutos", nullable = true)
    private Integer intervaloReintentoMinutos;

    @OneToMany(mappedBy = "canal", fetch = FetchType.LAZY)
    private List<EnvioNotificacion> envios = new ArrayList<>();

    @OneToMany(mappedBy = "canal", fetch = FetchType.LAZY)
    private List<PlantillaNotificacion> plantillas = new ArrayList<>();
}

