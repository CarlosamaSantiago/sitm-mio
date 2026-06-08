package edu.icesi.sitmmio.concurrent;

import edu.icesi.sitmmio.domain.SpeedResult;

import java.util.List;

public record ConcurrentCalculationResult(
        List<SpeedResult> results,
        ConcurrentMetricsSnapshot metrics
) {
}
