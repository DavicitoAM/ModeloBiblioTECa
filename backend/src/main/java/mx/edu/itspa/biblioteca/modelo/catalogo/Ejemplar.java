package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** M02, PDF impresa 18–21 / visor 19–22, RF-M02-26/34/36, RN-M02-02/04/15/16.
 * Ubicación actual 0..1 acordada; la política de ubicación de activos corresponde al service futuro. */
@Entity @Table(name = "ejemplares")
@Getter @Setter @NoArgsConstructor
public class Ejemplar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "codigo_barras", nullable = false, unique = true, length = 80)
    private String codigoBarras;
    @Column(name = "fecha_adquisicion", nullable = false)
    private LocalDate fechaAdquisicion;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoEjemplar estado;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacion", nullable = false, length = 30)
    private EstadoConservacion estadoConservacion;
    @Column(name = "estatus", nullable = false)
    private boolean estatus = true;
    @Column(name = "observaciones", length = 2000)
    private String observaciones;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ejemplares_libro"))
    private Libro libro;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_id", foreignKey = @ForeignKey(name = "fk_ejemplares_ubicacion"))
    private Ubicacion ubicacion;
    @OneToMany(mappedBy = "ejemplar", fetch = FetchType.LAZY)
    private List<HistorialEstadoEjemplar> historialEstados = new ArrayList<>();
}
