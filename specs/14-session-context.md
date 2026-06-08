---
spec-id: 14-session-context
component: SessionContextController
deployment-node: DataCenter
status: draft
depends-on: [00, 02, 09]
blocks: [07, 08]
---

# Spec 14 — `session-context`

## 1. Contexto

Resuelve `lineId → zoneId` y `userId → zoneId` para el filtrado server-side (R9.2) en `StreamProcessor` y `DatagramEventBus`. Backend mínimo: JSON estático para el piloto.

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `SessionContextController` |
| Requisitos | R8.1, R8.2, R9.2, R9.3 |
| ADR | ADR-7 |
| Patrón | Lookup |
| CRC | §2.11 (parte de controller-cco) |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `stream-processor` | Cliente | `zoneOfLine` |
| `datagram-event-bus` | Cliente | `zoneOfUser` |
| `auth-service` | Reuso | `UserContext.assignedZoneId` (en JWT) |

## 4. Interfaces (Slice)

```slice
interface SessionContextController {
  int zoneOfLine(int lineId);
  int zoneOfUser(string userId);
};
```

## 5. Modelo de datos

`admin/zone-mapping.json`:
```json
{
  "lineToZone": {"131": 1, "140": 1, "142": 2},
  "userToZone": {"ctrl-001": 1, "ctrl-002": 2, "admin": 0}
}
```

## 6. Diseño

```
nodes/datacenter/session-context/
└── src/main/java/edu/icesi/sitmmio/sessioncontext/
    ├── Main.java
    ├── adapter/SessionContextControllerI.java
    ├── domain/ZoneMap.java
    └── io/ZoneMappingLoader.java
```

Carga JSON al arranque; lookup O(1). Reload por SIGHUP opcional.

## 7. Aspectos distribuidos

- Cache 100% en memoria (~100 rutas + 41 usuarios).
- Lecturas concurrentes sin lock (`ConcurrentHashMap`).
- `lineId` desconocido → retorna `0`.
- Métricas: `lookups_total{type}`, `unknown_lookups_total`.

## 8. Criterios de aceptación

- [ ] `zoneOfLine(131)` retorna zona según JSON.
- [ ] `zoneOfLine(99999)` retorna 0.
- [ ] `zoneOfUser("ctrl-001")` retorna zona correcta.
- [ ] 1M lookups/s sin contención.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| JSON inconsistente con AuthService | Mismo seed file o validación al arranque. |
| Cambios requieren restart | Reload por endpoint admin (post-piloto). |

## 10. Decisiones diferidas

- BD relacional.
- API admin caliente.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `ZoneMappingLoader` + `ConcurrentHashMap`.
- [ ] `zone-mapping.example.json`.
- [ ] Tests lookup + fallback.

## 12. Post-mortem

A llenar tras implementación.
