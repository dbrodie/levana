# Fix: Colliding Notification and Alarm Identities

## Summary

`NotificationPoster` gives every reminder an integer id of `channelOrdinal * 10000 + dayOfYear`.
Android identifies a notification by `(tag, id)`, so a later post with the same id silently
replaces an earlier, unrelated notification:

- **Omer:** the morning reminder of date *D* uses `40000 + dayOfYear(D) + 1`, which is the id
  of the tzait reminder of date *D+1*; the next evening's reminder replaces the morning one if
  it is still shown.
- **Personal events / birthdays:** the id is the day id plus `key.hashCode() % 1000`, where the
  key is the event *title* or contact *name*. The modulo is a 1-in-~2000 bucket that can be
  negative, so it collides with other events, other days' ids (`30000 + doy ± 999` overlaps
  neighbouring days), and two contacts with the same display name (or two events with the
  same title) always share one id — the second overwrites the first.
- **Holidays / fasts:** one id per date, so two observances posted for the same date would
  replace each other.
- **Year boundaries:** `dayOfYear` repeats every year, so the same id is reused for the same
  day of a different year.

Alarm `PendingIntent`s in `NotificationAlarmScheduler` have the same `REQUEST_x + dayOfYear`
weakness (the full date is only an extra, which is not part of PendingIntent identity).

## Fix

### Notification identity: tag + per-type id

Every reminder is posted with `notify(tag, id, notification)`:

- **tag** — a full, human-readable, untruncated string built only from locale-independent
  data: `<type>/<ISO date>[/<observance or persistent identity>]`.
- **id** — a small constant per reminder type (scoped id), so two types can never share an
  identity even if tags were equal.

| Reminder | Tag | Id |
|---|---|---|
| Candle lighting | `candle_lighting/2026-10-23` | 1 |
| Holiday | `holiday/2026-12-05/Chanukah` (English `Holiday.name`, a constant) | 2 |
| Fast | `fast/2026-12-20/10 of Tevet` | 3 |
| Personal event | `personal_event/2026-10-20/42` (Room primary key) | 4 |
| Birthday | `birthday/2026-10-20/key:<contact lookup key>` (falls back to `name:<display name>` only if the lookup key is empty) | 5 |
| Omer, tzait | `omer_tzait/2026-04-03` | 6 |
| Omer, morning | `omer_morning/2026-04-03` | 7 |

The date is the date the reminder is about (holiday/fast date, the civil date of the evening
or morning), exactly as today, so:

- **Replacement is preserved:** reposting the same reminder (the worker runs more than once a
  day; holiday "days before" reminders are reposted each day until the holiday) replaces one
  notification. Tags contain no localized text, so reposting after an app-language change
  replaces the notification instead of adding a second one. Editing an event's title also
  replaces it, since the key is the event id.
- Same-title events and same-name contacts get distinct tags (distinct id / lookup key).
- Both Omer reminders and simultaneous observances are distinct; full ISO dates never repeat
  across years.

The update-available notification keeps its untagged id 9001 (no tag → cannot collide with
tagged reminders).

**Upgrade cleanup:** when a reminder is posted, the untagged notification the previous
version would have posted for the *same* reminder (its legacy integer id) is cancelled, so an
already-shown reminder that is reposted after upgrading is not duplicated. Legacy ids never
overlap 9001.

The content (tap) `PendingIntent` is unchanged: request code = epoch day of the target date,
same component and extras for every notification of that date, so it is already per-date.

### Alarm identity

Alarm broadcasts get a data URI `levana-alarm://<type>/<ISO date>`, making the full date part
of `Intent.filterEquals` identity; the request code becomes a per-type constant. Action and
extras are unchanged, so the receiver is unchanged. Rescheduling the same reminder for the
same date (`FLAG_UPDATE_CURRENT`) still replaces the one pending alarm.

**Migration:** when an alarm is scheduled, the alarm the previous version would have used for
the same type and date (legacy request code `REQUEST_x + dayOfYear`, no data) is cancelled if
it exists, so the reminder does not fire twice after upgrading. `cancelAll()` cancels legacy
alarms as before plus new-style alarms for a window of dates around the given date.

## Tests

- JVM tests for the identity builder: distinct tags for both Omer reminders on the same and
  adjacent dates, across Dec 31 / Jan 1 and the same day of different years, same-title events,
  same-name contacts, two holidays on one date; equal tags for repeated posts.
- Robolectric tests posting through `NotificationPoster`: same reminder posted twice (and in
  English then Hebrew) leaves one notification; Omer morning + next-day tzait both remain;
  same-name contacts both remain; legacy untagged notification is cancelled on repost.
- Robolectric tests for `NotificationAlarmScheduler`: alarms for the same type on the same
  day of different years and for both Omer types are distinct; rescheduling replaces; a legacy
  alarm for the same date is cancelled; `cancelAll()` removes new and legacy alarms.
