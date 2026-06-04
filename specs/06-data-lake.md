---
spec-id: 06-data-lake
component: DataLake + DataLakeStorage
deployment-node: DataCenter
status: draft
depends-on: [00, 01, 02]
---

# Spec 06 — `data-lake`

## 1. Contexto

Cold storage de datagramas crudos (R6.2, R30). Reemplaza el stub `ArchiveServiceI` del referente (I-3). Particiona por año-mes (ADR-8).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `DataLake` + `DataLakeStorage` |
| Requisitos | R6.2, R30, R30.1 |
| ADR | ADR-5 |
| Patrón | Cold storage |
| CRC | §2.6 |
| Inconsistencia | I-3 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `ingestion-gateway` | Escritor AMI | `ArchiveService.archiveDatagram` |
| `batch-master` | Lector planning | `LakeReader.listPartitions` |
| `batch-worker` | Lector data | `LakeReader.streamPartition(PartitionKey)` |

## 4. Interfaces (Slice)

```slice
interface ArchiveService {
  ["ami"] void archiveDatagram(Datagram d);
  SpeedReport getReport(int lineId, int year, int month) throws NoDataForPartition;
};
```

Local Java:
```java
public interface LakeReader {
  List<PartitionKey> listPartitions();
  Stream<Datagram> streamPartition(PartitionKey key);
}
```

## 5. Modelo de datos

```
lake/lineId=<n>/year=<yyyy>/month=<mm>/part-<seq>.csv
```

CSV inicial (compatible con `DatagramCsvReader`). Parquet diferido.

## 6. Diseño

```
nodes/datacenter/data-lake/
├── build.gradle
└── src/main/java/edu/icesi/sitmmio/datalake/
    ├── Main.java
    ├── adapter/ArchiveServiceI.java
    ├── service/Partitioner.java
    ├── domain/PartitionKey.java
    └── io/FileLakeStore.java
        LakeReader.java
```

`archiveDatagram`: deriva `(lineId, year, month)` desde `datagramDate`, append CSV, rota por tamaño.

## 7. Aspectos distribuidos

- AMI no bloquea StreamProcessor.
- Lectura particionada — worker abre solo su partición.
- Métricas: `archived_total{lineId,month}`, `write_throughput`.

## 8. Criterios de aceptación

- [ ] Escribe MiniPilot (~6.7 M válidos) en < 60 s.
- [ ] Layout correcto `lineId=.../year=.../month=...`.
- [ ] `streamPartition(131, 2019, 5)` retorna todos los datagramas de T31 mayo 2019.
- [ ] `NoDataForPartition` si no existe.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Muchos archivos pequeños | Rotación por tamaño. |
| Concurrencia entre threads | `ConcurrentHashMap<PartitionKey, FileChannel>`. |
| Duplicación tras retry | Marcar offset; dedupe opcional. |

## 10. Decisiones diferidas

- Parquet / S3.
- Sharding por hash de bus.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `Partitioner` + parser fecha.
- [ ] `FileLakeStore` con rotación.
- [ ] `LakeReader.streamPartition`.
- [ ] Test E2E ingesta → lake → lectura.

## 12. Post-mortem

A llenar tras implementación.
