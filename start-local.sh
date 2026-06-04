#!/bin/bash
# start-local.sh — arranca TODO el sistema V3 en localhost usando tmux
# Uso: ./start-local.sh

set -e
cd "$(/usr/bin/dirname "$0")"

command -v tmux >/dev/null 2>&1 || { echo "tmux no instalado. Run: brew install tmux"; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Java no encontrado"; exit 1; }
[ -f data/raw/lines-241-ActiveGT.csv ] || { echo "Falta data/raw/lines-241-ActiveGT.csv"; exit 1; }
[ -f data/raw/datagrams-MiniPilot.csv ] || { echo "Falta data/raw/datagrams-MiniPilot.csv (cópialo desde /opt/sitm-mio/)"; exit 1; }

echo "==> ./gradlew copyLibs (rápido si ya hecho)"
./gradlew copyLibs --console=plain --quiet || { echo "Build falló"; exit 1; }

mkdir -p admin
[ -f admin/zone-mapping.example.json ] || cat > admin/zone-mapping.example.json <<'JSON'
{
  "lineToZone": {"131":1,"140":1,"142":2,"217":3},
  "userToZone": {"admin":0,"ctrl-001":1,"ctrl-002":2}
}
JSON
[ -f admin/users.example.json ] || cat > admin/users.example.json <<'JSON'
[
  {"userId":"admin","passwordHash":"placeholder","role":"ADMIN","zoneId":0},
  {"userId":"ctrl-001","passwordHash":"placeholder","role":"CONTROLLER","zoneId":1}
]
JSON

SESSION=sitm
tmux kill-session -t $SESSION 2>/dev/null || true
tmux new-session -d -s $SESSION

run() {
  local name=$1; local cmd=$2
  tmux new-window -t $SESSION -n "$name"
  tmux send-keys -t $SESSION:"$name" "$cmd" C-m
}

CP() { echo "nodes/$1/build/runtime-libs/*:nodes/$1/build/classes/java/main"; }

run "data-lake"  "java -cp '$(CP datacenter/data-lake)' edu.icesi.sitmmio.datalake.Main --port 10001 --store lake"
run "queue"      "java -cp '$(CP queue/datagram-queue)' edu.icesi.sitmmio.datagramqueue.Main --port 10010 --store queue/store"
run "auth"       "java -cp '$(CP server-admin/auth-service)' edu.icesi.sitmmio.authservice.Main --port 10040 --users admin/users.example.json --secret demo-secret --salt sitm-salt"
run "analytics"  "java -cp '$(CP datacenter/analytics-store)' edu.icesi.sitmmio.analyticsstore.Main --port 10060"
run "session"    "java -cp '$(CP datacenter/session-context)' edu.icesi.sitmmio.sessioncontext.Main --port 10030 --mapping admin/zone-mapping.example.json"
run "event-bus"  "java -cp '$(CP datacenter/datagram-event-bus)' edu.icesi.sitmmio.datagrameventbus.Main --port 10020"

sleep 3

run "stream"     "java -cp '$(CP datacenter/stream-processor)' edu.icesi.sitmmio.streamprocessor.Main --queue-proxy 'DatagramQueue:default -h 127.0.0.1 -p 10010' --bus-proxy 'DatagramEventBus:default -h 127.0.0.1 -p 10020' --session-proxy 'SessionContextController:default -h 127.0.0.1 -p 10030' --consumers 2"
run "master"     "java -cp '$(CP datacenter/batch-master)' edu.icesi.sitmmio.batchmaster.Main --port 10050 --routes data/raw/lines-241-ActiveGT.csv"

sleep 2

run "ingestion"  "java -cp '$(CP datacenter/ingestion-gateway)' edu.icesi.sitmmio.ingestiongateway.Main --routes data/raw/lines-241-ActiveGT.csv --queue-proxy 'DatagramQueue:default -h 127.0.0.1 -p 10010' --archive-proxy 'ArchiveService:default -h 127.0.0.1 -p 10001'"
run "worker-1"   "java -cp '$(CP worker/batch-worker)' edu.icesi.sitmmio.batchworker.Main --worker-id w1 --port 10101 --lake lake --routes data/raw/lines-241-ActiveGT.csv --master-proxy 'BatchMaster:default -h 127.0.0.1 -p 10050'"
run "worker-2"   "java -cp '$(CP worker/batch-worker)' edu.icesi.sitmmio.batchworker.Main --worker-id w2 --port 10102 --lake lake --routes data/raw/lines-241-ActiveGT.csv --master-proxy 'BatchMaster:default -h 127.0.0.1 -p 10050'"
run "public-api" "java -cp '$(CP server-public-api/public-api):nodes/server-public-api/public-api/build/resources/main' edu.icesi.sitmmio.publicapi.Main --http-port 9090 --auth-proxy 'AuthService:default -h 127.0.0.1 -p 10040' --reports-proxy 'ReportProvider:default -h 127.0.0.1 -p 10060'"

echo ""
echo "==> 12 servicios arrancados en tmux session '$SESSION'"
echo ""
echo "Conéctate con:    tmux attach -t $SESSION"
echo "Navega ventanas:  Ctrl+B luego 0-9 (o p/n)"
echo "Sal sin matar:    Ctrl+B luego d"
echo "Mata todo:        ./stop-local.sh"
echo ""
echo "Cuando los logs estén verdes, en OTRA terminal lanza el simulador:"
echo "  ./start-sim.sh"
echo ""
echo "Y para correr R7 al final:"
echo "  ./run-r7.sh"
