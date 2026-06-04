---
spec-id: 09-auth-service
component: AuthService + AdminDB
deployment-node: ServerAdmin
status: draft
depends-on: [00, 02]
---

# Spec 09 — `auth-service`

## 1. Contexto

Emite y valida JWT (R10). Backend mínimo en `AdminDB` (JSON para piloto). Sin CRUD admin completo (R3.1–R3.3 fuera de piloto).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | `AuthService` + `AdminDB` |
| Requisitos | R10.1, R10.2, RNF-8 |
| ADR | ADR-7 |
| Patrón | Centralized auth (stateless tokens) |
| CRC | §2.13 |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| `public-api` | Cliente | `validate` |
| `cco-client` | Cliente | `login`, `validate` |

## 4. Interfaces (Slice)

```slice
interface AuthService {
  AuthToken login(string user, string password) throws Unauthorized;
  UserContext validate(string jwt) throws Unauthorized;
};
```

## 5. Modelo de datos

`admin/users.json` (semilla `users.example.json` con `admin/ctrl-001` ejemplo):
```json
[{"userId":"<id>","passwordHash":"<bcrypt>","role":"ADMIN|CONTROLLER","zoneId":<n>}]
```

## 6. Diseño

```
nodes/server-admin/auth-service/
└── src/main/java/edu/icesi/sitmmio/authservice/
    ├── Main.java
    ├── adapter/AuthServiceI.java
    ├── domain/Jwt.java                (HS256, secret en cfg)
    ├── service/PasswordHasher.java    (bcrypt)
    └── io/UserRepository.java
```

JWT claims: `sub`, `role`, `zoneId`, `exp` (1h). Librería `nimbus-jose-jwt`.

## 7. Aspectos distribuidos

- Stateless: JWT contiene todo.
- Clientes validan localmente con secret compartido (HS256). RSA + JWKS post-piloto.
- Métricas: `logins_total{success|fail}`, `validations_total`.

## 8. Criterios de aceptación

- [ ] `login(usuario, ok)` → JWT con `exp` futuro.
- [ ] `login(usuario, mal)` → `Unauthorized`.
- [ ] `validate(jwt)` retorna `UserContext`.
- [ ] JWT expirado → `Unauthorized`.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Secreto en cfg | Excluir del Git; `.example` plantilla. |
| Bruteforce | `--max-attempts=5/min`. |
| Reloj desalineado | `--clock-skew=30s`. |

## 10. Decisiones diferidas

- RSA + JWKS.
- 2FA.
- CRUD R3.

## 11. Checklist

- [ ] Módulo + servant.
- [ ] `Jwt` HS256.
- [ ] `UserRepository` bcrypt.
- [ ] `users.example.json`.
- [ ] Tests login + validate.

## 12. Post-mortem

A llenar tras implementación.
