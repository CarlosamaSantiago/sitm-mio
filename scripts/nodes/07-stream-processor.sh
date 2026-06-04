#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: StreamProcessor"
echo "  Consume queue → publica al EventBus"
echo "  R12 + R32 + R22 + R23.1"
echo "============================================"
java -cp "$(CP datacenter/stream-processor)" edu.icesi.sitmmio.streamprocessor.Main \
  --queue-proxy   "DatagramQueue:default -h $HOST -p 10010" \
  --bus-proxy     "DatagramEventBus:default -h $HOST -p 10020" \
  --session-proxy "SessionContextController:default -h $HOST -p 10030" \
  --consumers 2
