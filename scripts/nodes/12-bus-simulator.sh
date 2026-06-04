#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  CLIENTE: bus-simulator"
echo "  Emite el dataset MiniPilot vía Ice"
echo "============================================"
DATASET="${1:-data/raw/datagrams-MiniPilot.csv}"
java -cp "$(CP bus/bus-simulator)" edu.icesi.sitmmio.bussimulator.Main \
  --host $HOST --port 10000 \
  --dataset "$DATASET" \
  --throttle-ms 0 --rate-multiplier 1.0
