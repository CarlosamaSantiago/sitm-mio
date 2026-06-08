---
spec-id: 08-datagram-event-bus
component: DatagramEventBus
deployment-node: DataCenter
status: draft
depends-on: [00, 02, 14]
---

# Spec 08 — `datagram-event-bus`

## 1. Contexto

Broker pub-sub. Recibe del StreamProcessor; entrega a `RTStreamClient` y `AlertPanel` con filtrado por zona (R9.2). Formaliza el pub-sub que el referente hace implícito.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `DatagramEventBus` |
| Requisitos | R4.1, R9.2, R9.3, R19.2, R19.3, R22.2 |
| ADR | ADR-4 |
| Patrón | EDA Pub-Sub |
| CRC | §2.5 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `stream-processor` | Publisher | `publishUpdate`, `publishAlert` |
| `rt-stream-client` | Subscriber | `updateLocation` |
| `alert-panel` | Subscriber | `onCriticAlert` |

## 4. Interfaces (Slice)

```slice
interface DatagramEventBus {
  void publishUpdate(BusUpdate u);
  void publishAlert(CriticAlert a);
  void subscribe(MonitoringSubscriber* sub, int zoneId);
  void unsubscribe(MonitoringSubscriber* sub);
};
```

## 5. Modelo de datos

`Map<MonitoringSubscriberPrx, Set<Integer zoneId>>`. Zona `0` = administrador (todas).

## 6. Diseño

```
nodes/datacenter/datagram-event-bus/
└── src/main/java/edu/icesi/sitmmio/datagrameventbus/
    ├── Main.java
    ├── adapter/DatagramEventBusI.java
    ├── domain/SubscriberRegistry.java
    └── service/Dispatcher.java
```

`publishUpdate(u)`: para cada `(sub, zones)`, si `zones.contains(u.zoneId) || zones.contains(0)` → `sub.updateLocationAsync(u)`.
`publishAlert(a)`: igual; alertas `priority=="ALTA"` ignoran filtro (broadcast — R19.3).

## 7. Aspectos distribuidos

- AMI siempre.
- Auto-limpieza ante `ConnectionLost`.
- Fan-out paralelo con `ExecutorService`.
- Sin durabilidad (live-only).
- Métricas: `subscribers_total`, `published_updates`, `published_alerts`, `delivery_failures`.

## 8. Criterios de aceptación

- [ ] N subs reciben N×M updates al publicar M.
- [ ] Filtrado por zona correcto.
- [ ] Sub desconectado se remueve.
- [ ] Alerta crítica llega a todos.
- [ ] Latencia publish→entrega < 100 ms p95 local.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Sub lento bloquea otros | AMI + thread pool. |
| Memoria con muchos subs | Bounded pool + backpressure. |
| Pérdida en desconexión | Documentado; reanuda al reconectarse. |

## 10. Decisiones diferidas

- Persistencia (Kafka-like).
- Sub-buses por zona.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `SubscriberRegistry` thread-safe.
- [ ] `Dispatcher` AMI + Executor.
- [ ] Test filtrado + broadcast crítico.

## 12. Post-mortem

A llenar tras implementación.
