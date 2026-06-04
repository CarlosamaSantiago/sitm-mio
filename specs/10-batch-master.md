---
spec-id: 10-batch-master
component: BatchMaster
deployment-node: DataCenter
status: draft
depends-on: [00, 01, 02, 06, 11, 12]
---

# Spec 10 — `batch-master`

## 1. Contexto

Orquesta R7: particiona, asigna, consolida, persiste. **Master** del patrón Master-Worker (ADR-3). Resuelve I-2.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `BatchMaster` |
| Requisitos | R7.1, R7.4, R25 |
| ADR | ADR-3, ADR-8, ADR-9 |
| Patrón | Master-Worker |
| CRC | §2.7 |
| Inconsistencia | I-2 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `batch-worker` | Tareas | `IBatchWorker.computePartition` |
| `data-lake` | Planning | `LakeReader.listPartitions` |
| `analytics-store` | Persist | `ReportProvider.save` |
| `domain-core` | Lib | `RouteCsvReader`, `RouteMonthKey` |

## 4. Interfaces (Slice)

```slice
interface BatchMaster {
  void registerWorker(IBatchWorker* w);
  SpeedReportSeq runMonth(int year, int month);
  SpeedReportSeq runRange(int yf, int mf, int yt, int mt);
};
```

## 5. Modelo de datos

`Map<PartitionKey, IBatchWorkerPrx>` asignaciones; `List<IBatchWorkerPrx>` pool.

## 6. Diseño

```
nodes/datacenter/batch-master/
└── src/main/java/edu/icesi/sitmmio/batchmaster/
    ├── Main.java
    ├── adapter/BatchMasterI.java
    ├── service/
    │   ├── Partitioner.java
    │   ├── Scheduler.java
    │   ├── Consolidator.java
    │   └── WorkerRegistry.java
    └── io/AnalyticsStoreClient.java
```

`runMonth(y, m)`:
```
1. partitions = activeRoutes × {(y,m)}
2. latch = CountDownLatch(partitions.size())
3. for each p: worker = scheduler.pick()
   async worker.computePartition(p) → consolidator.add
                                catch → scheduler.reassign(p)
                                finally → latch.countDown
4. latch.await(timeout)
5. analyticsStore.saveAll(consolidator.snapshot())
```

Timeout `--partition-timeout-s=120`. Retries por partición = 3. Worker N fallos → `unhealthy`.

## 7. Aspectos distribuidos

- Workers se registran dinámicamente.
- Particiones independientes; orden libre.
- Idempotente: misma data → mismo `SpeedReportSeq`.
- Métricas: `partitions_total`, `_completed`, `_reassigned`, `workers_active`, `run_duration_ms`.

## 8. Criterios de aceptación

- [ ] Con 1 worker, `runMonth(2019, 5)` → 110 `SpeedReport` matching `monolith-results.csv`.
- [ ] Con N=4, mismo resultado, menor tiempo.
- [ ] Matar 1 worker durante → recovery + resultado correcto.
- [ ] Diff vs `monolith-results.csv` ≈ 0 (tolerancia 1e-4).

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Worker lento bloquea finalización | Reasignar por timeout. |
| Skew entre rutas | Asignación dinámica work-stealing. |
| Master SPOF | Aceptable piloto. |

## 10. Decisiones diferidas

- HA del Master.
- Persistir scheduler state.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] 4 servicios.
- [ ] Tests completitud, recovery, idempotencia.
- [ ] Métricas.

## 12. Post-mortem

A llenar tras implementación.
