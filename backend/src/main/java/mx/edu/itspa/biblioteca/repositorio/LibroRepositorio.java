package mx.edu.itspa.biblioteca.repositorio;

import mx.edu.itspa.biblioteca.modelo.catalogo.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LibroRepositorio extends JpaRepository<Libro, Long> {
    boolean existsByIsbn(String isbn);
    Optional<Libro> findByIsbn(String isbn);
}
