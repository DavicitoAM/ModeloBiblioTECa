package mx.edu.itspa.biblioteca.modelo.prestamos;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;
import mx.edu.itspa.biblioteca.modelo.personas.Persona;

/**
 * M03, PDF pp. impresas 22-28 (fisicas 23-29). RF-M03-01/07/10/12/13/63; RN-M03-01/03/09/30.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * La coleccion representa uno o varios detalles (RN-M03-03); la cardinalidad minima requiere validacion del futuro service.
 * La fecha de cabecera se conserva del diagrama; el vencimiento individual pertenece a cada detalle.
 * Bibliotecario y confirmacion son opcionales antes de confirmar la entrega. No se implementa autorizacion.
 * condicionesAplicadas permite almacenar una instantanea al confirmar; no congela por si sola una politica mutable.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "prestamos")
@Getter
@Setter
@NoArgsConstructor
public class Prestamo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPrestamo estado;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "fecha_confirmacion_entrega")
    private LocalDateTime fechaConfirmacionEntrega;

    @Column(name = "condiciones_aplicadas", length = 4000)
    private String condicionesAplicadas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prestamos_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "bibliotecario_id", foreignKey = @ForeignKey(name = "fk_prestamos_bibliotecario"))
    private CuentaUsuario bibliotecario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prestamos_politica"))
    private PoliticaPrestamo politica;

    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY)
    private List<DetallePrestamo> detalles = new ArrayList<>();
}
