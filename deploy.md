# Deploy V3 — Sala de cómputo (ZeroC ICE distribuido)

Guía para correr el sistema V3 entre varias máquinas de la sala, usando **Tomcat solo como mecanismo de transferencia HTTP** del dataset MiniPilot y de los jars compilados.

> **Importante:** los nodos V3 son procesos Java/Ice standalone. **NO se despliegan como WAR dentro de Tomcat.** Tomcat únicamente sirve los archivos estáticos para que las otras máquinas los descarguen.

---

## 0. Variables que vas a llenar al inicio

| Variable | Rellenar | Cómo obtenerlo |
|----------|----------|----------------|
| `IP_PC1` (DataCenter) | `____._____._____.____` | En PC1: `ip addr show \| grep inet` o `hostname -I` |
| `IP_PC2` (Workers) | `____._____._____.____` | En PC2: igual |
| `IP_PC3` (Cliente/Bus) | `____._____._____.____` | En PC3: igual |
| `TOMCAT_PORT` | usualmente `8080` | `ss -tlnp \| grep java` o ver `server.xml` de Tomcat |
| `TOMCAT_WEBAPPS` | usualmente `/opt/tomcat/webapps/` | `ps aux \| grep tomcat` → buscar `-Dcatalina.base=...` |
| `USUARIO` | tu user en la sala | `whoami` |

### Puertos Ice que usa V3 (no chocan con Tomcat)

| Puerto | Servicio | Máquina |
|--------|----------|---------|
| 10000 | IngestionGateway | PC1 |
| 10001 | DataLake (ArchiveService) | PC1 |
| 10010 | DatagramQueue | PC1 |
| 10020 | DatagramEventBus | PC1 |
| 10030 | SessionContextController | PC1 |
| 10040 | AuthService | PC1 |
| 10050 | BatchMaster | PC1 |
| 10060 | AnalyticsStore (ReportProvider) | PC1 |
| 10101–10104 | BatchWorker × N | PC2 |
| 9090 (REST) | PublicAPI | PC1 (no usa 8080 para no chocar con Tomcat) |

---

## 1. Topología

### Opción A — 2 máquinas (mínimo viable)

```
PC1 (IP_PC1)                       PC2 (IP_PC2)
─ DataCenter completo              ─ bus-simulator (con MiniPilot)
─ Workers en localhost             ─ batch-worker × N
─ Tomcat (file server)  ────────→  ─ Descarga jars + dataset
```

### Opción B — 3 máquinas (recomendado)

```
PC1 — DataCenter              PC2 — Workers              PC3 — Cliente
IP_PC1                        IP_PC2                     IP_PC3

data-lake :10001              batch-worker × 4           bus-simulator
datagram-queue :10010           :10101 :10102            (con MiniPilot)
ingestion-gw :10000  ←──        :10103 :10104
stream-processor              Necesita acceso al         → publica al
event-bus :10020              lake/ de PC1                IP_PC1:10000
batch-master :10050           (vía Tomcat o NFS)
analytics-store :10060
auth-service :10040           cco-client (opcional)
public-api :9090
Tomcat :8080
```

---

## 2. Transferir archivos vía Tomcat (paso a paso)

### 2.1 En PC1: empaquetar todo lo que las demás máquinas necesitan

```bash
cd ~/sitm-mio
./gradlew clean build copyLibs

# Empaquetar el código compilado + libs + recursos en un tar
tar czf sitm-mio-v3-dist.tar.gz \
  contracts/build/classes \
  contracts/build/runtime-libs \
  domain-core/build/classes \
  domain-core/build/runtime-libs \
  nodes/*/*/build/classes \
  nodes/*/*/build/runtime-libs \
  nodes/*/*/build/resources \
  data/raw/lines-241-ActiveGT.csv \
  admin/

ls -lh sitm-mio-v3-dist.tar.gz   # ~50-80 MB
```

### 2.2 Subirlo a Tomcat (PC1 sirve los archivos)

```bash
# Localizar webapps de Tomcat
TOMCAT_WEBAPPS=/opt/tomcat/webapps     # AJUSTA si es distinto
sudo mkdir -p ${TOMCAT_WEBAPPS}/sitm-mio
sudo chown -R ${USER}:${USER} ${TOMCAT_WEBAPPS}/sitm-mio

cp sitm-mio-v3-dist.tar.gz                ${TOMCAT_WEBAPPS}/sitm-mio/
cp data/raw/datagrams-MiniPilot.csv       ${TOMCAT_WEBAPPS}/sitm-mio/
cp data/raw/lines-241-ActiveGT.csv        ${TOMCAT_WEBAPPS}/sitm-mio/

ls -lh ${TOMCAT_WEBAPPS}/sitm-mio/        # 3 archivos
```

