package edu.icesi.sitmmio.datalake.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.datalake.io.FileLakeStore;

public class ArchiveServiceI implements SITM.ArchiveService {

    private final FileLakeStore store;

    public ArchiveServiceI(FileLakeStore store) { this.store = store; }

    @Override
    public void archiveDatagram(SITM.Datagram d, Current current) {
        try { store.archive(d); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public SITM.SpeedReport getReport(int lineId, int year, int month, Current current)
            throws SITM.NoDataForPartition {
        throw new SITM.NoDataForPartition();
    }
}
