# Fix: post-midnight candle lighting / havdalah, shifted-window alarms, Omer travel duplicates, Omer card language

One branch for five Beads bugs that share the reminder models and planning path:
`levana-yal`, `levana-jh0`, `levana-clp`, `levana-1bl`, `levana-3kn`. Each section has its own
behaviour, design and tests.

Real high-latitude cases used below (KosherJava 2.5.0, default `tzais`, 18 min candle offset):

| Case | Source evening | Event | Actual instant (location zone) |
|---|---|---|---|
| Tartu (58.378 N, `Europe/Tallinn`), Shavuot day 1 → day 2, Diaspora | Wed 31 May 2028 | candle lighting at nightfall | Thu 1 Jun 2028 00:08:25 |
| Tartu, Shabbat → Shavuot, then day 1 → day 2 (Diaspora) | Sat 31 May 2036, Sun 1 Jun 2036 | candle lighting at nightfall | 1 Jun 00:08:34, 2 Jun 00:12:47 |
| St. Petersburg (59.934 N, `Europe/Moscow`), Shabbat | Fri 17 Jul 2026 | havdalah | Sun 19 Jul 2026 00:13:59 |
| Riga (56.950 N, `Europe/Riga`), Shavuot Fri + Shabbat, Diaspora | Thu 10 Jun 2027 | havdalah (Yom Tov) | Sun 13 Jun 2027 00:12:48 |

## 1. levana-yal — post-midnight candle-lighting instants

### Problem

`planReminderAlarms` built the hours-before trigger as
`ZonedDateTime.of(sourceDate, candleLightingTime: LocalTime, zone) − N h`. When candle
lighting is at nightfall (Shabbat → Yom Tov, Yom Tov → Yom Tov) and nightfall is after civil
midnight, the `LocalTime` (e.g. 00:08) was re-attached to the source evening's date, so the
reminder fired almost 24 hours early (Tartu 2028: 2 h before → 30 May 22:08 instead of
31 May 22:08).

### Behaviour

- Hours-before reminders fire at *(actual candle-lighting instant) − N hours*, whatever civil
  date that instant falls on. Morning-mode reminders are unchanged (morning time on the
  source date, location zone).
- The reminder keeps the **source evening** *D* as its identity (alarm URI, notification tag
  `candle_lighting/D`, deep link, delivery log).
- The plan's previous evening (*today − 1*) candle-lighting reminder is planned too, but only
  while its candle-lighting instant is still ahead: a run between midnight and a
  post-midnight candle lighting keeps (or schedules) the source evening's reminder. Once the
  candles are lit it is no longer wanted.
- Ordinary sunset lighting, consecutive holy days (one reminder per source evening),
  Israel/Diaspora rules, DST and unavailable nightfall (no reminder) are unchanged.
- Notification wording for candle lighting is unchanged (time only, in the location zone).

### Design

- `ZmanimRepository.getCandleLightingInstant(date, location, offset, inIsrael): Instant?` —
  the same holy-day rules as `getShabbatInfo`; `getShabbatInfo` now derives its display
  `LocalTime` from it (display output unchanged).
- `ReminderAlarm.CandleLighting` gains `candleLightingAt: Instant`; `candleLightingTime`
  remains the location-zone wall time used for the text.
- `NotificationAlarmScheduler.scheduleAll` reconciles `ACTION_CANDLE_LIGHTING` for the
  previous evening as well as `ACTION_OMER`.

### Tests

- Tartu 2028: the 31 May reminder (2 h before) fires at `instant(1 Jun 00:08:25) − 2 h`
  = 31 May 22:08:25 EEST, keyed 31 May; expected instant written out independently.
- A run at 1 Jun 00:01 (source evening yesterday, candles not yet lit) keeps the 31 May
  reminder (overdue, not cancelled; 1-hour-before case is scheduled as future); after
  00:08:25 it is no longer planned and a pending one is cancelled.
- Ordinary Friday (sunset − 18 min) trigger unchanged; Tartu 2036 Shabbat → Yom Tov → Yom
  Tov: two post-midnight reminders, distinct identities, each from its own instant; Israel
  has only the first; DST (location zone ≠ device zone, spring-forward week); white nights
  (St. Petersburg 31 May 2028, no nightfall) → no reminder.
- Robolectric: the scheduled `AlarmManager` trigger equals the instant, and a rebuild after
  midnight keeps exactly one alarm for the source evening.

