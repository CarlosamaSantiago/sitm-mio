#!/bin/bash
# Abre pestañas en Terminal.app y arranca cada nodo en una.
# Uso:
#   ./scripts/open-terminals.sh           # 12 servicios (sin mapa)
#   ./scripts/open-terminals.sh --with-map # +cco-client (requiere JDK 17 arm64 nativo en Mac Apple Silicon)

set -e
WITH_MAP=0
[ "$1" = "--with-map" ] && WITH_MAP=1

SCRIPT_PATH="$( cd "$( /usr/bin/dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
ROOT="$( cd "$SCRIPT_PATH/.." >/dev/null 2>&1 && pwd )"
cd "$ROOT"

echo "==> Repo root: $ROOT"
echo "==> WITH_MAP=$WITH_MAP (usa --with-map para incluir cco-client)"
echo "==> ./gradlew copyLibs"
./gradlew copyLibs --quiet --console=plain || { echo "copyLibs falló"; exit 1; }

/bin/mkdir -p admin
if [ ! -f admin/zone-mapping.example.json ]; then
  /bin/cat > admin/zone-mapping.example.json <<'JSON'
{"lineToZone":{"131":1,"140":1,"142":2,"217":3},"userToZone":{"admin":0,"ctrl-001":1,"ctrl-002":2}}
JSON
fi
if [ ! -f admin/users.example.json ]; then
  /bin/cat > admin/users.example.json <<'JSON'
[{"userId":"admin","passwordHash":"placeholder","role":"ADMIN","zoneId":0},
 {"userId":"ctrl-001","passwordHash":"placeholder","role":"CONTROLLER","zoneId":1}]
JSON
fi

open_tab() {
  local title="$1"
  local script="$2"
  local cmd
  cmd="cd \\\"$ROOT\\\"; clear; echo '=== $title ==='; bash \\\"$script\\\""
  /usr/bin/osascript >/dev/null <<APPLESCRIPT
tell application "Terminal"
  activate
  tell application "System Events" to keystroke "t" using {command down}
  delay 0.3
  do script "$cmd" in selected tab of front window
  set custom title of selected tab of front window to "$title"
end tell
APPLESCRIPT
}

/usr/bin/open -a Terminal "$ROOT"
sleep 1

echo "==> Abriendo pestañas..."

open_tab "1-data-lake"   "$ROOT/scripts/nodes/01-data-lake.sh"
sleep 0.5
open_tab "2-queue"       "$ROOT/scripts/nodes/02-queue.sh"
sleep 0.5
open_tab "3-auth"        "$ROOT/scripts/nodes/03-auth.sh"
sleep 2

open_tab "4-analytics"   "$ROOT/scripts/nodes/04-analytics-store.sh"
sleep 0.5
open_tab "5-session"     "$ROOT/scripts/nodes/05-session-context.sh"
sleep 0.5
open_tab "6-event-bus"   "$ROOT/scripts/nodes/06-event-bus.sh"
sleep 2

open_tab "7-stream"      "$ROOT/scripts/nodes/07-stream-processor.sh"
sleep 0.5
open_tab "8-master"      "$ROOT/scripts/nodes/08-batch-master.sh"
sleep 2

open_tab "9-ingestion"   "$ROOT/scripts/nodes/09-ingestion-gateway.sh"
sleep 0.5
open_tab "10-worker-w1"  "$ROOT/scripts/nodes/10-worker-w1.sh"
sleep 0.5
open_tab "11-worker-w2"  "$ROOT/scripts/nodes/11-worker-w2.sh"
sleep 0.5
open_tab "12-public-api" "$ROOT/scripts/nodes/11-public-api.sh"

if [ "$WITH_MAP" = "1" ]; then
  sleep 2
  open_tab "13-cco-client" "$ROOT/scripts/nodes/13-cco-client.sh"
  echo ""
  echo "==> 13 pestañas (incluido cco-client con mapa)"
else
  echo ""
  echo "==> 12 pestañas (sin cco-client)"
  echo "==> Para incluir el mapa: ./scripts/open-terminals.sh --with-map"
  echo "==> Requiere JDK 17 arm64 nativo en Mac Apple Silicon. Ver:"
  echo "      brew install openjdk@17"
fi

echo ""
echo "Próximos pasos:"
echo "  1. Espera que los logs digan 'listening on ...' (todos los nodos)"
echo "  2. En otra terminal:  ./scripts/nodes/12-bus-simulator.sh"
echo "  3. Después del DONE:  ./run-r7.sh"
echo "  4. Para apagar:       ./stop-local.sh"
