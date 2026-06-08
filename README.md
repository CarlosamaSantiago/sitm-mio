# SITM-MIO — V3 Distribuida (ZeroC ICE)

Proyecto académico de Ingeniería de Software IV, Universidad Icesi.
Cálculo de **velocidades promedio por ruta y por ruta-mes** para el SITM-MIO de Cali, sobre la arquitectura distribuida especificada en `docs/Deployment.pdf`.

> **Rama actual:** `Distributed-ICE`
> **V1 monolítica (oracle):** rama `monolith-approach`

---

## 1. Contexto

El **SITM-MIO** moviliza ~450 000 pasajeros/día con ~1000 buses (proyección 2500) sobre ~100 rutas. Cada bus emite un *datagrama* GPS cada 20–30 s vía GPRS → **2.5–3 M eventos/día** que el Centro de Control de Operación (CCO) debe ingerir, procesar en tiempo real y analizar históricamente.

Esta versión cubre los **dos casos de uso primarios** del Dorfmann, ahora con **soporte multi-mes completo** (13 meses del dataset Pilot4):

| UC | Objetivo | Requisitos satisfechos |
|----|----------|------------------------|
| **R4** | Monitorear flota en tiempo real | R4, R6, R9, R10, R22, R23, R24, R30, R35 |
| **R7** | Velocidad promedio por ruta y mes | R7 (Multi-Month), R6, R10, R12, R14, R25, R27 |

Arquitectura **distribuida con tres patrones combinados**:

- **Reliable Messaging** — `DatagramQueue` persistente entre ingesta y procesamiento.
- **Master-Worker** — `BatchMaster` reparte particiones `(lineId, año-mes)` entre N `BatchWorker`.
- **Event-Driven Architecture** — `DatagramEventBus` publica posiciones RT + alertas críticas a clientes.

Detalle en `docs/ARCHITECTURE_PLAN.md` y `docs/CRC&DeploymentDiagram.md`.

---

## 2. Stack

- **JDK 17** (toolchain Gradle)
- **Gradle 8.6** (wrapper incluido)
- **ZeroC ICE 3.7.10** + plugin `com.zeroc.gradle.ice-builder.slice:1.5.2`
- **JavaFX 17** + **Leaflet 1.9.4** (cliente CCO)
- **JUnit 5** (testing)

### Prerrequisitos del sistema

```bash
# macOS (Homebrew)
brew install ice
export ICE_HOME=/opt/homebrew/opt/ice

# Linux
sudo apt install zeroc-ice-all-runtime zeroc-ice-all-dev
export ICE_HOME=/usr
```

El plugin Slice resuelve `iceHome` desde `$ICE_HOME` (default `/opt/homebrew/opt/ice` en macOS).

---

## 3. Estructura del proyecto

```
sitm-mio/
├── docs/                              Documentación de Fase 1 (congelada)
├── specs/                             Specs incrementales 00–20
├── data/
│   ├── raw/                           Datasets piloto (gitignored)
│   │   ├── lines-241-ActiveGT.csv     (Git — catálogo de rutas)
│   │   ├── datagrams-MiniPilot.csv    (8.1M filas)
│   │   └── datagrams4Pilot.csv        (~73M filas — 9× MiniPilot)
│   └── output/
│       ├── monolith-results.csv       Oracle V1 (correctness)
│       └── experiment-results.csv     Métricas comparativas
├── contracts/                         Slice + SliceMapper
├── domain-core/                       Dominio Java puro (Datagram, GeoPoint, ...)
└── nodes/                             1 módulo Gradle por nodo del Deployment
    ├── bus/bus-simulator/             GPRSTransmitter
    ├── queue/datagram-queue/          DatagramQueue (Reliable Messaging)
    ├── datacenter/
    │   ├── ingestion-gateway/         IngestionGateway (R35)
    │   ├── stream-processor/          StreamProcessor (R12+R32+R22+R23.1)
    │   ├── datagram-event-bus/        DatagramEventBus (EDA)
    │   ├── data-lake/                 DataLake (Cold storage)
    │   ├── batch-master/              BatchMaster (R7 orchestrator)
    │   ├── analytics-store/           AnalyticsStore (R7.4)
    │   ├── controller-cco/            (stub)
    │   └── session-context/           SessionContextController (R9.2)
    ├── worker/batch-worker/           BatchWorker {1..N}
    ├── server-admin/auth-service/     AuthService (JWT HS256)
    ├── server-public-api/public-api/  PublicAPI REST (R14)
    ├── client-cco/cco-client/         JavaFX + Leaflet (UC-R4)
    └── extern-client/citizen-cli/     Cliente HTTP externo (R14)
```

