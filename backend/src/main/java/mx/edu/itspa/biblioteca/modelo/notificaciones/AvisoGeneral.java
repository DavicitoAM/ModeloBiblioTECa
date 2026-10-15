package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.CuentaUsuario;

/**
 * M07, p. impresa 57 (visor 58), RF-M07-67, RF-M07-75; RN-M07-20, RN-M07-21, RN-M07-22, RN-M07-23, RN-M07-25, RN-M07-27 (pp. 58-59).
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "avisos_generales")
@Getter
@Setter
@NoArgsConstructor
public class AvisoGeneral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 10000)
    private String mensaje;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_expiracion", nullable = true)
    private LocalDateTime fechaExpiracion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "publico", nullable = false)
    private boolean publico;

    @Column(name = "fecha_modificacion", nullable = true)
    private LocalDateTime fechaModificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_avisos_generales_responsable"))
    private CuentaUsuario responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "responsable_modificacion_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_avisos_generales_responsable_modificacion"))
    private CuentaUsuario responsableModificacion;

    @OneToMany(mappedBy = "aviso", fetch = FetchType.LAZY)
    private List<DestinatarioAviso> destinatarios = new ArrayList<>();
}

