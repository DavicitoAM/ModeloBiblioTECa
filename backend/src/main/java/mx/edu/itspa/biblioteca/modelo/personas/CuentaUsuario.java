package mx.edu.itspa.biblioteca.modelo.personas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import mx.edu.itspa.biblioteca.modelo.configuracion.SesionUsuario;

/**
 * M01, PDF pp. impresas 10-14 (fisicas 11-15), RF-M01-11/18/19/38/39/41, RN-M01-04/05/06/12.
 * Cuenta pertenece a una sola Persona mediante FK UNIQUE; Persona puede no tener cuenta.
 * Decision aprobada: roles N:M. contrasenaHash representa exclusivamente un hash, nunca texto plano;
 * no se incluyen valores de credenciales ni servicios de autenticacion. Estados de verificacion abiertos.
 */
@Entity
@Table(name = "cuentas_usuario")
@Getter
@Setter
@NoArgsConstructor
public class CuentaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "usuario", nullable = false, length = 100, unique = true)
    private String usuario;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_verificacion_correo")
    private LocalDateTime fechaVerificacionCorreo;

    @Column(name = "estado_verificacion", length = 60)
    private String estadoVerificacion;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cuentas_roles", joinColumns = @JoinColumn(name = "cuenta_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "cuenta", fetch = FetchType.LAZY)
    private Set<VerificacionInstitucional> verificaciones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "cuenta", fetch = FetchType.LAZY)
    private Set<SesionUsuario> sesiones = new LinkedHashSet<>();
}
