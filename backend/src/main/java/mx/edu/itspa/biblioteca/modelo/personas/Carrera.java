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
 * M01, PDF pp. impresas 8-9 (fisicas 9-10), RF-M01-02; atributos del diagrama M01.
 * Decision aprobada: existe una unica Carrera para estudiantes; no se duplica la casilla
 * rotulada Carrera que contiene atributos de Docente.
 */
@Entity
@Table(name = "carreras")
@Getter
@Setter
@NoArgsConstructor
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY)
    private Set<Estudiante> estudiantes = new LinkedHashSet<>();
}
