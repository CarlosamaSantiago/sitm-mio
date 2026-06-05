#!/bin/bash
cd "$(/usr/bin/dirname "$0")"
DATASET=data/raw/datagrams-MiniPilot.csv
if [ -f data/raw/datagrams4Pilot.csv ]; then
  DATASET=data/raw/datagrams4Pilot.csv
fi

java -cp "nodes/bus/bus-simulator/build/runtime-libs/*:nodes/bus/bus-simulator/build/classes/java/main" \
  edu.icesi.sitmmio.bussimulator.Main \
  --host 127.0.0.1 --port 10000 \
  --dataset "$DATASET" \
  --throttle-ms 0 --rate-multiplier 1.0
