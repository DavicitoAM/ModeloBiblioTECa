package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M06, pp. impresas 44/49-51 (visor 45/50-52), RF-M06-74, RF-M06-80, RN-M06-06, RN-M06-31, RN-M06-32.
 * Las condiciones configurables se conservan como datos, sin calcular multas ni aplicar restricciones.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "politicas_sancion")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaSancion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_incidencia", nullable = false, length = 60)
    private TipoIncidencia tipoIncidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_sancion_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_politicas_sancion_tipo_sancion"))
    private TipoSancion tipoSancion;

    @Column(name = "reglas", nullable = false, length = 5000)
    private String reglas;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "tipo_usuario", nullable = true, length = 100)
    private String tipoUsuario;

    @Column(name = "dias_suspension", nullable = true)
    private Integer diasSuspension;

    @Column(name = "monto_multa", nullable = true, precision = 12, scale = 2)
    private BigDecimal montoMulta;

    @Column(name = "multas_autorizadas", nullable = false)
    private boolean multasAutorizadas;

    @Column(name = "permite_pagos_parciales", nullable = false)
    private boolean permitePagosParciales;

    @Column(name = "servicios_restringidos", nullable = true, length = 2000)
    private String serviciosRestringidos;

    @OneToMany(mappedBy = "politica", fetch = FetchType.LAZY)
    private List<Sancion> sanciones = new ArrayList<>();
}