## 2. levana-jh0 — post-midnight havdalah end dates

### Problem

`getObservanceEnd` returned the last holy day plus the nightfall `LocalTime`. For a
post-midnight havdalah (St. Petersburg, Shabbat 18 Jul 2026 → 19 Jul 00:13) the
notification said "Shabbat ends 12:13 AM" as if on Saturday, hiding that it is Sunday.

### Behaviour

- `ObservanceEnd.date` is the **civil date of havdalah** in the location zone; `time` is the
  wall time on that date; new `lastDay` is the last consecutive holy day (the date whose
  nightfall ends it). They differ only when havdalah is after midnight.
- The notification names the weekday whenever the civil end date is not the day after the
  source evening (existing rule), so post-midnight havdalah reads "Shabbat ends Sunday
  12:13 AM" / "יום ראשון 00:13".
- The walk over consecutive Shabbat / Yom Tov days, `includesYomTov`, Israel/Diaspora and
  display zmanim are unchanged. No calculation-method change.

### Design

- `ObservanceEnd(date, time, includesYomTov, lastDay = date)`; `getObservanceEnd` uses
  `getTzaitInstant(lastDay)` in the location zone.
- Alarm extras add `end_civil_date_epoch_day`; `end_date_epoch_day` keeps the last holy
  day. The receiver prefers the civil date and falls back to the old extra for alarms set
  by a previous version (old behaviour for those until the next rebuild replaces them).

### Tests

- St. Petersburg 17 Jul 2026: end `date` 19 Jul, `time` 00:13, `lastDay` 18 Jul, not Yom
  Tov; Riga 10 Jun 2027: 13 Jun 00:12, `lastDay` 12 Jun, Yom Tov. Expected dates written out.
- Ordinary NY Friday and existing consecutive-day/Israel cases unchanged.
- Text (EN/HE): post-midnight end names Sunday; ordinary next-day end is time only;
  multi-day end names the weekday.
- Receiver: new and legacy extras both produce the right text.

## 3. levana-clp — alarms outside a shifted planning window

### Problem

`scheduleAll` only cancelled unwanted alarms for the dates of the current window
(`today − 1 .. today + 7`). After a date-changing time-zone move or manual clock change,
alarms scheduled for dates now outside the window kept their old trigger times — including
alarms of a category that was later disabled. AlarmManager offers no way to list pending
alarms.

### Behaviour

- Every reminder alarm the app schedules is recorded in a durable **scheduled-alarm
  registry** (identity = action + source date). On every reconciliation, each registered
  identity that the current plan does not want is cancelled and unregistered, wherever
  its date lies. Arbitrary shifts are therefore handled by identity, not by a fixed window.
- *Still wanted* (unchanged rules): the plan's future alarms, its overdue alarms (window
  dates and the previous evening) and today's due morning candle-lighting notice. Overdue
  alarms are never cancelled by a settings or time-change rebuild.
- The planning window starts at the earlier of the device's and the active location's civil
  "today" (both are the dev override when set), so location-zone reminders the location has
  not reached yet are still wanted when the device zone is ahead of the location.
- Disabling a category cancels all its registered alarms, in or out of the window.
- Order: register before `setAndAllowWhileIdle`, unregister after cancelling, so a crash
  leaves at worst a harmless phantom entry (cancelling a missing alarm is a no-op), never an
  untracked alarm. Reboot clears alarms; phantoms are removed when they become unwanted.
- Bounded: entries only exist for scheduled identities, and each rebuild removes every
  entry not wanted, so the registry holds at most the planned dates.
- Upgrade: the previous scheduler kept no registry. On the first reconciliation the
  scheduler probes (`FLAG_NO_CREATE`) each action for the dates `windowStart − 14 ..
  windowEnd + 14` and treats any pending alarm found as registered. Old-version alarms
  further away cannot be discovered (documented limitation); the window sweep of earlier
  versions is kept as well.
- Concurrency: reconciliation runs under the rebuilder's lock and a scheduler lock; the
  receiver does not modify the registry.

### Design

- `ScheduledAlarmRegistry` (DataStore `scheduled_alarms`, separate from settings):
  string set of `action|epochDay`, plus `swept` flag for the one-time upgrade probe.
