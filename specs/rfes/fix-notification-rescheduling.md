# Fix: rebuild notification alarms when notification settings change

## Problem

Candle-lighting and Omer reminders are exact alarms (AlarmManager) scheduled only by
`DailyNotificationWorker`, which runs once a day and on boot. Changing a notification
setting only writes DataStore, so:

- enabling a reminder, or changing its type/time, has no effect until the next daily run;
- changing the active location (Settings, the location chooser, the city picker, or a
  current-location fix) leaves alarms at the old location's times;
- disabling a category leaves its pending alarm in place, and the receiver posts it anyway.

## Fix

1. **`NotificationAlarmRebuilder`** (Koin single) owns alarm scheduling. `rebuild()` reads the
   current preferences, cancels every pending reminder alarm and schedules today's future
   candle-lighting / Omer alarms for the enabled categories only. It is serialized with a
   mutex so the worker and a settings change can't interleave and leave a stale alarm.
   `rebuild()` never posts a notification itself.
2. **Observe preferences, not call sites.** `LevanaApplication` collects the preferences flow,
   maps it to the settings that affect alarms (`AlarmSettings`: enable flags, candle mode /
   morning time / hours before / offset, Omer tzait / morning / morning time, active location,
   dev date override), and calls `rebuild()` whenever that projection changes. This covers
   every UI and location path without touching each one, and unrelated preference changes
   (theme, minhag, holiday/fast/personal-event toggles, zmanim list…) trigger nothing.
3. **Worker** delegates alarm scheduling to `rebuild()`; it still posts the immediate
   candle-lighting notice when "morning" mode's time has already passed, and still posts
   holiday / fast / personal-event notifications only on its own daily/boot run. So no
   settings change ever re-posts holiday or personal-event notifications.
4. **Delivery guard.** `NotificationAlarmReceiver` reads the preferences when an alarm fires
   and drops it unless its category (and Omer sub-option) is still enabled.

## Out of scope

Holiday / fast / personal-event notifications are posted (not scheduled) by the daily worker;
their timing is unchanged. No change to alarm request codes or notification texts.

## Tests (JVM / Robolectric)

- `AlarmSettings` projection changes for alarm-relevant settings only.
- Observer triggers a rebuild for alarm-relevant changes and not for unrelated ones.
- Rebuilder, against Robolectric's `AlarmManager`: enabling schedules the alarm; changing mode,
  time, or location replaces it; disabling cancels it.
- Receiver guard decision per action / preference combination.
