---
spec-id: 07-stream-processor
component: StreamProcessor
deployment-node: DataCenter
status: draft
depends-on: [00, 01, 02, 05, 08]
---

# Spec 07 — `stream-processor`

## 1. Contexto

Consume `DatagramQueue`, extrae tupla (R12), clasifica (R32), detecta pérdida de señal (R22), construye estado del bus (R23.1) y publica al `DatagramEventBus`. Resuelve I-4.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `StreamProcessor` |
| Requisitos | R12.1–R12.4, R32, R22.1, R22.2, R23.1, R9.2 |
| ADR | ADR-4 |
| Patrón | Consumidor + EDA publisher |
| CRC | §2.4 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `datagram-queue` | Fuente | `dequeueDatagram` |
| `datagram-event-bus` | Sink | `publishUpdate`, `publishAlert` |
| `session-context` | Lookup | `zoneOfLine` |
| `domain-core` | Lib | `Datagram`, `GeoPoint` |

## 4. Interfaces (Slice)

Sin servant expuesto. Cliente de Queue + publisher al EventBus.

## 5. Modelo de datos

Input: `Datagram` Slice. Output: `BusUpdate` + opcional `CriticAlert`.

Estado (R23.1): `EN_RUTA | PARADO | SIN_SENAL | CRITICO`.
Clasificación (R32): rutinario vs excepcional según `eventType`.

## 6. Diseño

```
nodes/datacenter/stream-processor/
├── build.gradle
└── src/main/java/edu/icesi/sitmmio/streamprocessor/
    ├── Main.java
    ├── adapter/QueueConsumer.java
    ├── adapter/EventBusPublisher.java
    ├── service/
    │   ├── DatagramExtractor.java       R12
    │   ├── EventClassifier.java         R32
    │   ├── SignalMonitor.java           R22
    │   └── BusStateBuilder.java         R23.1
    └── io/SessionContextClient.java
```

Loop:
```
while running:
  d = queue.dequeueDatagram()
  record = SliceMapper.toRecord(d)
  category = classifier.classify(record)
  state = stateBuilder.update(record, prev)
  zone = sessionCtx.zoneOfLine(record.lineId())
  update = BusUpdate(busId, point, lineId, ts, zone, state)
  eventBus.publishUpdate(update)
  if category == EXCEPTIONAL: eventBus.publishAlert(buildAlert)
```

## 7. Aspectos distribuidos

- Throughput objetivo cubre 2.5–3 M/día → ~35 msg/s sostenido (holgado).
- Pool consumidor `--consumers=N` (default 4).
- Idempotencia: tolerar reentrega de la cola.
- Métricas: `consumed_total`, `published_total`, `alerts_total`, `signal_loss_total`, `consumer_lag`.

## 8. Criterios de aceptación

- [ ] MiniPilot → N updates = válidos (~6.7 M).
- [ ] `SIGNAL_LOSS` para buses > 60 s sin reporte.
- [ ] Estado coherente en primeros 5 buses.
- [ ] Recuperación tras reinicio desde offset.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Estado en memoria pierde tras reinicio | Persistir opcional (no requerido en piloto). |
| Reentrega → doble alerta | Dedupe `(busId, timestamp)` en EventBus. |

## 10. Decisiones diferidas

- Mini-batches para throughput.
- Estado en Redis.

## 11. Checklist

- [ ] Módulo + Main.
- [ ] `QueueConsumer` con pool.
- [ ] 4 servicios + Publisher.
- [ ] Tests unit por servicio.
- [ ] Integración tiny → EventBus mock.

## 12. Post-mortem

A llenar tras implementación.
