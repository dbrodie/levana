# Fix: Evening Omer Reminder Uses the Wrong Count

## Summary

`DailyNotificationWorker.handleOmer` reads `omerDay` for the civil date `today` and uses it
for both reminders. The Hebrew day — and with it the Omer count — begins at nightfall, so the
tzait reminder fired on the evening of `today` must announce the count of the *upcoming*
Hebrew day (`today + 1`). The current code is off by one every evening, never reminds on the
first night (15 Nissan's civil date has no count yet), and reminds with a stale 49 on the
evening after the final count.

A second defect: `getTzaitTime(...) ?: return` exits the whole function, so when tzait cannot
be computed (e.g. polar locations) the morning reminder is silently dropped too.

## Fix

- Evening (tzait) reminder: count = `omerDay` of `today + 1`; scheduled only when that is
  non-null and tzait is available.
- Morning reminder: count = `omerDay` of `today`; scheduled whenever that is non-null,
  independent of tzait.
- The decision is extracted into a small internal `planOmerReminders(...)` function so it can be
  unit tested; `handleOmer` just schedules what the plan returns.

| Civil date (evening of) | Tzait reminder | Morning reminder |
|---|---|---|
| 15 Nissan | day 1 (first night) | none |
| 16 Nissan | day 2 | day 1 |
| 4 Sivan | day 49 (last night) | day 48 |
| 5 Sivan | none | day 49 |

No changes to scheduling, alarm request codes, receivers or notification texts.

## Tests

JVM/Robolectric tests for `planOmerReminders` using the real `CalendarRepository`: first
night, an ordinary night, last night, the evening after the final count, a date outside the
Omer, tzait unavailable (morning still planned), and each preference toggled off.
