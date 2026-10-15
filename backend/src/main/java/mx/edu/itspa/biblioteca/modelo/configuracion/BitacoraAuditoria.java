package mx.edu.itspa.biblioteca.modelo.configuracion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M09 texto / M10 grafico, PDF pp. impresas 70-71 y 74-76 (fisicas 71-72 y 75-77),
 * RF-M09-26/27/28/29/30/31/32/33/34/37, RN-M09-05/06/07/08/09/10/11/14/29.
 * Responsable opcional permite eventos automaticos o usuarios no identificados; operacionSistema
 * distingue el origen. No se guardan contrasenas ni tokens en resultados, motivos o detalles.
 * La relacion con entidad afectada usa nombre e ID porque puede abarcar cualquier modulo.
 */
@Entity
@Table(name = "bitacoras_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "modulo", nullable = false, length = 80)
    private String modulo;

    @Column(name = "entidad", length = 150)
    private String entidad;

    @Column(name = "id_registro")
    private Long idRegistro;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "resultado", nullable = false, length = 100)
    private String resultado;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @Column(name = "operacion_sistema", nullable = false)
    private Boolean operacionSistema = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private CuentaUsuario responsable;

    @OneToMany(mappedBy = "evento", fetch = FetchType.LAZY)
    private Set<DetalleCambio> detalles = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "eventosAuditoria", fetch = FetchType.LAZY)
    private Set<IncidenteSeguridad> incidentes = new LinkedHashSet<>();
}
