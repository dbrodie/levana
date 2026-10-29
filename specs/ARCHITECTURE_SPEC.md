# Levana — Architecture Specification

## Stack

Single Android app module; Kotlin and Gradle Kotlin DSL. Minimum API 34, target/compile API 36; JVM target 17. Dependency versions live in `gradle/libs.versions.toml`.

| Area | Decision |
|---|---|
| UI | Jetpack Compose + Material 3; Navigation Compose |
| Screen architecture | MVI: State / Intent / ViewModel / Screen |
| Dependency injection | Koin |
| Calendar and zmanim | KosherJava, wrapped by repositories |
| Storage | Room for personal events; DataStore for preferences and saved locations |
| Async | Kotlin Coroutines + Flow |
| Reminders | WorkManager + AlarmManager |
| Quality | Android Lint and ktlint |
| Tests | JUnit4, Robolectric, Compose testing APIs and Roborazzi snapshots |

## Application Structure

- Composables render immutable screen state and send explicit intents to ViewModels.
- ViewModels coordinate repositories and expose state through flows. Domain models and calculation helpers represent calendar and event rules.
- Repositories own persistence, Android provider access and KosherJava integration.
- Koin modules provide repositories, shared services and screen ViewModels.
- Room flows and Contacts/Calendar content observers drive lifecycle-aware, debounced refresh of affected screens.

## Data Ownership and Safety

- Room stores local personal events. Commit exported schemas and provide explicit migrations for schema changes; never use destructive migration fallback.
- DataStore stores settings, current/saved locations, calendar selection and notification preferences.
- Bundled `assets/cities.json` supplies city coordinates, elevation and timezone.
- Contacts stores custom Hebrew-birthday fields; Calendar Provider supplies read-only device events. Permissions gate both integrations.
- Personal-event import/export uses JSON/CSV through Android document URIs; it does not back up all app data.
- Provider and file IO must be cancellable and run off the main thread.

## Dates and Notifications

- `device/DeviceContext` is the one source of now, today, the device time zone and the device position. `DeviceContext.now()` returns a `DeviceNow` (instant, zone, position, developer day shift). Every date, reminder plan, notification text, theme/icon/tile date and Israel/Diaspora decision reads it; nothing else calls `LocalDate.now()`, `Clock.systemDefaultZone()` or `ZoneId.systemDefault()` (only its `realClock`/`realZone`/`realToday` helpers do).
- `UserPreferences.displayCity` is the selected city. Only the Zmanim screen, the day panel's times and the city pickers read it; using it for dates, notifications or Israel/Diaspora is a defect. Its zmanim are computed in its own zone for the device's date.
- The developer override is a whole-day shift of the device clock (`devDayShift`, test only). Reminder alarms are planned on the device clock and mapped back to real instants with `DeviceNow.toSystemInstant` before AlarmManager sees them.
- Preserve absolute instants for reminder triggers, including DST and post-midnight nightfall. Display dates, source observance dates and trigger instants are distinct.
- WorkManager refreshes reminder plans and delivers daily holiday, fast and personal-event notices.
- Inexact AlarmManager alarms deliver candle-lighting and Omer reminders. Reconcile alarms after relevant settings changes, reboot and device clock/timezone changes.
- Persistent alarm identities and delivery records support cancellation and duplicate suppression.

## Verification

Unit and Robolectric tests cover calendar rules, repositories, provider IO, migrations, ViewModels and reminder planning. Compose tests cover UI behavior and semantics; Roborazzi supplies snapshots. Device checks remain necessary for platform behavior and accessibility.

Build/test commands and agent workflow live in `AGENTS.md`; feature plans and verification evidence live in Backlog.md.
