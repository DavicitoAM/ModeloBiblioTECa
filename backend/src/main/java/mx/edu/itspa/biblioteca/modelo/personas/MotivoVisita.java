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
 * M01, PDF p. impresa 11 (fisica 12), RF-M01-27; atributos y relacion del diagrama M01.
 * Catalogo persistente de motivos, sin inventar ni precargar valores institucionales.
 */
@Entity
@Table(name = "motivos_visita")
@Getter
@Setter
@NoArgsConstructor
public class MotivoVisita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "estatus", nullable = false)
    private Boolean estatus = Boolean.TRUE;

    @OneToMany(mappedBy = "motivo", fetch = FetchType.LAZY)
    private Set<RegistroVisita> visitas = new LinkedHashSet<>();
}
