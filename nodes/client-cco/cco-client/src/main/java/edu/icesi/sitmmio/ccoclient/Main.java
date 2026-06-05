package edu.icesi.sitmmio.ccoclient;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.ccoclient.alertpanel.AlertPanel;
import edu.icesi.sitmmio.ccoclient.mapview.MapView;
import edu.icesi.sitmmio.ccoclient.rtstreamclient.MonitoringSubscriberI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * CCO Client (JavaFX + Leaflet). Suscriptor al DatagramEventBus.
 *
 * Configuración (orden de precedencia):
 *   1. Argumentos --event-bus-host, --event-bus-port, --reports-host, --reports-port,
 *      --year-fallback, --month-fallback
 *   2. Variables de entorno SITM_EVENT_BUS_HOST, SITM_EVENT_BUS_PORT,
 *      SITM_REPORTS_HOST, SITM_REPORTS_PORT, SITM_R7_YEAR, SITM_R7_MONTH
 *   3. Defaults (127.0.0.1 / puertos estándar / 2019 / 5)
 *
 * year/month son SOLO fallback. Lo normal es que el yearMonth se infiera del
 * timestamp del BusUpdate en MonitoringSubscriberI.
 */
public class Main extends Application {

    private Communicator communicator;
    private MonitoringSubscriberI servant;

    private String eventBusHost;
    private int    eventBusPort;
    private String reportsHost;
    private int    reportsPort;
    private int    yearFallback;
    private int    monthFallback;

    @Override
    public void start(Stage stage) {
        eventBusHost = pick("event-bus-host", "SITM_EVENT_BUS_HOST", "127.0.0.1");
        eventBusPort = Integer.parseInt(pick("event-bus-port", "SITM_EVENT_BUS_PORT", "10020"));
        reportsHost  = pick("reports-host",  "SITM_REPORTS_HOST",  "127.0.0.1");
        reportsPort  = Integer.parseInt(pick("reports-port",  "SITM_REPORTS_PORT",  "10060"));
        yearFallback = Integer.parseInt(pick("year-fallback", "SITM_R7_YEAR",  "2019"));
        monthFallback= Integer.parseInt(pick("month-fallback","SITM_R7_MONTH", "5"));

        System.out.println("[cco-client] JavaFX iniciando...");
        System.out.printf ("[cco-client] event-bus = %s:%d%n", eventBusHost, eventBusPort);
        System.out.printf ("[cco-client] reports   = %s:%d%n", reportsHost, reportsPort);
        System.out.printf ("[cco-client] fallback yearMonth = %04d-%02d%n", yearFallback, monthFallback);

        WebView webView = new WebView();
        webView.getEngine().setOnError(e -> System.err.println("[cco-client] WebEngine error: " + e.getMessage()));
        webView.getEngine().load(getClass().getResource("/map.html").toExternalForm());
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldS, newS) ->
            System.out.println("[cco-client] WebEngine state: " + newS));

        stage.setTitle("SITM-MIO CCO — " + eventBusHost);
        stage.setScene(new Scene(webView, 1200, 800));
        stage.show();

        MapView mapView = new MapView(webView.getEngine());
        AlertPanel alerts = new AlertPanel();

        new Thread(() -> initIce(mapView, alerts), "ice-init").start();
        new Thread(this::heartbeat, "heartbeat").start();
    }

    /** arg → env var → default. */
    private String pick(String argKey, String envKey, String def) {
        var params = getParameters().getNamed();
        if (params.containsKey(argKey)) return params.get(argKey);
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) return env;
        return def;
    }

    private void initIce(MapView mapView, AlertPanel alerts) {
        try {
            System.out.println("[cco-client] Inicializando Ice...");
            communicator = Util.initialize(new String[]{});

            String busProxy = "DatagramEventBus:default -h " + eventBusHost + " -p " + eventBusPort;
            System.out.println("[cco-client] Resolviendo bus: " + busProxy);
            ObjectPrx base = communicator.stringToProxy(busProxy);
            SITM.DatagramEventBusPrx bus = SITM.DatagramEventBusPrx.checkedCast(base);
            if (bus == null) {
                System.err.println("[cco-client] DatagramEventBus proxy invalido en " + eventBusHost);
                return;
            }

            SITM.ReportProviderPrx reports = null;
            try {
                ObjectPrx rb = communicator.stringToProxy(
                    "ReportProvider:default -h " + reportsHost + " -p " + reportsPort);
                reports = SITM.ReportProviderPrx.checkedCast(rb);
                if (reports != null) {
                    System.out.println("[cco-client] ReportProvider conectado en " + reportsHost);
                }
            } catch (Exception e) {
                System.out.println("[cco-client] ReportProvider no disponible — popups sin velocidad R7");
            }

            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "CcoCallbackAdapter", "default");
            servant = new MonitoringSubscriberI(mapView, alerts, reports,
                                                yearFallback, monthFallback);
            ObjectPrx proxy = adapter.add(servant, new Identity("cco-callback", ""));
            adapter.activate();
            bus.subscribe(SITM.MonitoringSubscriberPrx.uncheckedCast(proxy), 0);
            System.out.println("[cco-client] Suscrito al DatagramEventBus (zoneId=0)");

            if (reports != null) {
                Thread refresher = new Thread(() -> {
                    while (true) {
                        try { Thread.sleep(30_000); } catch (InterruptedException e) { return; }
                        if (servant != null && servant.cachedSpeeds() == 0) servant.invalidateSpeedCache();
                    }
                }, "speed-refresher");
                refresher.setDaemon(true);
                refresher.start();
            }

            communicator.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[cco-client] ICE error: " + e);
            e.printStackTrace();
        }
    }

    private void heartbeat() {
        while (true) {
            try { Thread.sleep(10_000); } catch (InterruptedException e) { return; }
            if (servant != null) {
                System.out.printf("[cco-client] heartbeat updates=%d alertas=%d velocidades_cacheadas=%d%n",
                        servant.updateCount(), servant.alertCount(), servant.cachedSpeeds());
            }
        }
    }

    @Override
    public void stop() { if (communicator != null) communicator.destroy(); }

    public static void main(String[] args) { launch(args); }
}