- `NotificationAlarmScheduler.scheduleAll` / `cancel(alarm)` become `suspend`.
- `planReminderAlarms(…, locationToday = today)`; the rebuilder passes the location's today.

### Tests (Robolectric, real `AlarmManager` shadow)

- Westward date jump (Tokyo → Honolulu, device zone and location): the morning alarm for the
  date now after the new window is cancelled instead of firing at the old zone's time; the
  window's dates are rescheduled in the new zone, none duplicated.
- Manual clock jumps of +30 and −30 days cancel every old registered alarm (arbitrary
  dates, far outside any fixed window) and schedule the new window.
- Disabling Omer / candle lighting after a shift cancels out-of-window alarms of that
  category only.
- An overdue undelivered in-window alarm survives the shift; a location-zone alarm still
  ahead for the location when the device zone is ahead survives.
- Upgrade: alarms created without a registry (prior scheduler) out of window are found by
  the probe and cancelled; wanted ones are kept; registry persists across a new scheduler
  instance (process restart).

## 4. levana-1bl — duplicate Omer notices after westward travel

### Problem

After a westward device-zone move (or a location change) the same civil date's Omer
morning reminder — and, after a location change, the evening one — is planned again in the
future and posted a second time.

### Behaviour

- The alarm receiver posts an Omer reminder at most once per **delivery identity**
  `(kind, Hebrew year of the counted day, count)`; kind is `evening` (tzait) or `morning`.
  Morning and evening of the same count stay separate; next year's same count is a new
  identity.
- The identity is recorded only after the notice was actually posted (permission granted
  and notifications/Omer channel enabled); a disabled category or missing permission
  records nothing.
