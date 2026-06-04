#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: AnalyticsStore  (puerto 10060)"
echo "  Persistencia de SpeedReport (R7.4)"
echo "============================================"
java -cp "$(CP datacenter/analytics-store)" edu.icesi.sitmmio.analyticsstore.Main \
  --port 10060
