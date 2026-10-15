package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M08, p. impresa 65 (visor 66), RF-M08-85, RF-M08-91; RN-M08-27, RN-M08-29 (p. 67).
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "historiales_reporte")
@Getter
@Setter
@NoArgsConstructor
public class HistorialReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "parametros", nullable = true, length = 5000)
    private String parametros;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "filtros", nullable = true, length = 5000)
    private String filtros;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historiales_reporte_reporte"))
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_reporte_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historiales_reporte_tipo_reporte"))
    private TipoReporte tipoReporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historiales_reporte_responsable"))
    private CuentaUsuario responsable;
}