**17 módulos Gradle** (`contracts` + `domain-core` + 15 nodos).

---

## 4. Build

```bash
# Compilar TODO + tests JUnit 5
./gradlew build

# Solo compilar (sin tests)
./gradlew assemble

# Listar módulos
./gradlew projects

# Tests por módulo
./gradlew :domain-core:test
./gradlew :nodes:worker:batch-worker:test

# Copiar dependencias Ice a build/libs/ de todos los módulos
./gradlew copyLibs
```

**Output esperado de `./gradlew build`:**
```
BUILD SUCCESSFUL in ~6s
~80 actionable tasks
```

**Tests:** 45 tests verdes, 0 fallos.

---

## 5. Preparación de datos

```bash
mkdir -p data/raw

# Copiar desde /opt/sitm-mio/ (entrega de la universidad)
cp /opt/sitm-mio/lines-241-ActiveGT.csv  data/raw/
cp /opt/sitm-mio/datagrams-MiniPilot.zip data/raw/
cp /opt/sitm-mio/datagrams4Pilot.zip     data/raw/

# Descomprimir (el bus-simulator también lee ZIP directamente)
unzip -o data/raw/datagrams-MiniPilot.zip -d data/raw/
unzip -o data/raw/datagrams4Pilot.zip    -d data/raw/

# Verificar
wc -l data/raw/lines-241-ActiveGT.csv     # ~111
wc -l data/raw/datagrams-MiniPilot.csv    # ~8,145,463
```

> Los ZIPs y CSVs grandes están en `.gitignore`. El catálogo `lines-241-ActiveGT.csv` sí va a Git.

---

## 6. Arranque del sistema distribuido (V3)

Cada nodo es un proceso Ice independiente. **El orden importa**: los servicios proveedores deben estar arriba antes que sus clientes.

### 6.1 Orden recomendado de arranque

```
┌──────────────────┐  ┌────────────────────┐  ┌──────────────────┐
│ (1) data-lake    │  │ (2) datagram-queue │  │ (3) auth-service │
│     :10001       │  │     :10010         │  │     :10040       │
└──────────────────┘  └────────────────────┘  └──────────────────┘
         ↓                      ↓                       ↓
┌──────────────────────────────────────────────────────────────────┐
│ (4) analytics-store :10060   (5) session-context :10030          │
│ (6) datagram-event-bus :10020                                    │
└──────────────────────────────────────────────────────────────────┘
         ↓
┌──────────────────────────────────────────────────────────────────┐
│ (7) stream-processor  →  consume queue → publica al event-bus    │
│ (8) batch-master :10050                                          │
│ (9) ingestion-gateway :10000 → escribe a queue + data-lake       │
│ (10) batch-worker × N :10100+ → se auto-registran al master      │
│ (11) public-api :8080         → REST + JWT                       │
└──────────────────────────────────────────────────────────────────┘
         ↓
┌──────────────────────────────────────────────────────────────────┐
│ (12) cco-client (JavaFX)  (13) bus-simulator → emite el dataset  │
│ (14) citizen-cli — consultas REST externas                       │
└──────────────────────────────────────────────────────────────────┘
```

### 6.2 Pre-requisito: copiar Ice/JavaFX a `build/libs/` de cada módulo

```bash
./gradlew copyLibs
```

### 6.3 Comandos por nodo (cada uno en su propia terminal)

