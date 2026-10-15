package mx.edu.itspa.biblioteca.modelo.reportes;

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
 * M08, pp. impresas 60/65-67 (visor 61/66-68), RF-M08-85, RF-M08-87; RN-M08-04, RN-M08-05, RN-M08-06, RN-M08-25, RN-M08-29.
 * Vinculo opcional a panel es decision tecnica explicita por cardinalidad grafica ambigua; generar reportes no esta implementado.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "reportes")
@Getter
@Setter
@NoArgsConstructor
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "parametros", nullable = true, length = 5000)
    private String parametros;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "filtros", nullable = true, length = 5000)
    private String filtros;

    @Column(name = "total_registros", nullable = true)
    private Long totalRegistros;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_reporte_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_reportes_tipo_reporte"))
    private TipoReporte tipoReporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_reportes_responsable"))
    private CuentaUsuario responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "panel_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_reportes_panel"))
    private PanelEstadistico panel;

    @OneToMany(mappedBy = "reporte", fetch = FetchType.LAZY)
    private List<ConfiguracionReporte> configuraciones = new ArrayList<>();

    @OneToMany(mappedBy = "reporte", fetch = FetchType.LAZY)
    private List<ExportacionReporte> exportaciones = new ArrayList<>();

    @OneToMany(mappedBy = "reporte", fetch = FetchType.LAZY)
    private List<HistorialReporte> historial = new ArrayList<>();
}