> Para archivos planos no es necesario `WEB-INF/web.xml`. Si Tomcat no muestra listing de directorio, accede directo al archivo por nombre.

### 2.3 Verificar desde otra máquina

```bash
# Desde PC2 o PC3
curl -I http://IP_PC1:8080/sitm-mio/datagrams-MiniPilot.csv
# HTTP/1.1 200 OK
# Content-Length: ~600MB
```

### 2.4 En PC2 y PC3: descargar y descomprimir

```bash
mkdir -p ~/sitm-mio
cd ~/sitm-mio

curl -O http://IP_PC1:8080/sitm-mio/sitm-mio-v3-dist.tar.gz
curl -O http://IP_PC1:8080/sitm-mio/datagrams-MiniPilot.csv     # solo en PC3
curl -O http://IP_PC1:8080/sitm-mio/lines-241-ActiveGT.csv

tar xzf sitm-mio-v3-dist.tar.gz
```

---

## 3. Arranque del sistema (orden + comandos)

> Pre-requisito en cada máquina: `export ICE_HOME=...` y JDK 17 disponible.

### PC1 — DataCenter (10 procesos)

Usa 10 terminales o el script `start-pc1.sh` de la sección §10.

#### Terminal 1 — DataLake

```bash
cd ~/sitm-mio
java -cp "nodes/datacenter/data-lake/build/runtime-libs/*:nodes/datacenter/data-lake/build/classes/java/main" \
  edu.icesi.sitmmio.datalake.Main --port 10001 --store /home/${USER}/lake
```

Espera: `[data-lake] listening on 10001`

#### Terminal 2 — DatagramQueue

```bash
java -cp "nodes/queue/datagram-queue/build/runtime-libs/*:nodes/queue/datagram-queue/build/classes/java/main" \
  edu.icesi.sitmmio.datagramqueue.Main --port 10010 --store /home/${USER}/queue/store
```

#### Terminal 3 — AuthService

```bash
java -cp "nodes/server-admin/auth-service/build/runtime-libs/*:nodes/server-admin/auth-service/build/classes/java/main" \
  edu.icesi.sitmmio.authservice.Main \
  --port 10040 --users admin/users.example.json \
  --secret cambia-este-secreto --salt sitm-salt
```

#### Terminal 4 — AnalyticsStore

```bash
java -cp "nodes/datacenter/analytics-store/build/runtime-libs/*:nodes/datacenter/analytics-store/build/classes/java/main" \
  edu.icesi.sitmmio.analyticsstore.Main --port 10060
```

#### Terminal 5 — SessionContextController

```bash
java -cp "nodes/datacenter/session-context/build/runtime-libs/*:nodes/datacenter/session-context/build/classes/java/main" \
  edu.icesi.sitmmio.sessioncontext.Main \
  --port 10030 --mapping admin/zone-mapping.example.json
```

#### Terminal 6 — DatagramEventBus

```bash
java -cp "nodes/datacenter/datagram-event-bus/build/runtime-libs/*:nodes/datacenter/datagram-event-bus/build/classes/java/main" \
  edu.icesi.sitmmio.datagrameventbus.Main --port 10020
```

#### Terminal 7 — StreamProcessor

```bash
java -cp "nodes/datacenter/stream-processor/build/runtime-libs/*:nodes/datacenter/stream-processor/build/classes/java/main" \
  edu.icesi.sitmmio.streamprocessor.Main \
  --queue-proxy   "DatagramQueue:default -h IP_PC1 -p 10010" \
  --bus-proxy     "DatagramEventBus:default -h IP_PC1 -p 10020" \
  --session-proxy "SessionContextController:default -h IP_PC1 -p 10030" \
  --consumers 2
```

#### Terminal 8 — BatchMaster

```bash
java -cp "nodes/datacenter/batch-master/build/runtime-libs/*:nodes/datacenter/batch-master/build/classes/java/main" \
  edu.icesi.sitmmio.batchmaster.Main \
  --port 10050 --routes data/raw/lines-241-ActiveGT.csv --timeout-s 120
```

