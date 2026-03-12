# Increment 11: Personal Events

## Overview

Add Room-backed personal events (birthdays, yahrzeits, and custom events) with full CRUD, a Hebrew date picker, yahrzeit Adar leap year recurrence rules, and calendar cell indicators.

## Event Types

| Type | Display Name | Recurrence | Notes |
|------|-------------|------------|-------|
| BIRTHDAY | `name`'s Birthday | Same Hebrew date each year | Simple recurrence |
| YAHRZEIT | `name`'s Yahrzeit | Special Adar rules (see below) | Explanatory text shown on add/edit screen |
| CUSTOM | User-provided `customTitle` | Same Hebrew date each year | User sets their own title |

## Yahrzeit Adar Rules (per Shulchan Aruch)

- Death in Adar of a non-leap year -> observe in Adar II of a leap year
- Death in Adar I of a leap year -> observe in Adar I
- Death in Adar II of a leap year -> observe in Adar II

## Data Model

### PersonalEvent (Room Entity)

```kotlin
@Entity(tableName = "personal_events")
data class PersonalEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // Person's name (birthday/yahrzeit) or empty (custom)
    val eventType: EventType,
    val customTitle: String? = null,      // Only for CUSTOM type
    val hebrewDay: Int,
    val hebrewMonth: Int,                 // JewishDate month constant
    val hebrewYear: Int,                  // Original year of the event
    val notes: String = ""
)
```

### EventType Enum

```kotlin
enum class EventType { BIRTHDAY, YAHRZEIT, CUSTOM }
```

## Room Database

- `LevanaDatabase` v1 with `PersonalEvent` entity
- `PersonalEventDao` with:
  - `getAll(): Flow<List<PersonalEvent>>`
  - `getById(id: Long): PersonalEvent?`
  - `insert(event: PersonalEvent): Long`
  - `update(event: PersonalEvent)`
  - `delete(event: PersonalEvent)`

## PersonalEventRepository

- Wraps DAO with domain logic
- `getEventsForHebrewDate(month: Int, day: Int, isLeapYear: Boolean): List<PersonalEvent>` — applies yahrzeit Adar rules
- `getEventsForGregorianDate(date: LocalDate): List<PersonalEvent>` — converts to Hebrew date, then queries
- `getEventsForMonth(yearMonth: YearMonth / HebrewYearMonth)` — returns map of date -> events for calendar indicators

## Screens

### Event List Screen (`PersonalEventsRoute`)
- List of all personal events, grouped by type
- FAB to add new event
- Tap event to edit
- Swipe or long-press to delete

### Add/Edit Event Screen (`AddEditEventRoute`)
- Event type selector (Birthday / Yahrzeit / Custom)
- Name field (for Birthday/Yahrzeit)
- Custom title field (for Custom type, shown instead of name)
- Hebrew date picker (day, month, year wheels)
- Notes field
- When Yahrzeit is selected: small explanatory text about Adar rules
- Save button

### Calendar Integration
- `HebrewDay` gains `hasPersonalEvent: Boolean` field
- Calendar cells show a small indicator (star or marker) for days with events
- Day detail screen shows personal events for that day

## Navigation

New routes:
- `PersonalEventsRoute` — event list
- `AddEditEventRoute(eventId: Long = 0)` — add (0) or edit (id)

Entry points:
- Bottom nav Settings -> "Personal Events" row
- Day detail screen -> "Add Event" button (pre-fills the date)

## Key Decisions

- Room database v1 — no migrations needed yet
- Type converters for EventType enum (stored as string)
- Hebrew date stored as raw ints (day, month, year) to avoid coupling to HebrewMonth enum in DB schema
- Yahrzeit Adar logic lives in PersonalEventRepository, not in the entity

## Acceptance Criteria

- [ ] Can add a birthday with Hebrew date and name
- [ ] Can add a yahrzeit with Hebrew date and name
- [ ] Can add a custom event with user-provided title and Hebrew date
- [ ] Selecting Yahrzeit type shows explanatory text about Adar rules
- [ ] Events appear in a list view
- [ ] Can edit and delete existing events
- [ ] Events show as indicators on their calendar cells
- [ ] Yahrzeit in Adar of non-leap year appears in Adar II of leap year
- [ ] Yahrzeit in Adar I of leap year stays in Adar I
- [ ] Hebrew date picker allows selecting valid Hebrew dates
- [ ] `./gradlew build` passes
