---
spec-id: 05-datagram-queue
component: DatagramQueue + Store
deployment-node: Queue
status: draft
depends-on: [00, 02]
---

# Spec 05 — `datagram-queue`

## 1. Contexto

Cola persistente FIFO entre `IngestionGateway` y `StreamProcessor`. Garantía at-least-once (RNF-4). Implementación del patrón **Reliable Messaging** (ADR-2). Resuelve I-1.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `DatagramQueue` + Store |
| Requisitos | R6.1, R6.2, RNF-2, RNF-4, RNF-6 |
| ADR | ADR-2 |
| Patrón | Reliable Messaging |
| CRC | §2.3 |
| Inconsistencia | I-1 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `ingestion-gateway` | Productor | `enqueueDatagram` |
| `stream-processor` | Consumidor | `dequeueDatagram` |

## 4. Interfaces (Slice)

```slice
interface DatagramQueue {
  void enqueueDatagram(Datagram d);
  Datagram dequeueDatagram();
  long queueSize();
}
```

## 5. Modelo de datos relevante

Log append-only: cada registro = `Datagram` serializado + offset. Persistencia local file-backed.

## 6. Diseño

```
nodes/queue/datagram-queue/
├── build.gradle
└── src/main/java/edu/icesi/sitmmio/datagramqueue/
    ├── Main.java
    ├── adapter/DatagramQueueI.java   : DatagramQueue
    ├── service/AckTracker.java
    ├── domain/QueuePolicy.java        (FIFO, timeout)
    └── io/AppendOnlyStore.java
```

Storage: `queue/store/queue.log` (append) + `queue/store/consumer.offset`. `RandomAccessFile + FileLock`. `fsync` cada N=1000 o T=100 ms.

## 7. Aspectos distribuidos

- Durabilidad: `fsync` configurable.
- At-least-once → consumidores idempotentes.
- Backpressure por tamaño.
- Métricas: `enqueued_total`, `dequeued_total`, `queue_size`, `lag_ms`.

## 8. Criterios de aceptación

- [ ] 10 000 enqueue → 10 000 dequeue en orden FIFO.
- [ ] Matar consumidor a mitad y reiniciar → continúa desde offset.
- [ ] Matar productor → reiniciarlo no duplica.
- [ ] Throughput ≥ 100k msg/s en SSD local.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Log sin compactar | Rotación + vacuum tras ACK. |
| `fsync` por mensaje | Batch cada N o T ms. |
| Crash entre enqueue y fsync | Ventana documentada. |

## 10. Decisiones diferidas

- Reemplazo por Kafka/RabbitMQ.
- Sharding con múltiples consumidores.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `AppendOnlyStore` con offset.
- [ ] Tests FIFO, recovery, throughput.
- [ ] Métricas expuestas.

## 12. Post-mortem

A llenar tras implementación.
