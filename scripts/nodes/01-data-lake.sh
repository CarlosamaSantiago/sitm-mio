#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: DataLake  (puerto 10001)"
echo "  Cold storage particionado por año-mes"
echo "============================================"
java -cp "$(CP datacenter/data-lake)" edu.icesi.sitmmio.datalake.Main \
  --port 10001 --store lake
