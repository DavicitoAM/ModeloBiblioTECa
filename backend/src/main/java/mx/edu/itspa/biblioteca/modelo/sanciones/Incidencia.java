package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import mx.edu.itspa.biblioteca.modelo.cubiculos.ReservaCubiculo;
import mx.edu.itspa.biblioteca.modelo.cubiculos.UsoCubiculo;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;
import mx.edu.itspa.biblioteca.modelo.personas.Persona;
import mx.edu.itspa.biblioteca.modelo.prestamos.DetallePrestamo;
import mx.edu.itspa.biblioteca.modelo.prestamos.Devolucion;
import mx.edu.itspa.biblioteca.modelo.prestamos.Prestamo;
import mx.edu.itspa.biblioteca.modelo.reservas.ReservaLibro;

/**
 * M06, pp. impresas 45-46 (visor 46-47), RF-M06-01, RF-M06-09, RF-M06-14, RF-M06-16, RF-M06-18; RN-M06-02, RN-M06-03 (p. 49).
 * Los contextos de origen son opcionales segun RF-M06-09; el modelo no detecta ni genera incidencias.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
public class Incidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 60)
    private TipoIncidencia tipo;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Column(name = "observaciones", nullable = true, length = 3000)
    private String observaciones;

    @Column(name = "evidencias", nullable = true, length = 5000)
    private String evidencias;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_incidencias_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_incidencias_responsable"))
    private CuentaUsuario responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ejemplar_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_ejemplar"))
    private Ejemplar ejemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "prestamo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_prestamo"))
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "detalle_prestamo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_detalle_prestamo"))
    private DetallePrestamo detallePrestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "devolucion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_devolucion"))
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reserva_libro_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_reserva_libro"))
    private ReservaLibro reservaLibro;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reserva_cubiculo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_reserva_cubiculo"))
    private ReservaCubiculo reservaCubiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "uso_cubiculo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_incidencias_uso_cubiculo"))
    private UsoCubiculo usoCubiculo;

    @OneToMany(mappedBy = "incidencia", fetch = FetchType.LAZY)
    private List<Sancion> sanciones = new ArrayList<>();
}

