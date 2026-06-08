package edu.icesi.sitmmio.ccoclient.rtstreamclient;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.ccoclient.alertpanel.AlertPanel;
import edu.icesi.sitmmio.ccoclient.mapview.MapView;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Subscriber del DatagramEventBus. Cachea velocidades por (lineId, year, month).
 *
 * yearMonth se infiere del timestamp del BusUpdate. Si el timestamp no parsea,
 * cae al fallback (yearFallback, monthFallback) que viene de env vars del Main.
 */
public class MonitoringSubscriberI implements SITM.MonitoringSubscriber {

    private final MapView mapView;
    private final AlertPanel alertPanel;
    private final SITM.ReportProviderPrx reports;
    private final int yearFallback;
    private final int monthFallback;

    /** clave (lineId, year, month) → velocidad cacheada (null si NO_DATA). */
    private final Map<SpeedKey, Optional<Double>> speedCache = new ConcurrentHashMap<>();
    private final Map<Integer, double[]> lastPosition = new ConcurrentHashMap<>();
    private final AtomicLong updateCount = new AtomicLong();
    private final AtomicLong alertCount = new AtomicLong();
    private final AtomicLong speedHits = new AtomicLong();

    private static final DateTimeFormatter[] TIMESTAMP_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    public MonitoringSubscriberI(MapView mapView, AlertPanel alertPanel,
                                 SITM.ReportProviderPrx reports,
                                 int yearFallback, int monthFallback) {
        this.mapView = mapView;
        this.alertPanel = alertPanel;
        this.reports = reports;
        this.yearFallback = yearFallback;
        this.monthFallback = monthFallback;
    }

    public void invalidateSpeedCache() {
        speedCache.clear();
        System.out.println("[cco-client] cache de velocidades invalidado");
    }

    @Override
    public void updateLocation(SITM.BusUpdate u, Current current) {
        long n = updateCount.incrementAndGet();
        lastPosition.put(u.busId, new double[]{ u.pos.latitude, u.pos.longitude });

        YearMonth ym = inferYearMonth(u.timestamp);
        Double avgSpeed = lookupSpeed(u.lineId, ym);

        mapView.updateBus(u.busId, u.pos.latitude, u.pos.longitude,
                u.lineId, u.timestamp, u.operationalState, avgSpeed);

        if (n <= 5 || n % 500 == 0) {
            String speedStr = avgSpeed != null
                    ? String.format("%.2f km/h", avgSpeed)
                    : "(no calculada)";
            System.out.printf("[cco-client] update #%d bus=%d ruta=%d ym=%s pos=(%.5f, %.5f) estado=%s v_ruta=%s%n",
                    n, u.busId, u.lineId, ym, u.pos.latitude, u.pos.longitude,
                    u.operationalState, speedStr);
        }
    }

    private YearMonth inferYearMonth(String timestamp) {
        if (timestamp != null && !timestamp.isEmpty()) {
            for (DateTimeFormatter fmt : TIMESTAMP_FORMATS) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(timestamp, fmt);
                    return YearMonth.of(dt.getYear(), dt.getMonthValue());
                } catch (DateTimeParseException ignored) {}
            }
        }
        return YearMonth.of(yearFallback, monthFallback);
    }

    private Double lookupSpeed(int lineId, YearMonth ym) {
        if (reports == null) return null;
        SpeedKey key = new SpeedKey(lineId, ym.getYear(), ym.getMonthValue());
        Optional<Double> cached = speedCache.get(key);
        if (cached != null) {
            if (cached.isPresent()) speedHits.incrementAndGet();
            return cached.orElse(null);
        }
        try {
            SITM.SpeedReport rpt = reports.getAverageSpeed(key.lineId, key.year, key.month);
            if (rpt != null && "OK".equals(rpt.status)) {
                speedCache.put(key, Optional.of(rpt.averageSpeedKmH));
                speedHits.incrementAndGet();
                return rpt.averageSpeedKmH;
            }
            speedCache.put(key, Optional.empty());
            return null;
        } catch (SITM.NoDataForPartition e) {
            speedCache.put(key, Optional.empty());
            return null;
        } catch (Exception e) {
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
        System.out.printf("[cco-client] ALERTA #%d bus=%d ruta=%d prio=%s%n",
                n, a.busId, a.lineId, a.priority);
    }

    public long updateCount()  { return updateCount.get(); }
    public long alertCount()   { return alertCount.get(); }
    public long speedHits()    { return speedHits.get(); }
    public int  cachedSpeeds() {
        return (int) speedCache.values().stream().filter(Optional::isPresent).count();
    }

    /** Clave inmutable (lineId, year, month) para el cache. */
    private static final class SpeedKey {
        final int lineId, year, month;
        SpeedKey(int lineId, int year, int month) {
            this.lineId = lineId; this.year = year; this.month = month;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof SpeedKey k)) return false;
            return lineId == k.lineId && year == k.year && month == k.month;
        }
        @Override public int hashCode() { return Objects.hash(lineId, year, month); }
    }
}
