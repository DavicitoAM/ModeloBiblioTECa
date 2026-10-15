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
 * M01, PDF pp. impresas 8-10 y 13 (fisicas 9-11 y 14), RF-M01-02/05, RN-M01-02.
 * Hereda la PK de Persona, sin idEstudiante adicional. Estado academico procede de la narrativa;
 * su vocabulario institucional no esta cerrado, por lo que se conserva como texto sin inventar valores.
 */
@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Estudiante extends Persona {

    @Column(name = "matricula", nullable = false, length = 40, unique = true)
    private String matricula;

    @Column(name = "semestre")
    private Integer semestre;

    @Column(name = "estado_academico", length = 80)
    private String estadoAcademico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;
}
