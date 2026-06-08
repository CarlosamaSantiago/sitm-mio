#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: IngestionGateway  (puerto 10000)"
echo "  Recibe datagramas → valida (R35) → encola"
echo "============================================"
java -cp "$(CP datacenter/ingestion-gateway)" edu.icesi.sitmmio.ingestiongateway.Main \
  --routes data/raw/lines-241-ActiveGT.csv \
  --queue-proxy   "DatagramQueue:default -h $HOST -p 10010" \
  --archive-proxy "ArchiveService:default -h $HOST -p 10001"
