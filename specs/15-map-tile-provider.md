---
spec-id: 15-map-tile-provider
component: MapTileProvider
deployment-node: MapsService (externo)
status: draft
depends-on: [00]
blocks: [16, 17, 18, 19]
---

# Spec 15 — `map-tile-provider` (base Leaflet)

## 1. Contexto

Proveedor externo de tiles (`<<external>> MapsService`). Documenta cómo el cliente CCO lo consume vía Leaflet y provee el **JS bridge estable** para `map-view`, `rt-stream-client`, `alert-panel`, `analytics-view`.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `MapTileProvider` |
| Requisitos | R4.2, R4.3, R27 |
| ADR | ADR-1 |
| Patrón | External service + JS bridge |
| CRC | §2.17 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `map-view` | Consumer | `IBaseMap` (tiles HTTP) |
| `rt-stream-client` | Consumer | `updateBus` |
| `alert-panel` | Consumer | `showAlert` |
| `analytics-view` | Consumer | `loadSpeedReport` |

## 4. Interfaces (Slice + JS)

Slice: ninguna.

JS bridge en `cco-client/resources/`:
```javascript
// map.html
function updateBus(busId, lat, lng, lineId, time, state)
function showStop(stopId, lat, lng)
function showStation(id, lat, lng)
function showAlert(busId, lineId, priority, description, lat, lng)

// analytics.html
function loadSpeedReport(lineId, yearMonth, avgSpeed, geometry)
function clearReports()
```

## 5. Modelo de datos

Tile: `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`. Centro Cali `[3.42, -76.52]`, zoom 12.

## 6. Diseño

```
nodes/client-cco/cco-client/src/main/resources/
├── map.html              extiende referente
├── analytics.html         choropleth por ruta-mes
├── alerts.html
├── leaflet/               (vendoring offline opcional)
└── lib/
    ├── bridge.js
    └── styles.css
```

`map.html`: tile layer OSM + `var markers/stops`; `updateBus` reusa marcador y cambia color por `state`.
`analytics.html`: polilíneas por ruta + escala chroma + tabla lateral.
`alerts.html`: `L.circleMarker` rojo + popup destacado + auto-fade.

## 7. Aspectos distribuidos

- Recursos servidos desde el JAR (`Application.getResource("/map.html")`).
- CDN OSM degradable (RI-10) → fondo gris si falla.
- JS bridge estable desacopla Java de Leaflet.

## 8. Criterios de aceptación

- [ ] `map.html` carga tiles OSM.
- [ ] `executeScript("updateBus(1069, 3.476, -76.487, 2241, '2019-05-27 20:14:43', 'EN_RUTA')")` crea/mueve marcador.
- [ ] Caída de OSM no rompe la UI.
- [ ] `analytics.html` y `alerts.html` cargan sin errores.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Rate-limit OSM | Cache + tile server propio post-piloto. |
| JS bridge frágil | `bridge.js` con try/catch + guard `document.ready`. |
| Acentos en `description` | UTF-8 en `executeScript`. |

## 10. Decisiones diferidas

- Tile server propio.
- MapLibre vectorial.

## 11. Checklist

- [ ] `map.html` con tiles + `updateBus`.
- [ ] `analytics.html`.
- [ ] `alerts.html`.
- [ ] `bridge.js`.
- [ ] Smoke en WebView.

## 12. Post-mortem

A llenar tras implementación.
