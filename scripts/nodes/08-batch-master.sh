#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: BatchMaster  (puerto 10050)"
echo "  Master del patrón Master-Worker (R7)"
echo "============================================"
java -cp "$(CP datacenter/batch-master)" edu.icesi.sitmmio.batchmaster.Main \
  --port 10050 --routes data/raw/lines-241-ActiveGT.csv --timeout-s 120
