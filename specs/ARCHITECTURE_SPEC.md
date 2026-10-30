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
- Wide windows (foldable inner displays, tablets): every screen is still one phone-style column. `ui/LargeScreenLayout.kt` caps single-column content at `ContentMaxWidth` (640dp, the day sheet's Material cap) and centres it: `Modifier.contentWidth()` after a `verticalScroll`, or `ContentWidthBox`'s gutter as lazy-list content padding, so the gutters still scroll. Below the cap it measures exactly as before, so phones are unchanged. The month grid stays full width. Text in fixed-size cells (month grid, date picker, walkthrough illustrations) caps its font scale at `CellMaxFontScale` (1.3) through `CappedFontScale`. Adaptive layouts (two panes, a navigation rail) are TASK-52.4.
- Colors: `ui/theme/levanaColorScheme` is the one scheme selection, shared by the app and the home-screen widgets. Its precedence is the holiday theme (holiday theming on and a holiday active, or the developer forced theme), then Material You wallpaper colors when enabled, then Levana's own colors. Light/dark applies to whichever wins. The app resolves light/dark from the Theme setting, and the status/navigation bar icons follow that, not the system mode. Widgets keep the launcher's (system) light/dark and refresh their wallpaper colors on their next update.

## Data Ownership and Safety

- Room stores local personal events. Commit exported schemas and provide explicit migrations for schema changes; never use destructive migration fallback.
- DataStore stores settings, current/saved locations, calendar selection and notification preferences.
- Bundled `assets/cities.json` supplies city coordinates, elevation and timezone.
- Contacts stores custom Hebrew-birthday fields; Calendar Provider supplies read-only device events. Permissions gate both integrations.
- Personal-event import/export uses JSON/CSV through Android document URIs; it does not back up all app data.
- Provider and file IO must be cancellable and run off the main thread.

## Dates and Notifications

- `device/DeviceContext` is the one source of now, today, the device time zone and the device position. `DeviceContext.now()` returns a `DeviceNow` (instant, zone, position, developer day shift). Every date, reminder plan, notification text, theme/icon/tile date and Israel/Diaspora decision reads it; nothing else calls `LocalDate.now()`, `Clock.systemDefaultZone()` or `ZoneId.systemDefault()` (only its `realClock`/`realZone`/`realToday` helpers do).
- `UserPreferences.displayCity` is the selected city. Only the Zmanim screen, the day panel's times, the agenda's candle-lighting/havdalah times and the city pickers read it; using it for dates, notifications or Israel/Diaspora is a defect. Its zmanim are computed in its own zone for the device's date.
- The developer override is a fixed date (`UserPreferences.devDateOverride`, test only, read only by `DeviceContext`): `now` is that date at the real time of day. `DeviceNow.devDayShift` is its current distance from the real date; reminder alarms are planned on the device clock and mapped to the same time on the real day with `DeviceNow.toSystemInstant`. With the override on, plans cover only that date, and the midnight roll-over (`icon/DateRollover`, which also keeps the launcher icon current while the app is stopped) re-plans them each real day.
- A stored location fix keeps only its place: `PreferencesRepository` reads it in the device's current zone, never the zone it was stored with, so a zone change re-zones it without a new fix. When its longitude is more than 4 hours of solar time from the device zone's offset, the app shell shows a one-time notice pointing to the system Date & time settings (`device/ZoneMismatch`); the times stay on the device clock.
- Without a location fix, `CityRepository.defaultForTimeZone` picks the device position: a city in the device zone or an alias of it, else the closest current UTC offset (ties: outside Israel, then at or west of the device offset, then by name). Israel auto-detect reads the device zone, never a city.
- Preserve absolute instants for reminder triggers, including DST and post-midnight nightfall. Display dates, source observance dates and trigger instants are distinct.
- WorkManager refreshes reminder plans and delivers daily holiday, fast and personal-event notices.
- Inexact AlarmManager alarms deliver candle-lighting and Omer reminders. Reconcile alarms after relevant settings changes, reboot and device clock/timezone changes.
- Persistent alarm identities and delivery records support cancellation and duplicate suppression.

## Feature Walkthrough

Each major feature has one walkthrough screen (`ui/walkthrough/`, TASK-43). The app records which screen IDs the user has seen; a seen screen is never shown again, so a new install sees every screen and an update shows only the screens it adds.

- **Registry:** `WalkthroughRegistry.screens` lists `FeatureScreen`s in display order: a permanent ID, title and body strings, an optional `WalkthroughPermission` asked on the screen, an optional `isAvailable` check, and the illustration.
- **Seen state:** `PreferencesRepository.walkthroughSeenIds` (DataStore key `walkthrough_seen_ids`, outside `UserPreferences`). A screen is seen once displayed; Skip, Get started and Got it mark the whole run seen. Replay from About records nothing; Developer settings → Reset walkthrough clears the set.
- **When it opens:** `LevanaApp` starts the activity-scoped `WalkthroughViewModel` and shows `WalkthroughScreen` in place of the shell while a run is active. A launch from a notification, widget or tile (a deep link) shows nothing; the screens wait for the next normal launch. `WalkthroughGate` is off under Robolectric and instrumentation, so tests see the app as before unless they bind `WalkthroughGate(showOnLaunch = true)` (see `ScreenshotTest.testModules`).
- **Page indicator:** counted over the run: one screen shows only Got it; 2–9 show dots; 10 or more show "n of m". Each page's title announces "Page n of m" to TalkBack.

To add a screen when a major feature ships:
1. Add a `FeatureScreen` to `WalkthroughRegistry.screens` at its place in the order, with a new kebab-case ID. Never rename, reuse or reorder IDs to mean something else; removing a screen leaves its ID unused for good.
2. Write its illustration in `WalkthroughIllustrations.kt`: a small live mini-UI built from the app's components or simplified versions, in the app theme, with fixed sample data, wrapped in `Decorative` so TalkBack reads the title and body instead.
3. Add the title, body and any sample strings in English and Hebrew (`walkthrough_strings.xml`). If the feature needs a permission, add a `WalkthroughPermission` with its reason, allow and granted strings.
4. Add the ID to `WalkthroughSelectionTest`'s approved list, record the new baselines in `WalkthroughScreenshotTest` and keep the 1/3/12-screen runs passing.

Bug fixes and small changes get no screen.

## Verification

Unit and Robolectric tests cover calendar rules, repositories, provider IO, migrations, ViewModels and reminder planning. Compose tests cover UI behavior and semantics. Device checks remain necessary for platform behavior and accessibility.

Roborazzi screenshot baselines cover the app shell and Settings:
- **Scope:** the drawer and the top bar of each route, Settings with its dialogs and sub-screens, Locations, the city picker, system calendars, the date picker and the feature walkthrough. Each is captured in light/dark × English/Hebrew (RTL).
- **Location:** tests in `app/src/test/.../screenshots/`; baselines committed in `app/src/test/screenshots/`.
- **How the tests run:** each launches the real `MainActivity` under Robolectric (NATIVE graphics, a fixed mdpi phone qualifier unless a test passes a foldable size) and navigates by UI. They use the production Koin graph with a fixed `DeviceContext` clock, an in-memory database and seeded preferences.
- **When images change:** a deliberate visual change re-records the affected baselines in the same commit; any other diff is a regression.
- **Foldable sizes:** `FoldableSize` holds the Galaxy Z Fold8 and Fold8 Ultra windows in dp: each cover, the inner display in portrait and landscape, and a split-screen half of the landscape inner. They are Samsung's published pixels at an assumed 420 dpi, as the whole display. A test passes `size.qualifiers` as the `ScreenshotTest` device, and the light/dark and language variant applies on top. `FoldableScreenshotTest` covers the key screens at every size in English light (calendar, date picker, event editor, Zmanim, Agenda, Settings), `FoldableWalkthroughScreenshotTest` the walkthrough, and `FoldableLargeTextScreenshotTest` 200% text on both covers (`ScreenshotTest` takes an optional `fontScale`). **Audit mode** renders every screenshot test at one size into a directory outside the worktree, for reviewing all screens without committing them: `./gradlew :app:recordRoborazziDebug --tests '*.screenshots.*' -Plevana.audit.size=FOLD8_INNER_LANDSCAPE -Plevana.audit.dir=/tmp/audit` (optionally `-Plevana.audit.fontScale=2`); never with verify. Specs and sources are in TASK-52.1. The matching emulators and fold/unfold commands are in the android-debug skill (`references/foldables.md`).
- Record: `./gradlew :app:recordRoborazziDebug --tests '*.screenshots.*'`
- Verify: `./gradlew :app:verifyRoborazziDebug`
- Both commands are up-to-date-cached; add `:app:testDebugUnitTest --rerun` to force a fresh run.

Build/test commands and agent workflow live in `AGENTS.md`; feature plans and verification evidence live in Backlog.md.
