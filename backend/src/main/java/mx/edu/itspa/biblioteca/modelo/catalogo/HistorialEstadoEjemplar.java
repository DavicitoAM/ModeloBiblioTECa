package mx.edu.itspa.biblioteca.modelo.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/** M02, PDF impresa 18/21, RF-M02-36/62 y RN-M02-15/17/23. Registro histórico sin borrado en cascada. */
@Entity @Table(name = "historiales_estado_ejemplar")
@Getter @Setter @NoArgsConstructor
public class HistorialEstadoEjemplar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false)
    private Ejemplar ejemplar;
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 30)
    private EstadoEjemplar estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoEjemplar estadoNuevo;
    @Column(name = "observaciones", length = 2000)
    private String observaciones;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private CuentaUsuario responsable;
}
