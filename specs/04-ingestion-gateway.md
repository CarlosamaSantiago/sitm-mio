---
spec-id: 04-ingestion-gateway
component: IngestionGateway
deployment-node: DataCenter
status: draft
depends-on: [00, 01, 02]
---

# Spec 04 — `ingestion-gateway`

## 1. Contexto

Frontera de confianza: recibe del `GPRSTransmitter`, valida (R35.1), audita rechazos (R35.2), persiste crudo (R6.2) y encola (R6.1). Separa lo que el referente mezcla en `event-processor` (I-4).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `IngestionGateway` |
| Requisitos | R6.1, R35.1, R35.2, R30, RNF-4, RNF-9 |
| ADR | ADR-2, ADR-6 |
| Patrón | Producer (Reliable Messaging) |
| CRC | §2.2 |
| Inconsistencia | I-1, I-3, I-6 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `bus-simulator` | Cliente | `DatagramReceiver.postDatagram` |
| `datagram-queue` | Sink | `DatagramQueue.enqueueDatagram` |
| `data-lake` | Sink | `ArchiveService.archiveDatagram` (AMI) |
| `domain-core` | Lib | `DatagramValidator`, `RouteCsvReader` |

## 4. Interfaces (Slice)

Provee: `DatagramReceiver`. Consume: `DatagramQueue`, `ArchiveService`.

## 5. Modelo de datos relevante

Catálogo `Map<Integer lineId, Route>` cargado desde `lines-241-ActiveGT.csv`. Validador: `rawLat/rawLon != -1`, `busId > 0`, `tripId >= 0`, `lineId ∈ activeRoutes`, `datagramDate != null`.

## 6. Diseño

```
nodes/datacenter/ingestion-gateway/
├── build.gradle
└── src/main/java/edu/icesi/sitmmio/ingestiongateway/
    ├── Main.java
    ├── adapter/IngestionGatewayI.java       : DatagramReceiver
    ├── service/RejectAuditor.java
    ├── validation/                          REUSA DatagramValidator
    └── io/ClientFactory.java
```

### 6.1 `postDatagram`

```
1. SliceMapper.toRecord(slice)
2. validator.isValid(record, activeRoutes)
   ├─ true  → archiveService.archiveDatagramAsync(slice)   // R6.2
   │        → datagramQueue.enqueueDatagram(slice)          // R6.1
   └─ false → rejectAuditor.audit(slice, reason); throw InvalidDatagram
```

### 6.2 Bootstrap

`--routes` (default `data/raw/lines-241-ActiveGT.csv`). Endpoint en `ingestion-gateway.cfg`: `IngestionGatewayAdapter.Endpoints=default -p 10000`.

## 7. Aspectos distribuidos

- `postDatagram` síncrono → cliente sabe si fue rechazado.
- `archiveDatagram` AMI → no bloquea servant.
- `enqueueDatagram` síncrono → backpressure natural si la cola cae.
- Métricas: `accepted_total`, `rejected_total{reason}`, `enqueue_latency_ms`.

## 8. Criterios de aceptación

- [ ] Válido → llega a Queue Y a Archive.
- [ ] `InvalidDatagram` por cada categoría R35.
- [ ] Log estructurado por rechazo.
- [ ] Tras MiniPilot, `rejected/total ≈ 17%` (consistente con V1).
- [ ] Caída de Queue propaga error sin perder estado.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Validación duplicada con BatchWorker | Defensa en profundidad. |
| Pérdida si Queue y AMI fallan | Acuse explícito tras enqueue. |

## 10. Decisiones diferidas

- Schema validation JSON externo.
- Rate-limiting por bus.

## 11. Checklist

- [ ] Módulo + `build.gradle`.
- [ ] Servant `IngestionGatewayI` + `Main`.
- [ ] `RejectAuditor` con log estructurado.
- [ ] Cargador de rutas.
- [ ] Test integración con MiniPilot.

## 12. Post-mortem

A llenar tras implementación.
