# Specs — SITM-MIO V3 (Distribuida con ZeroC ICE)

Specs incrementales de spec-driven development. Cada uno cubre un componente del `Deployment.pdf` y debe leerse en orden numérico.

## Alcance

UC primarios del Dorfmann (hojas `Bicolumnar R4` y `Bicolumnar R7`):
- **UC-R4** — Monitorear flota en tiempo real (R4, R6, R9, R10, R22, R23, R24, R30, R35).
- **UC-R7** — Calcular velocidad promedio por ruta y mes (R7, R6, R10, R12, R14, R25, R27).

## Orden

| # | Spec | Componente Deployment | UC |
|---|------|----------------------|----|
| 00 | project-scaffolding | infra Gradle/Ice/JavaFX | ambos |
| 01 | domain-core | dominio compartido (extraído del monolith) | ambos |
| 02 | slice-contracts | contracts/sitm.ice | ambos |
| 03 | bus-simulator | GPRSTransmitter | ambos |
| 04 | ingestion-gateway | IngestionGateway | ambos |
| 05 | datagram-queue | DatagramQueue | ambos |
| 06 | data-lake | DataLake | ambos |
| 07 | stream-processor | StreamProcessor | R4 |
| 08 | datagram-event-bus | DatagramEventBus | R4 |
| 09 | auth-service | AuthService | ambos |
| 10 | batch-master | BatchMaster | R7 |
| 11 | batch-worker | BatchWorker | R7 |
| 12 | analytics-store | AnalyticsStore | R7 |
| 13 | public-api | PublicAPI + ControllerPublicAPI | R7 |
| 14 | session-context | SessionContextController | R4 |
| 15 | map-tile-provider | MapTileProvider (Leaflet base) | R4 |
| 16 | rt-stream-client | RTStreamClient | R4 |
| 17 | map-view | MapView | R4 |
| 18 | alert-panel | AlertPanel | R4 |
| 19 | analytics-view | AnalyticsView | R7 |
| 20 | citizen-cli | Users / ExternClient | R7 (opcional) |

## Convenciones

- Paquete Java: `edu.icesi.sitmmio.<componente>` (alineado con `monolith-approach`).
- Layout interno: `service/ · domain/ · validation/ · io/ · adapter/ · Main.java`.
- Middleware: ZeroC ICE 3.7.10.
- JDK 17.
- UI: JavaFX 17 + WebView + Leaflet.

## Template (12 secciones)

Contexto · Trazabilidad · Colaboradores · Interfaces (Slice) · Modelo de datos · Diseño · Aspectos distribuidos · Criterios de aceptación · Riesgos · Decisiones diferidas · Checklist · Post-mortem.

## Diferidos (fuera del piloto)

- `ControllerAdministration` CRUD (R3.1–R3.3, R21).
- Perilla física (R2).
- Diagramas de transición (R15).
- PSO completo (R26, R28, R36).
