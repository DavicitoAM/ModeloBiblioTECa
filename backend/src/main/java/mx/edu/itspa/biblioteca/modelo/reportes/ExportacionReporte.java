package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M08, p. impresa 65 (visor 66), RF-M08-78, RF-M08-84; RN-M08-24, RN-M08-25 (p. 67).
 * Representa metadatos de un archivo; no exporta ni publica documentos.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "exportaciones_reporte")
@Getter
@Setter
@NoArgsConstructor
public class ExportacionReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "formato", nullable = false, length = 60)
    private String formato;

    @Column(name = "archivo", nullable = true, length = 1000)
    private String archivo;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_exportaciones_reporte_reporte"))
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_exportaciones_reporte_responsable"))
    private CuentaUsuario responsable;
}

