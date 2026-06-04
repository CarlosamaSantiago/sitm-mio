package edu.icesi.sitmmio.ingestiongateway.service;

import java.util.concurrent.atomic.LongAdder;

/** R35.2: log estructurado de rechazos + contador por razón. */
public final class RejectAuditor {
    private final LongAdder total = new LongAdder();

    public void audit(SITM.Datagram d, String reason) {
        total.increment();
        System.out.println("[ingestion-gateway] REJECT busId=" + d.busId
                + " lineId=" + d.lineId + " reason=" + reason);
    }

    public long totalRejected() { return total.sum(); }
}
