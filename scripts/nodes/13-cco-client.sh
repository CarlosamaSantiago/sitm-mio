#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  CLIENTE: CCO Client (JavaFX + Leaflet)"
echo "  Suscriptor al DatagramEventBus :10020"
echo "  Mapa de Cali con buses en RT"
echo "============================================"
./gradlew :nodes:client-cco:cco-client:run --console=plain
