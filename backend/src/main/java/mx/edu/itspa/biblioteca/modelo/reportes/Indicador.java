package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-10, RN-M08-11, RN-M08-15, RN-M08-16 (p. 66).
 * Formula y valor son datos del modelo, no calculos ejecutables; vinculo opcional a panel es decision tecnica.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "indicadores")
@Getter
@Setter
@NoArgsConstructor
public class Indicador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "formula", nullable = true, length = 3000)
    private String formula;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "valor", nullable = true, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "unidad_medida", nullable = true, length = 100)
    private String unidadMedida;

    @Column(name = "fecha_actualizacion", nullable = true)
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "panel_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_indicadores_panel"))
    private PanelEstadistico panel;
}

