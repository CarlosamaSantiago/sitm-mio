---
spec-id: 16-rt-stream-client
component: RTStreamClient
deployment-node: ClientCCO
status: draft
depends-on: [00, 02, 08, 09, 15]
blocks: [17]
---

# Spec 16 — `rt-stream-client`

## 1. Contexto

Subscriber Ice del `DatagramEventBus`; reenvía `BusUpdate` al WebView Leaflet vía `updateBus(...)`. Adapta el `MonitoringSubscriberI` del referente con filtro de zona (R9.3) y autenticación JWT.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `RTStreamClient` |
| Requisitos | R4.1, R9.3 |
| ADR | ADR-4, ADR-7 |
| Patrón | EDA Subscriber + JS bridge |
| CRC | §2.16 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `datagram-event-bus` | Publisher | `MonitoringSubscriber.*` |
| `auth-service` | Cliente | `login`, `validate` |
| `map-view` | Sink JS | `executeScript("updateBus(...)")` |

## 4. Interfaces (Slice)

Implementa `MonitoringSubscriber`:
```slice
interface MonitoringSubscriber {
  void updateLocation(BusUpdate u);
  void updateLocations(BusUpdateSeq us);
  void onCriticAlert(CriticAlert a);   // delegado a alert-panel
};
```

## 5. Modelo de datos

`BusUpdate{busId, pos{lat,lng}, lineId, timestamp, zoneId, operationalState}`.

## 6. Diseño

```
nodes/client-cco/cco-client/src/main/java/edu/icesi/sitmmio/cco/rtstreamclient/
├── RTStreamClient.java
├── MonitoringSubscriberI.java
├── BridgeAdapter.java
└── ZoneFilter.java
```

Flujo:
```
1. Login → AuthService → JWT
2. start(jwt): parse zoneId; create adapter + servant; eventBus.subscribe(self, zoneId)
3. updateLocation(u): Platform.runLater(() -> bridge.invokeUpdateBus(u))
4. onCriticAlert(a): delegar a AlertPanel.show(a)
5. logout: eventBus.unsubscribe(self)
```

## 7. Aspectos distribuidos

- Auto-reconexión con backoff.
- AMI server-side → cliente no bloquea event-loop.
- `Platform.runLater` envuelve cada `executeScript`.
- Métricas: `received_updates`, `dropped_updates`, `reconnects_total`.

## 8. Criterios de aceptación

- [ ] `ctrl-001` (zona 1) solo recibe updates de zona 1.
- [ ] Alertas `priority=="ALTA"` llegan sin importar zona.
- [ ] Reconnect tras matar EventBus 5 s.
- [ ] Marcadores se mueven con cada update.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Saturación UI | `executeScript` por batch. |
| Servant sin endpoint | `default -h *` (referente). |
| Token expira en sesión | Renovación silenciosa antes de exp. |

## 10. Decisiones diferidas

- Histórico local para replay.

## 11. Checklist

- [ ] `MonitoringSubscriberI` + `BridgeAdapter`.
- [ ] Login + parse JWT.
- [ ] Suscripción con zona.
- [ ] Reconnect.
- [ ] Smoke con publisher mock.

## 12. Post-mortem

A llenar tras implementación.
