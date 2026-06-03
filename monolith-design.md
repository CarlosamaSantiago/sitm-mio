# V1 Monolithic Baseline Design

## Purpose

Version 1 is the correctness and performance baseline for the SITM-MIO Software Architecture project. It calculates average speed per active route per month using the MiniPilot datagrams in a single Java process, without concurrency, distribution, ZeroC Ice, JavaFX, or Leaflet.

The later versions should preserve the same parsing, validation, distance calculation, and aggregation semantics so latency and throughput comparisons are meaningful.

## Input files

- `data/raw/lines-241-ActiveGT.csv`: active route catalog for plan version 241. It has a header and includes `LINEID`, `SHORTNAME`, and `DESCRIPTION`.
- `data/raw/datagrams-MiniPilot.csv`: pilot datagrams without a header. The relevant positions are:
  - `0 eventType`
  - `1 registerdate`
  - `2 stopId`
  - `3 odometer`
  - `4 latitude`
  - `5 longitude`
  - `6 taskId`
  - `7 lineId`
  - `8 tripId`
  - `9 unknown1`
  - `10 datagramDate`
  - `11 busId`

Latitude and longitude are stored as integers and are converted by dividing by `10_000_000.0`.

## Algorithm

1. Load all active routes into memory as `Map<Integer, Route>`.
2. Stream `datagrams-MiniPilot.csv` line by line.
3. Parse and validate each datagram.
4. Track the previous valid datagram by `busId + lineId + tripId`.
5. For each valid consecutive pair:
   - calculate Haversine distance in kilometers,
   - calculate elapsed time in hours,
   - skip non-positive time differences,
   - skip gaps above five minutes,
   - skip speeds above 120 km/h,
   - aggregate distance and time by `lineId + YearMonth`.
6. Calculate `averageSpeedKmH = totalDistanceKm / totalTimeHours`.
7. Emit one row for each active route and each month found in the active-route datagram stream. Route/month combinations without valid segments are marked `NO_DATA`.

## Validation rules

Rows are skipped without stopping the execution when:

- the datagram does not have exactly 12 columns,
- numeric fields cannot be parsed,
- `datagramDate` cannot be parsed as `yyyy-MM-dd HH:mm:ss`,
- latitude or longitude is `-1`,
- `busId <= 0`,
- `tripId < 0`,
- `lineId` is not present in the active route catalog,
- the current timestamp is not after the previous timestamp for the same `busId + lineId + tripId`,
- the segment time difference is not positive,
- the segment gap is above five minutes,
- the segment speed is above 120 km/h.

## Output files

`data/output/monolith-results.csv`

```text
lineId,shortName,description,yearMonth,totalDistanceKm,totalTimeHours,averageSpeedKmH,validSegments,skippedSegments,status
```

`data/output/experiment-results.csv`

```text
version,dataset,processedRows,validRows,skippedRows,executionTimeMs,throughputRowsPerSecond,memoryUsedMb,resultRows
```

For V1, `version` is `monolith`.

## Baseline rationale

This monolith establishes a clear reference behavior before adding Thread Pool execution or a distributed Master-Worker design. Keeping V1 simple makes it easier to verify correctness, measure single-process throughput, and isolate the effect of concurrency and distribution in later experiments.