- Check-and-record is serialized process-wide (one mutex) so concurrent deliveries of the
  same identity (e.g. a previous version's alarm and a new one) post once.
- Persistent (DataStore `notification_delivery`), so it survives rebuilds, process restart
  and reboot. Retention: identities older than the previous Hebrew year are pruned.
- Candle-lighting delivery is unchanged, except that it is no longer logged as delivered
  when nothing could be posted.

### Tests

- Receiver path with an isolated delivery log: evening count 33 posted once; re-delivery
  after a simulated westward reschedule is suppressed; morning count 33 still posts; count
  34 posts; same count next year posts.
- Disabled category / missing permission: not posted, not recorded, and a later enabled
  delivery posts.
- Persistence: a new `NotificationDeliveryLog` over the same file suppresses.
- Concurrency: parallel deliveries of the same identity post exactly once.
- Westward scenario end-to-end: Tokyo → Honolulu reschedules the next morning's alarm for
  the same date; firing it posts nothing new.

## 5. levana-3kn — day-panel Omer card language

### Problem

`DayInfo.omerFormatted` built English text in the domain model, so the day panel's Omer card
stayed English in Hebrew app language (and said "1 weeks", "1 days").

### Behaviour

- English: "Day 1 of the Omer", "Day 7, which is 1 week of the Omer", "Day 8, which is
  1 week and 1 day of the Omer", "Day 49, which is 7 weeks of the Omer".
- Hebrew: KosherJava's traditional day ("ח׳ בעומר", "ל״ג בעומר") with weeks/days:
  "ז׳ בעומר, שהם שבוע אחד", "ח׳ בעומר, שהם שבוע אחד ויום אחד".
- The card follows the app language (resources of the composition's context) and the
  selected date's count; it does not depend on notification settings or delivery.

### Design

- Remove `DayInfo.omerFormatted`. New `ui/OmerCountText.kt`: pure `omerDayLabel`,
  `omerWeeksText`, `omerCardText(context, omerDay)`; `omerNotificationText` reuses the first
  two (notification output unchanged). Plurals renamed to neutral `omer_weeks`,
  `omer_and_days`, `omer_weeks_and_days`; new `omer_card_day`, `omer_card_day_with_weeks`.
- `OmerSection(omerDay)` builds its text from `LocalContext`.

### Tests

- `omerCardText` English and Hebrew for boundaries 1, 6, 7, 8, 13, 14, 15, 21, 33, 48, 49
  with literal expected strings; all 1–49 in both languages are non-empty, Hebrew has no
  Latin letters/ASCII digits in the day label, English contains its number.
- Existing notification text tests unchanged.
- Emulator: card in English and Hebrew (RTL), live App-language switch, different dates.

## Out of scope

Late-zman display context in the day panel/zmanim screen (`levana-aw1`); candle-lighting
wording for post-midnight lighting; receiver lateness guards for alarms that fire during a
clock jump before reconciliation runs.

## Verification (2026-10-23)

Production commits: `d6eb7f6` (levana-3kn), `f0b9266` (levana-yal, -jh0, -clp, -1bl).

### JVM / Robolectric

`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline` (Android
Studio JBR): **608 tests in 84 suites, 0 failures, errors or skips**; lint passed (warnings
only, none new in the changed code); debug APK built.

New suites: `PostMidnightCandleLightingTest` (9), `PostMidnightHavdalahTest` (6),
`ShiftedWindowReconciliationTest` (7), `OmerTravelDeliveryTest` (6), `OmerCardTextTest` (4).
Mutation check, reverting one fix at a time in place: 5/9, 3/6, 5/7 and 5/6 of the
respective notification suites fail. One existing expectation changed on purpose:
`VerifyLookaheadPathTest` "daily runs slide the window" previously expected alarms for dates
that had slid out of the window to stay pending; they are now reconciled, except the
previous evening's Omer evening alarm.

### Emulator

Owned disposable `Levana_Agent_Template_API_36_1` (read-only), `emulator-5554`, Android 16 /
API 36, production `user` build (no root). APK 0.3.0-debug (300), SHA-256
`4283bec7b78790cf0497c0a3fe5b843b9f7e54fdb8290ce0478dcbd4d365d3d2`, installed hash
matched. The device clock and zone were changed with `cmd alarm set-time` /
`set-timezone` (real `TIME_SET` / `TIMEZONE_CHANGED` broadcasts); no dev date override
was used, and every delivery below is a real AlarmManager delivery to the manifest
receiver. Evidence (outside the repository): `/tmp/levana-emu-h3rx7pgl/artifacts/`
(`commands.log` with host UTC timestamps, `dumpsys alarm` / `notification` dumps,
screenshots, UI XML).

- **yal**: St. Petersburg, 1 hour before, device 2031-05-28 12:00 MSK. Shavuot day 2
  candles are lit at 2031-05-29 00:45:10; the alarm was set for **2031-05-28 23:45:10.189**
  and delivered at 23:45:25 as `candle_lighting/2031-05-28` ("Candle Lighting at 12:45 AM";
  no end shown because there is no nightfall after Shavuot day 2 in white nights).
- **jh0**: same location, device 2026-07-17. Delivered `candle_lighting/2026-07-17`:
  "Candle Lighting at 9:43 PM · Shabbat ends **Sunday** 12:13 AM".
- **clp**: the −5-year clock jump to 2026 cancelled the pending 2031-05-30 alarm. Tokyo →
  New York (zone change, then city change): the 1 June alarms outside the new 24–31 May
  window were cancelled, the rest moved to New York times without duplicates. Turning
  Sefirat HaOmer off left no app alarm pending.
- **1bl**: Tokyo, 24/25 May 2027: evening and morning count-33 notices both delivered.
  Notifications cleared, process killed (`am kill`), zone → New York (the broadcast
  started a new process). The rebuild replanned the 24 May evening (21:02 EDT) and
  25 May morning (07:00 EDT); both fired (AlarmManager stats: 2 Omer evening + 2 morning
  deliveries) and posted nothing. The 25 May evening (count 34) then posted.
- **3kn**: 25 May 2027 English "Day 33, which is 4 weeks and 5 days of the Omer"; after
  `cmd locale set-app-locales … iw` (same process, activity recreated) Hebrew RTL
  "ל״ג בעומר, שהם 4 שבועות ו־5 ימים"; 20 May in Hebrew "כ״ח בעומר, שהם 4 שבועות"; back
  to English on the same date "Day 28, which is 4 weeks of the Omer".

### Limitations

- The emulator is not a physical device; OEM alarm policies and Doze were not exercised.
  `setAndAllowWhileIdle` alarms have a one-hour window (existing behaviour); deliveries
  here happened at the trigger time.
- A previous-version alarm more than 14 days outside the window cannot be found by the
  one-time upgrade probe (AlarmManager cannot list alarms); it is reconciled once its date
  enters a window.
- Observed during verification, filed separately: a forward clock jump fires alarms that
  became past before reconciliation runs (`levana-vdq`); an identity kept as overdue may
  still fire later at an old-zone trigger after eastward travel (`levana-kq3`).
