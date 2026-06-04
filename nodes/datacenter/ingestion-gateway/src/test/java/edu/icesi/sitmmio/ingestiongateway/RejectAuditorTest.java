package edu.icesi.sitmmio.ingestiongateway;

import edu.icesi.sitmmio.ingestiongateway.service.RejectAuditor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RejectAuditorTest {
    @Test
    void countsAcrossCalls() {
        RejectAuditor a = new RejectAuditor();
        SITM.Datagram d = new SITM.Datagram();
        d.busId = 1; d.lineId = 99999;
        a.audit(d, "LINEID_NOT_ACTIVE");
        a.audit(d, "LINEID_NOT_ACTIVE");
        a.audit(d, "LAT_MINUS_ONE");
        assertEquals(3, a.totalRejected());
    }
}
