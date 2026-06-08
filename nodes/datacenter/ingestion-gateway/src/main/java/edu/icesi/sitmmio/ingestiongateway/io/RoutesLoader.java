package edu.icesi.sitmmio.ingestiongateway.io;

import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.io.RouteCsvReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class RoutesLoader {
    public static Map<Integer, Route> load(Path routesCsv) throws IOException {
        return new RouteCsvReader().readActiveRoutes(routesCsv);
    }
}
