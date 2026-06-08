package edu.icesi.sitmmio.citizencli;

import edu.icesi.sitmmio.citizencli.commands.Commands;
import edu.icesi.sitmmio.citizencli.io.ApiClient;

public class Main {
    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("SITMMIO_API", "http://127.0.0.1:8080");
        ApiClient api = new ApiClient(base, 5000);
        Commands cmd = new Commands(api);

        if (args.length == 0) { usage(); System.exit(2); }
        String op = args[0];
        java.util.Map<String, String> kv = parseFlags(args);

        int rc = switch (op) {
            case "login" -> cmd.login(kv.get("user"), kv.get("password"));
            case "speed" -> cmd.speed(
                    Integer.parseInt(kv.get("lineId")),
                    Integer.parseInt(kv.get("year")),
                    Integer.parseInt(kv.get("month")));
            case "speeds" -> cmd.speeds(
                    Integer.parseInt(kv.get("year")),
                    Integer.parseInt(kv.get("month")),
                    kv.getOrDefault("format", "json"));
            case "range" -> cmd.range(
                    Integer.parseInt(kv.get("yf")),
                    Integer.parseInt(kv.get("mf")),
                    Integer.parseInt(kv.get("yt")),
                    Integer.parseInt(kv.get("mt")));
            case "health" -> cmd.health();
            case "openapi" -> cmd.openapi(kv.getOrDefault("out", "openapi.yaml"));
            default -> { usage(); yield 2; }
        };
        System.exit(rc);
    }

    private static java.util.Map<String, String> parseFlags(String[] args) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) m.put(args[i].substring(2), args[i + 1]);
        }
        return m;
    }

    private static void usage() {
        System.out.println("citizen-cli {login|speed|speeds|range|health|openapi} [--flags]");
    }
}
