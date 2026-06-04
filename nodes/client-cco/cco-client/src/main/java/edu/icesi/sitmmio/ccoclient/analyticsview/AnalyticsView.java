package edu.icesi.sitmmio.ccoclient.analyticsview;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.util.Locale;

public final class AnalyticsView {
    private final WebEngine engine;
    private final SITM.ReportProviderPrx reports;

    public AnalyticsView(WebEngine engine, SITM.ReportProviderPrx reports) {
        this.engine = engine; this.reports = reports;
    }

    public void loadRange(int yf, int mf, int yt, int mt) {
        if (reports == null) return;
        Platform.runLater(() -> engine.executeScript("clearReports()"));
        SITM.SpeedReport[] all = reports.getRangeReports(yf, mf, yt, mt);
        for (SITM.SpeedReport r : all) {
            String script = String.format(Locale.US,
                    "loadSpeedReport(%d, '%d-%02d', %f, '')",
                    r.lineId, r.year, r.month, r.averageSpeedKmH);
            Platform.runLater(() -> engine.executeScript(script));
        }
    }
}
