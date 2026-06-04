package edu.icesi.sitmmio.ccoclient.mapview;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.util.Locale;

public final class MapView {
    private final WebEngine engine;
    public MapView(WebEngine engine) { this.engine = engine; }

    /**
     * Actualiza un bus en el mapa.
     * avgSpeed: velocidad promedio de la ruta del bus (km/h) si está calculada (R7),
     *           o null si aún no se ha calculado.
     */
    public void updateBus(int busId, double lat, double lng, int lineId,
                          String time, String state, Double avgSpeed) {
        String speedJs = avgSpeed != null
                ? String.format(Locale.US, "%.4f", avgSpeed)
                : "null";
        String s = String.format(Locale.US,
                "updateBus(%d, %f, %f, %d, '%s', '%s', %s)",
                busId, lat, lng, lineId, escape(time), escape(state), speedJs);
        Platform.runLater(() -> engine.executeScript(s));
    }

    public void showAlert(int busId, int lineId, String priority, String description, double lat, double lng) {
        String s = String.format(Locale.US,
                "showAlert(%d, %d, '%s', '%s', %f, %f)",
                busId, lineId, escape(priority), escape(description), lat, lng);
        Platform.runLater(() -> engine.executeScript(s));
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
