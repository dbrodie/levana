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
| Tests | JUnit4, Robolectric, Compose testing APIs and Roborazzi screenshot baselines |

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
- The developer override is a fixed date (`UserPreferences.devDateOverride`, test only, read only by `DeviceContext`): `now` is that date at the real time of day. `DeviceNow.devDayShift` is its current distance from the real date; reminder alarms are planned on the device clock and mapped to the same time on the real day with `DeviceNow.toSystemInstant`. With the override on, plans cover only that date, and the midnight roll-over (`icon/DateRollover`, which also keeps the launcher icon current while the app is stopped) re-plans them each real day.
- A stored location fix keeps only its place: `PreferencesRepository` reads it in the device's current zone, never the zone it was stored with, so a zone change re-zones it without a new fix. When its longitude is more than 4 hours of solar time from the device zone's offset, the app shell shows a one-time notice pointing to the system Date & time settings (`device/ZoneMismatch`); the times stay on the device clock.
- Without a location fix, `CityRepository.defaultForTimeZone` picks the device position: a city in the device zone or an alias of it, else the closest current UTC offset (ties: outside Israel, then at or west of the device offset, then by name). Israel auto-detect reads the device zone, never a city.
- Preserve absolute instants for reminder triggers, including DST and post-midnight nightfall. Display dates, source observance dates and trigger instants are distinct.
- WorkManager refreshes reminder plans and delivers daily holiday, fast and personal-event notices.
- Inexact AlarmManager alarms deliver candle-lighting and Omer reminders. Reconcile alarms after relevant settings changes, reboot and device clock/timezone changes.
- Persistent alarm identities and delivery records support cancellation and duplicate suppression.

## Verification

Unit and Robolectric tests cover calendar rules, repositories, provider IO, migrations, ViewModels and reminder planning. Compose tests cover UI behavior and semantics. Device checks remain necessary for platform behavior and accessibility.

Roborazzi screenshot baselines cover the app shell and Settings:
- **Scope:** the drawer and the top bar of each route, Settings with its dialogs and sub-screens, Locations, the city picker and system calendars. Each is captured in light/dark × English/Hebrew (RTL).
- **Location:** tests in `app/src/test/.../screenshots/`; baselines committed in `app/src/test/screenshots/`.
- **How the tests run:** each launches the real `MainActivity` under Robolectric (NATIVE graphics, one fixed mdpi phone qualifier) and navigates by UI. They use the production Koin graph with a fixed `DeviceContext` clock, an in-memory database and seeded preferences.
- **When images change:** a deliberate visual change re-records the affected baselines in the same commit; any other diff is a regression.
- Record: `./gradlew :app:recordRoborazziDebug --tests '*.screenshots.*'`
- Verify: `./gradlew :app:verifyRoborazziDebug`
- Both commands are up-to-date-cached; add `:app:testDebugUnitTest --rerun` to force a fresh run.

Build/test commands and agent workflow live in `AGENTS.md`; feature plans and verification evidence live in Backlog.md.
