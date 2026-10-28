package mx.edu.itspa.biblioteca.controlador;

import jakarta.validation.Valid;
import mx.edu.itspa.biblioteca.dto.PersonasDtos.*;
import mx.edu.itspa.biblioteca.services.PersonasServicio;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Controllers delegan la autorización y las transacciones a los services. */
@RestController
@RequestMapping("/api/app")
public class PersonasControlador {
    private final PersonasServicio servicio;
    public PersonasControlador(PersonasServicio servicio){this.servicio=servicio;}
    @GetMapping("/personas") public List<Map<String,Object>> personas(@RequestParam(required=false) String q){return servicio.personas(q);}
    @GetMapping("/personas/{id}") public Map<String,Object> persona(@PathVariable Long id){return servicio.persona(id);}
    @PostMapping("/personas") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> crearPersona(@Valid @RequestBody PersonaSolicitud s){return servicio.guardarPersona(null,s);}
    @PutMapping("/personas/{id}") public Map<String,Object> editarPersona(@PathVariable Long id,@Valid @RequestBody PersonaSolicitud s){return servicio.guardarPersona(id,s);}
    @GetMapping("/cuentas") public List<Map<String,Object>> cuentas(){return servicio.cuentas();}
    @PostMapping("/cuentas") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> crearCuenta(@Valid @RequestBody CuentaSolicitud s){return servicio.guardarCuenta(null,s);}
    @PutMapping("/cuentas/{id}") public Map<String,Object> editarCuenta(@PathVariable Long id,@Valid @RequestBody CuentaSolicitud s){return servicio.guardarCuenta(id,s);}
    @PostMapping("/cuentas/{id}/verificacion") public Map<String,Object> verificar(@PathVariable Long id,@Valid @RequestBody VerificacionSolicitud s){return servicio.verificar(id,s);}
    @GetMapping("/cuentas/{id}/verificaciones") public List<Map<String,Object>> verificaciones(@PathVariable Long id){return servicio.verificaciones(id);}
    @GetMapping("/roles") public List<Map<String,Object>> roles(){return servicio.roles();}
    @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> crearRol(@Valid @RequestBody RolSolicitud s){return servicio.guardarRol(null,s);}
    @PutMapping("/roles/{id}") public Map<String,Object> editarRol(@PathVariable Long id,@Valid @RequestBody RolSolicitud s){return servicio.guardarRol(id,s);}
    @GetMapping("/catalogos-personas") public Map<String,Object> catalogos(){return servicio.catalogos();}
    @PostMapping("/catalogos-personas/{tipo}") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> crearCatalogo(@PathVariable String tipo,@Valid @RequestBody CatalogoSolicitud s){return servicio.guardarCatalogo(tipo,null,s);}
    @PutMapping("/catalogos-personas/{tipo}/{id}") public Map<String,Object> editarCatalogo(@PathVariable String tipo,@PathVariable Long id,@Valid @RequestBody CatalogoSolicitud s){return servicio.guardarCatalogo(tipo,id,s);}
    @GetMapping("/visitas") public List<Map<String,Object>> visitas(@RequestParam(defaultValue="false") boolean activas,@RequestParam(required=false) Long persona_id){return servicio.visitas(activas,persona_id);}
    @PostMapping("/visitas") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> visita(@Valid @RequestBody VisitaSolicitud s){return servicio.registrarVisita(s);}
    @PostMapping("/visitas/{id}/salida") public Map<String,Object> salida(@PathVariable Long id){return servicio.salida(id);}
    @PutMapping("/visitas/{id}") public Map<String,Object> corregirVisita(@PathVariable Long id,@Valid @RequestBody CorreccionVisita s){return servicio.corregirVisita(id,s);}
}
