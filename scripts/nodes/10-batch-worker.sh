#!/bin/bash
# Uso: ./10-batch-worker.sh <N>   (N = 1,2,3,4...)
source "$( /usr/bin/dirname "$0" )/../_common.sh"
N=${1:-1}
PORT=$((10100 + N))
echo "============================================"
echo "  NODO: BatchWorker w$N  (puerto $PORT)"
echo "  Worker del patrón Master-Worker"
echo "  Cálculo Haversine + agregado mensual"
echo "============================================"
java -cp "$(CP worker/batch-worker)" edu.icesi.sitmmio.batchworker.Main \
  --worker-id "w$N" --port $PORT --lake lake \
  --routes data/raw/lines-241-ActiveGT.csv \
  --master-proxy "BatchMaster:default -h $HOST -p 10050"
