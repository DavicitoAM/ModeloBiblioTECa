package mx.edu.itspa.biblioteca;

import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import static org.assertj.core.api.Assertions.assertThat;

/** RNF-MAN-07 y RNF-INT-01: mapeo íntegro sin JDBC ni generación de esquema.
 * NO sustituye la validación del esquema real en PostgreSQL. */
class ModeloMetadatosTest {
    @Test
    void todasLasEntidadesFormanUnUnicoModeloJpaValidoSinAccederABd() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var entidades = scanner.findCandidateComponents("mx.edu.itspa.biblioteca.modelo");
        assertThat(entidades).hasSize(70);
        var registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .applySetting("jakarta.persistence.schema-generation.database.action", "none")
            .applySetting("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .build();
        try {
            var sources = new MetadataSources(registry);
            for (var entidad : entidades) sources.addAnnotatedClass(Class.forName(entidad.getBeanClassName()));
            try (SessionFactory factory = sources.buildMetadata().buildSessionFactory()) {
                assertThat(factory.getMetamodel().getEntities()).hasSize(70);
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
