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
 * M01, PDF pp. impresas 8-9 (fisicas 9-10), RF-M01-03/04; diagrama M01.
 * Decision aprobada: adscripcion de Docente y opcionalmente Empleado a Departamento.
 * La asociacion no reemplaza el area de trabajo narrativa del Empleado.
 */
@Entity
@Table(name = "departamentos")
@Getter
@Setter
@NoArgsConstructor
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @OneToMany(mappedBy = "departamento", fetch = FetchType.LAZY)
    private Set<Docente> docentes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "departamento", fetch = FetchType.LAZY)
    private Set<Empleado> empleados = new LinkedHashSet<>();
}
