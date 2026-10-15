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
 * M01, PDF pp. impresas 8-10 y 13-14 (fisicas 9-11 y 14-15), RF-M01-01/05/09, RN-M01-01/03/04/09/17.
 * Decision aprobada: herencia JOINED e ID Long comun; numeroEmpleado se centraliza aqui para
 * garantizar unicidad entre Docente y Empleado. Fecha de registro proviene de narrativa;
 * fecha de nacimiento e identificacion provienen del diagrama. No contiene credenciales.
 */
@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Column(name = "correo", length = 254)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "tipo_identificacion", length = 60)
    private String tipoIdentificacion;

    @Column(name = "numero_identificacion", length = 80)
    private String numeroIdentificacion;

    @Column(name = "numero_empleado", length = 40, unique = true)
    private String numeroEmpleado;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @OneToOne(mappedBy = "persona", fetch = FetchType.LAZY)
    private CuentaUsuario cuenta;

    @OneToMany(mappedBy = "persona", fetch = FetchType.LAZY)
    private Set<RegistroVisita> visitas = new LinkedHashSet<>();
}