```bash
# (1) DataLake — Cold storage particionado año-mes
java -cp "nodes/datacenter/data-lake/build/runtime-libs/*:nodes/datacenter/data-lake/build/classes/java/main" \
  edu.icesi.sitmmio.datalake.Main --port 10001 --store lake

# (2) DatagramQueue — Reliable Messaging
java -cp "nodes/queue/datagram-queue/build/runtime-libs/*:nodes/queue/datagram-queue/build/classes/java/main" \
  edu.icesi.sitmmio.datagramqueue.Main --port 10010 --store queue/store

# (3) AuthService — JWT HS256
java -cp "nodes/server-admin/auth-service/build/runtime-libs/*:nodes/server-admin/auth-service/build/classes/java/main" \
  edu.icesi.sitmmio.authservice.Main \
  --port 10040 --users admin/users.example.json \
  --secret change-me --salt sitm-salt

# (4) AnalyticsStore — resultados R7
java -cp "nodes/datacenter/analytics-store/build/runtime-libs/*:nodes/datacenter/analytics-store/build/classes/java/main" \
  edu.icesi.sitmmio.analyticsstore.Main --port 10060

# (5) SessionContextController — zonas (R9.2)
java -cp "nodes/datacenter/session-context/build/runtime-libs/*:nodes/datacenter/session-context/build/classes/java/main" \
  edu.icesi.sitmmio.sessioncontext.Main \
  --port 10030 --mapping admin/zone-mapping.example.json

# (6) DatagramEventBus — pub/sub
java -cp "nodes/datacenter/datagram-event-bus/build/runtime-libs/*:nodes/datacenter/datagram-event-bus/build/classes/java/main" \
  edu.icesi.sitmmio.datagrameventbus.Main --port 10020

# (7) StreamProcessor — consume queue, publica al bus
java -cp "nodes/datacenter/stream-processor/build/runtime-libs/*:nodes/datacenter/stream-processor/build/classes/java/main" \
  edu.icesi.sitmmio.streamprocessor.Main \
  --queue-proxy   "DatagramQueue:default -h 127.0.0.1 -p 10010" \
  --bus-proxy     "DatagramEventBus:default -h 127.0.0.1 -p 10020" \
  --session-proxy "SessionContextController:default -h 127.0.0.1 -p 10030" \
  --consumers 2

# (8) BatchMaster — orquestador R7
java -cp "nodes/datacenter/batch-master/build/runtime-libs/*:nodes/datacenter/batch-master/build/classes/java/main" \
  edu.icesi.sitmmio.batchmaster.Main \
  --port 10050 --routes data/raw/lines-241-ActiveGT.csv --timeout-s 120

# (9) IngestionGateway — frontera de confianza
java -cp "nodes/datacenter/ingestion-gateway/build/runtime-libs/*:nodes/datacenter/ingestion-gateway/build/classes/java/main" \
  edu.icesi.sitmmio.ingestiongateway.Main \
  --routes data/raw/lines-241-ActiveGT.csv \
  --queue-proxy   "DatagramQueue:default -h 127.0.0.1 -p 10010" \
  --archive-proxy "ArchiveService:default -h 127.0.0.1 -p 10001"

# (10) BatchWorker — uno o más (cada uno con id y port único)
java -cp "nodes/worker/batch-worker/build/runtime-libs/*:nodes/worker/batch-worker/build/classes/java/main" \
  edu.icesi.sitmmio.batchworker.Main \
  --worker-id w1 --port 10101 --lake lake \
  --routes data/raw/lines-241-ActiveGT.csv \
  --master-proxy "BatchMaster:default -h 127.0.0.1 -p 10050"

# Arranca N=4 workers en 4 terminales (cambiando id y puerto):
#   w1 :10101    w2 :10102    w3 :10103    w4 :10104

# (11) PublicAPI — REST + JWT + OpenAPI
java -cp "nodes/server-public-api/public-api/build/runtime-libs/*:nodes/server-public-api/public-api/build/classes/java/main:nodes/server-public-api/public-api/build/resources/main" \
  edu.icesi.sitmmio.publicapi.Main \
  --http-port 8080 \
  --auth-proxy    "AuthService:default -h 127.0.0.1 -p 10040" \
  --reports-proxy "ReportProvider:default -h 127.0.0.1 -p 10060"

# (12) CCO Client — JavaFX + Leaflet (UC-R4)
java --module-path "$PATH_TO_OPENJFX/lib" \
     --add-modules javafx.controls,javafx.web \
     -cp "nodes/client-cco/cco-client/build/runtime-libs/*:nodes/client-cco/cco-client/build/classes/java/main:nodes/client-cco/cco-client/build/resources/main" \
     edu.icesi.sitmmio.ccoclient.Main

# (13) Bus Simulator — emite el dataset por GPRS simulado
java -cp "nodes/bus/bus-simulator/build/runtime-libs/*:nodes/bus/bus-simulator/build/classes/java/main" \
  edu.icesi.sitmmio.bussimulator.Main \
  --host 127.0.0.1 --port 10000 \
  --dataset data/raw/datagrams-MiniPilot.csv \
  --throttle-ms 0 --rate-multiplier 1.0

# (14) Citizen CLI — consultas REST externas
java -cp "nodes/extern-client/citizen-cli/build/runtime-libs/*:nodes/extern-client/citizen-cli/build/classes/java/main" \
  edu.icesi.sitmmio.citizencli.Main health

java -cp "nodes/extern-client/citizen-cli/build/runtime-libs/*:nodes/extern-client/citizen-cli/build/classes/java/main" \
  edu.icesi.sitmmio.citizencli.Main speed --lineId 131 --year 2019 --month 5
```

