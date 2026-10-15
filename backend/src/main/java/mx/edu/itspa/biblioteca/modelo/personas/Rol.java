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
 * M01, PDF pp. impresas 9, 11 y 13-14 (fisicas 10, 12 y 14-15), RF-M01-19/20/21/22, RN-M01-06/11/13.
 * Bibliotecario y Administrador son valores de rol, no subclases de Persona.
 * Decision aprobada: Rol-Permiso N:M sustituye el 1:1 ambiguo del grafico; no se precargan roles ni permisos.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 80, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<CuentaUsuario> cuentas = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "roles_permisos", joinColumns = @JoinColumn(name = "rol_id"),
        inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new LinkedHashSet<>();
}
