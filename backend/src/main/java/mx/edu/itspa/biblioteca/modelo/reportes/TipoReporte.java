package mx.edu.itspa.biblioteca.modelo.reportes;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.Rol;

/**
 * M08, p. impresa 60 (visor 61), elementos principales; RF-M08-90 (p. 65), RN-M08-05, RN-M08-21, RN-M08-30 (pp. 66-67).
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "tipos_reporte")
@Getter
@Setter
@NoArgsConstructor
public class TipoReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "tipoReporte", fetch = FetchType.LAZY)
    private List<Reporte> reportes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tipos_reporte_roles",
        joinColumns = @JoinColumn(name = "tipo_reporte_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tipos_reporte_roles_tipo_reporte")),
        inverseJoinColumns = @JoinColumn(name = "rol_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tipos_reporte_roles_rol")))
    private Set<Rol> rolesPermitidos = new LinkedHashSet<>();
}

