import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;

/**
 * RNF-MAN-07 (PDF p. impresa 81 / visor 82): ejecucion local de las pruebas
 * seleccionadas de M02, sin descargas ni JDBC. Usa el motor JUnit instalado
 * con su descubrimiento, ciclo de vida y parametrizacion. No sustituye Surefire.
 * Procedimiento y limites: documentacion/PRUEBAS_SIN_BD.md.
 */
public class JUnitSinBDLocal {
    public static void main(String[] args) throws Exception {
        List<DiscoverySelector> selectors = List.of(
            "LibroServicioTest", "EjemplarServicioTest", "ValidacionSolicitudesTest",
            "LibroEjemplarRelacionTest", "ApiPilotoTest", "ModeloMetadatosTest", "GuardiaBasePruebasTest"
        ).stream().<DiscoverySelector>map(name -> DiscoverySelectors.selectClass("mx.edu.itspa.biblioteca." + name)).toList();
        ConfigurationParameters config = new ConfigurationParameters() {
            public Optional<String> get(String key) { return Optional.empty(); }
            public Optional<Boolean> getBoolean(String key) { return Optional.empty(); }
            public int size() { return 0; }
            public Set<String> keySet() { return Set.of(); }
        };
        OutputDirectoryProvider output = new OutputDirectoryProvider() {
            public Path getRootDirectory() { return Path.of("target/junit-local-output").toAbsolutePath(); }
            public Path createOutputDirectory(TestDescriptor descriptor) throws java.io.IOException {
                return Files.createDirectories(getRootDirectory().resolve(Integer.toHexString(descriptor.getUniqueId().hashCode())));
            }
        };
        EngineDiscoveryRequest request = new EngineDiscoveryRequest() {
            public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> type) {
                return selectors.stream().filter(type::isInstance).map(type::cast).toList();
            }
            public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> type) { return List.of(); }
            public ConfigurationParameters getConfigurationParameters() { return config; }
            public OutputDirectoryProvider getOutputDirectoryProvider() { return output; }
        };
        JupiterTestEngine engine = new JupiterTestEngine();
        TestDescriptor root = engine.discover(request, UniqueId.forEngine(engine.getId()));
        List<String> lines = new ArrayList<>();
        Map<String, int[]> counts = new LinkedHashMap<>();
        int[] failedContainers = {0};
        lines.add("ITSPA - Ejecucion directa de JUnit Jupiter (sin Maven Surefire)");
        lines.add("Fecha: " + OffsetDateTime.now());
        lines.add("JDK: " + System.getProperty("java.version") + "; JUnit: " + engine.getVersion().orElse("desconocida"));
        lines.add("Seleccion explicita: 7 clases sin BD; ModeloPersistenciaIntegracionTest excluida.");
        EngineExecutionListener listener = new EngineExecutionListener() {
            private String className(TestDescriptor d) {
                return d.getUniqueId().getSegments().stream().filter(s -> s.getType().equals("class"))
                    .map(UniqueId.Segment::getValue).findFirst().orElse("contenedor");
            }
            public void executionSkipped(TestDescriptor d, String reason) {
                if (d.isTest()) counts.computeIfAbsent(className(d), k -> new int[4])[3]++;
                lines.add("SKIP " + d.getUniqueId() + " " + reason);
            }
            public void executionFinished(TestDescriptor d, TestExecutionResult result) {
                if (d.isTest()) {
                    int[] stats = counts.computeIfAbsent(className(d), k -> new int[4]);
                    stats[0]++;
                    stats[result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? 1 : 2]++;
                    lines.add(result.getStatus() + " " + className(d) + " :: " + d.getDisplayName());
                } else if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                    failedContainers[0]++;
                    lines.add("CONTAINER " + result.getStatus() + " " + d.getUniqueId());
                }
                result.getThrowable().ifPresent(error -> {
                    lines.add(error.toString());
                    error.printStackTrace(System.err);
                });
            }
        };
        engine.execute(ExecutionRequest.create(root, listener, config, output));
        lines.add("\nResumen por clase: ejecutadas / correctas / fallidas-abortadas / omitidas");
        counts.forEach((name, stats) -> lines.add(name + " : " + stats[0] + " / " + stats[1] + " / " + stats[2] + " / " + stats[3]));
        int total = counts.values().stream().mapToInt(s -> s[0]).sum();
        int passed = counts.values().stream().mapToInt(s -> s[1]).sum();
        int failed = counts.values().stream().mapToInt(s -> s[2]).sum();
        int skipped = counts.values().stream().mapToInt(s -> s[3]).sum();
        lines.add("TOTAL ejecutadas=" + total + ", correctas=" + passed + ", fallidas-abortadas=" + failed
            + ", omitidas=" + skipped + ", contenedores fallidos=" + failedContainers[0]);
        Path report = Path.of("target/resultado-junit-sin-bd-local.txt");
        Files.write(report, lines);
        lines.forEach(System.out::println);
        if (failed > 0 || skipped > 0 || failedContainers[0] > 0 || counts.size() != 7 || total == 0) System.exit(1);
    }
}
