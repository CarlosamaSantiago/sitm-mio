---
spec-id: 11-batch-worker
component: BatchWorker
deployment-node: Worker {1..N}
status: draft
depends-on: [00, 01, 02, 06]
---

# Spec 11 — `batch-worker`

## 1. Contexto

Worker del patrón Master-Worker (ADR-3). Recibe `PartitionKey`, lee DataLake, calcula velocidad, retorna `SpeedReport`. **Reusa `domain-core`** → correctness por construcción.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `BatchWorker` |
| Requisitos | R7.2, R7.3 |
| ADR | ADR-3, ADR-8 |
| Patrón | Master-Worker |
| CRC | §2.8 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `batch-master` | Recibe | `IBatchWorker.computePartition` |
| `data-lake` | Lee | `LakeReader.streamPartition` |
| `domain-core` | Lib | `DistanceCalculator`, `SpeedAccumulator` |

## 4. Interfaces (Slice)

```slice
interface IBatchWorker {
  SpeedReport computePartition(PartitionKey k) throws NoDataForPartition;
  WorkerMetrics lastMetrics();
  string workerId();
};
```

## 5. Modelo de datos

In: `PartitionKey(lineId, year, month)`. Out: `SpeedReport`.

## 6. Diseño

```
nodes/worker/batch-worker/
└── src/main/java/edu/icesi/sitmmio/batchworker/
    ├── Main.java
    ├── adapter/BatchWorkerI.java
    ├── service/
    │   ├── PartitionLoader.java
    │   ├── SegmentBuilder.java
    │   ├── OutlierFilter.java
    │   └── Aggregator.java
    └── io/LakeReaderClient.java
```

`computePartition(k)`:
```
stream = lake.streamPartition(k) ; if empty → NoDataForPartition
groupBy busId; sort por (busId, datagramDate)
for each pair (a, b):
  dist = Haversine(a.point, b.point)
  dt   = b.ts - a.ts
  speed = dist / hours(dt)
  if OutlierFilter.accept(speed, dist, dt): accumulator.add(dist, dt); valid++
  else skipped++
avg = totalDistKm / totalTimeHours
status = valid > 0 ? "OK" : "NO_DATA"
return SpeedReport(...)
```

OutlierFilter: `speed > 80`, `dt > 60 min`, `dist > 5 km en < 30 s`.

## 7. Aspectos distribuidos

- Stateless → idempotente.
- Args: `--master-host`, `--master-port`, `--worker-id`, `--lake-dir`.
- Al arrancar: `master.registerWorker(self)`.
- Múltiples workers por nodo posibles.
- Métricas via `lastMetrics()`.

## 8. Criterios de aceptación

- [ ] `computePartition((131, 2019, 5))` → `avg ≈ 17.375 km/h` ± 1e-3 (oracle V1).
- [ ] `computePartition((-1, 2019, 5))` → `avg ≈ 0.0768` (oracle V1 TESTGT1).
- [ ] `computePartition((9999, …))` → `NoDataForPartition`.
- [ ] 1000 ejecuciones consecutivas → mismo valor.
- [ ] Métricas no nulas.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Memory por partición | Stream lazy + reducir incremental. |
| Skew | Master usa work-stealing. |
| Divergencia con V1 | Test de equivalencia obligatorio. |

## 10. Decisiones diferidas

- Sub-particionamiento por bus.
- Reducción cross-worker.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] 4 servicios.
- [ ] Tests vs monolith para todas las rutas.
- [ ] Smoke registro contra master.

## 12. Post-mortem

A llenar tras implementación.
