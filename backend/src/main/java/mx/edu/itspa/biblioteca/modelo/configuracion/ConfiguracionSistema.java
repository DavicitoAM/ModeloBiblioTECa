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
 * M09 del texto / M10 del diagrama, PDF pp. impresas 68-70, 74 y 76 (fisicas 69-71, 75 y 77),
 * RF-M09-01/02/03/04/05/06/08/09/72/87/88/89, RN-M09-30/32.
 * Se conserva la diferencia de numeracion. Atributos institucionales, contacto, zona horaria, formatos
 * y version proceden del PDF; no contienen valores reales ni politicas operativas precargadas.
 */
@Entity
@Table(name = "configuraciones_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "nombre_institucion", nullable = false, length = 200)
    private String nombreInstitucion;

    @Column(name = "nombre_biblioteca", nullable = false, length = 200)
    private String nombreBiblioteca;

    @Column(name = "correo_contacto", length = 254)
    private String correoContacto;

    @Column(name = "telefono_contacto", length = 30)
    private String telefonoContacto;

    @Column(name = "direccion_contacto", length = 500)
    private String direccionContacto;

    @Column(name = "zona_horaria", nullable = false, length = 60)
    private String zonaHoraria;

    @Column(name = "formato_fecha", length = 80)
    private String formatoFecha;

    @Column(name = "formato_hora", length = 80)
    private String formatoHora;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Column(name = "version_aplicacion", length = 80)
    private String versionAplicacion;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "frecuencia_respaldos", length = 100)
    private String frecuenciaRespaldos;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_modificacion_id")
    private CuentaUsuario responsableModificacion;

    @OneToMany(mappedBy = "configuracion", fetch = FetchType.LAZY)
    private Set<ParametroSistema> parametros = new LinkedHashSet<>();
}
