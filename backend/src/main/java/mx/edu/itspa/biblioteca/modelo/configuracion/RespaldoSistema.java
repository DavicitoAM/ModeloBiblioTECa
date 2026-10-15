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
 * M09 texto / M10 grafico, PDF pp. impresas 68, 73 y 75 (fisicas 69, 74 y 76),
 * RF-M09-67/68/69/70/71/74/75/76, RN-M09-24/25/26/27.
 * Registra metadatos de respaldo o restauracion, no el contenido ni credenciales del archivo.
 * Tipo y estado mantienen vocabularios abiertos del diagrama; responsable puede faltar en ejecucion automatica.
 */
@Entity
@Table(name = "respaldos_sistema")
@Getter
@Setter
@NoArgsConstructor
public class RespaldoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "archivo", length = 500)
    private String archivo;

    @Column(name = "tipo", nullable = false, length = 80)
    private String tipo;

    @Column(name = "estado", nullable = false, length = 80)
    private String estado;

    @Column(name = "resultado", length = 2000)
    private String resultado;

    @Column(name = "verificacion_integridad", length = 1000)
    private String verificacionIntegridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private CuentaUsuario responsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_auditoria_id")
    private BitacoraAuditoria eventoAuditoria;
}
