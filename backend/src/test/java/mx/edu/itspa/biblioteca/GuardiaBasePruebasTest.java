package mx.edu.itspa.biblioteca;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.assertj.core.api.Assertions.*;

class GuardiaBasePruebasTest {
    @Test void permiteLaBaseDeTest() {
        try (var ctx = contexto("jdbc:postgresql://localhost:5432/biblioteca_modelo_test", null)) {
            assertThatCode(() -> new GuardiaBasePruebas().initialize(ctx)).doesNotThrowAnyException();
        }
    }
    @Test void rechazaLaBaseNormalAntesDeIniciarFlyway() {
        try (var ctx = contexto("jdbc:postgresql://localhost:5432/biblioteca_modelo", null)) {
            assertThatThrownBy(() -> new GuardiaBasePruebas().initialize(ctx)).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rechazaUnaBaseDistintaDePostgres() {
        try (var ctx = contexto("jdbc:h2:mem:biblioteca_modelo_test", null)) {
            assertThatThrownBy(() -> new GuardiaBasePruebas().initialize(ctx)).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rechazaTestSiDbUrlApuntaAlMismoDestino() {
        String url = "jdbc:postgresql://localhost:5432/biblioteca_modelo_test";
        try (var ctx = contexto(url, url)) {
            assertThatThrownBy(() -> new GuardiaBasePruebas().initialize(ctx)).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rechazaElMismoDestinoConAliasLocalDistinto() {
        try (var ctx = contexto("jdbc:postgresql://127.0.0.1:5432/biblioteca_modelo_test",
                "jdbc:postgresql://localhost/biblioteca_modelo_test")) {
            assertThatThrownBy(() -> new GuardiaBasePruebas().initialize(ctx)).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rechazaDatasourceIndependienteDeFlyway() { rechazaOverride("spring.flyway.url"); }
    @Test void rechazaOverrideDeHikari() { rechazaOverride("spring.datasource.hikari.jdbc-url"); }
    @Test void rechazaJndiNoInspeccionable() { rechazaOverride("spring.datasource.jndi-name"); }
    private void rechazaOverride(String propiedad) {
        try (var ctx = contexto("jdbc:postgresql://localhost:5432/biblioteca_modelo_test", null)) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("alternativa",
                Map.of(propiedad, "jdbc:postgresql://localhost:5432/biblioteca_modelo")));
            assertThatThrownBy(() -> new GuardiaBasePruebas().initialize(ctx)).isInstanceOf(IllegalStateException.class);
        }
    }
    private GenericApplicationContext contexto(String url, String principal) {
        var ctx = new GenericApplicationContext();
        Map<String, Object> valores = new HashMap<>(); valores.put("spring.datasource.url", url);
        valores.put("DB_URL", principal == null ? "jdbc:postgresql://localhost:5432/biblioteca_modelo" : principal);
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba", valores));
        return ctx;
    }
}
