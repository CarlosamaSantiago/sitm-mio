package edu.icesi.sitmmio.analyticsstore.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.analyticsstore.domain.ReportIndex;
import edu.icesi.sitmmio.contracts.SliceMapper;
import edu.icesi.sitmmio.domain.SpeedResult;

import java.util.List;

public class AnalyticsStoreI implements SITM.ReportProvider {
    private final ReportIndex index;

    public AnalyticsStoreI(ReportIndex index) { this.index = index; }

    @Override
    public SITM.SpeedReport getAverageSpeed(int lineId, int year, int month, Current current)
            throws SITM.NoDataForPartition {
        return index.get(lineId, year, month)
                .map(SliceMapper::toSlice)
                .orElseThrow(SITM.NoDataForPartition::new);
    }

    @Override
    public SITM.SpeedReport[] getMonthlyReports(int year, int month, Current current) {
        List<SpeedResult> r = index.getMonth(year, month);
        SITM.SpeedReport[] out = new SITM.SpeedReport[r.size()];
        for (int i = 0; i < r.size(); i++) out[i] = SliceMapper.toSlice(r.get(i));
        return out;
    }

    @Override
    public SITM.SpeedReport[] getRangeReports(int yf, int mf, int yt, int mt, Current current) {
        List<SpeedResult> r = index.getRange(yf, mf, yt, mt);
        SITM.SpeedReport[] out = new SITM.SpeedReport[r.size()];
        for (int i = 0; i < r.size(); i++) out[i] = SliceMapper.toSlice(r.get(i));
        return out;
    }

    // API local para escritura (no en Slice)
    public void saveAll(List<SpeedResult> results) { for (SpeedResult r : results) index.put(r); }
    public ReportIndex index() { return index; }
}
