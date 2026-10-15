package mx.edu.itspa.biblioteca.modelo.reservas;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;

/**
 * M04, PDF pp. impresas 29/31/34-35 (fisicas 30/32/35-36). RF-M04-16/20/21/22/23/24/26; RN-M04-10/11/23.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Entidad exigida por la narrativa aunque omitida del PNG; se conserva una lista por libro.
 * El orden se almacena en ReservaLibro.posicionEspera y se expone ordenado por posicion y fecha; no se inventa una regla de prioridad.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "listas_espera")
@Getter
@Setter
@NoArgsConstructor
public class ListaEspera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_listas_espera_libro"))
    private Libro libro;

    @OneToMany(mappedBy = "listaEspera", fetch = FetchType.LAZY)
    @OrderBy("posicionEspera ASC, fechaReserva ASC")
    private List<ReservaLibro> reservas = new ArrayList<>();
}
