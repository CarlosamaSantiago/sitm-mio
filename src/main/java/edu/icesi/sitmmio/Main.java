package edu.icesi.sitmmio;

import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.ResultCsvWriter;
import edu.icesi.sitmmio.io.RouteCsvReader;
import edu.icesi.sitmmio.service.MetricsCollector;
import edu.icesi.sitmmio.service.MonolithicSpeedCalculator;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Path DEFAULT_ROUTES = Path.of("data", "raw", "lines-241-ActiveGT.csv");
    private static final Path DEFAULT_DATAGRAMS = Path.of("data", "raw", "datagrams-MiniPilot.csv");
    private static final Path DEFAULT_RESULTS = Path.of("data", "output", "monolith-results.csv");
    private static final Path DEFAULT_METRICS = Path.of("data", "output", "experiment-results.csv");

    public static void main(String[] args) throws Exception {
        Path routesPath = args.length > 0 ? Path.of(args[0]) : DEFAULT_ROUTES;
        Path datagramsPath = args.length > 1 ? Path.of(args[1]) : DEFAULT_DATAGRAMS;
        Path resultsPath = args.length > 2 ? Path.of(args[2]) : DEFAULT_RESULTS;
        Path metricsPath = args.length > 3 ? Path.of(args[3]) : DEFAULT_METRICS;

        System.out.println("SITM-MIO V1 monolith");
        System.out.println("Routes: " + routesPath);
        System.out.println("Datagrams: " + datagramsPath);

        RouteCsvReader routeCsvReader = new RouteCsvReader();
        Map<Integer, Route> activeRoutes = routeCsvReader.readActiveRoutes(routesPath);
        System.out.println("Loaded active routes: " + activeRoutes.size());

        MonolithicSpeedCalculator calculator = new MonolithicSpeedCalculator();
        MetricsCollector metrics = new MetricsCollector("monolith", datagramsPath.getFileName().toString());
        metrics.start();
        List<SpeedResult> results = calculator.calculate(datagramsPath, activeRoutes, metrics);
        metrics.finish(results.size());

        ResultCsvWriter writer = new ResultCsvWriter();
        writer.writeSpeedResults(resultsPath, results);
        writer.writeMetrics(metricsPath, metrics.snapshot());

        System.out.println("Results written to: " + resultsPath);
        System.out.println("Metrics written to: " + metricsPath);
        System.out.println(metrics.summary());
    }
}
