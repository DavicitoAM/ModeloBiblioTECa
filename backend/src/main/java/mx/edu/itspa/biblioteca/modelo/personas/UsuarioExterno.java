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
 * M01, PDF pp. impresas 8 y 12-14 (fisicas 9 y 13-15), RF-M01-36/37, RN-M01-15.
 * Hereda la identidad de Persona. Institucion es atributo del diagrama, no una clase adicional.
 * El modelo no concede permisos ni crea una autenticacion de demostracion.
 */
@Entity
@Table(name = "usuarios_externos")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class UsuarioExterno extends Persona {

    @Column(name = "institucion", length = 200)
    private String institucion;
}
