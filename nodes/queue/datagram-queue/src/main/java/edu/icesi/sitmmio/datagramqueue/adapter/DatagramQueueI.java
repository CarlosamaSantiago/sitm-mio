package edu.icesi.sitmmio.datagramqueue.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.datagramqueue.io.AppendOnlyStore;
import edu.icesi.sitmmio.datagramqueue.service.DatagramSerializer;

public class DatagramQueueI implements SITM.DatagramQueue {
    private final AppendOnlyStore store;

    public DatagramQueueI(AppendOnlyStore store) { this.store = store; }

    @Override
    public void enqueueDatagram(SITM.Datagram d, Current current) {
        try { store.enqueue(DatagramSerializer.toCsv(d)); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public SITM.Datagram dequeueDatagram(Current current) {
        try {
            String line = store.dequeueBlocking();
            if (line == null) return null;
            return DatagramSerializer.fromCsv(line).orElse(null);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public long queueSize(Current current) { return store.size(); }
}
