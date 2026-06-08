---
spec-id: 19-analytics-view
component: AnalyticsView
deployment-node: ClientCCO
status: draft
depends-on: [00, 02, 09, 12, 15]
---

# Spec 19 — `analytics-view`

## 1. Contexto

Vista comparativa de velocidades promedio por ruta-mes (R27). Consulta `AnalyticsStore` vía `ReportProvider` y renderiza en `analytics.html`.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `AnalyticsView` |
| Requisitos | R27.1, R27.2 |
| ADR | ADR-5, ADR-7 |
| Patrón | Cliente Ice + JS bridge |
| CRC | §2.16 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `analytics-store` | Cliente | `getMonthlyReports`, `getRangeReports` |
| `map-tile-provider` | Base | `analytics.html` |
| `auth-service` | Cliente | `validate(jwt)` |

## 4. Interfaces

Slice `ReportProvider`. JS: `loadSpeedReport(lineId, yearMonth, avgSpeed, geometry)`, `clearReports()`.

Java:
```java
public class AnalyticsView {
  void initialize(WebView webView);
  void loadRange(int yf, int mf, int yt, int mt, Set<Integer> lineIds);
  void exportCsv(Path output);
}
```

## 5. Modelo de datos

`SpeedReport` Slice → JS bridge. Filtro `Set<Integer> lineIds` opcional.

## 6. Diseño

```
nodes/client-cco/cco-client/src/main/java/edu/icesi/sitmmio/cco/analyticsview/
├── AnalyticsView.java
├── controllers/RangePickerController.java
├── controllers/ComparisonTableController.java
├── adapter/JsBridge.java
└── io/ReportProviderClient.java
```

Toolbar: `RangePicker` + `MultiSelect<Route>`. WebView con `analytics.html`. Panel lateral `TableView<SpeedReport>` ordenable. Botón "Exportar" usa `ResultCsvWriter`.

## 7. Aspectos distribuidos

- Cliente Ice síncrono; queries acotadas (110 rutas × N meses).
- JWT obligatorio.
- Cache LRU 50.
- Métricas: `queries_total`, `query_latency_p95`.

## 8. Criterios de aceptación

- [ ] `loadRange(2019, 5, 2019, 5, all)` muestra 110 rutas.
- [ ] Tabla ordenable por `averageSpeedKmH`.
- [ ] Comparativo destaca rutas con mayor delta entre meses.
- [ ] Export CSV ≈ `monolith-results.csv` para mayo 2019.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Polilíneas pesadas | Simplificar geometría o marcador en estación origen. |
| Latencia Ice | Cache LRU + loading. |
| Geometría rutas inconsistente | Piloto: segmento estación inicio→fin. |

## 10. Decisiones diferidas

- Heatmap velocidades.
- Comparación con PSO (R26).

## 11. Checklist

- [ ] `initialize` carga `analytics.html`.
- [ ] `RangePicker` + `MultiSelect`.
- [ ] `ComparisonTable`.
- [ ] `ReportProviderClient` con cache.
- [ ] `exportCsv` usa `ResultCsvWriter`.
- [ ] Smoke contra mock.

## 12. Post-mortem

A llenar tras implementación.
