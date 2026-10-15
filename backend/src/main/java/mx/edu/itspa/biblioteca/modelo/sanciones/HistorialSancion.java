package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M06, pp. impresas 46-47/51 (visor 47-48/52), RF-M06-30, RF-M06-32; RN-M06-27, RN-M06-28, RN-M06-29, RN-M06-30.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "historiales_sancion")
@Getter
@Setter
@NoArgsConstructor
public class HistorialSancion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = true, length = 60)
    private EstadoSancion estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 60)
    private EstadoSancion estadoNuevo;

    @Column(name = "observaciones", nullable = true, length = 3000)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sancion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historiales_sancion_sancion"))
    private Sancion sancion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historiales_sancion_responsable"))
    private CuentaUsuario responsable;
}

