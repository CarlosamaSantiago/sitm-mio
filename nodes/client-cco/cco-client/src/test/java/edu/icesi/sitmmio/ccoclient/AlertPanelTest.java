package edu.icesi.sitmmio.ccoclient;

import edu.icesi.sitmmio.ccoclient.alertpanel.AlertPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertPanelTest {
    @Test
    void historyCappedAt50() {
        AlertPanel p = new AlertPanel();
        for (int i = 0; i < 60; i++) {
            SITM.CriticAlert a = new SITM.CriticAlert();
            a.busId = i; a.lineId = 131; a.priority = "ALTA";
            a.timestamp = "2019-05-27 10:00:00"; a.description = "test";
            p.show(a);
        }
        assertEquals(60, p.receivedTotal());
        assertEquals(50, p.historySize());
    }
}
