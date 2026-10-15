package mx.edu.itspa.biblioteca.modelo.sanciones;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * M06, p. impresa 44 (visor 45), elementos principales; RF-M06-23 (p. 46), RN-M06-06 (p. 50).
 * Catalogo exigido por la narrativa, omitido en el grafico; sin filas predeterminadas.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "tipos_sancion")
@Getter
@Setter
@NoArgsConstructor
public class TipoSancion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, length = 2000)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private boolean estatus;

    @OneToMany(mappedBy = "tipo", fetch = FetchType.LAZY)
    private List<Sancion> sanciones = new ArrayList<>();

    @OneToMany(mappedBy = "tipoSancion", fetch = FetchType.LAZY)
    private List<PoliticaSancion> politicas = new ArrayList<>();
}

