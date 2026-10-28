package mx.edu.itspa.biblioteca.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Objects;

/** Identidad obtenida únicamente de la sesión autenticada del servidor. */
@Service
public class IdentidadActual {
    private SesionServicioImpl.CuentaPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof SesionServicioImpl.CuentaPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return p;
    }
    public Long cuentaId() { return principal().cuentaId(); }
    public Long personaId() { return principal().personaId(); }
    public boolean esGestion() { return tieneRol("ADMINISTRADOR") || tieneRol("BIBLIOTECARIO"); }
    public boolean tieneRol(String rol) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_" + rol));
    }
    public void exigirPropietario(Long personaId) {
        if (!esGestion() && !Objects.equals(personaId(), personaId)) denegar();
    }
    public void exigirGestion() { if (!esGestion()) denegar(); }
    public void exigirAdministrador() { if (!tieneRol("ADMINISTRADOR")) denegar(); }
    private void denegar() { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para esta operación."); }
}
