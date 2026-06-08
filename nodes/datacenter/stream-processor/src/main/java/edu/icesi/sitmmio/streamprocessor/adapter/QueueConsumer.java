package edu.icesi.sitmmio.streamprocessor.adapter;

import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.streamprocessor.service.BusStateBuilder;
import edu.icesi.sitmmio.streamprocessor.service.DatagramExtractor;
import edu.icesi.sitmmio.streamprocessor.service.EventClassifier;
import edu.icesi.sitmmio.streamprocessor.service.SignalMonitor;

import java.util.concurrent.atomic.LongAdder;

public final class QueueConsumer implements Runnable {

    private final SITM.DatagramQueuePrx queue;
    private final EventBusPublisher publisher;
    private final DatagramExtractor extractor;
    private final EventClassifier classifier;
    private final SignalMonitor monitor;
    private final BusStateBuilder stateBuilder;
    private final SITM.SessionContextControllerPrx sessionCtx;
    private final LongAdder consumed = new LongAdder();
    private final LongAdder published = new LongAdder();
    private final LongAdder skipped = new LongAdder();
    private volatile boolean running = true;

    public QueueConsumer(SITM.DatagramQueuePrx queue,
                         EventBusPublisher publisher,
                         DatagramExtractor extractor,
                         EventClassifier classifier,
                         SignalMonitor monitor,
                         BusStateBuilder stateBuilder,
                         SITM.SessionContextControllerPrx sessionCtx) {
        this.queue = queue;
        this.publisher = publisher;
        this.extractor = extractor;
        this.classifier = classifier;
        this.monitor = monitor;
        this.stateBuilder = stateBuilder;
        this.sessionCtx = sessionCtx;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        while (running) {
            SITM.Datagram sliceD;
            try { sliceD = queue.dequeueDatagram(); }
            catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ie) { return; }
                continue;
            }
            // Guard contra struct vacío que Ice devuelve cuando el servant retorna null
            // (no podemos confiar en null porque structs Ice no son nullable)
            if (sliceD == null || sliceD.datagramDate == null
                    || sliceD.datagramDate.isEmpty() || sliceD.busId == 0) {
                skipped.increment();
                try { Thread.sleep(100); } catch (InterruptedException ie) { return; }
                continue;
            }

            consumed.increment();
            Datagram d;
            try {
                d = extractor.extract(sliceD);
            } catch (Exception parseEx) {
                skipped.increment();
                continue;
            }

            monitor.record(d);
            BusStateBuilder.State state = stateBuilder.build(d);
            int zoneId = sessionCtx != null ? safeZone(d.lineId()) : 0;

            SITM.BusUpdate u = new SITM.BusUpdate();
            u.busId = d.busId();
            u.pos = new SITM.Location(d.point().latitude(), d.point().longitude());
            u.lineId = d.lineId();
            u.timestamp = d.datagramDate().toString();
            u.zoneId = zoneId;
            u.operationalState = state.name();
            publisher.publishUpdate(u);
            published.increment();

            if (classifier.classify(d) == EventClassifier.Category.EXCEPTIONAL) {
                SITM.CriticAlert a = new SITM.CriticAlert();
                a.busId = d.busId(); a.lineId = d.lineId(); a.zoneId = zoneId;
                a.eventType = d.eventType(); a.priority = "ALTA";
                a.timestamp = d.datagramDate().toString();
                a.description = "Excepcional eventType=" + d.eventType();
                publisher.publishAlert(a);
            }

            if ((consumed.sum() % 50_000) == 0) {
                System.out.println("[stream-processor] consumed=" + consumed.sum()
                        + " published=" + published.sum() + " skipped=" + skipped.sum());
            }
        }
    }

    private int safeZone(int lineId) {
        try { return sessionCtx.zoneOfLine(lineId); }
        catch (Exception e) { return 0; }
    }

    public long consumedTotal()  { return consumed.sum(); }
    public long publishedTotal() { return published.sum(); }
    public long skippedTotal()   { return skipped.sum(); }
}
