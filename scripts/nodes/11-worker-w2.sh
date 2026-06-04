#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: BatchWorker w2 (puerto 10102)"
echo "============================================"
java -cp "$(CP worker/batch-worker)" edu.icesi.sitmmio.batchworker.Main \
  --worker-id w2 --port 10102 --lake lake \
  --routes data/raw/lines-241-ActiveGT.csv \
  --master-proxy "BatchMaster:default -h $HOST -p 10050"
