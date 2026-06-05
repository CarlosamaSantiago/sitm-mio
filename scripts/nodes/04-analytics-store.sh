#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: AnalyticsStore  (puerto 10060)"
echo "  Persistencia de SpeedReport (R7.4)"
echo "============================================"
ARGS=(--port 10060)
if [ -n "${SITM_REPORTS_CSV:-}" ]; then
  ARGS+=(--load-csv "$SITM_REPORTS_CSV")
fi
java -cp "$(CP datacenter/analytics-store)" edu.icesi.sitmmio.analyticsstore.Main \
  "${ARGS[@]}"
