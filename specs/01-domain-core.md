---
spec-id: 01-domain-core
component: (módulo compartido — dominio extraído del monolith)
deployment-node: (transversal)
status: draft
depends-on: [00-project-scaffolding]
blocks: [02..20 que usen dominio]
---

# Spec 01 — `domain-core`

## 1. Contexto y problema

`monolith-approach` ya validó el cálculo contra MiniPilot (output en `data/output/monolith-results.csv`). V3 NO duplica ese dominio: lo extrae a un módulo Gradle reusable que todos los nodos consumen. Beneficios: **correctness por construcción** (V3 ≡ V1) + velocidad de desarrollo (nodos solo añaden capa Ice).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componentes Deployment soportados | `IngestionGateway` (R35), `StreamProcessor` (R12, R23.1), `BatchWorker` (R7.2, R7.3), `AnalyticsStore` (R7.4) |
| Requisitos | R7.1–R7.4, R12.1–R12.4, R30, R35.1, R35.2 |
| ADR | ADR-1, ADR-6, ADR-9 |
| Patrón | Reuso de V1 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `ingestion-gateway` | Cliente | `DatagramValidator.isValid` |
| `stream-processor` | Cliente | `Datagram`, `GeoPoint` |
| `batch-worker` | Cliente | `DistanceCalculator`, `SpeedAccumulator`, `RouteMonthKey` |
| `data-lake` | Cliente | `DatagramCsvReader`, `RouteCsvReader` |
| `analytics-store` | Cliente | `ResultCsvWriter`, `SpeedResult` |
| `contracts` | Mapeo | `SliceMapper` Slice DTO ↔ `domain.Datagram` |

## 4. Interfaces (Slice)

Ninguna — `domain-core` es Java puro.

## 5. Modelo de datos relevante

### 5.1 Records (heredados del monolith)

```java
public record Datagram(
    int eventType, String registerDate, int stopId, long odometer,
    GeoPoint point, int rawLatitude, int rawLongitude,
    int taskId, int lineId, long tripId, long unknown1,
    LocalDateTime datagramDate, int busId) {}

public record GeoPoint(double latitude, double longitude) {}
public record Route(int lineId, String shortName, String description) {}
public record RouteMonthKey(int lineId, YearMonth yearMonth) {}
public record SpeedResult(
    int lineId, String shortName, String description, YearMonth yearMonth,
    double totalDistanceKm, double totalTimeHours, double averageSpeedKmH,
    long validSegments, long skippedSegments, String status) {}
```

### 5.2 Servicios

- `DistanceCalculator.haversineKm(GeoPoint, GeoPoint)` — radio 6371.0088 km.
- `SpeedAccumulator` — acumula distancia/tiempo → produce `SpeedResult`.
- `MetricsCollector` — cuenta procesadas/válidas/skippeadas.
- `DatagramValidator.isValid(Datagram, Map<Integer, Route>)` — R35.1.

### 5.3 Constantes

```java
public final class GpsConstants {
    public static final int COORD_SCALE = 10_000_000;
    public static final double EARTH_RADIUS_KM = 6371.0088;
    public static final int NULL_SENTINEL = -1;
}
```

## 6. Diseño propuesto

```
domain-core/
├── build.gradle             (sin Ice; solo JDK + JUnit)
└── src/
    ├── main/java/edu/icesi/sitmmio/
    │   ├── domain/          records
    │   ├── service/         calculadoras
    │   ├── validation/      R35
    │   └── io/              CSV
    └── test/                tests heredados del monolith
```

Copia literal desde `monolith-approach`: `domain/*`, `service/{DistanceCalculator, SpeedAccumulator, MetricsCollector}`, `validation/DatagramValidator`, `io/*`. NO copiar `MonolithicSpeedCalculator` (esa lógica se reparte en master/worker).

## 7. Aspectos distribuidos

- **Sin Ice** → unit-testeable trivialmente.
- **Idempotencia:** funciones puras → BatchWorker reintenta sin efectos secundarios.
- **Inmutabilidad:** records → thread-safe sin synchronized.

## 8. Criterios de aceptación

- [ ] `./gradlew :domain-core:test` pasa con tests del monolith.
- [ ] JAR sin dependencia transitiva a Ice.
- [ ] `DistanceCalculator` produce mismos valores que V1 sobre 5 pares conocidos.
- [ ] `DatagramValidator` rechaza las 5 categorías: `rawLat/rawLon=-1`, `busId<=0`, `tripId<0`, `lineId∉activeRoutes`.
- [ ] `DatagramCsvReader` parsea `datagrams-tiny.csv` y maneja notación científica en `unknown1`.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Divergencia V1 vs V3 por refactor accidental | Test de equivalencia: mismo input → mismo `SpeedResult`. |
| `unknown1` overflow `double→long` | Preservar lógica del monolith. |

## 10. Decisiones diferidas

- `SpeedAccumulator` mergeable para particiones < ruta×mes.

## 11. Checklist

- [ ] Crear módulo `domain-core/`.
- [ ] Copiar dominio + service + validation + io desde monolith.
- [ ] Crear `GpsConstants`.
- [ ] Copiar tests existentes.
- [ ] Verificar ausencia de Ice.
- [ ] `domain-core/README.md` documenta contrato V1↔V3.

## 12. Post-mortem

A llenar tras implementación.
