package mx.edu.itspa.biblioteca.repositorio;

import mx.edu.itspa.biblioteca.modelo.catalogo.Ejemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EjemplarRepositorio extends JpaRepository<Ejemplar, Long> {
    boolean existsByCodigoBarras(String codigoBarras);
    List<Ejemplar> findByLibroId(Long libroId);
}
