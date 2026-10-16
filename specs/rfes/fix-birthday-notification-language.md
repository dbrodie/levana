# Fix: Birthday notifications follow the app language

## Problem

Hebrew birthday notifications posted by `DailyNotificationWorker` are always in
English ("Personal Event" / "Today is Dani's Hebrew Birthday"), even when
Settings → App language is set to Hebrew (עברית), or the app language is System
default on a Hebrew device.

## Root cause

1. **Hardcoded strings.** The birthday notification is built from three English
   literals that never go through resources:
   - `"${contactName}'s Hebrew Birthday"` in `DailyNotificationWorker.handlePersonalEvents`
   - the title `"Personal Event"` and the body `"Today is $eventTitle"` in
     `NotificationPoster.postPersonalEvent`

   Only the occurrence label (e.g. "delayed from 30 Cheshvan") was already
   resource-based, via `occurrenceTitle(context, …)`.
2. **The language is applied implicitly.** The app language setting is a per-app
   locale stored by the platform: `SettingsViewModel` writes
   `LocaleManager.applicationLocales` (`iw` for Hebrew, empty list for System
   default); `locales_config.xml` lists `en` and `iw`. There is no app-level
   copy of the setting and nothing in the app re-applies it. Activities get it
   from the framework, but the worker just uses `applicationContext` and relies
   on the process-level configuration having the per-app locale merged in. That
   is not guaranteed for a process started in the background for a job, or
   right after the setting changes in a running process.

## Fix

- Add `Context.withAppLocale()` (notifications package): reads
  `LocaleManager.applicationLocales` (the same source the Settings screen writes)
  and, when non-empty, returns `createConfigurationContext` with those locales.
  When empty (System default), returns the context unchanged so the device
  language decides.
- `DailyNotificationWorker` builds a localized context once per run and uses it
  for notification text (birthdays, and the occurrence label of personal events,
  which was already resource-based).
- New `NotificationPoster.postBirthday(context, date, contactName, label)` and a
  pure text builder `birthdayNotificationText(context, contactName, label)`
  returning title and body from string resources:

  | key | English (unchanged output) | Hebrew |
  |---|---|---|
  | `notification_birthday_title` | Personal Event | אירוע אישי |
  | `notification_birthday_body` | Today is %1$s | היום %1$s |
  | `notification_birthday_event` | %1$s's Hebrew Birthday | יום ההולדת העברי של %1$s |

  The occurrence label keeps using the existing `occurrence_*` resources.
- Out of scope: other notification text that is still hardcoded English
  (candle lighting, holiday, fast, personal event title/body, Omer, update
  available). They are left as is.

## Test plan

- Robolectric unit tests:
  - English: title "Personal Event", body "Today is Dani's Hebrew Birthday", and
    with a `DelayedFrom30(CHESHVAN)` label
    "Today is Dani's Hebrew Birthday (delayed from 30 Cheshvan)".
  - Hebrew (`iw` qualifier): "אירוע אישי",
    "היום יום ההולדת העברי של דני", and with the label
    "היום יום ההולדת העברי של דני (נדחה מל׳ חשון)".
  - `withAppLocale()` yields Hebrew strings when application locales are `iw`
    on an English device, and leaves the context alone when they are empty.
- `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` pass.
- Emulator: add a contact birthday on a 30th that is delayed on the overridden
  date, force the daily worker job, read the posted text with
  `dumpsys notification --noredact`, in Hebrew and System default (English).
