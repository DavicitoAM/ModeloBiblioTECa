package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M06, p. impresa 48 (visor 49), RF-M06-61, RF-M06-67; RN-M06-19, RN-M06-20 (p. 50).
 * La narrativa vincula reposicion con sancion y ejemplar, sin exigir una multa: la disposicion grafica es ambigua.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "reposiciones_material")
@Getter
@Setter
@NoArgsConstructor
public class ReposicionMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "costo", nullable = true, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "condiciones", nullable = true, length = 3000)
    private String condiciones;

    @Column(name = "material_reemplazo", nullable = true, length = 2000)
    private String materialReemplazo;

    @Column(name = "fecha_cumplimiento", nullable = true)
    private LocalDate fechaCumplimiento;

    @Column(name = "fecha_validacion", nullable = true)
    private LocalDateTime fechaValidacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sancion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_reposiciones_material_sancion"))
    private Sancion sancion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_reposiciones_material_ejemplar"))
    private Ejemplar ejemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ejemplar_reposicion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_reposiciones_material_ejemplar_reposicion"))
    private Ejemplar ejemplarReposicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "validador_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_reposiciones_material_validador"))
    private CuentaUsuario validador;
}

