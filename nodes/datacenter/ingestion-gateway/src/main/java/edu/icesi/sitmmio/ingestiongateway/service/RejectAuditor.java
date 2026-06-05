package edu.icesi.sitmmio.ingestiongateway.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/** R35.2: log estructurado de rechazos + contador por razón. */
public final class RejectAuditor {
    private static final long VERBOSE_FIRST_REJECTS = 20L;
    private static final long SUMMARY_INTERVAL = 100_000L;

    private final LongAdder total = new LongAdder();
    private final ConcurrentMap<String, LongAdder> byReason = new ConcurrentHashMap<>();

    public void audit(SITM.Datagram d, String reason) {
        total.increment();
        byReason.computeIfAbsent(reason, ignored -> new LongAdder()).increment();
        long rejected = total.sum();
        if (rejected <= VERBOSE_FIRST_REJECTS) {
            System.out.println("[ingestion-gateway] REJECT busId=" + d.busId
                    + " lineId=" + d.lineId + " reason=" + reason);
        } else if (rejected % SUMMARY_INTERVAL == 0L) {
            System.out.println("[ingestion-gateway] rejectedTotal=" + rejected
                    + " byReason=" + summary());
        }
    }

    public long totalRejected() { return total.sum(); }

    private String summary() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : byReason.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(entry.getValue().sum());
            first = false;
        }
        return sb.append("}").toString();
    }
}
