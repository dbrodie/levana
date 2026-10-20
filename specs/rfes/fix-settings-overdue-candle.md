# Fix: settings changes drop today's undelivered morning candle-lighting alarm

## Problem

`planReminderAlarms` puts today's morning-mode candle-lighting notice whose time has passed in
`ReminderAlarmPlan.dueCandleLighting` (not `overdue`). `NotificationAlarmScheduler.scheduleAll`
treats only `alarms + overdue` as wanted, so it cancels that pending alarm. The daily worker
then posts the notice itself (once, via `NotificationDeliveryLog`), but a settings-only
`NotificationAlarmRebuilder.rebuild()` posts nothing — the reminder is lost.

Reproduction: Friday 2026-10-23, New York, morning mode 09:30. At 08:00 the 09:30 alarm is
scheduled. At 09:35, before Android has delivered it (e.g. Doze), the user changes only the
Omer morning time; the settings rebuild cancels the candle-lighting alarm and nothing posts.

## Fix

1. `scheduleAll` treats `dueCandleLighting` as wanted, like `overdue`: it is neither cancelled
   nor rescheduled (its trigger has passed), so an undelivered alarm keeps its identity and
   still fires. Reminders that are no longer wanted (category off, not a candle day, other
   dates in the window) are still cancelled.
2. The daily worker's catch-up (`applyReminderAlarmPlan`) keeps its semantics: it explicitly
   cancels today's due candle-lighting alarm, then posts the notice once (delivery log).
3. `NotificationAlarmRebuilder` takes a `Clock` (default system clock) so the real `rebuild()`
   path can be tested at controlled times.

Forward horizon, Omer and holiday behaviour are unchanged.

## Tests (Robolectric)

- Real `rebuild()` with a controlled clock: 08:00 schedules 09:30; at 09:35 an Omer-time change
  keeps the same pending alarm; switching candle lighting off cancels it.
- Worker path at 09:35 cancels the alarm and posts exactly once across repeated runs.
- The verifier reproduction (`IntegrationVerifierNotificationTest`, d6c2b35) passes.