Espera: `[batch-master] active routes=110` `[batch-master] listening on 10050`

#### Terminal 9 — IngestionGateway

```bash
java -cp "nodes/datacenter/ingestion-gateway/build/runtime-libs/*:nodes/datacenter/ingestion-gateway/build/classes/java/main" \
  edu.icesi.sitmmio.ingestiongateway.Main \
  --routes data/raw/lines-241-ActiveGT.csv \
  --queue-proxy   "DatagramQueue:default -h IP_PC1 -p 10010" \
  --archive-proxy "ArchiveService:default -h IP_PC1 -p 10001"
```

Espera: `[ingestion-gateway] loaded 110 active routes` `[ingestion-gateway] listening on port 10000`

#### Terminal 10 — PublicAPI

```bash
java -cp "nodes/server-public-api/public-api/build/runtime-libs/*:nodes/server-public-api/public-api/build/classes/java/main:nodes/server-public-api/public-api/build/resources/main" \
  edu.icesi.sitmmio.publicapi.Main \
  --http-port 9090 \
  --auth-proxy    "AuthService:default -h IP_PC1 -p 10040" \
  --reports-proxy "ReportProvider:default -h IP_PC1 -p 10060"
```

---

### PC2 — Workers (4 procesos)

Los workers necesitan acceso al `lake/` que está en PC1. **3 opciones:**

#### Opción A — copiar lake tras ingesta

```bash
# En PC2, cuando termine la ingesta:
rsync -av ${USER}@IP_PC1:/home/${USER}/lake/ /home/${USER}/lake/
```

#### Opción B — NFS desde PC1 (recomendado para varios runs)

En PC1:
```bash
sudo apt install nfs-kernel-server
echo "/home/${USER}/lake IP_PC2(rw,sync,no_subtree_check)" | sudo tee -a /etc/exports
sudo exportfs -a
sudo systemctl restart nfs-server
```

En PC2:
```bash
sudo apt install nfs-common
sudo mkdir -p /mnt/pc1-lake
sudo mount IP_PC1:/home/${USER}/lake /mnt/pc1-lake
```

Y en los comandos de worker usa `--lake /mnt/pc1-lake`.

#### Opción C — Tomcat sirve `lake.tar.gz` (más fácil sin permisos NFS)

En PC1, tras ingesta:
```bash
cd /home/${USER}
tar czf lake.tar.gz lake/
cp lake.tar.gz ${TOMCAT_WEBAPPS}/sitm-mio/
```

En PC2:
```bash
curl -O http://IP_PC1:8080/sitm-mio/lake.tar.gz
tar xzf lake.tar.gz
```

#### Arrancar 4 workers (4 terminales en PC2)

```bash
# Worker 1
java -cp "nodes/worker/batch-worker/build/runtime-libs/*:nodes/worker/batch-worker/build/classes/java/main" \
  edu.icesi.sitmmio.batchworker.Main \
  --worker-id w1 --port 10101 --lake /home/${USER}/lake \
  --routes data/raw/lines-241-ActiveGT.csv \
  --master-proxy "BatchMaster:default -h IP_PC1 -p 10050"

# Worker 2 — otra terminal — cambiar a --worker-id w2 --port 10102
# Worker 3 — otra terminal — cambiar a --worker-id w3 --port 10103
# Worker 4 — otra terminal — cambiar a --worker-id w4 --port 10104
```

Espera en cada worker: `[batch-worker] id=wN listening on 1010N` `[batch-worker] registered with master`
En el log de Terminal 8 de PC1 (master) deberías ver: `[batch-master] worker registered. total=N`

---

### PC3 — Cliente / Bus Simulator

#### Bus Simulator (emite el dataset hacia PC1)

```bash
cd ~/sitm-mio
java -cp "nodes/bus/bus-simulator/build/runtime-libs/*:nodes/bus/bus-simulator/build/classes/java/main" \
  edu.icesi.sitmmio.bussimulator.Main \
  --host IP_PC1 --port 10000 \
  --dataset /home/${USER}/datagrams-MiniPilot.csv \
  --throttle-ms 0 --rate-multiplier 1.0
```

Output esperado:
```
[bus-simulator] host=IP_PC1 port=10000 dataset=/home/.../datagrams-MiniPilot.csv throttle=0ms rate=1.0
[bus-simulator] sent=10000 skipped=2150 errors=0
[bus-simulator] sent=20000 skipped=4310 errors=0
...
[bus-simulator] DONE sent=6744428 skipped=1401034 errors=0 elapsed=Xs throughput=Y/s
```

