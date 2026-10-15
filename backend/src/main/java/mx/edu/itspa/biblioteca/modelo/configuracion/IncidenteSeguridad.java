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
 * RF-M09-59/60/61/62/63/64/65, RN-M09-21/22/23/29.
 * Estado y gravedad permanecen como texto sin inventar niveles institucionales. Relacion N:M
 * con eventos de auditoria permite registrar origen y acciones sucesivas del incidente;
 * cuenta afectada y responsable son opcionales para incidentes aun no identificados/asignados.
 */
@Entity
@Table(name = "incidentes_seguridad")
@Getter
@Setter
@NoArgsConstructor
public class IncidenteSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 80)
    private String estado;

    @Column(name = "gravedad", nullable = false, length = 60)
    private String gravedad;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "acciones_correctivas", length = 2000)
    private String accionesCorrectivas;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_afectada_id")
    private CuentaUsuario cuentaAfectada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private CuentaUsuario responsable;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "incidentes_auditoria", joinColumns = @JoinColumn(name = "incidente_id"),
        inverseJoinColumns = @JoinColumn(name = "evento_id"))
    private Set<BitacoraAuditoria> eventosAuditoria = new LinkedHashSet<>();
}
