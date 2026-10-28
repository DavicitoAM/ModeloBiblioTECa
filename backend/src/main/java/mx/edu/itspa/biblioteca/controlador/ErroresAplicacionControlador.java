package mx.edu.itspa.biblioteca.controlador;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;

/** Mensajes para formularios, sin publicar SQL, hashes ni trazas internas. */
@RestControllerAdvice
public class ErroresAplicacionControlador {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> estado(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"No se pudo completar la operación.":e.getReason()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,String>> validacion(MethodArgumentNotValidException e){String texto=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).distinct().collect(Collectors.joining("; "));return ResponseEntity.badRequest().body(Map.of("error",texto));}
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<Map<String,String>> integridad(DataIntegrityViolationException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","El registro coincide con un identificador existente o tiene relaciones que impiden esa operación. Actualiza la información e intenta de nuevo."));}
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<Map<String,String>> permisos(AccessDeniedException e){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","No tienes permiso para esta operación."));}
}
