package edu.icesi.sitmmio.ccoclient.rtstreamclient;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.ccoclient.alertpanel.AlertPanel;
import edu.icesi.sitmmio.ccoclient.mapview.MapView;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MonitoringSubscriberI implements SITM.MonitoringSubscriber {

    private final MapView mapView;
    private final AlertPanel alertPanel;
    private final SITM.ReportProviderPrx reports;   // puede ser null si no disponible
    private final int year;
    private final int month;

    private final Map<Integer, double[]> lastPosition = new ConcurrentHashMap<>();
    // speedCache: lineId → Optional<velocidad>. Empty = consultado y NO había data.
    private final Map<Integer, Optional<Double>> speedCache = new ConcurrentHashMap<>();
    private final AtomicLong updateCount = new AtomicLong();
    private final AtomicLong alertCount = new AtomicLong();
    private final AtomicLong speedHits = new AtomicLong();

    public MonitoringSubscriberI(MapView mapView, AlertPanel alertPanel,
                                 SITM.ReportProviderPrx reports, int year, int month) {
        this.mapView = mapView; this.alertPanel = alertPanel;
        this.reports = reports; this.year = year; this.month = month;
    }

    /** Limpiar cache de velocidades — útil tras correr R7 nuevo. */
    public void invalidateSpeedCache() {
        speedCache.clear();
        System.out.println("[cco-client] cache de velocidades invalidado");
    }

    @Override
    public void updateLocation(SITM.BusUpdate u, Current current) {
        long n = updateCount.incrementAndGet();
        lastPosition.put(u.busId, new double[]{ u.pos.latitude, u.pos.longitude });

        Double avgSpeed = lookupSpeed(u.lineId);

        mapView.updateBus(u.busId, u.pos.latitude, u.pos.longitude,
                u.lineId, u.timestamp, u.operationalState, avgSpeed);

        if (n <= 5 || n % 500 == 0) {
            String speedStr = avgSpeed != null
                    ? String.format("%.2f km/h", avgSpeed)
                    : "(no calculada)";
            System.out.printf("[cco-client] update #%d  bus=%d  ruta=%d  pos=(%.5f, %.5f)  estado=%s  v_ruta=%s%n",
                    n, u.busId, u.lineId, u.pos.latitude, u.pos.longitude, u.operationalState, speedStr);
        }
    }

    /**
     * Busca la velocidad promedio de una ruta. Cachea el resultado para evitar
     * consultas repetidas. Si no hay ReportProvider o no hay data → null.
     */
    private Double lookupSpeed(int lineId) {
        if (reports == null) return null;
        Optional<Double> cached = speedCache.get(lineId);
        if (cached != null) {
            if (cached.isPresent()) speedHits.incrementAndGet();
            return cached.orElse(null);
        }
        // Primera vez para este lineId — consultar (sync porque es rápido si está cacheado serverside)
        try {
            SITM.SpeedReport rpt = reports.getAverageSpeed(lineId, year, month);
            if (rpt != null && "OK".equals(rpt.status)) {
                speedCache.put(lineId, Optional.of(rpt.averageSpeedKmH));
                speedHits.incrementAndGet();
                return rpt.averageSpeedKmH;
            }
            speedCache.put(lineId, Optional.empty());
            return null;
        } catch (SITM.NoDataForPartition e) {
            speedCache.put(lineId, Optional.empty());
            return null;
        } catch (Exception e) {
            // No cachear errores transitorios — reintentar próximamente
            return null;
        }
    }

    @Override
    public void updateLocations(SITM.BusUpdate[] us, Current current) {
        System.out.println("[cco-client] updateLocations batch=" + us.length);
        for (SITM.BusUpdate u : us) updateLocation(u, current);
    }

    @Override
    public void onCriticAlert(SITM.CriticAlert a, Current current) {
        long n = alertCount.incrementAndGet();
        alertPanel.show(a);
        double[] pos = lastPosition.getOrDefault(a.busId, new double[]{3.42, -76.52});
        mapView.showAlert(a.busId, a.lineId, a.priority, a.description, pos[0], pos[1]);
        System.out.printf("[cco-client] ALERTA #%d  bus=%d  ruta=%d  prio=%s%n",
                n, a.busId, a.lineId, a.priority);
    }

    public long updateCount()  { return updateCount.get(); }
    public long alertCount()   { return alertCount.get(); }
    public long speedHits()    { return speedHits.get(); }
    public int  cachedSpeeds() { return (int) speedCache.values().stream().filter(Optional::isPresent).count(); }
}
