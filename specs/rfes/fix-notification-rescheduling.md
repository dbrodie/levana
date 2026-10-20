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
   dev date override, Israel/Diaspora), and calls `rebuild()` whenever that projection changes.
   This covers every UI and location path without touching each one, and unrelated preference
   changes (theme, minhag, holiday/fast/personal-event settings, zmanim list…) trigger no rebuild.
3. **Worker** delegates alarm scheduling to `rebuild()`; it still posts the immediate
   candle-lighting notice when "morning" mode's time has already passed, but only on its full
   daily/boot run — an alarm rebuild never posts.
4. **Delivery guard.** `NotificationAlarmReceiver` reads the preferences when an alarm fires
   and drops it unless its category (and Omer sub-option) is still enabled.
5. **Daily categories refresh per category.** Holiday, fast and personal-event notifications are
   posted (not scheduled) by the worker. The application also watches each category's own
   settings, kept only while that category is enabled:
   - holidays: lead days, Israel/Diaspora;
   - fasts: Israel/Diaspora, active location (fast start/end times);
   - personal events: yahrzeit rules;
   - all: dev date override.
   When a category's settings change and it is enabled (including being switched on), a one-time
   `DailyNotificationWorker` run is enqueued for **just that category** (unique work per
   category, latest wins). Other categories are not re-posted, and unrelated settings trigger
   nothing. Switching a category off cancels its pending refresh, and the worker re-reads the
   saved preferences right before posting each category, so a disabled category is never
   delivered by a stale or in-flight run. A run without a category list is the full daily/boot
   run (alarms + every enabled category), as before.
6. `isInIsrael` is an alarm setting too (candle-lighting times may depend on it).

## Out of scope

No change to alarm request codes, notification IDs or texts, or the daily schedule.

## Tests (JVM / Robolectric)

- `AlarmSettings` projection changes for alarm-relevant settings only.
- Observer triggers a rebuild for alarm-relevant changes and not for unrelated ones.
- Rebuilder, against Robolectric's `AlarmManager`: enabling schedules the alarm; changing mode,
  time, or location replaces it; disabling cancels it.
- Receiver guard decision per action / preference combination.
- Per-category change detection (enable, lead days, Israel, location, unrelated settings).
- Real DataStore preferences + the real worker: enabling / changing one category posts only that
  category without re-posting the others; unrelated changes post nothing; a stale refresh or
  daily run doesn't post a disabled category.
