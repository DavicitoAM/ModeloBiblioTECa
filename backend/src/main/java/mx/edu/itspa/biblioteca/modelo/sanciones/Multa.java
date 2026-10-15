package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M06, pp. impresas 47-50 (visor 48-51), RF-M06-43, RF-M06-49; RN-M06-11, RN-M06-12, RN-M06-13, RN-M06-14, RN-M06-18.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "multas")
@Getter
@Setter
@NoArgsConstructor
public class Multa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_limite_pago", nullable = true)
    private LocalDate fechaLimitePago;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "saldo_pendiente", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "observaciones", nullable = true, length = 3000)
    private String observaciones;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sancion_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_multas_sancion"))
    private Sancion sancion;

    @OneToMany(mappedBy = "multa", fetch = FetchType.LAZY)
    private List<PagoMulta> pagos = new ArrayList<>();
}

