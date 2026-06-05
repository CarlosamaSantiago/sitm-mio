package edu.icesi.sitmmio.ccoclient.rtstreamclient;

import edu.icesi.sitmmio.ccoclient.alertpanel.AlertPanel;
import edu.icesi.sitmmio.ccoclient.mapview.MapView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringSubscriberITest {

    @Mock
    private MapView mapView;

    @Mock
    private AlertPanel alertPanel;

    @Mock
    private SITM.ReportProviderPrx reports;

    @Test
    void testInferYearMonthFromTimestamp() {
        MonitoringSubscriberI subscriber = new MonitoringSubscriberI(mapView, alertPanel, reports, 2019, 5);

        SITM.BusUpdate u = new SITM.BusUpdate();
        u.busId = 1069;
        u.lineId = 131;
        u.pos = new SITM.Location(3.4, -76.5);
        u.operationalState = "EN_RUTA";
        
        // Test with a 2018 timestamp
        u.timestamp = "2018-08-15 10:00:00";
        subscriber.updateLocation(u, null);
        
        // Verify that lookupSpeed (indirectly via report provider) was called with 2018-08
        // Note: we can't easily verify the private map, but we can verify the reports call if it's not cached
        try {
            verify(reports).getAverageSpeed(eq(131), eq(2018), eq(8));
        } catch (SITM.NoDataForPartition e) {
            // ignored
        }
    }

    @Test
    void testFallbackYearMonth() {
        MonitoringSubscriberI subscriber = new MonitoringSubscriberI(mapView, alertPanel, reports, 2018, 1);

        SITM.BusUpdate u = new SITM.BusUpdate();
        u.busId = 1069;
        u.lineId = 131;
        u.pos = new SITM.Location(3.4, -76.5);
        u.operationalState = "EN_RUTA";
        
        // Test with invalid timestamp
        u.timestamp = "invalid-date";
        subscriber.updateLocation(u, null);
        
        try {
            // Should use fallback 2018-01
            verify(reports).getAverageSpeed(eq(131), eq(2018), eq(1));
        } catch (SITM.NoDataForPartition e) {
            // ignored
        }
    }
}
