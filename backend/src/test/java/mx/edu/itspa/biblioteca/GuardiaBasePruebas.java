package mx.edu.itspa.biblioteca;

import java.net.URI;
import java.util.Locale;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Impide iniciar Flyway desde la suite si el destino no es la BD de pruebas aprobada. */
public class GuardiaBasePruebas implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        for (String propiedad : new String[]{"spring.flyway.url", "spring.datasource.hikari.jdbc-url", "spring.datasource.jndi-name"}) {
            String alternativa = context.getEnvironment().getProperty(propiedad);
            if (alternativa != null && !alternativa.isBlank()) {
                throw new IllegalStateException("Pruebas detenidas: no se admite un destino alternativo mediante " + propiedad);
            }
        }
        String url = context.getEnvironment().getProperty("spring.datasource.url", "");
        if (!url.startsWith("jdbc:postgresql:")) throw new IllegalStateException("Las pruebas requieren PostgreSQL de test");
        URI destino = URI.create(url.substring("jdbc:".length()));
        if (!"/biblioteca_modelo_test".equals(destino.getPath())) {
            throw new IllegalStateException("Pruebas detenidas: la base debe llamarse biblioteca_modelo_test");
        }
        String normal = context.getEnvironment().getProperty("DB_URL", "jdbc:postgresql://localhost:5432/modeloBiblioteca");
        URI principal = URI.create(normal.substring("jdbc:".length()));
        int puerto = destino.getPort() == -1 ? 5432 : destino.getPort();
        int puertoPrincipal = principal.getPort() == -1 ? 5432 : principal.getPort();
        if (normalizarHost(destino.getHost()).equals(normalizarHost(principal.getHost())) && puerto == puertoPrincipal
                && destino.getPath().equals(principal.getPath())) {
            throw new IllegalStateException("Pruebas detenidas: DB_URL y el destino de test son la misma base");
        }
    }

    private String normalizarHost(String host) {
        if (host == null) throw new IllegalStateException("La URL PostgreSQL debe indicar el servidor");
        String normalizado = host.toLowerCase(Locale.ROOT);
        return switch (normalizado) {
            case "localhost", "127.0.0.1", "::1", "[::1]" -> "loopback";
            default -> normalizado;
        };
    }
}
