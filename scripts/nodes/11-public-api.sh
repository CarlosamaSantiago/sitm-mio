#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: PublicAPI  (HTTP puerto 9090)"
echo "  REST + JWT + OpenAPI (R14)"
echo "============================================"
java -cp "$(CP server-public-api/public-api):nodes/server-public-api/public-api/build/resources/main" \
  edu.icesi.sitmmio.publicapi.Main \
  --http-port 9090 \
  --auth-proxy    "AuthService:default -h $HOST -p 10040" \
  --reports-proxy "ReportProvider:default -h $HOST -p 10060"
