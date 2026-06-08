package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.RouteMonthKey;

import java.util.Map;

record WorkerResult(
        Map<RouteMonthKey, SpeedTotals> accumulators,
        long validRows,
        long skippedRows
) {
}
