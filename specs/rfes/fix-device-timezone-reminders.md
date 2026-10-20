# Fix: reminder alarms keep the old device time zone after a time-zone change

## Problem

The Omer morning reminder fires at a configured wall-clock time in the **device** time zone
(e.g. 07:00). `NotificationAlarmRebuilder` is a Koin singleton whose default `Clock` is
`Clock.systemDefaultZone()`, captured once at construction, so every later rebuild in the same
process plans "today" and the morning trigger in the zone the process started in. Nothing
reacts to Android's time-zone change either: pending alarms are absolute instants, so they stay
where the old zone put them until a settings change or the next daily run.

Reproduction: device in New York, Omer morning reminder at 07:00. Change the device zone to
Moscow without restarting the process. The pending alarms (and any rebuild in that process)
still fire at 07:00 New York = 14:00 Moscow.

## Fix

1. **Resolve the clock at rebuild time.** `NotificationAlarmRebuilder` takes a clock provider
   (default `Clock::systemDefaultZone`) and reads it once per `rebuild()`, so "today", "now"
   and the device zone are those current when the rebuild runs. A constructor taking a fixed
   `Clock` remains for tests.
2. **React to the system time change broadcasts.** A manifest-registered `TimeChangeReceiver`
   handles `ACTION_TIMEZONE_CHANGED` and `ACTION_TIME_CHANGED` (`TIME_SET`), both exempt from
   the implicit-broadcast limits, so it runs even when no app UI is open (the process is
   started for it). It calls the injected singleton's `rebuild()` under `goAsync()`.
   - It uses the settings-change rebuild (no delivery log, no posting): planned future
     alarms are rescheduled under their existing identities (replaced, not duplicated), alarms
     for disabled reminders are cancelled, and passed alarms, including today's due
     candle-lighting notice, are left pending so one Android has not delivered yet still fires
     once. The candle-lighting delivery log is not touched.
   - Holiday, fast and personal-event notifications are not re-posted; no worker is enqueued.
3. **Unchanged semantics.** Candle-lighting and the Omer nightfall reminder stay in the active
   location's explicit zone, so a device zone change does not move them. Only the device-zone
   reminder (Omer morning) and the civil "today" follow the device zone.

## Out of scope

Launcher icon / Hebrew date refresh on zone change, Omer delivery deduplication across zones,
and planner/repository nightfall date logic.

## Tests (JVM / Robolectric)

- Real Koin graph's singleton, constructed in New York: after the default zone becomes Moscow,
  `rebuild()` schedules the Omer morning alarm at 07:00 Moscow.
- Manifest: `TimeChangeReceiver` is registered for `TIMEZONE_CHANGED` and `TIME_SET`.
- Broadcasting `TIMEZONE_CHANGED` through the manifest receiver moves every pending Omer morning
  alarm to 07:00 in the new zone, one alarm per date; nightfall and candle-lighting alarms keep
  their location-zone instants; no notification is posted and no worker is enqueued.
- With Omer reminders disabled, a zone change schedules no Omer alarm.
- An overdue undelivered morning candle-lighting alarm survives a zone change, nothing is
  posted, and the delivery log is unchanged.
