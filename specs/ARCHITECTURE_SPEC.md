# Levana - Architecture Specification

## Document Status

**Version:** 1.0
**Status:** Draft

---

## 1. Overview

This document defines the technical architecture decisions for Levana, a Hebrew calendar Android application.

---

## 2. Technology Stack

| Area | Decision | Rationale |
|------|----------|-----------|
| Language | Kotlin | Modern, null-safe, coroutines, required for Compose |
| UI Framework | Jetpack Compose | Declarative, Material 3 native, aligns with MVI |
| Architecture | MVI | Predictable state, testable, single source of truth |
| Dependency Injection | Koin | Simple, fast builds, no annotation processing |
| Local Storage | Room + DataStore | Room for structured data, DataStore for preferences |
| Navigation | Navigation Compose | Official, well-supported |
| Hebrew Calendar | KosherJava | Battle-tested, comprehensive, handles all edge cases |
| Async/Reactive | Coroutines + Flow | Kotlin-native, Compose integration |
| Build System | Gradle Kotlin DSL | Type-safe, IDE autocomplete |
| Code Quality | ktlint | Standard Kotlin linting |
| City Database | KosherJava built-in | Start simple, expand later |

---

## 3. Testing Stack

| Purpose | Tool |
|---------|------|
| Unit Tests | JUnit4 |
| Assertions | Truth |
| Mocking | MockK |
| Snapshot Testing | Paparazzi |
| Compose UI Tests | Compose Testing APIs |
| Flow Testing | Turbine |
| DI Validation | koin-test (required since Koin lacks compile-time checks) |

---

## 4. Architecture Pattern: MVI

### 4.1 Core Principles

| Principle | Description |
|-----------|-------------|
| Single State | Each screen has one immutable state object |
| Unidirectional Flow | State flows down, events flow up |
| Pure UI | Composables are pure functions of state |
| Explicit Intents | All user actions are explicit intent objects |
| Deterministic | Same state always produces same UI |

### 4.2 Data Flow

```
Intent → ViewModel → State → UI
   ↑                         │
   └─────────────────────────┘
         User Events
```

---

## 5. Architecture Layers

| Layer | Responsibility |
|-------|----------------|
| **UI** | Render state, capture user events |
| **State** | Manage screen state, process intents |
| **Domain** | Business logic, use cases |
| **Data** | Data access, storage, KosherJava integration |

Dependencies flow inward: UI → State → Domain → Data

---

## 6. Storage Strategy

| Data Type | Storage | Examples |
|-----------|---------|----------|
| Structured data | Room | Personal events (birthdays, yahrzeits) |
| User preferences | DataStore | Location, minhag, notification settings |
| City locations | KosherJava built-in | Initial implementation, expand later |

---

## 7. Testing Strategy

### 7.1 State Catalog Pattern

Each screen will define explicit state catalogs for:
- Automated snapshot testing of all UI variations
- Predictable, reproducible test cases
- Documentation of possible screen states

### 7.2 Koin Validation

DI configuration must be validated via koin-test to catch runtime errors that would otherwise be missed due to lack of compile-time checking.

---

*This document should be updated as architecture decisions evolve.*