#### CCO Client (opcional — UI tiempo real)

```bash
java --module-path /path/to/javafx-sdk-17/lib \
     --add-modules javafx.controls,javafx.web \
     -cp "nodes/client-cco/cco-client/build/runtime-libs/*:nodes/client-cco/cco-client/build/classes/java/main:nodes/client-cco/cco-client/build/resources/main" \
     edu.icesi.sitmmio.ccoclient.Main
```

> El CCO conecta al EventBus en `IP_PC1:10020` por configuración hard-coded en `Main.java`. Edita ese archivo si necesitas otra IP.

---

## 4. Verificación de conectividad antes de empezar

En PC2 y PC3 probar que llega a los puertos de PC1:

```bash
nc -zv IP_PC1 10000   # IngestionGateway
nc -zv IP_PC1 10010   # DatagramQueue
nc -zv IP_PC1 10020   # EventBus
nc -zv IP_PC1 10050   # BatchMaster
nc -zv IP_PC1 10060   # AnalyticsStore
nc -zv IP_PC1 8080    # Tomcat (para descargas)
```

Si alguno falla → firewall en PC1:
```bash
sudo ufw allow from IP_PC2 to any port 10000:10110 proto tcp
sudo ufw allow from IP_PC3 to any port 10000:10110 proto tcp
```

---

## 5. Correr el experimento R7 (cálculo distribuido de velocidades)

Tras la ingesta completa (`bus-simulator` reporta `DONE`):

### Opción 1 — cliente Ice ad-hoc

Crear `RunR7.java` en PC1:

```java
import com.zeroc.Ice.*;

public class RunR7 {
    public static void main(String[] args) {
        try (Communicator c = Util.initialize(args)) {
            ObjectPrx base = c.stringToProxy("BatchMaster:default -h IP_PC1 -p 10050");
            SITM.BatchMasterPrx master = SITM.BatchMasterPrx.checkedCast(base);
            long t0 = System.currentTimeMillis();
            SITM.SpeedReport[] results = master.runMonth(2019, 5);
            long elapsed = System.currentTimeMillis() - t0;
            System.out.println("=== V3 Result ===");
            System.out.println("Routes processed: " + results.length);
            System.out.println("Time: " + elapsed + " ms");
            for (var r : results) {
                if ("OK".equals(r.status)) {
                    System.out.printf("  lineId=%d shortName=%s avgKmH=%.4f%n",
                        r.lineId, r.shortName, r.averageSpeedKmH);
                }
            }
        }
    }
}
```

Compilar y correr:
```bash
javac -cp "contracts/build/runtime-libs/*:contracts/build/classes/java/main" RunR7.java
java  -cp ".:contracts/build/runtime-libs/*:contracts/build/classes/java/main" RunR7
```

### Opción 2 — REST del PublicAPI

```bash
curl http://IP_PC1:9090/api/v1/health
# {"status":"OK"}

curl -H "Authorization: Bearer $TOKEN" \
  "http://IP_PC1:9090/api/v1/speeds/131?year=2019&month=5"
# {"lineId":131,"shortName":"T31","yearMonth":"2019-05","averageSpeedKmH":17.375...,"status":"OK"}
```

### Verificación de correctness contra V1

```bash
# Oracle V1 (ya en repo en data/output/monolith-results.csv)
diff <(sort data/output/monolith-results.csv) \
     <(sort analytics-db/speed-reports.csv)

# Valores oracle conocidos para 2019-05:
# lineId=131 (T31) → averageSpeedKmH ≈ 17.375445   (validSegments=206566)
# lineId=140 (T40) → averageSpeedKmH ≈ 15.454446   (validSegments=105358)
# lineId=142 (T42) → averageSpeedKmH ≈ 17.273641   (validSegments=156536)
# lineId=217 (P17) → averageSpeedKmH ≈ 15.743564   (validSegments=25196)
```

---

## 6. Medir el experimento (entregable #4 del enunciado)

