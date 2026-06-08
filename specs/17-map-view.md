---
spec-id: 17-map-view
component: MapView
deployment-node: ClientCCO
status: draft
depends-on: [00, 15, 16]
---

# Spec 17 — `map-view`

## 1. Contexto

Renderiza buses, paradas y estaciones (R4.2, R4.3, R24.2). Filtros (R4.4). Detalle al click (R24.1, R24.2). Estado operativo (R23.2) como color del marcador.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `MapView` |
| Requisitos | R4.2, R4.3, R4.4, R23.2, R24.1, R24.2 |
| ADR | ADR-4 |
| Patrón | JS bridge + JavaFX WebView |
| CRC | §2.16 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `rt-stream-client` | Updates | JS `updateBus` |
| `analytics-store` | Cliente click | `ReportProvider.getAverageSpeed` |
| `map-tile-provider` | Base | `map.html` |

## 4. Interfaces

JS (def spec 15): `updateBus`, `showStop`, `showStation`, `setFilter`.

Java:
```java
public class MapView {
  void initialize(WebView webView);
  void applyFilter(MapFilter filter);
  void showBusDetail(int busId, BusDetail detail);
  void switchToAnalytics();
}
```

## 5. Modelo de datos

`MapFilter{Set<Integer> lineIds, Integer zoneId, TimeRange}`. `BusDetail{busId, currentLine, lastEvent, position, operationalState}`.

## 6. Diseño

```
nodes/client-cco/cco-client/src/main/java/edu/icesi/sitmmio/cco/mapview/
├── MapView.java
├── controllers/FilterController.java
├── controllers/BusDetailController.java
└── adapter/JsBridge.java
```

Filtros: toolbar JavaFX con `ChoiceBox<Route>` + `RangeSlider`. Click → callback JS → `bridge.onBusClick(busId)` → Java consulta `ArchiveService` + estado → panel lateral.

Estado operativo: `EN_RUTA`=verde, `PARADO`=amarillo, `SIN_SENAL`=gris, `CRITICO`=rojo.

## 7. Aspectos distribuidos

- Client-side; filtros propagables al server vía re-suscripción al EventBus.
- Métricas UI: `markers_active`, `filter_changes_total`.

## 8. Criterios de aceptación

- [ ] `updateBus` mueve marcador en < 50 ms.
- [ ] Filtro `lineId=131` oculta otras rutas.
- [ ] Click → panel lateral con `BusDetail`.
- [ ] Color cambia con `operationalState`.
- [ ] Alterna `analytics.html` sin crash.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Muchos marcadores | `Leaflet.markercluster`. |
| Filtros vs stream inconsistentes | Re-suscribirse con nuevo `zoneId`. |
| Cierre pierde estado | `Preferences`. |

## 10. Decisiones diferidas

- Heatmap.
- Replay temporal.

## 11. Checklist

- [ ] `MapView.initialize` carga `map.html`.
- [ ] `FilterController`.
- [ ] `BusDetailController`.
- [ ] `JsBridge` con guards.
- [ ] Smoke con datos sintéticos.

## 12. Post-mortem

A llenar tras implementación.
