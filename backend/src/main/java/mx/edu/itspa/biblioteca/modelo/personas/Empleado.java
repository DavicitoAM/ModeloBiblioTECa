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
 * M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-04/05, RN-M01-03/17.
 * Hereda PK y numeroEmpleado de Persona. Area de trabajo procede del PDF; la relacion opcional
 * con Departamento permite la adscripcion aprobada sin sustituir el area ni inventar un departamento.
 */
@Entity
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Empleado extends Persona {

    @Column(name = "area_trabajo", nullable = false, length = 150)
    private String areaTrabajo;

    @Column(name = "puesto", length = 150)
    private String puesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;
}
