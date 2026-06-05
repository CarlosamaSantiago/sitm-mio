# SITM-MIO Bulk Cluster Design

## What happened in the 20-machine run

The first approach tried to push the full `datagrams4Pilot.csv` through the
runtime ingestion path:

`bus-simulator -> IngestionGateway -> DatagramQueue/DataLake -> BatchWorker`

With one simulator the gateway accepted roughly 33k rows/s. Starting more bus
simulators did not scale linearly because they all synchronized on the same
gateway/queue/data-lake path. The bottleneck moved to the centralized
ingestion components instead of the CSV reader.

The successful run used a bulk path:

1. The coordinator streamed `datagrams4Pilot.zip`.
2. Rows were sharded by `busId % N` and written directly to each worker.
3. Each worker built a local lake from its chunk using `BulkLakeBuilder`.
4. `BatchMaster.runMonth(2019, 5)` sent every route/month partition to every
   registered worker.
5. Workers returned partial `SpeedReport` values.
6. The master merged partials by summing distance, time, valid segments, and
   skipped segments, then recalculated average speed.

This finished the distributed calculation in about 61 seconds after the local
lakes had been materialized.

## Difference from `docs/Deployment.pdf`

The deployment document describes the classic V3 runtime deployment:

- Coordinator: gateway, queue, data lake, event bus, stream processor,
  session context, analytics store, public API, batch master.
- Worker nodes: batch workers reading a copied or mounted lake.
- Client/bus node: simulator and UI.

That topology is still valid for the real-time/demo path and for smaller
datasets. It is not ideal for the full 806M-row Pilot4 file because the
centralized ingestion path becomes the limiting architectural element.

The bulk cluster path is an optimization of the R7 experiment, not a rewrite of
the whole system. Architecturally it adds a batch-oriented data plane beside
the real-time Ice data plane:

- Control plane: ZeroC Ice `BatchMaster` and `BatchWorker` remain distributed.
- Bulk data plane: CSV rows are partitioned before calculation and stored
  locally on workers.
- Merge point: `BatchMaster` acts as fan-out/fan-in coordinator.

## Correctness contract

V1 is the oracle. V2 and V3 must use the same rules:

- Parse 12 CSV columns.
- Convert coordinates by dividing latitude/longitude by `10_000_000.0`.
- Accept only active `lineId` values from `lines-241-ActiveGT.csv`.
- Reject `latitude == -1`, `longitude == -1`, `busId <= 0`, and
  `tripId < 0`.
- Keep previous point by `busId + lineId + tripId`.
- Require strictly increasing timestamps.
- Skip segment if the time delta is `<= 0` or greater than 5 minutes.
- Skip segment if speed is greater than 120 km/h.
- Aggregate by `lineId + YearMonth`.

If V1 and V3 are compared with different datasets, stale result CSVs, or
different validation rules, `V3 == V1` is not meaningful.

## Why the previous diff appeared

The run completed successfully, but `run-r7.sh` compared the new V3 result with
whatever file existed at `data/output/monolith-results.csv`. In the observed
run, V3 processed the 806M-row dataset, while the oracle file appeared to be a
stale or different V1 result. That explains large differences such as segment
counts differing by millions rather than tiny floating-point drift.

## Recommended experiment flow

1. Generate or select the V1 oracle for the exact same dataset.
2. Distribute chunks by `busId % N`.
3. Build local worker lakes.
4. Start `BatchMaster`.
5. Start all `BatchWorker` processes.
6. Run `run-r7.sh`.
7. Compare against the matching V1 oracle only.

Use `scripts/cluster-bulk.sh` as the repeatable operational wrapper.
