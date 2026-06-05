# Soporte multi-mes en V3

## Contexto

El dataset `datagrams4Pilot.csv` (Pilot4) NO contiene únicamente mayo de 2019.
Contiene los siguientes 13 meses de datagramas reales del SITM-MIO:

| Año  | Meses |
|------|-------|
| 2018 | 05, 06, 07, 08, 09, 10, 11, 12 |
| 2019 | 01, 02, 03, 04, 05 |

Durante la implementación inicial de V3 (commit `355257a` en `Distributed-ICE`),
varios componentes quedaron hardcoded a `year=2019, month=5`. Esto producía
diffs masivos contra cualquier oracle V1 que se generara sobre Pilot4 completo,
porque V3 sólo procesaba 1/13 del dataset.

## Componentes corregidos

| Archivo | Cambio |
|---------|--------|
| `run-r7.sh` | Soporta `--all-months` (default), `--range Y M Y M`, y modo single-month `Y M`. Compila el cliente Ice (`RunR7`) que llama `runRange` o `runMonth` del `BatchMaster`. |
| `scripts/cluster-bulk.sh` | El step `run-r7` ya no hardcodea `2019 5`. Usa `${SITM_R7_ARGS:---all-months}`. Para correr un solo mes en el cluster: `SITM_R7_ARGS='2019 5' ./scripts/cluster-bulk.sh compute`. |
| `nodes/client-cco/cco-client/.../Main.java` | Removidas constantes `YEAR_R7` y `MONTH_R7`. Ahora son **fallbacks** configurables por args (`--year-fallback`, `--month-fallback`) o env vars (`SITM_R7_YEAR`, `SITM_R7_MONTH`). |
| `nodes/client-cco/.../MonitoringSubscriberI.java` | El cache de velocidades cambió de `Map<lineId, Double>` a `Map<(lineId, year, month), Double>`. El `yearMonth` se infiere del `timestamp` del `BusUpdate`; sólo cae al fallback si el timestamp no es parseable. |
| `BatchMaster.runRange(yf, mf, yt, mt)` | Ya existía en el Slice y la implementación itera correctamente mes a mes. Sin cambios algorítmicos requeridos. |
| `BatchWorker.computePartition(PartitionKey)` | Recibe `(lineId, year, month)` por parámetro. Sin hardcode. `PartitionAggregator` filtra por `lineId == key.lineId()` y consume el stream del `LakeReader` que ya filtra por `year/month`. |
| Diff contra oracle | El `run-r7.sh` compara por llave `(lineId, yearMonth)` ordenando por las columnas 1 y 4 del CSV. Soporta múltiples meses. |

## Cómo correr todo el rango Pilot4

### Local (Mac, MiniPilot)

```bash
# Default: rango 2018-05..2019-05
./run-r7.sh

# Mismo efecto, explícito
./run-r7.sh --all-months

# Un solo mes
./run-r7.sh 2019 5

# Rango personalizado
./run-r7.sh --range 2018 5 2018 12
```

### Cluster (sala 20 equipos)

```bash
# Default: todo el rango
./scripts/cluster-bulk.sh compute

# Forzar un mes específico (sobreescribe el default)
SITM_R7_ARGS='2019 5' ./scripts/cluster-bulk.sh compute

# Forzar otro rango
SITM_R7_ARGS='--range 2018 5 2018 12' ./scripts/cluster-bulk.sh compute
```

## Configuración del cco-client multi-mes

El `cco-client` (mapa Leaflet) ahora consulta la velocidad de la ruta del bus
usando el `yearMonth` derivado del `timestamp` del `BusUpdate` que viene del
`StreamProcessor`. Esto garantiza que un bus emitido en agosto 2018 muestre la
velocidad de su ruta para agosto 2018, no la fija de mayo 2019.

Si el timestamp del datagrama no es parseable (formato inesperado), el cliente
cae a un fallback configurable:

```bash
# Forzar fallback a julio 2018 si el timestamp no parsea
SITM_R7_YEAR=2018 SITM_R7_MONTH=7 ./scripts/view-map.sh 192.168.131.103
```

O por argumentos:

```bash
./gradlew :nodes:client-cco:cco-client:run \
    --args="--event-bus-host=192.168.131.103 --year-fallback=2018 --month-fallback=7"
```

## Oracle V1 multi-mes

**IMPORTANTE**: el oracle `data/output/monolith-results.csv` que se encuentra
en el repo fue generado únicamente para `2019-05` sobre MiniPilot. NO sirve
como oracle de Pilot4 multi-mes.

Para validar V3-Bulk sobre Pilot4 multi-mes hay que generar el oracle de V1
sobre el mismo dataset y mismo rango. Pasos:

1. En la rama `monolith-approach`, asegurar que la V1 soporta todos los meses
   del dataset (no debería tener filtro hardcoded por mes — algorítmicamente
   procesa todos los datagramas válidos).
2. Correr V1 sobre `datagrams4Pilot.csv` con `lines-241-ActiveGT.csv`.
3. El output `monolith-results.csv` debe contener `110 rutas × 13 meses = 1430`
   filas (excluyendo `NO_DATA`).
4. Copiar ese oracle a `data/output/monolith-results.csv` y comparar con `diff`.

Hasta que ese oracle esté disponible, V3-Bulk Pilot4 sólo se puede validar por
**consistencia interna** (mismos resultados entre N=1, N=4, N=19) y por
verificación spot-check de rutas conocidas como T31 contra el oracle de mayo
2019.

## Compatibilidad

- Java 17, Gradle 8.6, ZeroC Ice 3.7.10 — sin cambios.
- Sin dependencias nuevas.
- API Slice (`runMonth`, `runRange`) sin cambios.
- Compatible con el path RT (R4): el cco-client sigue consumiendo el event-bus
  igual que antes; solo cambió cómo deriva el `yearMonth` para consultar
  velocidades.

## Verificación

```bash
# 1. Build verde
./gradlew build

# 2. Correr local con MiniPilot
./scripts/start-full.sh 4 data/raw/datagrams-MiniPilot.csv

# Esperado en MiniPilot (que SÍ es solo 2019-05):
# [V3] runRange(2018-05 .. 2019-05)
# [V3] Reportes: 1430 (OK=110, NO_DATA=1320)
#                       ↑      ↑
#                       solo  los otros 12 meses sin datos en MiniPilot
#                       mayo
# Diff vs V1 oracle (data/output/monolith-results.csv, solo 2019-05):
#    V3 incluye los 12 meses NO_DATA además del oracle → diff ≠ 0 esperable.
#    Verificar manualmente que las 110 filas de 2019-05 SÍ coinciden con V1.
```
