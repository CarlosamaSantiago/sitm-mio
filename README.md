# SITM-MIO

Academic Software Architecture project for calculating SITM-MIO average speeds by active route and month.

## Version 1: monolith

This repository currently contains the Java 17 monolithic baseline. It streams the local pilot datagram CSV, validates usable GPS records, calculates Haversine segment distances, aggregates distance and time by route/month, and writes the baseline result and metrics files.

Real data files are expected locally and ignored by git:

- `data/raw/lines-241-ActiveGT.csv`
- `data/raw/datagrams-MiniPilot.csv`

Run the tests:

```bash
gradle test
```

Run the monolith:

```bash
gradle run
```

Outputs:

- `data/output/monolith-results.csv`
- `data/output/experiment-results.csv`
