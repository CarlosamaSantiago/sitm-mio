---
spec-id: 03-bus-simulator
component: GPRSTransmitter
deployment-node: Bus — Embedded Computer {1..1000}
status: draft
depends-on: [00, 01, 02]
---

# Spec 03 — `bus-simulator` (GPRSTransmitter)

## 1. Contexto y problema

Simulador del computador embebido. Emite datagramas al `IngestionGateway` por Ice. Reusa patrón del referente (`bus-simulator/Main.java`) extendido con: lectura ZIP streamed, parámetros CLI, soporte de notación científica en `unknown1`.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `GPRSTransmitter` |
| Requisitos | R6.1, R30, R31 |
| ADR | ADR-1 |
| Patrón | Producer (Producer-Consumer) |
| CRC | `docs/CRC&DeploymentDiagram.md §2.1` |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `ingestion-gateway` | Sink | `DatagramReceiver.postDatagram` |
| `domain-core` | Lib | `DatagramCsvReader`, `GpsConstants` |
| `contracts` | Lib | proxy `DatagramReceiverPrx`, `Datagram` Slice |

## 4. Interfaces (Slice)

Consume `DatagramReceiver.postDatagram(Datagram d)`. No expone.

## 5. Modelo de datos relevante

CSV (sin header): `eventType, registerDate, stopId, odometer, latitude, longitude, taskId, lineId, tripId, unknown1, datagramDate, busId`.

- `latitude/longitude` enteros x10⁷.
- `unknown1` puede venir en notación científica (`6.255401365E9`) → `Double.parseDouble` + cast a int.
- `datagramDate` formato `YYYY-MM-DD HH:MM:SS`.

## 6. Diseño propuesto

```
nodes/bus/bus-simulator/
├── build.gradle             implementation project(':contracts'), project(':domain-core')
└── src/main/java/edu/icesi/sitmmio/bussimulator/
    ├── Main.java            CLI
    ├── adapter/             SliceMapper.toSlice(record)
    ├── service/             EmissionScheduler (throttle)
    └── io/                  ZipCsvStream, CsvLineParser
```

### 6.1 CLI

```
--host <ip>             default 127.0.0.1
--port <p>              default 10000
--dataset <path>        default data/raw/datagrams-MiniPilot.csv (acepta .zip)
--throttle-ms <n>       default 0
--rate-multiplier <x>   default 1.0
--max-records <n>       default ∞
```

### 6.2 Pseudocódigo

```
1. Initialize Communicator Ice
2. proxy = stringToProxy("DatagramReceiver:default -h $host -p $port")
3. open ZipInputStream o FileInputStream según extensión
4. for each line:
     a. parseCsvLine → record
     b. SliceMapper.toSlice(record)
     c. proxy.postDatagram(slice)
     d. si throttle > 0 → Thread.sleep
5. Reportar throughput + errores
```

### 6.3 Errores

- `NumberFormatException` por línea → log + skip.
- `Ice.ConnectionRefusedException` → backoff 1s→2s→4s→8s → abort.

## 7. Aspectos distribuidos

- Cliente Ice puro (sin servant).
- Endpoint en `bus-simulator.cfg` overrideable por CLI.
- Rate multiplier desacopla cadencia GPRS real de cadencia del experimento.

## 8. Criterios de aceptación

- [ ] Lee `datagrams-tiny.csv` y emite N datagramas válidos.
- [ ] Lee `datagrams-MiniPilot.zip` sin descomprimir.
- [ ] `unknown1` en notación científica parseado.
- [ ] Líneas malformadas reportadas y omitidas.
- [ ] Throttle 0 alcanza > 50k/s en localhost.
- [ ] Reconexión automática.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Throttle 0 inunda el gateway | Backpressure síncrono. |
| ZIP con múltiples entries | Iterar entries `.csv`. |
| Memoria con datasets grandes | Lectura streamed. |

## 10. Decisiones diferidas

- Múltiples instancias para 1000 buses concurrentes.
- Generador sintético sin CSV.

## 11. Checklist

- [ ] `build.gradle`.
- [ ] `Main.java` con CLI.
- [ ] `ZipCsvStream`.
- [ ] `bus-simulator.cfg`.
- [ ] Tests: parser notación científica + skip línea inválida.
- [ ] Smoke contra mock gateway.

## 12. Post-mortem

A llenar tras implementación.
