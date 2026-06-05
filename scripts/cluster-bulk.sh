#!/bin/bash
set -euo pipefail

cd "$(/usr/bin/dirname "$0")/.."
ROOT="$(pwd)"

HOSTS_FILE="${SITM_HOSTS_FILE:-$ROOT/scripts/cluster-hosts.txt}"
REMOTE_USER="${SITM_USER:-$(whoami)}"
REMOTE_DIR="${SITM_REMOTE_DIR:-/home/$REMOTE_USER/sitm-mio-Distributed-ICE}"
DATASET_ZIP="${SITM_DATASET_ZIP:-/opt/sitm-mio/datagrams4Pilot.zip}"
JAVA_HOME_REMOTE="${SITM_JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
ROUTES="${SITM_ROUTES:-data/raw/lines-241-ActiveGT.csv}"
MASTER_PORT="${SITM_MASTER_PORT:-10050}"

if [ ! -f "$HOSTS_FILE" ]; then
  echo "Missing hosts file: $HOSTS_FILE"
  echo "Copy scripts/cluster-hosts.example to scripts/cluster-hosts.txt and edit it."
  exit 1
fi

mapfile -t HOSTS < <(/usr/bin/grep -v '^#' "$HOSTS_FILE" | /usr/bin/grep -v '^$')
COORDINATOR="${HOSTS[0]}"
WORKERS=("${HOSTS[@]:1}")
NUM_WORKERS="${#WORKERS[@]}"

java_env="export JAVA_HOME=$JAVA_HOME_REMOTE && export PATH=\$JAVA_HOME/bin:\$PATH"

show_topology() {
  echo "coordinator=$COORDINATOR"
  echo "workers=$NUM_WORKERS"
  printf '  %s\n' "${WORKERS[@]}"
  echo "remote_dir=$REMOTE_DIR"
  echo "dataset_zip=$DATASET_ZIP"
}

dataset_stream() {
  if [ -f "$DATASET_ZIP" ]; then
    unzip -p "$DATASET_ZIP"
  else
    ssh "$REMOTE_USER@$COORDINATOR" "unzip -p '$DATASET_ZIP'"
  fi
}

prepare_dirs() {
  for ip in "${WORKERS[@]}"; do
    echo "prepare $ip"
    ssh "$REMOTE_USER@$ip" "mkdir -p '$REMOTE_DIR/data/raw' '$REMOTE_DIR/logs'"
  done
}

copy_routes() {
  for ip in "${WORKERS[@]}"; do
    echo "routes -> $ip"
    scp "$ROOT/$ROUTES" "$REMOTE_USER@$ip:$REMOTE_DIR/$ROUTES"
  done
}

stream_shard() {
  echo "streaming zip from coordinator and sharding to $NUM_WORKERS workers"
  local ips="${WORKERS[*]}"
  dataset_stream | \
  awk -F',' -v ips="$ips" -v user="$REMOTE_USER" -v remote_dir="$REMOTE_DIR" '
  BEGIN {
    n = split(ips, ip, " ");
    started = systime();
    for (i = 0; i < n; i++) {
      cmd[i] = "ssh " user "@" ip[i+1] " \"cat > " remote_dir "/data/raw/chunk.csv\"";
    }
  }
  {
    bus = $12 + 0;
    shard = bus % n;
    print $0 | cmd[shard];
    if (NR % 1000000 == 0) {
      elapsed = systime() - started;
      if (elapsed < 1) elapsed = 1;
      printf("[stream-shard] total=%d throughput=%d/s\n", NR, NR / elapsed) > "/dev/stderr";
    }
  }
  END {
    for (i = 0; i < n; i++) close(cmd[i]);
    elapsed = systime() - started;
    if (elapsed < 1) elapsed = 1;
    printf("[stream-shard] DONE total=%d throughput=%d/s\n", NR, NR / elapsed) > "/dev/stderr";
  }'
}