| Métrica | Cómo medir |
|---------|------------|
| Tiempo V3 con 1 worker | Apaga workers 2/3/4 en PC2, corre `RunR7`, anota `Time:` |
| Tiempo V3 con N workers | Levanta N workers, corre `RunR7`, anota `Time:` |
| Throughput de ingesta | Log final de `bus-simulator`: `throughput=Y/s` |
| CPU/RAM por nodo | `top -p $(pgrep -d',' -f 'edu.icesi.sitmmio')` |
| Tamaño del lake en disco | `du -sh /home/${USER}/lake` |
| Speedup | `t(V1) / t(V3)` y `t(V2) / t(V3)` |

V1 monolítica baseline = **14.5 s sobre MiniPilot** (8.1M filas, 560k rows/s, 77 MB RAM).

---

## 7. Apagar y limpiar entre pruebas

En cada máquina con servicios corriendo: `Ctrl+C` o:

```bash
pkill -f 'edu.icesi.sitmmio'
```

Limpiar estado durable entre experimentos:

```bash
# En PC1
rm -rf /home/${USER}/queue/store /home/${USER}/lake ~/sitm-mio/analytics-db
```

---

## 8. Troubleshooting

| Síntoma | Causa | Fix |
|---------|-------|-----|
| `Ice.ConnectionRefusedException` | Servicio no arrancó o IP/puerto mal | `nc -zv IP_PCx puerto`; revisar firewall |
| `[bus-simulator] could not reconnect to gateway` | IngestionGateway no escucha | Verificar Terminal 9 de PC1 |
| Workers no aparecen en log del master | `--master-proxy` con IP mal | Revisar IP en el flag |
| `slice2java not found` al hacer `./gradlew build` | `ICE_HOME` no apunta a la instalación | `export ICE_HOME=/usr` o donde esté |
| Tomcat no sirve los archivos | Permisos de `webapps/sitm-mio/` | `chmod -R 755 ${TOMCAT_WEBAPPS}/sitm-mio` |
| Workers no encuentran el lake | NFS no montado / ruta incorrecta | `mount`; `ls /mnt/pc1-lake` |
| `OutOfMemoryError` en batch-worker | Heap default insuficiente | Añadir `-Xmx2g` al comando `java` |
| Resultados V3 ≠ V1 | OutlierFilter / filtro `lineId=-1` distinto | Revisar `PartitionAggregator.aggregate` |
| Conflicto puerto 8080 | Tomcat ya lo usa | PublicAPI corre en `--http-port 9090` |

### Logs útiles

```bash
ps aux | grep edu.icesi.sitmmio                    # procesos vivos
df -h /home/${USER}/lake                            # espacio disponible
du -sh /home/${USER}/queue/store                    # tamaño cola
tail -f /opt/tomcat/logs/catalina.out               # Tomcat (PC1)
```

---

## 9. Checklist final antes del demo

- [ ] `IP_PC1`, `IP_PC2`, `IP_PC3` confirmadas con `ip addr` o `hostname -I`.
- [ ] Puertos 10000–10110 + 9090 abiertos entre las 3 máquinas (`nc -zv`).
- [ ] Tomcat de PC1 sirviendo `sitm-mio-v3-dist.tar.gz` y `datagrams-MiniPilot.csv`.
- [ ] PC2 y PC3 descargaron el tar y descomprimieron.
- [ ] PC1: los 10 servicios arrancan sin error.
- [ ] PC2: N workers registrados (log del master en PC1 confirma).
- [ ] PC3: `bus-simulator` envía datagramas y PC1 los recibe.
- [ ] Tras `DONE` del simulador, `RunR7` produce 110 reportes.
- [ ] Diff vs `monolith-results.csv` = 0 (módulo tolerancia 1e-4).

---

## 10. Scripts de atajo

### `start-pc1.sh` — arranca todo el DataCenter en `tmux`

