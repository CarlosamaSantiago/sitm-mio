---
spec-id: 00-project-scaffolding
component: (infra)
deployment-node: (todos)
status: draft
depends-on: []
blocks: [01..20]
---

# Spec 00 — Project Scaffolding

## 1. Contexto y problema

V3 requiere estructura multi-módulo Gradle que aloje `contracts/`, `domain-core/` y 13 módulos bajo `nodes/`. Combina multi-módulo + ZeroC ICE 3.7.10 (referente `EjemploSITMMIO`) con convención de paquete `edu.icesi.sitmmio` (monolith).

## 2. Trazabilidad

| Eje | Referencia |
|-----|------------|
| Componente | (infra) |
| Requisitos | R5, RNF-10, RNF-12 |
| ADR | ADR-1, ADR-10 |
| Patrón | — |
| Inconsistencia resuelta | Unifica convención monolith con multi-módulo Ice. |

## 3. Colaboradores

| Colaborador | Relación | Interfaz |
|-------------|----------|----------|
| Todos los módulos | Padre Gradle | `subprojects { ... }` |

## 4. Interfaces (Slice)

Ninguna — este spec configura build.

## 5. Modelo de datos relevante

Ninguno.

## 6. Diseño propuesto

### 6.1 `settings.gradle`

```groovy
rootProject.name = 'sitm-mio'

include 'contracts'
include 'domain-core'

include ':nodes:bus:bus-simulator'
include ':nodes:queue:datagram-queue'
include ':nodes:datacenter:ingestion-gateway'
include ':nodes:datacenter:stream-processor'
include ':nodes:datacenter:datagram-event-bus'
include ':nodes:datacenter:data-lake'
include ':nodes:datacenter:batch-master'
include ':nodes:datacenter:analytics-store'
include ':nodes:datacenter:controller-cco'
include ':nodes:datacenter:session-context'
include ':nodes:worker:batch-worker'
include ':nodes:server-admin:auth-service'
include ':nodes:server-public-api:public-api'
include ':nodes:client-cco:cco-client'
include ':nodes:extern-client:citizen-cli'
```

### 6.2 `build.gradle` raíz

```groovy
plugins { id 'java' }

allprojects {
    group = 'edu.icesi.sitmmio'
    version = '3.0.0-SNAPSHOT'
    repositories { mavenCentral() }
}

subprojects {
    apply plugin: 'java'
    java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
    dependencies {
        implementation 'com.zeroc:ice:3.7.10'
        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    }
    test { useJUnitPlatform() }
    tasks.register('copyLibs', Copy) {
        from { configurations.runtimeClasspath }
        into "${buildDir}/libs"
    }
}
```

### 6.3 Layout por módulo de nodo

```
nodes/<grupo>/<componente>/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/edu/icesi/sitmmio/<componente>/
    │   │   ├── Main.java       bootstrap Ice
    │   │   ├── adapter/        servants Ice
    │   │   ├── service/        puertos + casos de uso
    │   │   ├── domain/         lógica pura
    │   │   ├── validation/     reglas R35 / outliers
    │   │   └── io/             FS / HTTP / BD
    │   └── resources/          map.html / openapi.yaml / *.cfg
    └── test/
        └── java/edu/icesi/sitmmio/<componente>/
```

### 6.4 Plugins extras por módulo

| Módulo | Plugin |
|--------|--------|
| `contracts` | `com.zeroc.gradle.ice-builder.slice:1.5.2` |
| `cco-client` | `org.openjfx.javafxplugin:0.1.0` (`controls, fxml, web`) |
| `public-api` | Javalin 6.x o equivalente |

## 7. Aspectos distribuidos

- Cada módulo `nodes/` produce su fat-jar vía `copyLibs + jar`.
- Endpoints Ice externalizados en `nodes/<x>/<y>/src/main/resources/<componente>.cfg` (no hardcoded como en referencia).
- Versionado coordinado `3.0.0-SNAPSHOT`.

## 8. Criterios de aceptación

- [ ] `./gradlew build` compila los 15 módulos.
- [ ] `./gradlew :contracts:compileSlice` genera Java desde `sitm.ice` (spec 02).
- [ ] `./gradlew :nodes:datacenter:ingestion-gateway:copyLibs` deja Ice en `build/libs/`.
- [ ] Cada submódulo tiene `Main.java` ejecutable.
- [ ] Smoke test JUnit 5 pasa en cada módulo.

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Conflictos de versiones Ice transitivas | `resolutionStrategy.force 'com.zeroc:ice:3.7.10'`. |
| Build lento | `org.gradle.parallel=true`. |
| IPs hardcoded | Externalizar a `*.cfg` desde día 1. |

## 10. Decisiones diferidas

- Docker por nodo.
- Cache Gradle remoto.

## 11. Checklist

- [ ] `settings.gradle` con 15 includes.
- [ ] `build.gradle` raíz.
- [ ] `gradle.properties` con paralelismo + heap.
- [ ] Estructura `nodes/<grupo>/<componente>/` para 13 nodos.
- [ ] `contracts/build.gradle` con plugin Slice.
- [ ] `domain-core/build.gradle`.
- [ ] `Main.java` vacío por nodo.
- [ ] `SmokeTest.java` por módulo.
- [ ] `.gitignore` (build/, .gradle/, data/raw/*.zip, data/raw/*Pilot*.csv).

## 12. Post-mortem

A llenar tras implementación.
