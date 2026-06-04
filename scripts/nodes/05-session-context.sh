#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: SessionContextController  (puerto 10030)"
echo "  Lookup lineId→zoneId (R9.2)"
echo "============================================"
java -cp "$(CP datacenter/session-context)" edu.icesi.sitmmio.sessioncontext.Main \
  --port 10030 --mapping admin/zone-mapping.example.json
