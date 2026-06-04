---
spec-id: 12-analytics-store
component: AnalyticsStore + AnalyticsDB
deployment-node: DataCenter
status: draft
depends-on: [00, 01, 02]
---

# Spec 12 — `analytics-store`

## 1. Contexto

Persiste `SpeedReport` (R7.4) y expone vía `ReportProvider`. Backend inicial: CSV alineado con `monolith-results.csv` (reusa `ResultCsvWriter`).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `AnalyticsStore` + `AnalyticsDB` |
| Requisitos | R7.4, R14.1, R27 |
| ADR | ADR-5, ADR-9 |
| Patrón | Warm storage |
| CRC | §2.9 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `batch-master` | Escritor | `saveAll` |
| `public-api` | Lector | `getAverageSpeed`, `getMonthlyReports` |
| `analytics-view` | Lector | `getRangeReports` |
| `domain-core` | Lib | `ResultCsvWriter`, `SpeedResult` |

## 4. Interfaces (Slice)

```slice
interface ReportProvider {
  SpeedReport getAverageSpeed(int lineId, int year, int month) throws NoDataForPartition;
  SpeedReportSeq getMonthlyReports(int year, int month);
  SpeedReportSeq getRangeReports(int yf, int mf, int yt, int mt);
};
```

Java-only: `void saveAll(List<SpeedReport>)`.

## 5. Modelo de datos

CSV `analytics-db/speed-reports.csv` con mismo header que `monolith-results.csv`. Índice in-memory `Map<(lineId, YearMonth), SpeedReport>`.

## 6. Diseño

```
nodes/datacenter/analytics-store/
└── src/main/java/edu/icesi/sitmmio/analyticsstore/
    ├── Main.java
    ├── adapter/AnalyticsStoreI.java
    ├── domain/ReportIndex.java
    └── io/CsvBackend.java       REUSA ResultCsvWriter
```

Append + actualizar índice. Lecturas desde índice.

## 7. Aspectos distribuidos

- Idempotencia: `saveAll` last-write-wins por `(lineId, year, month)`.
- Lecturas síncronas rápidas.
- Métricas: `reports_total`, `reads_total`, `writes_total`, `last_run_at`.

## 8. Criterios de aceptación

- [ ] `saveAll` 110 reportes → CSV con 110 filas + header.
- [ ] `getAverageSpeed(131, 2019, 5)` → `≈ 17.375`.
- [ ] `getMonthlyReports(2019, 5)` → 110.
- [ ] `getAverageSpeed(9999, 2099, 1)` → `NoDataForPartition`.
- [ ] Diff vs `monolith-results.csv` ≈ 0.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Concurrencia escritura | Single-writer (master); lock interno. |
| CSV no escala | Migrar a SQLite si > 1M. |
| Pérdida tras crash | `fsync` tras `saveAll`. |

## 10. Decisiones diferidas

- SQLite/Parquet.
- Replicación lectora.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `CsvBackend` reusa `ResultCsvWriter`.
- [ ] `ReportIndex` thread-safe.
- [ ] Tests + correctness vs monolith.

## 12. Post-mortem

A llenar tras implementación.
