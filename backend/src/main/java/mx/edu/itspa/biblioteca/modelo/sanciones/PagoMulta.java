package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M06, p. impresa 48 (visor 49), RF-M06-51, RF-M06-59; RN-M06-15, RN-M06-16, RN-M06-17, RN-M06-29 (pp. 50-51).
 * Registro persistente sin procesamiento de pagos ni actualizacion automatica de saldos.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "pagos_multa")
@Getter
@Setter
@NoArgsConstructor
public class PagoMulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo", nullable = false, length = 100)
    private String metodo;

    @Column(name = "comprobante", nullable = true, length = 1000)
    private String comprobante;

    @Column(name = "referencia", nullable = true, length = 250)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "multa_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pagos_multa_multa"))
    private Multa multa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pagos_multa_responsable"))
    private CuentaUsuario responsable;
}

