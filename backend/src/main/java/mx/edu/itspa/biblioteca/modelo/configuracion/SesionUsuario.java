package mx.edu.itspa.biblioteca.modelo.configuracion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M09 texto / M10 grafico, PDF pp. impresas 68, 72 y 75 (fisicas 69, 73 y 76),
 * RF-M09-47/48/49/50/51/52/53/54/58, RN-M09-17/18/19.
 * Sesion pertenece a CuentaUsuario. Ultima actividad soporta el dato necesario para controlar
 * inactividad posteriormente; no almacena identificadores de sesion utilizables ni tokens.
 * Estatus conserva el tipo string del diagrama sin inventar un catalogo institucional.
 */
@Entity
@Table(name = "sesiones_usuario")
@Getter
@Setter
@NoArgsConstructor
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "estatus", nullable = false, length = 60)
    private String estatus;

    @Column(name = "motivo_cierre", length = 500)
    private String motivoCierre;

    @Column(name = "cierre_administrativo", nullable = false)
    private Boolean cierreAdministrativo = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaUsuario cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrada_por_id")
    private CuentaUsuario cerradaPor;
}
