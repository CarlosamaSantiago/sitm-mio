---
spec-id: 13-public-api
component: PublicAPI + ControllerPublicAPI
deployment-node: ServerPublicAPI
status: draft
depends-on: [00, 02, 09, 12]
---

# Spec 13 — `public-api`

## 1. Contexto

REST + OpenAPI para ciudadanos/entidades (R14). Valida JWT con `AuthService`. Perfil de consumidor (R33). Consulta `AnalyticsStore`.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `PublicAPI` + `ControllerPublicAPI` |
| Requisitos | R14.1, R14.2, R14.3, R33, RNF-7, RNF-8 |
| ADR | ADR-5, ADR-7 |
| Patrón | Anti-corruption layer REST↔Ice |
| CRC | §2.14, §2.15 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `auth-service` | Cliente | `validate` |
| `analytics-store` | Cliente | `ReportProvider.*` |
| Consumidores externos | HTTP | REST |

## 4. Interfaces

Slice (interno):
```slice
interface PublicAPI {
  SpeedReport publicSpeed(int lineId, int year, int month) throws NoDataForPartition;
  string systemStatus();
};
```

REST:
```
GET /api/v1/speeds/{lineId}?year=YYYY&month=MM
GET /api/v1/speeds?year=YYYY&month=MM
GET /api/v1/speeds/range?yf=&mf=&yt=&mt=
GET /api/v1/health
GET /api/v1/openapi.yaml
Header: Authorization: Bearer <jwt>
```

## 5. Modelo de datos

JSON:
```json
{"lineId":131,"shortName":"T31","yearMonth":"2019-05",
 "averageSpeedKmH":17.375,"totalDistanceKm":19066.45,
 "validSegments":206566,"status":"OK"}
```

## 6. Diseño

```
nodes/server-public-api/public-api/
├── build.gradle             implementation 'io.javalin:javalin:6.x'
└── src/main/
    ├── java/edu/icesi/sitmmio/publicapi/
    │   ├── Main.java
    │   ├── adapter/PublicApiI.java
    │   ├── controllers/SpeedController.java, HealthController.java
    │   ├── domain/ConsumerProfile.java, JsonMapper.java
    │   ├── service/JwtValidator.java
    │   └── io/AuthServiceClient.java, ReportProviderClient.java
    └── resources/openapi.yaml
```

Filtros: `auth-filter` (excepto `/health`, `/openapi.yaml`), `rate-limit-filter` por perfil (ANON=10/min, PARTNER=100/min, GOV=∞).

## 7. Aspectos distribuidos

- Stateless (JWT autocontiene contexto).
- Pool de proxies Ice `--analytics-store-replicas`.
- Métricas: `requests_total{endpoint,status}`, `latency_p95`, `rate_limited_total`.

## 8. Criterios de aceptación

- [ ] `GET /api/v1/speeds/131?year=2019&month=5` → 200 + JSON correcto.
- [ ] Sin JWT → 401.
- [ ] JWT expirado → 401.
- [ ] Rate-limit ANON hit 11 → 429.
- [ ] OpenAPI válida (`swagger-cli`).
- [ ] `/health` → 200 + estado de dependencias.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Latencia Ice | Pool + timeout corto. |
| OpenAPI vs impl | Generador automático o test E2E. |
| JWT replay | Expiración corta + nonce opcional. |

## 10. Decisiones diferidas

- `/v2`.
- Cache CDN.

## 11. Checklist

- [ ] Módulo + `build.gradle` Javalin.
- [ ] Controllers + JsonMapper.
- [ ] Filtros auth + rate-limit.
- [ ] OpenAPI YAML.
- [ ] Tests E2E HTTP.

## 12. Post-mortem

A llenar tras implementación.
