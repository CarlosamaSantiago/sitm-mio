package edu.icesi.sitmmio.analyticsstore.io;

import edu.icesi.sitmmio.analyticsstore.domain.ReportIndex;
import edu.icesi.sitmmio.domain.SpeedResult;
import edu.icesi.sitmmio.io.ResultCsvWriter;

import java.io.IOException;
import java.nio.file.Path;

public final class CsvBackend {
    private final Path file;

    public CsvBackend(Path file) { this.file = file; }

    public void saveAll(ReportIndex index) throws IOException {
        java.util.List<SpeedResult> all = new java.util.ArrayList<>(
                index.getRange(1900, 1, 2999, 12));
        all.sort(java.util.Comparator
                .comparingInt(SpeedResult::lineId)
                .thenComparing(SpeedResult::yearMonth));
        new ResultCsvWriter().writeSpeedResults(file, all);
    }
}
