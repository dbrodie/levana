# Fix: Daily Worker Only Schedules Today's Reminder Alarms

## Summary

`DailyNotificationWorker` is a 1-day `PeriodicWorkRequest` that runs at an arbitrary time of
day. Each run schedules AlarmManager alarms for **today only**, and
`NotificationAlarmScheduler` drops any trigger that is already in the past. So:

- A run after nightfall drops today's tzait Omer alarm, and tomorrow's isn't scheduled until
  the next run, which may again come after nightfall. The evening reminder can be missed
  every night.
- A run after the Omer morning time drops today's morning reminder in the same way.
- A run after the "hours before" candle-lighting time drops that reminder. The "morning"
  mode only works because it falls back to posting immediately, which can be hours late.
- Doze or app-standby delays of a day or more leave future mornings and evenings without
  alarms.

## Scope

**In scope:** every worker run plans and schedules the alarm-based reminders (candle
lighting, Omer tzait, Omer morning) for a bounded window of upcoming civil dates, with
correct dates, Omer counts, time zones and DST handling.

**Out of scope (separate task):** rescheduling when settings change (watching preferences,
triggering a rebuild on a preference write), and refactoring alarm request codes or
notification IDs. Neither is done here. Request codes stay `base + dayOfYear`; these never
collide inside a window shorter than a year.

Posted notices (holidays, fasts, personal events, birthdays) are unchanged. They are still
posted only for today's run, and no future notice is posted early.

## Design

### Reusable forward plan (`ReminderAlarmPlan.kt`)

A pure function computes the plan, so both the daily worker and a future alarm rebuilder can
use it:

```kotlin
internal fun planReminderAlarms(
    today: LocalDate, now: Instant, deviceZone: ZoneId,
    location: Location, prefs: UserPreferences,
    zmanimRepository: ZmanimRepository, calendarRepository: CalendarRepository,
    lookaheadDays: Int = REMINDER_LOOKAHEAD_DAYS,
): ReminderAlarmPlan
```

- The window is the civil dates `today .. today + lookaheadDays`, with
  `REMINDER_LOOKAHEAD_DAYS = 7`. That leaves a week of margin if periodic work is deferred.
- `ReminderAlarmPlan.alarms` lists `ReminderAlarm`s (`CandleLighting`, `OmerEvening`,
  `OmerMorning`). Each has its civil `date` and an absolute `triggerAt: Instant`. Only
  triggers strictly after `now` are included.
- `ReminderAlarmPlan.dueCandleLighting` is today's "morning"-mode candle-lighting notice when
  its morning trigger has already passed. This keeps the existing post-immediately fallback,
  and it applies to **today only**.
- The plan respects `notifyCandleLighting` and `notifyOmer`.

Per date, the existing semantics are kept as they are:

- Candle lighting uses `hasCandles`, `getShabbatInfo(..).candleLightingTime` and
  `getObservanceEnd`. These include the verified holiday-transition rules.
- Omer uses the existing `planOmerReminders(date, …)` unchanged. The evening count is the
  `date + 1` count at tzait of `date`, the morning count is the `date` count, and a missing
  tzait drops only the evening reminder.

### Trigger instants: time zones and DST

Each trigger is resolved with `ZonedDateTime.of(date, time, zone)` for **its own date**, so a
DST change inside the window gives each day the correct UTC offset. Zones are unchanged from
before:

| Reminder | Wall-clock time | Zone |
|---|---|---|
| Candle lighting, morning mode | user morning time | location zone |
| Candle lighting, hours-before mode | candle-lighting time − N hours (elapsed) | location zone |
| Omer tzait | tzait of the date | location zone |
| Omer morning | user morning time | device zone |

- A time that falls in a DST gap moves forward (java.time default). In an overlap, the
  earlier offset is used.
- "Hours before" is now subtracted on the zoned date-time rather than on `LocalTime`, so it
  can no longer wrap past midnight onto the wrong day.
- The "has today's morning trigger passed" check now compares instants. Before, it compared
  device-zone `LocalTime.now()` against a location-zone alarm time.
- `today` is `devDateOverride ?: LocalDate.now(clock)`, where the clock uses the device zone.

### Scheduling

`NotificationAlarmScheduler` gets `schedule(alarm: ReminderAlarm)`, which keeps the existing
actions, extras and request codes. Its past-trigger guard reads an injectable `now` (by
default `System::currentTimeMillis`). Each run also cancels, for every date in the window,
the alarm kinds that are not in the current plan. That keeps the window consistent with
today's computation, for example after the Omer ends. Re-scheduling an existing alarm
replaces it (`FLAG_UPDATE_CURRENT`), so repeated runs are idempotent.

### No duplicate morning candle-lighting notice

With lookahead, today's morning alarm is usually set by an earlier run. A later run the same
day after the morning time would otherwise post the notice a second time. A small
`NotificationDeliveryLog` records the date of the last candle-lighting notice delivered,
whether by the alarm receiver or by the worker's fallback. It uses its own DataStore file,
`notification_delivery`, separate from user preferences, so writes don't look like settings
changes. The worker posts `dueCandleLighting` only if today's notice hasn't been delivered.
After a reboot, which clears alarms, the boot-time run still posts a missed notice.

## Tests (JVM / Robolectric)

`ReminderAlarmPlanTest`, using the real repositories and fixed `now` values:

- A run before the morning time schedules today's morning alarms plus future days.
- A run after the morning time posts today's candle-lighting notice as due and schedules
  future mornings. Today's Omer morning alarm is skipped, and later days are still planned.
- A run after nightfall skips today's tzait alarm, and tomorrow's and later evenings are
  planned with the correct counts (first night, ordinary night, day 49, nothing after).
- "Hours before" in a future week is planned, and today's passed trigger is not.
- DST: New York spring-forward and fall-back windows. Morning alarms resolve to the correct
  offset per date, and a gap time moves forward.
- Location zone differs from device zone: Omer morning uses the device zone, and candle
  lighting and tzait use the location zone.
- Date and year rollover: a window that spans 31 Dec → 1 Jan produces distinct request codes
  and the correct dates.
- Disabled preferences produce no alarms.

`ReminderAlarmSchedulingTest` (Robolectric, `ShadowAlarmManager`):

- Scheduling a plan registers each alarm at its planned instant with the correct extras.
- Passed triggers are not registered.
- Re-running a plan replaces alarms instead of duplicating them.
- Applying a newer plan cancels alarms that are no longer planned.
- The overdue morning candle-lighting notice is posted once per date, and not at all if the
  delivery log already records it.
