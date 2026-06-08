package edu.icesi.sitmmio;

import edu.icesi.sitmmio.concurrent.ConcurrentCalculationResult;
import edu.icesi.sitmmio.concurrent.ConcurrentMetricsWriter;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.io.ResultCsvWriter;
import edu.icesi.sitmmio.io.RouteCsvReader;
import edu.icesi.sitmmio.service.ConcurrentSpeedCalculator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class MainV2 {
    private static final Path DEFAULT_ROUTES = Path.of("data", "raw", "lines-241-ActiveGT.csv");
    private static final Path DEFAULT_DATAGRAMS_MINI = Path.of("data", "raw", "datagrams-MiniPilot.csv");
    private static final Path DEFAULT_DATAGRAMS_PILOT = Path.of("data", "raw", "datagrams4Pilot.csv");
    private static final Path DEFAULT_RESULTS = Path.of("data", "output", "concurrent-results.csv");
    private static final Path DEFAULT_METRICS = Path.of("data", "output", "experiment-results.csv");

    private MainV2() {
    }

    public static void main(String[] args) throws Exception {
        Cli cli = Cli.parse(args);
        Path datagramsPath = cli.datagramsPath;
        if (datagramsPath == null) {
            datagramsPath = Files.exists(DEFAULT_DATAGRAMS_PILOT)
                    ? DEFAULT_DATAGRAMS_PILOT
                    : DEFAULT_DATAGRAMS_MINI;
        }

        System.out.println("SITM-MIO V2 concurrent");
        System.out.println("Routes: " + cli.routesPath);
        System.out.println("Datagrams: " + datagramsPath);
        System.out.println("Workers: " + cli.workers);

        Map<Integer, Route> activeRoutes = new RouteCsvReader().readActiveRoutes(cli.routesPath);
        System.out.println("Loaded active routes: " + activeRoutes.size());

        ConcurrentSpeedCalculator calculator = new ConcurrentSpeedCalculator(cli.workers);
        ConcurrentCalculationResult calculation = calculator.calculate(datagramsPath, activeRoutes);

        new ResultCsvWriter().writeSpeedResults(cli.outputPath, calculation.results());
        new ConcurrentMetricsWriter().write(cli.metricsPath, calculation.metrics());

        System.out.println("Results written to: " + cli.outputPath);
        System.out.println("Metrics written to: " + cli.metricsPath);
        System.out.println(calculation.metrics().summary());
    }

    private static final class Cli {
        private final Path routesPath;
        private final Path datagramsPath;
        private final Path outputPath;
        private final Path metricsPath;
        private final int workers;

        private Cli(Path routesPath, Path datagramsPath, Path outputPath, Path metricsPath, int workers) {
            this.routesPath = routesPath;
            this.datagramsPath = datagramsPath;
            this.outputPath = outputPath;
            this.metricsPath = metricsPath;
            this.workers = workers;
        }

        private static Cli parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--version".equals(arg)) {
                    i++;
                } else if (arg.startsWith("--") && i + 1 < args.length) {
                    values.put(arg, args[++i]);
                }
            }

            Path routes = Path.of(values.getOrDefault("--routes", DEFAULT_ROUTES.toString()));
            Path datagrams = values.containsKey("--datagrams") ? Path.of(values.get("--datagrams")) : null;
            Path output = Path.of(values.getOrDefault("--output", DEFAULT_RESULTS.toString()));
            Path metrics = Path.of(values.getOrDefault("--metrics", DEFAULT_METRICS.toString()));
            int workers = Integer.parseInt(values.getOrDefault(
                    "--workers",
                    Integer.toString(Runtime.getRuntime().availableProcessors())
            ));
            return new Cli(routes, datagrams, output, metrics, workers);
        }
    }
}
