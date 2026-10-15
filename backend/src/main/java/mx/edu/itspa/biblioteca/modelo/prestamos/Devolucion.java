package mx.edu.itspa.biblioteca.modelo.prestamos;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.EstadoConservacion;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M03, PDF pp. impresas 22/24-28 (fisicas 23/25-29). RF-M03-27/29/30/31/32/35/39; RN-M03-13/14/15.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Cada devolucion corresponde a un unico detalle; su FK UNIQUE impide dos devoluciones definitivas.
 * estado conserva el atributo textual del diagrama; estadoConservacion registra por separado la condicion fisica exigida por RF-M03-31.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "devoluciones")
@Getter
@Setter
@NoArgsConstructor
public class Devolucion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_devolucion", nullable = false)
    private LocalDateTime fechaDevolucion;

    @Column(name = "estado", length = 30)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacion", nullable = false, length = 30)
    private EstadoConservacion estadoConservacion;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_prestamo_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_devoluciones_detalle_prestamo"))
    private DetallePrestamo detallePrestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bibliotecario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_devoluciones_bibliotecario"))
    private CuentaUsuario bibliotecario;
}
