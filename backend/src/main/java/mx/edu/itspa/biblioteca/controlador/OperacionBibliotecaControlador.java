package mx.edu.itspa.biblioteca.controlador;

import java.util.List;
import java.util.Map;
import mx.edu.itspa.biblioteca.services.OperacionBibliotecaServicio;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Adaptador HTTP M02–M06. Validaciones, autorización por propietario y reglas en Service. */
@RestController
@RequestMapping("/api/app")
public class OperacionBibliotecaControlador {
    private final OperacionBibliotecaServicio servicio;
    public OperacionBibliotecaControlador(OperacionBibliotecaServicio servicio){this.servicio=servicio;}
    @GetMapping("/catalogo") public List<Map<String,Object>> catalogo(@RequestParam(required=false)String q,@RequestParam(required=false)String categoria,@RequestParam(required=false)String disponibilidad){return servicio.catalogo(q,categoria,disponibilidad);}
    @GetMapping("/catalogo/{id}") public Map<String,Object> libro(@PathVariable long id){return servicio.libro(id);}
    @PostMapping("/catalogo") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> nuevoLibro(@RequestBody Map<String,Object>d){return servicio.guardarLibro(null,d);}
    @PutMapping("/catalogo/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> editarLibro(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.guardarLibro(id,d);}
    @GetMapping("/ejemplares") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public List<Map<String,Object>> ejemplares(@RequestParam(name="libro_id",required=false)Long libroId){return servicio.ejemplares(libroId);}
    @PostMapping("/ejemplares") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> nuevoEjemplar(@RequestBody Map<String,Object>d){return servicio.guardarEjemplar(null,d);}
    @PutMapping("/ejemplares/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> editarEjemplar(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.guardarEjemplar(id,d);}
    @GetMapping("/catalogos") public Map<String,Object> catalogos(){return servicio.catalogos();}
    @PostMapping("/catalogos/{tipo}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> catalogoNuevo(@PathVariable String tipo,@RequestBody Map<String,Object>d){return servicio.crearCatalogo(tipo,d);}
    @PutMapping("/catalogos/{tipo}/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> catalogoEditar(@PathVariable String tipo,@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.editarCatalogo(tipo,id,d);}
    @GetMapping("/politicas/{tipo}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public List<Map<String,Object>> politicas(@PathVariable String tipo){return servicio.politicas(tipo);}
    @PostMapping("/politicas/{tipo}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Map<String,Object> politicaNueva(@PathVariable String tipo,@RequestBody Map<String,Object>d){return servicio.guardarPolitica(tipo,null,d);}
    @PutMapping("/politicas/{tipo}/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Map<String,Object> politicaEditar(@PathVariable String tipo,@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.guardarPolitica(tipo,id,d);}
    @GetMapping("/prestamos") public List<Map<String,Object>> prestamos(){return servicio.prestamos();}
    @PostMapping("/prestamos") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> prestar(@RequestBody Map<String,Object>d){return servicio.prestar(d);}
    @PostMapping("/prestamos/{id}/devolver") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> devolver(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.devolver(id,d);}
    @PostMapping("/prestamos/{id}/renovar") public Map<String,Object> renovar(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.renovar(id,d);}
    @GetMapping("/reservas") public List<Map<String,Object>> reservas(){return servicio.reservas();}
    @PostMapping("/reservas") public Map<String,Object> reservar(@RequestBody Map<String,Object>d){return servicio.reservar(d);}
    @PostMapping("/reservas/{id}/{accion}") public Map<String,Object> reservaAccion(@PathVariable long id,@PathVariable String accion,@RequestBody(required=false)Map<String,Object>d){return servicio.accionReserva(id,accion,d==null?Map.of():d);}
    @GetMapping("/cubiculos") public List<Map<String,Object>> cubiculos(){return servicio.cubiculos();}
    @PostMapping("/cubiculos") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> nuevoCubiculo(@RequestBody Map<String,Object>d){return servicio.guardarCubiculo(null,d);}
    @PutMapping("/cubiculos/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> editarCubiculo(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.guardarCubiculo(id,d);}
    @GetMapping("/horarios") public List<Map<String,Object>> horarios(){return servicio.horarios();}
    @PostMapping("/horarios") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> horarioNuevo(@RequestBody Map<String,Object>d){return servicio.guardarHorario(null,d);}
    @PutMapping("/horarios/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> horarioEditar(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.guardarHorario(id,d);}
    @GetMapping("/bloqueos-cubiculo") public List<Map<String,Object>> bloqueos(){return servicio.bloqueos();}
    @PostMapping("/bloqueos-cubiculo") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> bloqueoNuevo(@RequestBody Map<String,Object>d){return servicio.guardarBloqueo(d);}
    @PostMapping("/bloqueos-cubiculo/{id}/cancelar") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> bloqueoCancelar(@PathVariable long id,@RequestBody Map<String,Object>d){return servicio.cancelarBloqueo(id,d);}
    @GetMapping("/reservas-cubiculo") public List<Map<String,Object>> reservasCubiculo(){return servicio.reservasCubiculo();}
    @PostMapping("/reservas-cubiculo") public Map<String,Object> reservarCubiculo(@RequestBody Map<String,Object>d){return servicio.reservarCubiculo(d);}
    @PostMapping("/reservas-cubiculo/{id}/{accion}") public Map<String,Object> cubiculoAccion(@PathVariable long id,@PathVariable String accion,@RequestBody(required=false)Map<String,Object>d){return servicio.accionCubiculo(id,accion,d==null?Map.of():d);}
    @GetMapping("/incidencias") public List<Map<String,Object>> incidencias(){return servicio.incidencias();}
    @PostMapping("/incidencias") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> incidenciaNueva(@RequestBody Map<String,Object>d){return servicio.registrarIncidencia(d);}
    @GetMapping("/sanciones") public List<Map<String,Object>> sanciones(){return servicio.sanciones();}
    @PostMapping("/sanciones") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> sancionNueva(@RequestBody Map<String,Object>d){return servicio.sancionar(d);}
    @PostMapping("/sanciones/{id}/{accion}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')") public Map<String,Object> sancionAccion(@PathVariable long id,@PathVariable String accion,@RequestBody Map<String,Object>d){return servicio.accionSancion(id,accion,d);}
}
