# Localize all notifications and put Hebrew zman names first in Hebrew

Status: in progress (branch `localize-notifications-zman-names`, based on
`codex/claude-design-refresh`)

## Issue 1: notifications other than birthdays are English only

### Problem

Birthday notifications follow Settings → App language (see
`fix-birthday-notification-language.md`), but every other notification is built
from English literals in `NotificationPoster`:

- Candle lighting: "Candle Lighting", "Candle Lighting at …", "· Shabbat ends …"
- Holiday: "Holiday", "Today is …", "… begins tomorrow", "… begins in N days",
  with the English holiday name
- Fast: "Fast Day", "· Begins …", "· Ends …", with the English fast name
- Personal event: "Personal Event", "Today is …"
- Sefirat HaOmer (tzait and morning): title and day / week count
- Update notice (`UpdateCheckWorker`): "Update Available",
  "Levana v… is available — tap to download"
- All times use a hardcoded `h:mm a`
- Channel names and descriptions in `NotificationChannels`

The candle-lighting and Omer notifications are posted from
`NotificationAlarmReceiver` (AlarmManager), which passes the receiver's plain
context; `UpdateCheckWorker` passes `applicationContext`. Neither resolves the
app language.

### Approach

- Every entry point resolves the app language once with `Context.withAppLocale()`:
  `DailyNotificationWorker` (all handlers, not only personal events),
  `NotificationAlarmReceiver` and `UpdateCheckWorker`.
- Text is built by pure functions in `notifications/NotificationTexts.kt`
  (title + body, from string resources), like `birthdayNotificationText`.
  `NotificationPoster` only posts. This makes every notification unit-testable
  under Robolectric.
- Strings go in `res/values/localized_ui_strings.xml` and
  `res/values-iw/localized_ui_strings.xml` next to the birthday strings. Counts
  use `<plurals>` (holiday "in N days", Omer weeks and days).
- Names come from existing sources, not new translations:
  - Holiday and fast names: `Holiday.name` / `Holiday.hebrewName` from
    `HolidayMapper`, chosen by the context language. The worker now passes the
    `Holiday` itself instead of `holiday.name`.
  - The candle-lighting notification names no holiday today; it only says
    "Shabbat ends". It keeps that structure (Hebrew: "צאת השבת").
- Times: `notificationTime(context, time)`.
  - English: `h:mm a`, exactly as today.
  - Hebrew: the app's own rule, `DateFormat.is24HourFormat(context)` on the
    localized context, which gives `HH:mm` unless the user forced 12-hour on the
    device (the same check the day panel and zmanim screen use).
- Omer:
  - English keeps the current structure ("Tonight is day 8 of the Omer (1 week
    and 1 day)"), now through plurals.
  - Hebrew uses the traditional count written with Hebrew numerals from
    KosherJava's `HebrewDateFormatter.formatOmer` (e.g. "ח׳ בעומר",
    "ל״ג בעומר"), followed by the weeks and days in Hebrew:
    "הלילה ח׳ בעומר (שבוע אחד ויום אחד)" / morning "היום ח׳ בעומר (…)".
- Channels: `NotificationChannels.createAll(context)` takes names and
  descriptions from resources. Android keeps a channel's name until the app
  calls `createNotificationChannel` again with the same id (allowed for name
  and description), so channels are re-created in the app language:
  - at app start (`LevanaApplication.onCreate`)
  - on configuration change (`LevanaApplication.onConfigurationChanged`, which
    fires when the per-app locale changes)
  - when the user changes App language in Settings
  - at the start of every `DailyNotificationWorker` run
- Notification ids stay keyed on non-localized data (a personal event is keyed
  on its raw title and label), so a language change replaces a notification
  instead of adding a second one.

### Interpretations (owner may change)

1. English output is unchanged character for character, **except** the Omer
   count, where the old text said "(1 weeks)" and "(… and 1 days)". With
   plurals it now says "1 week" / "1 day". Every other count stays the same.
2. Hebrew holiday phrasing avoids grammatical gender, since holiday names have
   mixed gender: "היום X", "מחר: X", "בעוד יומיים: X", "בעוד N ימים: X".
3. Hebrew times follow the app (24-hour unless the device forces 12-hour).
   English times keep `h:mm a` even on a 24-hour device, because English must
   not change.
4. The day panel's Omer card (`DayInfo.omerFormatted`) is still English only. It
   is a screen, not a notification, so it is out of scope here.

### Test plan

Robolectric tests (`NotificationTextsTest`), each in English and in Hebrew
(app language `iw` on an English device):

- candle lighting with and without an end time (times: 6:06 PM / 18:06)
- holiday today, tomorrow, in 2 days and in 3 days (plural, including the
  Hebrew dual), with the Hebrew holiday name from `HolidayMapper`
- fast with begin and end times, with the Hebrew fast name
- personal event
- Omer tzait and morning: day 5, 7, 8, 33 (plurals and KosherJava formatting)
- update notice
- channel names and descriptions after `createAll` in each language

Manual check on the emulator: switch App language to Hebrew, trigger the daily
job, read posted notifications with `dumpsys notification`.

## Issue 2: zman names show English first in Hebrew mode

### Problem

On the full zmanim screen (`ZmanimScreen`) each row shows the English name as the
main line and the Hebrew name underneath, even in Hebrew. The zman names are
also defined in three places: `ZmanimRepository.getZmanim`, the
`ZMAN_HEBREW_NAMES` / `ALL_ZMANIM` lists in `SettingsScreen`, and literals for
the candle-lighting / havdalah cards in `DayDetailScreen`.

### Approach

- One source: `ZmanimRepository.ZMAN_NAMES`, an ordered list of `ZmanName`
  (English key + Hebrew). `getZmanim`, Settings → Halachic Times and the
  candle-lighting / havdalah cards all read from it. English names stay the
  persisted keys of `selectedZmanim`.
- One rule: `zmanDisplayName(english, hebrew, hebrewUi)` returns
  `(primary, secondary)`: Hebrew first in Hebrew, English first otherwise.

### Interpretation (owner may change)

In Hebrew the Hebrew name is the main line and the English name is the smaller
secondary line, mirroring the English layout. English mode is unchanged.

### Places checked

| Place | Before | After |
|---|---|---|
| Full zmanim screen | English main, Hebrew secondary, always | Uses the rule |
| Day panel zmanim list | Already Hebrew first in Hebrew | Uses the shared rule (no visible change) |
| Candle-lighting / havdalah cards | Already Hebrew first; names hardcoded | Names from `ZMAN_NAMES`, shared rule |
| NETZ / SHKIA / TZEIT tiles | Localized labels (`sheet_*`), single line | Labels unchanged. Lookups use the name constants instead of substring matching |
| Settings → Halachic Times | Single line, localized | Names from `ZMAN_NAMES`. Stays single line (adding a secondary line there would change English) |

### Test plan

Unit test for `zmanDisplayName` in both languages, and that every zman returned by
`getZmanim` is in `ZMAN_NAMES` with the same Hebrew name.
