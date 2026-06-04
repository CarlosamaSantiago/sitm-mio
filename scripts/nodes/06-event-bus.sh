#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: DatagramEventBus  (puerto 10020)"
echo "  Pub/Sub — broker EDA"
echo "============================================"
java -cp "$(CP datacenter/datagram-event-bus)" edu.icesi.sitmmio.datagrameventbus.Main \
  --port 10020
