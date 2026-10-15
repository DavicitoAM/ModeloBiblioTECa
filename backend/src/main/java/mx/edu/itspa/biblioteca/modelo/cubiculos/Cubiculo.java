package mx.edu.itspa.biblioteca.modelo.cubiculos;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M05, PDF pp. impresas 36-38/41-43 (fisicas 37-39/42-44). RF-M05-01/02/03/04/07/08; RN-M05-01/02/04/05.
 * Atributos del PNG completados con la narrativa. ID, alias SQL y longitudes son decisiones tecnicas.
 * Codigo y ubicacion completan los atributos del PNG con RF-M05-02/03; ubicacion es descriptiva, sin inventar otra entidad.
 * Equipamiento se comparte mediante una relacion muchos a muchos aprobada; no se borra en cascada.
 * Modelo persistente sin services operativos para este modulo y sin cascada de eliminacion.
 */
@Entity
@Table(name = "cubiculos")
@Getter
@Setter
@NoArgsConstructor
public class Cubiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "ubicacion", length = 250)
    private String ubicacion;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoCubiculo estado;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "cubiculo", fetch = FetchType.LAZY)
    private List<ReservaCubiculo> reservas = new ArrayList<>();

    @OneToMany(mappedBy = "cubiculo", fetch = FetchType.LAZY)
    private List<BloqueoCubiculo> bloqueos = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cubiculos_equipamientos",
        joinColumns = @JoinColumn(name = "cubiculo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cubiculos_equipamientos_cubiculo")),
        inverseJoinColumns = @JoinColumn(name = "equipamiento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cubiculos_equipamientos_equipamiento")))
    private Set<Equipamiento> equipamientos = new HashSet<>();
}
