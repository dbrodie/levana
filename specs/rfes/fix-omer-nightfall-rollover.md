# Fix: Omer evening reminder when nightfall falls after civil midnight

## Problem

At high latitudes in late spring, nightfall (KosherJava `tzais`) for a civil date can fall
after the following civil midnight. For St. Petersburg (59.93 N, `Europe/Moscow`), nightfall
for 24 May 2027 is 25 May 2027 ≈ 00:02.

The evening Omer reminder was planned from `ZmanimRepository.getTzaitTime`, which returns only
a `LocalTime`. Joining that time back onto the evening's own date (`24 May` + `00:02`) produced
a trigger of 24 May 00:02 — almost 24 hours early — carrying the count of the Hebrew day that
actually begins on the night of 24/25 May (day 33), while the current count was still 32.

## Behaviour

- The evening Omer reminder for civil date *D* fires at the actual computed nightfall instant
  of *D*, whatever civil date that instant falls on. It keeps *D* as its identity (alarm and
  notification keys), and carries the count of the Hebrew day beginning that night (the count
  of *D* + 1). The next evening (*D* + 1) remains a separate alarm.
- The morning reminder is unchanged: device-zone morning time on *D*, count of *D*.
- Missing nightfall (e.g. no `tzais` during white nights) drops only that evening's reminder;
  morning reminders are kept, including on the first and final Omer days.
- Planning window: the plan still covers civil dates `today .. today + lookahead`. In addition,
  the evening Omer reminder of `today - 1` (the plan's *previous evening*) is planned, since
  its nightfall can fall inside today: as a future alarm while that nightfall is still ahead
  (so a run between midnight and a post-midnight nightfall schedules it), otherwise as an
  overdue alarm that is kept. Reconciliation (`scheduleAll`) also covers the evening Omer
  alarm of the previous evening, so turning the reminder off cancels it; no other reminder
  kind is reconciled for that date.
- The trigger is an absolute instant, so it is independent of the device zone and of DST
  transitions in the location zone.

## Design

- `ZmanimRepository.getTzaitInstant(date, location): Instant?` — the nightfall instant
  computed for `date`, without discarding its civil date. `getTzaitTime` is unchanged and is
  still used for display and candle-lighting/havdalah times.
- `planOmerReminders` takes the nightfall as `() -> Instant?`; `EveningOmerReminder` holds the
  nightfall `Instant` (`tzaitAt`) instead of a `LocalTime`. `planReminderAlarms` uses it
  directly as the `OmerEvening` trigger.
- `ReminderAlarmPlan.previousEvening: LocalDate?` names the extra date whose `ACTION_OMER`
  alarm `NotificationAlarmScheduler.scheduleAll` reconciles alongside the window.
- `NotificationAlarmScheduler.scheduleOmerReminder(date, LocalTime, …)` (legacy, unused by the
  planning path) is left as is.

## Out of scope

- Candle lighting at nightfall (Yom Tov into Yom Tov) and havdalah keep their `LocalTime`
  representation; the same rollover could affect an hours-before candle-lighting trigger at
  such latitudes and is tracked separately.
- `NotificationAlarmRebuilder` and system time/zone change handling.

## Tests

- `OmerNightfallRolloverTest` (JVM):
  - St. Petersburg, 24 May 2027: evening alarm dated 24 May fires at the repository nightfall
    instant on 25 May with count 33; never at 24 May 00:02.
  - Ordinary night (Jerusalem) trigger equals the nightfall instant on the same civil date.
  - Successive post-midnight evenings stay distinct, ordered, one per date, counts
    increasing by one, each after the previous evening's.
  - Run at 00:01 on 25 May (before the 00:02 nightfall): the 24 May evening is still planned
    as a future alarm with count 33; after nightfall it is overdue (kept), not rescheduled.
  - Previous evening is not planned when the reminder is off or the Omer is over.
  - First (count 1) and last (count 49) Omer evenings.
  - Missing nightfall (white nights) keeps morning alarms through the end of the Omer.
  - DST: Santiago across the April 2026 change, and a location zone different from the
    device zone; triggers equal the nightfall instant.
  - Horizon: the last date of the window plans its post-midnight evening.
- Existing `planOmerReminders` tests adapted to the `Instant` nightfall.
- `OmerNightfallRolloverSchedulingTest` (Robolectric, production apply path):
  - The 24 May evening alarm is set at the 25 May nightfall instant with count 33, and a run
    at 00:01 on 25 May keeps exactly that one alarm.
  - A first run at 00:01 on 25 May schedules it.
  - Turning the evening reminder off at 00:01 cancels it.
  - A run after that nightfall leaves an undelivered alarm in place.
