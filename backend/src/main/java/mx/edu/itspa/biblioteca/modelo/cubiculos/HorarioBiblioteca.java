package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M05, PDF pp. impresas 36/40/42-43 (fisicas 37/41/43-44). RF-M05-60/61/62; RN-M05-10/30.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * fechaEspecial y cerrado permiten representar cierres/horarios especiales de RF-M05-62; no se fija un calendario.
 * Las horas admiten nulos para un dia cerrado; interpretar precedencia entre horario semanal y especial queda para el futuro service.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "horarios_biblioteca")
@Getter
@Setter
@NoArgsConstructor
public class HorarioBiblioteca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dia_semana", length = 20)
    private String diaSemana;

    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "fecha_especial")
    private LocalDate fechaEspecial;

    @Column(name = "cerrado")
    private Boolean cerrado;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "horarioBiblioteca", fetch = FetchType.LAZY)
    private List<UsoCubiculo> usos = new ArrayList<>();
}
