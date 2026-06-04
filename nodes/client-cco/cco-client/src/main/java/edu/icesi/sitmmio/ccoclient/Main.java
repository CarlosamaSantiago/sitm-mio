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

public class Main extends Application {

    private Communicator communicator;
    private MonitoringSubscriberI servant;

    // Año/mes para consulta de velocidades (matchea el del MiniPilot)
    private static final int YEAR_R7  = 2019;
    private static final int MONTH_R7 = 5;

    @Override
    public void start(Stage stage) {
        System.out.println("[cco-client] JavaFX iniciando...");
        WebView webView = new WebView();
        webView.getEngine().setOnError(e -> System.err.println("[cco-client] WebEngine error: " + e.getMessage()));
        webView.getEngine().load(getClass().getResource("/map.html").toExternalForm());
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldS, newS) -> {
            System.out.println("[cco-client] WebEngine state: " + newS);
        });

        stage.setTitle("SITM-MIO CCO — RT + Analytics");
        stage.setScene(new Scene(webView, 1200, 800));
        stage.show();
        System.out.println("[cco-client] Ventana JavaFX visible");

        MapView mapView = new MapView(webView.getEngine());
        AlertPanel alerts = new AlertPanel();

        new Thread(() -> initIce(mapView, alerts), "ice-init").start();
        new Thread(this::heartbeat, "heartbeat").start();
    }

    private void initIce(MapView mapView, AlertPanel alerts) {
        try {
            System.out.println("[cco-client] Inicializando Ice...");
            communicator = Util.initialize(new String[]{});

            // 1. Conectar al EventBus (obligatorio)
            String busProxy = "DatagramEventBus:default -h 127.0.0.1 -p 10020";
            System.out.println("[cco-client] Resolviendo proxy: " + busProxy);
            ObjectPrx base = communicator.stringToProxy(busProxy);
            SITM.DatagramEventBusPrx bus = SITM.DatagramEventBusPrx.checkedCast(base);
            if (bus == null) {
                System.err.println("[cco-client] ❌ DatagramEventBus proxy inválido");
                return;
            }

            // 2. Conectar al ReportProvider (opcional — sirve para popups con velocidad)
            SITM.ReportProviderPrx reports = tryReports("ReportProvider:default -h 127.0.0.1 -p 10060");
            if (reports == null) {
                System.out.println("[cco-client] ⚠  ReportProvider no disponible — popups sin velocidad de ruta");
            } else {
                System.out.println("[cco-client] ✅ ReportProvider OK — popups mostrarán velocidad de ruta (R7) cuando esté calculada");
            }

            // 3. Crear servant + suscribirse
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "CcoCallbackAdapter", "default -h 127.0.0.1");
            servant = new MonitoringSubscriberI(mapView, alerts, reports, YEAR_R7, MONTH_R7);
            ObjectPrx proxy = adapter.add(servant, new Identity("cco-callback", ""));
            adapter.activate();
            bus.subscribe(SITM.MonitoringSubscriberPrx.uncheckedCast(proxy), 0);
            System.out.println("[cco-client] ✅ Suscrito al DatagramEventBus (zoneId=0). Esperando updates...");

            // 4. Refresco periódico del cache de velocidades (cada 30s)
            //    Esto detecta cuando R7 se ejecuta tras el arranque del cco-client.
            if (reports != null) {
                Thread refresher = new Thread(() -> {
                    while (true) {
                        try { Thread.sleep(30_000); } catch (InterruptedException e) { return; }
                        if (servant != null && servant.cachedSpeeds() == 0) {
                            servant.invalidateSpeedCache();
                            System.out.println("[cco-client] refresh: aún no hay velocidades calculadas, reintentando...");
                        }
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

    private SITM.ReportProviderPrx tryReports(String proxy) {
        try {
            ObjectPrx p = communicator.stringToProxy(proxy);
            return SITM.ReportProviderPrx.checkedCast(p);
        } catch (Exception e) { return null; }
    }

    private void heartbeat() {
        while (true) {
            try { Thread.sleep(10_000); } catch (InterruptedException e) { return; }
            if (servant != null) {
                System.out.printf("[cco-client] heartbeat — updates=%d  alertas=%d  velocidades_cacheadas=%d/%d hits%n",
                        servant.updateCount(), servant.alertCount(),
                        servant.cachedSpeeds(), servant.speedHits());
            }
        }
    }

    @Override
    public void stop() { if (communicator != null) communicator.destroy(); }

    public static void main(String[] args) { launch(args); }
}
