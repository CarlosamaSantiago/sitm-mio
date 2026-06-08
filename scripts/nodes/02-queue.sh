#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: DatagramQueue  (puerto 10010)"
echo "  Reliable Messaging — append-only FIFO"
echo "============================================"
java -cp "$(CP queue/datagram-queue)" edu.icesi.sitmmio.datagramqueue.Main \
  --port 10010 --store queue/store
