# Increment 15: Notifications

## Summary

Notification system for candle lighting, holidays, fasts, personal events, and Omer counting. A single daily WorkManager job runs each morning, posting immediate notifications and scheduling exact AlarmManager alarms for time-sensitive events. All notification categories are independently configurable in Settings with user-controlled timing. Deep-links from notifications open the relevant day detail screen.

## What Was Built

### Notification Channels
- `NotificationChannels` object creates five Android notification channels on app startup:
  - **Candle Lighting** (`candle_lighting`) — `IMPORTANCE_HIGH` for time-sensitive reminders
  - **Holidays** (`holidays`) — `IMPORTANCE_DEFAULT`
  - **Fasts** (`fasts`) — `IMPORTANCE_DEFAULT`
  - **Personal Events** (`personal_events`) — `IMPORTANCE_DEFAULT`
  - **Omer** (`omer`) — `IMPORTANCE_DEFAULT`

### NotificationPoster
- Builds `NotificationCompat` notifications with appropriate channel, title, and body
- Creates deep-link `PendingIntent` targeting `MainActivity` with `EXTRA_DATE_EPOCH_DAY`
- Collision-safe notification IDs: `channelOrdinal * 10000 + dayOfYear`
- Methods: `postCandleLighting()`, `postHoliday()`, `postFast()`, `postPersonalEvent()`, `postOmer()`
- Candle lighting body includes both lighting time and Shabbat end time
- Fast body includes start and end times
- Omer body includes day count with weeks/days breakdown

### NotificationAlarmScheduler
- Schedules inexact alarms via `AlarmManager.setAndAllowWhileIdle()`
- `scheduleCandleLighting()` — for "hours before" mode, fires N hours before candle lighting
- `scheduleOmerReminder()` — fires at sunset for Omer counting
- No special permission needed — fires within a few minutes of target time
- Converts `LocalTime` + timezone → epoch millis; skips if time already past
- `cancelAll()` cancels all pending alarm intents

### NotificationAlarmReceiver
- `BroadcastReceiver` for exact alarm intents
- Reads action (`CANDLE_LIGHTING_ALARM` / `OMER_ALARM`) and extras
- Dispatches to `NotificationPoster` to build and post the notification

### DailyNotificationWorker
- `CoroutineWorker` with `KoinComponent` for dependency injection
- Runs daily via `PeriodicWorkRequest` (enqueued in `LevanaApplication.onCreate()`)
- Reads `UserPreferences` to determine which categories are enabled
- **Candle lighting**: Checks `hasCandles(today)`, supports "morning" mode (immediate or scheduled) and "hours before" mode (alarm)
- **Holidays**: Looks ahead `holidayNotifyDaysBefore` days for Torah/Rabbinic holidays
- **Fasts**: Checks tomorrow for fast days — all fasts notify the day before
- **Personal events**: Posts for today's personal events and contact birthdays
- **Omer**: Schedules alarm at sunset for today's Omer count
- Companion: `enqueueDaily()` and `enqueueImmediate()`

### BootReceiver
- `BroadcastReceiver` for `ACTION_BOOT_COMPLETED`
- Calls `DailyNotificationWorker.enqueueImmediate()` to reschedule after reboot

### Settings UI
- `NotificationsSection` in `SettingsScreen` with:
  - **Candle Lighting** toggle + mode selector (morning/hours-before) + time/hours config
  - **Holidays** toggle + days-before picker (0-14)
  - **Fasts** toggle (no extra config — always notifies day before)
  - **Personal Events** toggle
  - **Omer** toggle
- `POST_NOTIFICATIONS` permission requested on first toggle enable

### Deep-Link Navigation
- `MainActivity` reads `EXTRA_DATE_EPOCH_DAY` from intent
- `LevanaApp` navigates to `DayDetailRoute` via `LaunchedEffect`

### ZmanimRepository Helpers
- `getSunsetTime(date, location)` — extracts sunset time for Omer alarm scheduling
- `getFastTimes(date, location)` — returns start/end times for fast days (dawn for minor fasts, previous sunset for Tisha B'Av; nightfall for end)

## Notification Timing

| Category | When Notification Fires |
|----------|------------------------|
| Candle Lighting (morning mode) | Configurable morning time (default 8:00 AM) on erev Shabbat/Yom Tov |
| Candle Lighting (hours-before mode) | N hours before candle lighting time (alarm) |
| Holidays | Up to N days before (configurable 0-14, default 1) |
| Fasts | Morning of the day before the fast |
| Personal Events | Morning of the Hebrew date match |
| Omer | At sunset (alarm) |

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scheduling | WorkManager daily + AlarmManager inexact | WorkManager for reliable daily trigger; AlarmManager for time-based notifications (no special permission needed) |
| DI in Worker | KoinComponent interface | WorkManager creates workers via factory; KoinComponent allows field injection |
| Fast timing | All fasts notify day before | User requested: gives advance notice for all fasts |
| Notification IDs | `channelOrdinal * 10000 + dayOfYear` | Prevents collisions across channels while allowing same-day updates |
| Permission flow | Request on first toggle enable | Non-intrusive; only asks when user explicitly wants notifications |

## New Files

- `notifications/NotificationChannels.kt` — Channel creation
- `notifications/NotificationPoster.kt` — Notification building and posting
- `notifications/NotificationAlarmScheduler.kt` — Exact alarm scheduling
- `notifications/NotificationAlarmReceiver.kt` — Alarm broadcast receiver
- `notifications/DailyNotificationWorker.kt` — Daily WorkManager job
- `notifications/BootReceiver.kt` — Boot completed receiver

## Files Modified

- `gradle/libs.versions.toml` — Added `work = "2.10.0"` and `androidx-work-runtime-ktx`
- `app/build.gradle.kts` — Added `implementation(libs.androidx.work.runtime.ktx)`
- `AndroidManifest.xml` — Added `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` permissions; registered receivers
- `LevanaApplication.kt` — Channel creation and worker enqueue on startup
- `AppModules.kt` — Added `NotificationAlarmScheduler` to Koin
- `UserPreferences.kt` — Added 9 notification preference fields
- `PreferencesRepository.kt` — Added DataStore keys, mapping, and save methods
- `SettingsState.kt` / `SettingsIntent.kt` / `SettingsViewModel.kt` — MVI wiring for notification settings
- `SettingsScreen.kt` — `NotificationsSection` composable
- `MainActivity.kt` — Deep-link intent handling
- `ZmanimRepository.kt` — `getSunsetTime()` and `getFastTimes()` helpers

## Acceptance Criteria

- [x] Candle lighting notification fires at configured time on erev Shabbat/Yom Tov
- [x] Holiday notification fires configurable days before holiday begins
- [x] Fast notification fires the day before the fast with start/end times
- [x] Personal event notification fires on the day of the event
- [x] Omer counting reminder fires at sunset during Omer period
- [x] Tapping a notification opens the relevant day detail screen
- [x] Notifications survive device reboot (BootReceiver)
- [x] Per-category toggles in settings enable/disable notifications
- [x] `./gradlew build` passes
