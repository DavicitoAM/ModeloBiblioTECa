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
 * M01, PDF pp. impresas 11-14 (fisicas 12-15), RF-M01-24/25/26/27/28/30/45/46, RN-M01-19/20/21/22.
 * Cada visita pertenece a Persona y MotivoVisita. Fecha de salida nula representa una visita abierta.
 * La migracion aplica unicidad parcial de visita abierta por persona y orden temporal;
 * las correcciones se documentan mediante BitacoraAuditoria, sin un servicio operativo adicional.
 */
@Entity
@Table(name = "registros_visita")
@Getter
@Setter
@NoArgsConstructor
public class RegistroVisita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada = LocalDateTime.now();

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoVisita motivo;
}
