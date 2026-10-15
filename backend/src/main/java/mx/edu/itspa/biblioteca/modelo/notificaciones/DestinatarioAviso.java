package mx.edu.itspa.biblioteca.modelo.notificaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.itspa.biblioteca.modelo.personas.Rol;

/**
 * M07, p. impresa 57 (visor 58), RF-M07-70; RN-M07-24, RN-M07-25 (p. 59); atributos del diagrama.
 * idReferencia conserva el atributo polimorfico del grafico y no es una FK generica. Los destinatarios por rol cuentan con FK concreta; su interpretacion restante esta pendiente.
 * Long/IDENTITY, alias snake_case y longitudes son decisiones tecnicas de persistencia.
 * Sin logica de negocio ni eliminacion en cascada.
 */
@Entity
@Table(name = "destinatarios_aviso")
@Getter
@Setter
@NoArgsConstructor
public class DestinatarioAviso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo_usuario", nullable = false, length = 100)
    private String tipoUsuario;

    @Column(name = "id_referencia", nullable = true)
    private Long idReferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aviso_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_destinatarios_aviso_aviso"))
    private AvisoGeneral aviso;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "rol_id", nullable = true,
        foreignKey = @ForeignKey(name = "fk_destinatarios_aviso_rol"))
    private Rol rol;
}

