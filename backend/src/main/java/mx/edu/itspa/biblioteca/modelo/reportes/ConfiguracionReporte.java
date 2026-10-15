package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M08, pp. impresas 60/64-65 (visor 61/65-66), RF-M08-70, RF-M08-71, RF-M08-78, RF-M08-81, RF-M08-89; RN-M08-05, RN-M08-07 (p. 66).
 * Programado se conserva como atributo del grafico; no se crean tareas automaticas.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "configuraciones_reporte")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "formato", nullable = false, length = 60)
    private String formato;

    @Column(name = "filtros", nullable = true, length = 5000)
    private String filtros;

    @Column(name = "programado", nullable = false)
    private boolean programado;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "fecha_inicio", nullable = true)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = true)
    private LocalDate fechaFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_configuraciones_reporte_reporte"))
    private Reporte reporte;
}

