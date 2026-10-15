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
 * M09 texto / M10 grafico, PDF pp. impresas 68-70 y 74-76 (fisicas 69-71 y 75-77),
 * RF-M09-10/11/12/14/15/16/17, RN-M09-02/03/04/32.
 * Cada parametro pertenece a una configuracion, con clave unica. Tipo de dato es texto explicito
 * sin imponer un catalogo institucional inexistente. Obligatorio y esencial expresan RF-M09-15/16.
 */
@Entity
@Table(name = "parametros_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "clave", nullable = false, length = 100, unique = true)
    private String clave;

    @Column(name = "valor", nullable = false, length = 2000)
    private String valor;

    @Column(name = "tipo_dato", nullable = false, length = 60)
    private String tipoDato;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio = Boolean.FALSE;

    @Column(name = "esencial", nullable = false)
    private Boolean esencial = Boolean.FALSE;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuracion_id", nullable = false)
    private ConfiguracionSistema configuracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_modificacion_id")
    private CuentaUsuario responsableModificacion;
}
