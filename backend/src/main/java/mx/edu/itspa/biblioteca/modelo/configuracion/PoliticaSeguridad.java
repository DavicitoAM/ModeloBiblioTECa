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
 * RF-M09-50/56/57/58, RN-M09-17/18/19/20.
 * Atributos de duracion e intentos representan condiciones configurables; no se asignan limites
 * institucionales por defecto ni se implementan autenticacion o bloqueo de cuentas.
 */
@Entity
@Table(name = "politicas_seguridad")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    /** Atributo reglas de la casilla M10 del diagrama; configuración descriptiva, sin motor de reglas. */
    @Column(name = "reglas", length = 4000)
    private String reglas;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @Column(name = "inactividad_maxima_minutos")
    private Integer inactividadMaximaMinutos;

    @Column(name = "intentos_fallidos_maximos")
    private Integer intentosFallidosMaximos;

    @Column(name = "bloqueo_temporal_minutos")
    private Integer bloqueoTemporalMinutos;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_modificacion_id")
    private CuentaUsuario responsableModificacion;
}
