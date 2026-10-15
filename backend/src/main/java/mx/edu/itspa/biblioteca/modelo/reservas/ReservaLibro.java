package mx.edu.itspa.biblioteca.modelo.reservas;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;
import mx.edu.itspa.biblioteca.modelo.personas.Persona;
import mx.edu.itspa.biblioteca.modelo.prestamos.DetallePrestamo;

/**
 * M04, PDF pp. impresas 29-35 (fisicas 30-36). RF-M04-08/09/16/17/21/39/41/44/56; RN-M04-02/03/05/16/18/26.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * La solicitud se relaciona con Libro; el Ejemplar solo se incorpora mediante AsignacionReserva (RN-M04-05).
 * Una lista por libro y una posicion opcional por reserva son decisiones tecnicas expresamente aprobadas; no calculan prioridad.
 * Politica y condiciones aplicadas se completan al evaluar la solicitud; el modelo no garantiza por si solo su inmutabilidad.
 * El responsable de cancelacion es opcional porque una cancelacion puede solicitarla la propia Persona.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "reservas_libro", uniqueConstraints = @UniqueConstraint(name = "uk_reservas_lista_posicion", columnNames = {"lista_espera_id", "posicion_espera"}))
@Getter
@Setter
@NoArgsConstructor
public class ReservaLibro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoReservaLibro estado;

    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    @Column(name = "posicion_espera")
    private Integer posicionEspera;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 2000)
    private String motivoCancelacion;

    @Column(name = "condiciones_aplicadas", length = 4000)
    private String condicionesAplicadas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_libro_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_libro_libro"))
    private Libro libro;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "politica_id", foreignKey = @ForeignKey(name = "fk_reservas_libro_politica"))
    private PoliticaReservaLibro politica;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "lista_espera_id", foreignKey = @ForeignKey(name = "fk_reservas_libro_lista_espera"))
    private ListaEspera listaEspera;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "detalle_prestamo_id", unique = true, foreignKey = @ForeignKey(name = "fk_reservas_libro_detalle_prestamo"))
    private DetallePrestamo detallePrestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_cancelacion_id", foreignKey = @ForeignKey(name = "fk_reservas_libro_responsable_cancelacion"))
    private CuentaUsuario responsableCancelacion;

    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<AsignacionReserva> asignaciones = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", fetch = FetchType.LAZY)
    private List<HistorialReserva> historial = new ArrayList<>();
}
