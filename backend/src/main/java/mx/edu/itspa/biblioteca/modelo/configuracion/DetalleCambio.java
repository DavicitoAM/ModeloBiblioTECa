package mx.edu.itspa.biblioteca.modelo.configuracion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * M09 texto / M10 grafico, PDF pp. impresas 71 y 75 (fisicas 72 y 76),
 * RF-M09-40/41/42/43/44/45/46, RN-M09-13/14.
 * Cada detalle pertenece obligatoriamente a un evento. Valores son opcionales para altas/bajas
 * o informacion que no sea seguro conservar; no deben contener contrasenas, tokens ni secretos.
 */
@Entity
@Table(name = "detalles_cambio")
@Getter
@Setter
@NoArgsConstructor
public class DetalleCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "campo", nullable = false, length = 150)
    private String campo;

    @Column(name = "valor_anterior", length = 4000)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 4000)
    private String valorNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private BitacoraAuditoria evento;
}
