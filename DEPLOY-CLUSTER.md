# Cluster Deployment Quick Guide

This guide captures the working Pilot4 strategy used with coordinator
`192.168.131.103` and the available Linux workers.

## One-time setup

On the coordinator:

```bash
cd /home/swarch/sitm-mio-ccdg/sitm-mio-Distributed-ICE
cp scripts/cluster-hosts.example scripts/cluster-hosts.txt
```

Edit `scripts/cluster-hosts.txt` if the room changes. The first IP must be the
coordinator and the rest are workers.

Make sure the project is compiled on every worker and the routes file exists:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
gradle classes copyLibs --console=plain
```

## Bulk experiment

From the coordinator:

```bash
cd /home/swarch/sitm-mio-ccdg/sitm-mio-Distributed-ICE
export SITM_USER=swarch
export SITM_REMOTE_DIR=/home/swarch/sitm-mio-Distributed-ICE
export SITM_DATASET_ZIP=/opt/sitm-mio/datagrams4Pilot.zip

./scripts/cluster-bulk.sh topology
./scripts/cluster-bulk.sh prepare-dirs
./scripts/cluster-bulk.sh copy-routes
./scripts/cluster-bulk.sh shard
./scripts/cluster-bulk.sh build-lakes
./scripts/cluster-bulk.sh lake-status
./scripts/cluster-bulk.sh compute
```

`shard` streams the zip and writes one `data/raw/chunk.csv` per worker.
`build-lakes` converts every chunk into `/home/swarch/lake-local`.
`compute` starts `BatchMaster`, starts all workers, and runs R7.

## Important correctness rule

Only compare `experiment/v3-results.csv` against a V1 oracle generated from the
same dataset, routes file, and validation rules. A stale
`data/output/monolith-results.csv` from MiniPilot or from a previous algorithm
will produce a large diff even when V3 is working.

For architecture rationale, see `docs/bulk-cluster-design.md`.
