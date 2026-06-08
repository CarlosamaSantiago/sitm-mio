# V2 Concurrent Design

## Purpose

V2 is the local concurrent baseline for the SITM-MIO speed calculation. It keeps
V1 as the correctness oracle and changes only the execution strategy: instead of
one thread owning all previous-point state and accumulators, V2 uses multiple
local workers.

V2 does not include ZeroC Ice, Master-Worker distribution, Leaflet, JavaFX,
queues, gateways, simulators, or distributed data lakes.

## Inputs

- `data/raw/lines-241-ActiveGT.csv`
- `data/raw/datagrams-MiniPilot.csv` or `data/raw/datagrams4Pilot.csv`

The route catalog is loaded once. Datagram rows are read as a stream, so the
full datagram file is not loaded into memory.

## Algorithm

1. The main reader thread reads the datagram CSV line by line.
2. Each row is parsed and validated using the same domain-core parser and
   validator used by the monolithic baseline.
3. Valid rows are partitioned by a stable key:
   `hash(busId, lineId, tripId) % workerCount`.
4. Each partition is sent to one worker queue.
5. Each worker processes its queue sequentially and owns its own
   `previousByTrip` map.
6. Each worker emits partial totals by `RouteMonthKey`.
7. The main thread merges partial totals and writes one CSV row per active route
   per observed month.

## Why This Avoids Races

All datagrams that belong to the same trajectory key
`busId + lineId + tripId` are always sent to the same worker. That worker sees
the rows in file order and updates its previous-point map without sharing it
with other threads. There is no global lock around the hot segment calculation.

## Outputs

Speed results:

```text
data/output/concurrent-results.csv
```

Metrics:

```text
data/output/experiment-results.csv
```

The V2 metrics include the same fields as V1 plus `workerCount`.

## Commands

Run with the default number of workers:

```bash
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv"
```

Run explicit experiments:

```bash
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv --workers 1"
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv --workers 2"
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv --workers 4"
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv --workers 8"
```

Compare V1 and V2 on the same dataset:

```bash
gradle run --args="data/raw/lines-241-ActiveGT.csv data/raw/datagrams-MiniPilot.csv data/output/monolith-results.csv"
gradle :v2-concurrent:run --args="--routes data/raw/lines-241-ActiveGT.csv --datagrams data/raw/datagrams-MiniPilot.csv --output data/output/concurrent-results.csv --workers 4"
diff <(sort data/output/monolith-results.csv) <(sort data/output/concurrent-results.csv)
```

On PowerShell, replace the process-substitution diff with:

```powershell
Compare-Object (Get-Content data/output/monolith-results.csv | Sort-Object) `
               (Get-Content data/output/concurrent-results.csv | Sort-Object)
```

## Relationship To V3

V2 prepares the path to V3 because every worker produces partial accumulators.
In V3, the same partial-calculation idea can be moved from local threads to
remote workers coordinated by a master.