build_lakes() {
  local i=0
  for ip in "${WORKERS[@]}"; do
    local part
    part="$(printf 'w%02d' "$i")"
    echo "bulk lake $ip $part"
    ssh -f "$REMOTE_USER@$ip" "cd '$REMOTE_DIR' && $java_env && rm -rf /home/$REMOTE_USER/lake-local && mkdir -p /home/$REMOTE_USER/lake-local logs && nohup java -cp domain-core/build/classes/java/main edu.icesi.sitmmio.tools.BulkLakeBuilder --input data/raw/chunk.csv --routes '$ROUTES' --lake /home/$REMOTE_USER/lake-local --part-id '$part' > logs/bulk-$part.log 2>&1 < /dev/null &"
    i=$((i + 1))
  done
}

lake_status() {
  for ip in "${WORKERS[@]}"; do
    echo "== $ip =="
    ssh -o ConnectTimeout=3 "$REMOTE_USER@$ip" "tail -n 3 '$REMOTE_DIR'/logs/bulk-*.log 2>/dev/null || true"
  done
}

start_master() {
  echo "starting BatchMaster on coordinator $COORDINATOR"
  ssh "$REMOTE_USER@$COORDINATOR" "cd '$REMOTE_DIR' && $java_env && mkdir -p logs && pkill -f '^java .*edu.icesi.sitmmio.batchmaster.Main' || true"
  ssh -f "$REMOTE_USER@$COORDINATOR" "cd '$REMOTE_DIR' && $java_env && nohup java -cp 'nodes/datacenter/batch-master/build/runtime-libs/*:nodes/datacenter/batch-master/build/classes/java/main' edu.icesi.sitmmio.batchmaster.Main --port '$MASTER_PORT' --routes '$ROUTES' --timeout-s 900 > logs/batch-master.log 2>&1 < /dev/null &"
}

start_workers() {
  local i=0
  for ip in "${WORKERS[@]}"; do
    local port wid
    port=$((10100 + i))
    wid="$(printf 'calc%02d' "$i")"
    echo "worker $wid on $ip:$port"
    ssh "$REMOTE_USER@$ip" "pkill -f '^java .*edu.icesi.sitmmio.batchworker.Main' || true"
    ssh -f "$REMOTE_USER@$ip" "cd '$REMOTE_DIR' && $java_env && mkdir -p logs && nohup java -cp 'nodes/worker/batch-worker/build/runtime-libs/*:nodes/worker/batch-worker/build/classes/java/main' edu.icesi.sitmmio.batchworker.Main --worker-id '$wid' --port '$port' --lake /home/$REMOTE_USER/lake-local --routes '$ROUTES' --master-proxy 'BatchMaster:default -h $COORDINATOR -p $MASTER_PORT' > logs/worker-$wid.log 2>&1 < /dev/null &"
    i=$((i + 1))
  done
}

worker_status() {
  for ip in "${WORKERS[@]}"; do
    echo "== $ip =="
    ssh -o ConnectTimeout=3 "$REMOTE_USER@$ip" "tail -n 5 '$REMOTE_DIR'/logs/worker-*.log 2>/dev/null || true"
  done
}

run_r7() {
  ssh "$REMOTE_USER@$COORDINATOR" "cd '$REMOTE_DIR' && $java_env && SITM_MASTER_HOST='$COORDINATOR' ./run-r7.sh ${SITM_R7_ARGS:---all-months}"
}

stop_all() {
  for ip in "${HOSTS[@]}"; do
    echo "stop $ip"
    ssh "$REMOTE_USER@$ip" "pkill -f 'edu.icesi.sitmmio' || true"
  done
}

case "${1:-}" in
  topology) show_topology ;;
  prepare-dirs) prepare_dirs ;;
  copy-routes) copy_routes ;;
  shard) stream_shard ;;
  build-lakes) build_lakes ;;
  lake-status) lake_status ;;
  start-master) start_master ;;
  start-workers) start_workers ;;
  worker-status) worker_status ;;
  run-r7) run_r7 ;;
  compute) start_master; sleep 3; start_workers; sleep 10; worker_status; run_r7 ;;
  stop) stop_all ;;
  all) show_topology; prepare_dirs; copy_routes; stream_shard; build_lakes ;;
  *)
    echo "Usage: $0 {topology|prepare-dirs|copy-routes|shard|build-lakes|lake-status|start-master|start-workers|worker-status|run-r7|compute|stop|all}"
    exit 1
    ;;
esac