### 6.4 Variables de entorno

```bash
export ICE_HOME=/opt/homebrew/opt/ice          # macOS Homebrew
export SITMMIO_API=http://127.0.0.1:8080       # citizen-cli
export PATH_TO_OPENJFX=/path/to/javafx-sdk-17  # solo cco-client
```

### 6.5 Secretos y configuración

- `admin/users.example.json` — plantilla; copiar a `admin/users.json` (gitignored) y rellenar.
- `admin/zone-mapping.example.json` — plantilla zonas (gitignored).
- Secreto JWT: pasar por `--secret` o config; **nunca commitear**.

---

## 7. Outputs esperados durante un run E2E

| Componente | Log clave |
|------------|-----------|
| `data-lake` | `[data-lake] listening on 10001` |
| `datagram-queue` | `[datagram-queue] listening on 10010 store=queue/store` |
| `ingestion-gateway` | `[ingestion-gateway] loaded 110 active routes`<br>`REJECT busId=X lineId=Y reason=LINEID_NOT_ACTIVE` (R35.2) |
| `bus-simulator` | `[bus-simulator] sent=10000 skipped=0 errors=0`<br>Final: `DONE sent=6744428 skipped=1401034 errors=0 elapsed=Xs throughput=Y/s` |
| `stream-processor` | `[stream-processor] running consumers=2` |
| `batch-master` | `[batch-master] active routes=110`<br>`[batch-master] worker registered. total=4` |
| `batch-worker` | `[batch-worker] id=w1 listening on 10101`<br>`[batch-worker] registered with master` |
| `analytics-store` | `[analytics-store] listening on 10060` |
| `public-api` | `[public-api] HTTP on 8080` |
| `cco-client` | Ventana JavaFX con mapa Leaflet + marcadores moviéndose |

### Archivos / directorios generados en disco

```
queue/store/queue.log                                cola persistente FIFO
queue/store/consumer.offset                          offset del consumidor
lake/lineId=<n>/year=<yyyy>/month=<mm>/part-0.csv    particiones del data-lake
analytics-db/speed-reports.csv                       resultados R7.4 (formato == monolith-results.csv)
~/.sitmmio/token                                     JWT cacheado por citizen-cli
```

---

## 8. Experimento R7 — V1 vs V3

### 8.1 Disparar el cálculo distribuido

Una vez la ingesta haya llenado el lake (o tras ingesta completa), invocar al master. Opciones:

- **A.** Usar el script `run-r7.sh` (recomendado):
  ```bash
  ./run-r7.sh --all-months        # Rango completo 2018-05..2019-05
  ./run-r7.sh 2019 5              # Solo mayo 2019
  ./run-r7.sh --range 2018 5 2018 12
  ```
