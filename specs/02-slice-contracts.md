---
spec-id: 02-slice-contracts
component: contracts (Slice central)
deployment-node: (transversal)
status: draft
depends-on: [00-project-scaffolding, 01-domain-core]
blocks: [03..20]
---

# Spec 02 — `contracts` (Slice + DTOs Ice)

## 1. Contexto y problema

Slice único compartido. El referente tiene un `sitm.ice` mínimo; se extiende para cubrir R4+R7 completos. Slice **estable** = rebuild en cascada al cambiarlo.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente Deployment | Todas las interfaces nombradas |
| Requisitos | Todos |
| ADR | ADR-1, ADR-2, ADR-3, ADR-4, ADR-7 |
| Patrón | Contracts as Code |

## 3. Colaboradores

| Colaborador | Relación |
|-------------|----------|
| Todos los nodos | Compilan stubs Java desde `contracts.jar` |
| `01-domain-core` | `SliceMapper` adapta Slice ↔ record |

## 4. Interfaces (Slice)

### 4.1 DTOs

```slice
module SITM {
  struct Datagram {
    int eventType; string registerDate; int stopId; int odometer;
    int latitude; int longitude; int taskId; int lineId;
    int tripId; int unknown1; string datagramDate; int busId;
  };

  struct Location { double latitude; double longitude; };
  struct BusUpdate {
    int busId; Location pos; int lineId; string timestamp;
    int zoneId; string operationalState;
  };
  sequence<BusUpdate> BusUpdateSeq;

  struct CriticAlert {
    int busId; int lineId; int zoneId; int eventType;
    string priority; string timestamp; string description;
  };

  struct SpeedReport {
    int lineId; string shortName; string description;
    int year; int month;
    double totalDistanceKm; double totalTimeHours; double averageSpeedKmH;
    long validSegments; long skippedSegments;
    string status;
  };
  sequence<SpeedReport> SpeedReportSeq;

  struct PartitionKey { int lineId; int year; int month; };
  struct WorkerMetrics {
    long datagramsRead; long validSegments; long outliersDropped;
    long minusOneFiltered; long elapsedMillis;
  };

  exception InvalidDatagram { string reason; };
  exception NoDataForPartition {};
  exception Unauthorized {};

  struct AuthToken { string jwt; long expiresAtEpochMs; };
  struct UserContext { string userId; string role; int assignedZoneId; };
  struct RouteInfo { int lineId; string shortName; string description; };
```

### 4.2 Interfaces

```slice
  interface DatagramReceiver {
    void postDatagram(Datagram d) throws InvalidDatagram;
    void subscribe(MonitoringSubscriber* sub);
  };

  interface DatagramQueue {
    void enqueueDatagram(Datagram d);
    Datagram dequeueDatagram();
    long queueSize();
  };

  interface ArchiveService {
    ["ami"] void archiveDatagram(Datagram d);
    SpeedReport getReport(int lineId, int year, int month) throws NoDataForPartition;
  };

  interface MonitoringSubscriber {
    void updateLocation(BusUpdate u);
    void updateLocations(BusUpdateSeq us);
    void onCriticAlert(CriticAlert a);
  };

  interface DatagramEventBus {
    void publishUpdate(BusUpdate u);
    void publishAlert(CriticAlert a);
    void subscribe(MonitoringSubscriber* sub, int zoneId);
    void unsubscribe(MonitoringSubscriber* sub);
  };

  interface IBatchWorker {
    SpeedReport computePartition(PartitionKey k) throws NoDataForPartition;
    WorkerMetrics lastMetrics();
    string workerId();
  };

  interface BatchMaster {
    void registerWorker(IBatchWorker* w);
    SpeedReportSeq runMonth(int year, int month);
    SpeedReportSeq runRange(int yearFrom, int monthFrom, int yearTo, int monthTo);
  };

  interface ReportProvider {
    SpeedReport getAverageSpeed(int lineId, int year, int month) throws NoDataForPartition;
    SpeedReportSeq getMonthlyReports(int year, int month);
    SpeedReportSeq getRangeReports(int yf, int mf, int yt, int mt);
  };

  interface AuthService {
    AuthToken login(string user, string password) throws Unauthorized;
    UserContext validate(string jwt) throws Unauthorized;
  };

  interface PublicAPI {
    SpeedReport publicSpeed(int lineId, int year, int month) throws NoDataForPartition;
    string systemStatus();
  };

  interface SessionContextController {
    int zoneOfLine(int lineId);
    int zoneOfUser(string userId);
  };
};
```

### 4.3 `SliceMapper`

En `domain-core/adapter/`. Adapta `SITM.Datagram` ↔ `edu.icesi.sitmmio.domain.Datagram`: des-escala lat/lon con `GpsConstants.COORD_SCALE`, parsea `datagramDate` con formato `yyyy-MM-dd HH:mm:ss`.

## 5. Modelo de datos relevante

12 campos del datagrama → `struct Datagram` 1:1 con Diccionario. `SpeedReport` extendido = mismo esquema de `monolith-results.csv`. `BusUpdate` con `zoneId`/`operationalState` añadidos para R9.2 y R23.1.

## 6. Diseño propuesto

```
contracts/
├── build.gradle
└── src/main/slice/sitm.ice
```

`build.gradle`:
```groovy
plugins { id 'com.zeroc.gradle.ice-builder.slice' version '1.5.2' }
slice { java { files = fileTree(dir: 'src/main/slice', include: '**/*.ice') } }
```

## 7. Aspectos distribuidos

- Cambios al Slice → rebuild de todos los consumidores. Política: agregar al final; nunca renombrar.
- `["ami"]` en `archiveDatagram` libera al StreamProcessor.
- Excepciones tipadas para manejo idiomático.
- `subscribe(sub, zoneId)` aplica filtro server-side (R9.2).

## 8. Criterios de aceptación

- [ ] `:contracts:compileSlice` genera Java sin warnings.
- [ ] Roundtrip `SliceMapper`: identidad para 5 datagramas de muestra.
- [ ] `BusUpdate` serializa/deserializa todos los campos.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Breaking change | Revisión obligatoria de PRs a `sitm.ice`. |
| Slice ↔ record divergente | Tests bidireccionales del `SliceMapper`. |
| `int unknown1` overflow > 2^31 | Mantener `int` (referente lo hace); documentar. |

## 10. Decisiones diferidas

- Split en `sitm-rt.ice` y `sitm-batch.ice` si crece.

## 11. Checklist

- [ ] `contracts/build.gradle` con plugin Slice.
- [ ] `sitm.ice` con DTOs e interfaces.
- [ ] `SliceMapper` en `domain-core` + tests.
- [ ] Verificar generación Java.

## 12. Post-mortem

A llenar tras implementación.
