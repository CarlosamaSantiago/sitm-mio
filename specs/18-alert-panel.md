---
spec-id: 18-alert-panel
component: AlertPanel
deployment-node: ClientCCO
status: draft
depends-on: [00, 02, 08, 15, 16]
---

# Spec 18 — `alert-panel`

## 1. Contexto

Recibe `CriticAlert` (vía `RTStreamClient` o suscripción directa al `DatagramEventBus`) y muestra una alerta visual destacada sobre el mapa (R19.3). Incluye historial breve, sonido opcional y acuse.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `AlertPanel` |
| Requisitos | R19.1, R19.2, R19.3, R22.2 |
| ADR | ADR-4 |
| Patrón | EDA Subscriber + JS bridge |
| CRC | §2.16 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `rt-stream-client` | Delegador | `MonitoringSubscriber.onCriticAlert` |
| `map-view` | Overlay | JS `showAlert(...)` |
| `controller-cco` | Acuse (futuro) | `acknowledgeAlert` |

## 4. Interfaces

JS: `showAlert(busId, lineId, priority, description, lat, lng)`.

Java:
```java
public class AlertPanel {
  void initialize(BorderPane container);
  void show(CriticAlert alert);
  List<CriticAlert> history();
  void acknowledge(int alertId);
}
```

## 5. Modelo de datos

`CriticAlert{busId, lineId, zoneId, eventType, priority, timestamp, description}`. Historial in-memory N=50 (buffer circular).

## 6. Diseño

```
nodes/client-cco/cco-client/src/main/java/edu/icesi/sitmmio/cco/alertpanel/
├── AlertPanel.java
├── controllers/AlertListController.java
├── adapter/JsBridge.java
└── audio/SoundPlayer.java
```

UI: `VBox` lateral con `ListView<CriticAlert>` desc por timestamp; doble click centra el mapa. Overlay rojo destacado vía `showAlert`.

## 7. Aspectos distribuidos

- Recibe por delegación del `RTStreamClient` (mismo proceso JavaFX).
- Sin estado servidor.
- Métricas: `alerts_received`, `alerts_acknowledged`, `alerts_dropped`.

## 8. Criterios de aceptación

- [ ] Alerta crítica → marcador rojo + entrada en lista.
- [ ] Beep opcional configurable.
- [ ] Doble click centra mapa en bus.
- [ ] Auto-fade tras 60 s sin acuse.
- [ ] Historial conserva últimas 50.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Flood de alertas | Throttle UI: agrupar por bus en ventana 5 s. |
| Sonido molesto | Toggle en preferencias. |
| Memoria | Buffer circular fijo. |

## 10. Decisiones diferidas

- Persistencia en backend.
- Reglas R19 ampliado.

## 11. Checklist

- [ ] `AlertPanel.initialize`.
- [ ] `show()` invoca JS `showAlert`.
- [ ] Historial circular.
- [ ] Smoke con stream mock.

## 12. Post-mortem

A llenar tras implementación.