- **B.** Cliente Ice ad-hoc en Java que llame `BatchMaster.runRange(yf, mf, yt, mt)`.

### 8.2 Verificación de correctness vs V1

Oracle: `data/output/monolith-results.csv` (110 filas).
Columnas: `lineId, shortName, description, yearMonth, totalDistanceKm, totalTimeHours, averageSpeedKmH, validSegments, skippedSegments, status`.

```bash
# Comparar (tolerancia 1e-4 en averageSpeedKmH)
diff <(sort data/output/monolith-results.csv) \
     <(sort analytics-db/speed-reports.csv)
```

**Valores oracle clave (V1, MiniPilot, 2019-05):**

| lineId | shortName | averageSpeedKmH | validSegments |
|--------|-----------|-----------------|---------------|
| 131 | T31 | **17.375445** | 206 566 |
| 140 | T40 | 15.454446 | 105 358 |
| 142 | T42 | 17.273641 | 156 536 |
| 217 | P17 | 15.743564 | 25 196 |

### 8.3 Métricas a registrar

| Variable | Métrica |
|----------|---------|
| Versión | V1 (mono), V2 (concurrente), V3 (distribuida) |
| Dataset | `datagrams-MiniPilot.csv` (8.1M) vs `datagrams4Pilot.csv` (~73M) |
| Workers V3 | `N ∈ {1, 2, 4, 8, 16}` |
| Tiempo total | desde primer datagrama hasta último `SpeedReport` persistido |
| Throughput | filas procesadas / segundo |
| Speedup | t(V1)/t(V3) y t(V2)/t(V3) |
| Punto de cruce | menor N o tamaño donde V3 supera V2 |

**V1 monolítica reporta como baseline:** 14.5 s sobre MiniPilot (560k rows/s, 77 MB RAM).

---

## 9. Consultas REST (UC-R7 expuesto al ciudadano)

```bash
# Health
curl http://127.0.0.1:8080/api/v1/health
# {"status":"OK"}

# OpenAPI
curl http://127.0.0.1:8080/api/v1/openapi.yaml -o openapi.yaml

# Velocidad ruta 131 mayo 2019 (requiere JWT)
TOKEN=...   # obtener vía AuthService.login
curl -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8080/api/v1/speeds/131?year=2019&month=5"
# {"lineId":131,"shortName":"T31","yearMonth":"2019-05",
#  "averageSpeedKmH":17.375445,...,"status":"OK"}

# Todas las rutas en un mes
curl -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8080/api/v1/speeds?year=2019&month=5"

# Rango
curl -H "Authorization: Bearer $TOKEN" \
  "http://127.0.0.1:8080/api/v1/speeds/range?yf=2019&mf=4&yt=2019&mt=5"
```

Códigos HTTP:
- `200` — éxito
- `401` — sin/mal token
- `404` — `NoDataForPartition`
- `429` — rate limit excedido por perfil de consumidor (ANONYMOUS=10/min, PARTNER=100/min, GOV=∞)

---

## 10. Apagar y reiniciar

`Ctrl+C` en cada terminal. La `DatagramQueue` y el `DataLake` son durables: al reiniciar continúan desde donde quedaron.

Para limpiar el estado runtime entre experimentos:
```bash
rm -rf queue/store lake analytics-db
```

---

## 11. Referencias

- **Documentación arquitectónica:** `docs/`
- **Specs incrementales (post-mortem checklists):** `specs/`
- **V1 monolítica (oracle):** rama `monolith-approach`
- **Enunciado del piloto:** `docs/ISW4-ProyFinal-EnunciadoBase.pdf`
- **Deployment Diagram (fuente de verdad):** `docs/Deployment.pdf`
- **Diccionario de datos:** `docs/Diccionario_De_Datos-OkGTM.pdf`

---

## 12. Créditos

Trabajo académico — Universidad Icesi, Departamento de Computación y Sistemas Inteligentes.
Curso: Ingeniería de Software IV.
