package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M07, p. impresa 52 (visor 53), elementos principales; RF-M07-06 (p. 53), RN-M07-04 (p. 58).
 * Catalogo narrativo omitido en el diagrama; no contiene datos precargados.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "tipos_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class TipoNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @Column(name = "obligatoria", nullable = false)
    private boolean obligatoria;

    @OneToMany(mappedBy = "tipoNotificacion", fetch = FetchType.LAZY)
    private List<Notificacion> notificaciones = new ArrayList<>();

    @OneToMany(mappedBy = "tipoNotificacion", fetch = FetchType.LAZY)
    private List<PlantillaNotificacion> plantillas = new ArrayList<>();
}

