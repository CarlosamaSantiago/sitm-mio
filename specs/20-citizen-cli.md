---
spec-id: 20-citizen-cli
component: Users / ExternClient
deployment-node: ExternClient
status: draft (opcional)
depends-on: [00, 13]
---

# Spec 20 — `citizen-cli`

## 1. Contexto

Cliente CLI/HTTP que consume `PublicAPI` (R14) desde fuera del DataCenter. Materializa el nodo `ExternClient` del Deployment. Sirve también como smoke E2E del experimento (valida que la API responde con los mismos números que `monolith-results.csv`).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `Users` (ExternClient) |
| Requisitos | R14.1, R14.2, R14.3, R33 |
| ADR | ADR-7 |
| Patrón | API consumer |
| CRC | §2.18 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `public-api` | Sink | REST |
| `auth-service` | Indirecto vía `public-api` | login |

## 4. Interfaces

```
citizen-cli login --user <u> --password <p>
citizen-cli speed --lineId <n> --year <yyyy> --month <mm>
citizen-cli speeds --year <yyyy> --month <mm>
citizen-cli range --yf <> --mf <> --yt <> --mt <>
citizen-cli health
citizen-cli openapi --out openapi.yaml
```

Output: JSON default; `--format=csv` para alinear con `monolith-results.csv`.

## 5. Modelo de datos

JSON respuesta de la `PublicAPI` (spec 13). Token JWT cacheado en `~/.sitmmio/token`.

## 6. Diseño

```
nodes/extern-client/citizen-cli/
└── src/main/java/edu/icesi/sitmmio/citizencli/
    ├── Main.java                   Picocli
    ├── commands/
    │   ├── LoginCommand.java
    │   ├── SpeedCommand.java
    │   ├── SpeedsCommand.java
    │   ├── RangeCommand.java
    │   ├── HealthCommand.java
    │   └── OpenApiCommand.java
    ├── io/
    │   ├── ApiClient.java          java.net.http
    │   └── TokenStore.java         ~/.sitmmio/token
    └── domain/CsvFormatter.java
```

## 7. Aspectos distribuidos

- HTTP cliente síncrono con `--timeout-ms=5000`.
- Reintento backoff ante 5xx (1s → 2s → 4s).
- Token expirado → relogin auto con env vars.
- Métricas (log): `requests_total`, `latency_ms`, `retries_total`.

## 8. Criterios de aceptación

- [ ] `login` exitoso → token guardado.
- [ ] `speed --lineId 131 --year 2019 --month 5` → JSON con `averageSpeedKmH ≈ 17.375`.
- [ ] `speeds --year 2019 --month 5 --format=csv` → diff ≈ 0 vs `monolith-results.csv`.
- [ ] `health` → 200.
- [ ] Sin token → mensaje "login required".

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Token en plano en `~/.sitmmio/token` | Permisos `0600`; documentar trade-off. |
| Rate-limit ANONYMOUS | `--profile=PARTNER` con credenciales. |
| Cambios API rompen CLI | Tests E2E contra `PublicAPI` real. |

## 10. Decisiones diferidas

- TUI interactivo.
- WebSocket RT.

## 11. Checklist

- [ ] Módulo + `build.gradle` con Picocli.
- [ ] Commands.
- [ ] `ApiClient` timeout + retry.
- [ ] `TokenStore` seguro.
- [ ] Tests E2E contra `public-api`.

## 12. Post-mortem

A llenar tras implementación.
