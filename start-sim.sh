#!/bin/bash
cd "$(/usr/bin/dirname "$0")"
java -cp "nodes/bus/bus-simulator/build/runtime-libs/*:nodes/bus/bus-simulator/build/classes/java/main" \
  edu.icesi.sitmmio.bussimulator.Main \
  --host 127.0.0.1 --port 10000 \
  --dataset data/raw/datagrams-MiniPilot.csv \
  --throttle-ms 0 --rate-multiplier 1.0
