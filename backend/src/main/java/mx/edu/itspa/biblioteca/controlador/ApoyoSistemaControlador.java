package mx.edu.itspa.biblioteca.controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import mx.edu.itspa.biblioteca.services.ApoyoSistemaServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Adaptador HTTP sin lógica de negocio; M07 pp.52–59, M08 pp.60–67, M09 pp.68–76. */
@RestController
@RequestMapping("/api/app")
public class ApoyoSistemaControlador {
    private final ApoyoSistemaServicio servicio;

    public ApoyoSistemaControlador(ApoyoSistemaServicio servicio) { this.servicio = servicio; }

    @GetMapping("/inicio")
    public Map<String, Object> inicio() { return servicio.inicio(); }

    @GetMapping("/panel")
    public Map<String, Object> panel() { return servicio.panel(); }

    @GetMapping("/notificaciones")
    public Map<String, Object> notificaciones(@RequestParam(required=false) String estado,
            @RequestParam(required=false) String tipo, @RequestParam(defaultValue="false") boolean archivadas,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="false") boolean gestion,
            @RequestParam(name="cuenta_id",required=false) Long cuentaId) {
        return servicio.notificaciones(estado, tipo, archivadas, desde, hasta, gestion, cuentaId);
    }

    @PostMapping("/notificaciones/{id}/leer")
    public Map<String, Object> leer(@PathVariable long id) { return servicio.leerNotificacion(id); }

    @PostMapping("/notificaciones/todas-leidas")
    public Map<String, Object> leerTodas() { return servicio.leerTodas(); }

    @PostMapping("/notificaciones/{id}/archivar")
    public Map<String, Object> archivar(@PathVariable long id) { return servicio.archivarNotificacion(id); }

    @GetMapping("/preferencias")
    public Map<String, Object> preferencias() { return servicio.preferencias(); }

    @PutMapping("/preferencias")
    public Map<String, Object> preferencias(@RequestBody Map<String, Object> solicitud) { return servicio.guardarPreferencias(solicitud); }

    @GetMapping("/avisos")
    public List<Map<String, Object>> avisos(@RequestParam(defaultValue="false") boolean gestion) { return servicio.avisos(gestion); }

    @PostMapping("/avisos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String, Object> crearAviso(@RequestBody Map<String, Object> solicitud) { return servicio.guardarAviso(null, solicitud); }

    @PutMapping("/avisos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String, Object> editarAviso(@PathVariable long id, @RequestBody Map<String, Object> solicitud) { return servicio.guardarAviso(id, solicitud); }

    @GetMapping("/plantillas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> plantillas() { return servicio.plantillas(); }

    @PostMapping("/plantillas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> plantilla(@RequestBody Map<String, Object> solicitud) { return servicio.guardarPlantilla(null,solicitud); }

    @PutMapping("/plantillas/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> plantilla(@PathVariable long id,@RequestBody Map<String, Object> solicitud) { return servicio.guardarPlantilla(id,solicitud); }

    @PostMapping("/plantillas/previsualizar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> previsualizar(@RequestBody Map<String, Object> solicitud) { return servicio.previsualizarPlantilla(solicitud); }

    @PutMapping("/canales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> canal(@PathVariable long id,@RequestBody Map<String, Object> solicitud) { return servicio.guardarCanal(id,solicitud); }

    @GetMapping("/reportes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public Map<String, Object> reportes(@RequestParam(defaultValue="catalogo") String tipo,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) { return servicio.reporte(tipo, desde, hasta); }

    @GetMapping("/reportes/exportar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BIBLIOTECARIO')")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue="catalogo") String tipo,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="csv") String formato) { return descargar(servicio.exportar(tipo, desde, hasta, formato)); }

    @GetMapping("/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> configuracion() { return servicio.configuracion(); }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> configuracion(@RequestBody Map<String, Object> solicitud) { return servicio.guardarConfiguracion(solicitud); }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Map<String, Object>> auditoria(@RequestParam(required=false) String modulo,
            @RequestParam(required=false) String accion, @RequestParam(required=false) String usuario,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) { return servicio.auditoria(modulo, accion, usuario, desde, hasta); }

    @GetMapping("/respaldos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Map<String, Object>> respaldos() { return servicio.respaldos(); }

    @PostMapping("/respaldos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> crearRespaldo() { return servicio.crearRespaldo(); }

    @GetMapping("/respaldos/{id}/descargar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> respaldo(@PathVariable long id) { return descargar(servicio.descargarRespaldo(id)); }

    @GetMapping("/incidentes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Map<String, Object>> incidentes() { return servicio.incidentes(); }

    @PostMapping("/incidentes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> incidente(@RequestBody Map<String, Object> solicitud) { return servicio.guardarIncidente(null, solicitud); }

    @PutMapping("/incidentes/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> incidente(@PathVariable long id, @RequestBody Map<String, Object> solicitud) { return servicio.guardarIncidente(id, solicitud); }

    private ResponseEntity<byte[]> descargar(ApoyoSistemaServicio.Descarga descarga) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(descarga.nombre()).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_TYPE, descarga.tipoContenido()).body(descarga.contenido());
    }
}
