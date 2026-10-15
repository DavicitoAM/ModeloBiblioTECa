package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.cubiculos.ReservaCubiculo;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;
import mx.edu.itspa.biblioteca.modelo.prestamos.Devolucion;
import mx.edu.itspa.biblioteca.modelo.prestamos.Prestamo;
import mx.edu.itspa.biblioteca.modelo.reservas.ReservaLibro;
import mx.edu.itspa.biblioteca.modelo.sanciones.PagoMulta;
import mx.edu.itspa.biblioteca.modelo.sanciones.ReposicionMaterial;
import mx.edu.itspa.biblioteca.modelo.sanciones.Sancion;

/**
 * M07, pp. impresas 53/55/57-59 (visor 54/56/58-60), RF-M07-02, RF-M07-07, RF-M07-38, RF-M07-44; RN-M07-02, RN-M07-04, RN-M07-05, RN-M07-10, RN-M07-14, RN-M07-28.
 * El destinatario es una cuenta registrada por RN-M07-14. Estado de lectura y resultado de envio son independientes; no hay envio automatico.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "asunto", nullable = false, length = 250)
    private String asunto;

    @Column(name = "mensaje", nullable = false, length = 10000)
    private String mensaje;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "fecha_lectura", nullable = true)
    private LocalDateTime fechaLectura;

    @Column(name = "archivada", nullable = false)
    private boolean archivada;

    @Column(name = "evento_origen", nullable = true, length = 150)
    private String eventoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notificaciones_destinatario"))
    private CuentaUsuario destinatario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_notificacion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notificaciones_tipo_notificacion"))
    private TipoNotificacion tipoNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "plantilla_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_plantilla"))
    private PlantillaNotificacion plantilla;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "prestamo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_prestamo"))
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "devolucion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_devolucion"))
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reserva_libro_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_reserva_libro"))
    private ReservaLibro reservaLibro;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reserva_cubiculo_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_reserva_cubiculo"))
    private ReservaCubiculo reservaCubiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sancion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_sancion"))
    private Sancion sancion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "pago_multa_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_pago_multa"))
    private PagoMulta pagoMulta;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reposicion_material_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_notificaciones_reposicion_material"))
    private ReposicionMaterial reposicionMaterial;

    @OneToMany(mappedBy = "notificacion", fetch = FetchType.LAZY)
    private List<EnvioNotificacion> envios = new ArrayList<>();
}

