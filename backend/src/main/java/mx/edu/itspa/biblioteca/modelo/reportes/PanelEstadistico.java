package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M08, pp. impresas 60-61 (visor 61-62), RF-M08-01, RF-M08-11; RN-M08-09, RN-M08-25 (pp. 66-67).
 * Panel 1:N Indicador con pertenencia opcional es una decision tecnica declarada; la narrativa no fija la multiplicidad exacta.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "paneles_estadisticos")
@Getter
@Setter
@NoArgsConstructor
public class PanelEstadistico {
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

    @Column(name = "fecha_actualizacion", nullable = true)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "panel", fetch = FetchType.LAZY)
    private List<Indicador> indicadores = new ArrayList<>();

    @OneToMany(mappedBy = "panel", fetch = FetchType.LAZY)
    private List<Reporte> reportes = new ArrayList<>();
}

