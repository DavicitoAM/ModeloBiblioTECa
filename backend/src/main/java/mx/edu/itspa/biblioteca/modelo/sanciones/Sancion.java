package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M06, pp. impresas 46-47/49-51 (visor 47-48/50-52), RF-M06-21, RF-M06-32, RF-M06-80, RN-M06-04, RN-M06-05, RN-M06-12, RN-M06-27, RN-M06-28.
 * CUMPLIDA y RESUELTA se mantienen distintos porque ambos terminos aparecen en la fuente; no hay transiciones automaticas.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "sanciones")
@Getter
@Setter
@NoArgsConstructor
public class Sancion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 60)
    private EstadoSancion estado;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Column(name = "condiciones_cumplimiento", nullable = true, length = 3000)
    private String condicionesCumplimiento;

    @Column(name = "fecha_fin", nullable = true)
    private LocalDate fechaFin;

    @Column(name = "fecha_resolucion", nullable = true)
    private LocalDateTime fechaResolucion;

    @Column(name = "motivo_cancelacion", nullable = true, length = 2000)
    private String motivoCancelacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidencia_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_sanciones_incidencia"))
    private Incidencia incidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_sancion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_sanciones_tipo_sancion"))
    private TipoSancion tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_sanciones_politica"))
    private PoliticaSancion politica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_sanciones_responsable"))
    private CuentaUsuario responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_cancelacion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_sanciones_responsable_cancelacion"))
    private CuentaUsuario responsableCancelacion;

    @OneToOne(mappedBy = "sancion", fetch = FetchType.LAZY)
    private Multa multa;

    @OneToMany(mappedBy = "sancion", fetch = FetchType.LAZY)
    private List<RestriccionServicio> restricciones = new ArrayList<>();

    @OneToMany(mappedBy = "sancion", fetch = FetchType.LAZY)
    private List<ReposicionMaterial> reposiciones = new ArrayList<>();

    @OneToMany(mappedBy = "sancion", fetch = FetchType.LAZY)
    private List<HistorialSancion> historial = new ArrayList<>();
}

