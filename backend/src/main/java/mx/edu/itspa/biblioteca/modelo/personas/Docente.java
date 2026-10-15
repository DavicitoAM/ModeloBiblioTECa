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
 * M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-03/05, RN-M01-03/17.
 * Decision aprobada: numeroEmpleado y PK se heredan de Persona; departamento academico es una
 * asociacion a Departamento. No se reproduce la segunda casilla Carrera ambigua del diagrama.
 */
@Entity
@Table(name = "docentes")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Docente extends Persona {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;
}
