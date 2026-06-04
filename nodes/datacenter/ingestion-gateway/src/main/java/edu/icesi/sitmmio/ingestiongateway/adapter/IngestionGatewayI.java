package edu.icesi.sitmmio.ingestiongateway.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;
import edu.icesi.sitmmio.ingestiongateway.service.RejectAuditor;
import edu.icesi.sitmmio.validation.DatagramValidator;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class IngestionGatewayI implements SITM.DatagramReceiver {

    private final DatagramValidator validator;
    private final Map<Integer, Route> activeRoutes;
    private final RejectAuditor auditor;
    private final SITM.DatagramQueuePrx queue;
    private final SITM.ArchiveServicePrx archive;
    private final LongAdder accepted = new LongAdder();

    public IngestionGatewayI(DatagramValidator validator,
                             Map<Integer, Route> activeRoutes,
                             RejectAuditor auditor,
                             SITM.DatagramQueuePrx queue,
                             SITM.ArchiveServicePrx archive) {
        this.validator = validator;
        this.activeRoutes = activeRoutes;
        this.auditor = auditor;
        this.queue = queue;
        this.archive = archive;
    }

    @Override
    public void postDatagram(SITM.Datagram d, Current current) throws SITM.InvalidDatagram {
        Datagram record;
        try {
            record = SliceMapper.toRecord(d);
        } catch (RuntimeException e) {
            auditor.audit(d, "MALFORMED:" + e.getClass().getSimpleName());
            throw new SITM.InvalidDatagram("malformed datagram");
        }
        if (!validator.isValid(record, activeRoutes)) {
            String reason = classify(record);
            auditor.audit(d, reason);
            throw new SITM.InvalidDatagram(reason);
        }
        // R6.2 — archivado AMI no bloqueante
        if (archive != null) {
            try { archive.archiveDatagramAsync(d); } catch (Exception ignored) {}
        }
        // R6.1 — enqueue síncrono
        if (queue != null) {
            queue.enqueueDatagram(d);
        }
        accepted.increment();
    }

    @Override
    public void subscribe(SITM.MonitoringSubscriberPrx sub, Current current) {
        // Compatibilidad con referente: este gateway no mantiene subs.
        // Los suscriptores deben conectarse al DatagramEventBus (spec 08).
    }

    private String classify(Datagram r) {
        if (r.rawLatitude() == -1) return "LAT_MINUS_ONE";
        if (r.rawLongitude() == -1) return "LON_MINUS_ONE";
        if (r.busId() <= 0) return "BUSID_NONPOSITIVE";
        if (r.tripId() < 0) return "TRIPID_NEGATIVE";
        if (!activeRoutes.containsKey(r.lineId())) return "LINEID_NOT_ACTIVE";
        if (r.datagramDate() == null) return "NULL_DATE";
        return "UNKNOWN";
    }

    public long acceptedTotal() { return accepted.sum(); }
}
