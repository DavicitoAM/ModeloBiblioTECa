package mx.edu.itspa.biblioteca.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Adaptadores M01: las respuestas nunca contienen contraseñas ni entidades JPA. */
public final class PersonasDtos {
    private PersonasDtos() { }

    public record Inicializacion(@NotBlank @Size(max=100) String nombre,
        @NotBlank @Size(max=150) String apellidos, @NotBlank @Size(max=100) String usuario,
        @NotBlank @Size(min=10,max=72) String password) { }
    public record Login(@NotBlank @Size(max=100) String usuario, @NotBlank @Size(max=72) String password) { }
    public record CambioPassword(@NotBlank String actual, @NotBlank @Size(min=10,max=72) String nueva) { }
    public record Perfil(@NotBlank @Size(max=100) String nombre,
        @NotBlank @Size(max=150) String apellidos, @Email @Size(max=254) String correo,
        @Size(max=30) String telefono) { }
    public record PersonaSolicitud(@NotBlank String tipo, @NotBlank @Size(max=100) String nombre,
        @NotBlank @Size(max=150) String apellidos, @Email @Size(max=254) String correo,
        @Size(max=30) String telefono, LocalDate fecha_nacimiento,
        @Size(max=60) String tipo_identificacion, @Size(max=80) String numero_identificacion,
        @Size(max=40) String numero_empleado, Boolean estatus, @Size(max=40) String matricula,
        @Min(1) @Max(20) Integer semestre, @Size(max=80) String estado_academico,
        Long carrera_id, Long departamento_id, @Size(max=150) String area_trabajo,
        @Size(max=150) String puesto, @Size(max=200) String institucion) { }
    public record CuentaSolicitud(@NotNull Long persona_id, @NotBlank @Size(max=100) String usuario,
        @Size(max=72) String password, @NotEmpty List<String> roles, Boolean estatus,
        @Size(max=60) String estado_verificacion) { }
    public record VisitaSolicitud(@NotNull Long persona_id, @NotNull Long motivo_id,
        @Size(max=1000) String observaciones) { }
    public record CorreccionVisita(@NotNull LocalDateTime fecha_entrada, LocalDateTime fecha_salida,
        @NotBlank @Size(max=1000) String motivo) { }
    public record CatalogoSolicitud(@NotBlank @Size(max=150) String nombre,
        @Size(max=500) String descripcion, Boolean estatus) { }
    public record RolSolicitud(@NotBlank @Size(max=80) String nombre,
        @Size(max=500) String descripcion, Boolean estatus, List<Long> permisos_ids) { }
    public record RegistroSolicitud(@NotNull @jakarta.validation.Valid PersonaSolicitud persona,
        @NotBlank @Size(max=100) String usuario, @NotBlank @Size(min=10,max=72) String password) { }
    public record VerificacionSolicitud(@NotBlank @Size(max=80) String resultado,
        @NotBlank @Size(max=1000) String observaciones) { }
}
