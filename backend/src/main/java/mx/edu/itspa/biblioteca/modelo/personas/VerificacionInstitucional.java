package mx.edu.itspa.biblioteca.modelo.personas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * M01, PDF pp. impresas 12-14 (fisicas 13-15), RF-M01-38/39/40/41, RN-M01-14/16/18.
 * Decision de modelado: verificacion pertenece a CuentaUsuario por RF-M01-39; no a una visita.
 * Tipo y resultado conservan texto porque las fuentes no fijan vocabularios cerrados.
 * El responsable puede faltar en verificaciones automaticas; no se almacenan tokens ni secretos.
 */
@Entity
@Table(name = "verificaciones_institucionales")
@Getter
@Setter
@NoArgsConstructor
public class VerificacionInstitucional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 80)
    private String tipo;

    @Column(name = "resultado", length = 80)
    private String resultado;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaUsuario cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private CuentaUsuario responsable;
}
