package mx.edu.itspa.biblioteca.modelo.personas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * M01, PDF pp. impresas 11 y 13-14 (fisicas 12 y 14-15), RF-M01-21/22/23, RN-M01-08/11/13.
 * Decision aprobada: permisos reutilizables entre roles mediante N:M. La existencia del modelo
 * no implementa control de acceso ni autoriza operaciones del usuario.
 */
@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @ManyToMany(mappedBy = "permisos", fetch = FetchType.LAZY)
    private Set<Rol> roles = new LinkedHashSet<>();
}