```bash
#!/bin/bash
set -e
cd ~/sitm-mio
SESSION=sitm
HOST_IP=$(hostname -I | awk '{print $1}')

tmux new-session -d -s $SESSION

run() {
  local name=$1; local cmd=$2
  tmux new-window -t $SESSION -n "$name"
  tmux send-keys -t $SESSION:"$name" "$cmd" C-m
}

CP() { echo "nodes/$1/build/runtime-libs/*:nodes/$1/build/classes/java/main"; }

run "data-lake"  "java -cp '$(CP datacenter/data-lake)' edu.icesi.sitmmio.datalake.Main --port 10001 --store ~/lake"
run "queue"      "java -cp '$(CP queue/datagram-queue)' edu.icesi.sitmmio.datagramqueue.Main --port 10010 --store ~/queue/store"
run "auth"       "java -cp '$(CP server-admin/auth-service)' edu.icesi.sitmmio.authservice.Main --port 10040 --users admin/users.example.json --secret X --salt Y"
run "analytics"  "java -cp '$(CP datacenter/analytics-store)' edu.icesi.sitmmio.analyticsstore.Main --port 10060"
run "session"    "java -cp '$(CP datacenter/session-context)' edu.icesi.sitmmio.sessioncontext.Main --port 10030 --mapping admin/zone-mapping.example.json"
run "event-bus"  "java -cp '$(CP datacenter/datagram-event-bus)' edu.icesi.sitmmio.datagrameventbus.Main --port 10020"
sleep 3
run "stream"     "java -cp '$(CP datacenter/stream-processor)' edu.icesi.sitmmio.streamprocessor.Main --queue-proxy 'DatagramQueue:default -h $HOST_IP -p 10010' --bus-proxy 'DatagramEventBus:default -h $HOST_IP -p 10020' --session-proxy 'SessionContextController:default -h $HOST_IP -p 10030' --consumers 2"
run "master"     "java -cp '$(CP datacenter/batch-master)' edu.icesi.sitmmio.batchmaster.Main --port 10050 --routes data/raw/lines-241-ActiveGT.csv"
sleep 2
run "ingestion"  "java -cp '$(CP datacenter/ingestion-gateway)' edu.icesi.sitmmio.ingestiongateway.Main --routes data/raw/lines-241-ActiveGT.csv --queue-proxy 'DatagramQueue:default -h $HOST_IP -p 10010' --archive-proxy 'ArchiveService:default -h $HOST_IP -p 10001'"
run "public-api" "java -cp '$(CP server-public-api/public-api):nodes/server-public-api/public-api/build/resources/main' edu.icesi.sitmmio.publicapi.Main --http-port 9090 --auth-proxy 'AuthService:default -h $HOST_IP -p 10040' --reports-proxy 'ReportProvider:default -h $HOST_IP -p 10060'"

tmux attach-session -t $SESSION
```

Navegar entre ventanas: `Ctrl+B` luego número (0–9). Salir sin matar: `Ctrl+B` luego `d`. Matar todo: `tmux kill-session -t sitm`.

### `start-pc2-workers.sh` — N workers en PC2

```bash
#!/bin/bash
# Uso: bash start-pc2-workers.sh IP_PC1 [N=4]
IP_PC1=${1:?"uso: $0 IP_PC1 [N]"}
N=${2:-4}
cd ~/sitm-mio
tmux new-session -d -s workers

for i in $(seq 1 $N); do
  port=$((10100 + i))
  tmux new-window -t workers -n "w$i"
  tmux send-keys -t workers:"w$i" \
    "java -cp 'nodes/worker/batch-worker/build/runtime-libs/*:nodes/worker/batch-worker/build/classes/java/main' edu.icesi.sitmmio.batchworker.Main --worker-id w$i --port $port --lake ~/lake --routes data/raw/lines-241-ActiveGT.csv --master-proxy \"BatchMaster:default -h $IP_PC1 -p 10050\"" C-m
done

tmux attach-session -t workers
```

Uso: `bash start-pc2-workers.sh 10.0.0.10 4`

### `start-pc3-sim.sh` — bus simulator en PC3

```bash
#!/bin/bash
# Uso: bash start-pc3-sim.sh IP_PC1 [DATASET]
IP_PC1=${1:?"uso: $0 IP_PC1 [dataset]"}
DATASET=${2:-/home/${USER}/datagrams-MiniPilot.csv}
cd ~/sitm-mio

java -cp "nodes/bus/bus-simulator/build/runtime-libs/*:nodes/bus/bus-simulator/build/classes/java/main" \
  edu.icesi.sitmmio.bussimulator.Main \
  --host "$IP_PC1" --port 10000 \
  --dataset "$DATASET" \
  --throttle-ms 0 --rate-multiplier 1.0
```

---

## 11. Referencias

- **Comandos detallados localhost:** `README.md` §6.3
- **Arquitectura:** `docs/ARCHITECTURE_PLAN.md`
- **CRC + flujo end-to-end:** `docs/CRC&DeploymentDiagram.md`
- **Specs por componente:** `specs/00-project-scaffolding.md` … `specs/20-citizen-cli.md`
- **Oracle V1:** `data/output/monolith-results.csv` (110 filas, baseline 14.5 s sobre MiniPilot)
