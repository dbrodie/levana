# RFE: Crash report dialog that emails the stack trace

## Summary

When Levana crashes, show a dialog asking the user whether to send a crash report. If they agree, open their email app with a message to the developer that contains the stack trace and basic device/app info. The user sees the email and sends it themselves. Nothing is sent automatically.

## Why email via ACRA

Levana works offline and has no backend, analytics or accounts, so server-based crash reporting (Firebase Crashlytics, Sentry) does not fit. It would add a tracking SDK and change the privacy policy and Play Data safety answers.

The usual approach in open-source and F-Droid apps is [ACRA](https://github.com/ACRA/acra) with two plugins:

- `acra-dialog`: an "app crashed, send report?" dialog shown right after the crash, in ACRA's own `:acra` process
- `acra-mail`: on confirm, an `ACTION_SENDTO` email intent prefilled with the report

ACRA handles the hard parts (catching the crash, starting a fresh process to show the dialog, gathering device data) and is maintained, small, and has no network code of its own when only the mail sender is used.

## Behaviour

1. An uncaught exception happens.
2. ACRA writes the report and shows a dialog: title "Levana crashed", text explaining that a report can be emailed to the developer, buttons **Send report** / **Don't send**.
3. **Send report** opens the email app picker with:
   - To: the developer address (`CrashReporting.REPORT_EMAIL`)
   - Subject: `Levana crash report (<versionName>)`
   - Body: the report as readable key/value lines
4. **Don't send** discards the report.

Dialog strings are localized (en + iw).

### Report contents

Kept to what is needed to debug, so no personal data from the user's calendar, events or contacts goes into it:

- `APP_VERSION_NAME`, `APP_VERSION_CODE`
- `ANDROID_VERSION`, `BRAND`, `PHONE_MODEL`, `PRODUCT`
- `USER_CRASH_DATE`
- `STACK_TRACE`

Logcat is **not** included: it can contain event names or locations.

## Design

- `ninja.doskey.app.levana.crash.CrashReporting` holds the recipient address and an `init(app)` that calls `initAcra { … }` with the dialog and mail plugins.
- `LevanaApplication.attachBaseContext()` calls `CrashReporting.init(this)`. ACRA requires init this early so crashes during `onCreate` are caught.
- `LevanaApplication.onCreate()` returns early in ACRA's `:acra` process (`ACRA.isACRASenderServiceProcess()`), so Koin, notification channels and workers are not started a second time there.
- Developer Settings gets a **Test crash** row that throws a `RuntimeException`, to check the flow by hand.
- The dialog is a plain framework Activity, so it gets `Theme.Levana.CrashDialog` (`values/themes.xml` + `values-night/themes.xml`), a DeviceDefault alert theme that follows light/dark mode instead of ACRA's default Holo look.
- ACRA ships its own consumer ProGuard rules, so no R8 changes are needed.
- ACRA is pinned to 5.13.1: 5.14.x requires compileSdk 37, and Levana is on 36 (AGP 8.13 supports up to 36).

## Privacy

Reports leave the device only when the user taps **Send report** and then sends the email from their own mail app. `docs/privacy-policy.md` is updated: "no crash reporting" becomes a section saying crash reports are optional, user-reviewed emails, and listing what they contain. Play Data safety stays "no data collected", since the app itself transmits nothing (the user sends the email).

## Files

**New:**
- `app/src/main/java/ninja/doskey/app/levana/crash/CrashReporting.kt`
- `app/src/main/res/values/themes.xml`, `values-night/themes.xml`

**Modified:**
- `gradle/libs.versions.toml`, `app/build.gradle.kts` (ACRA mail + dialog)
- `app/src/main/java/ninja/doskey/app/levana/LevanaApplication.kt`
- `app/src/main/java/ninja/doskey/app/levana/ui/settings/SettingsScreen.kt` (Test crash row)
- `app/src/main/res/values/localized_ui_strings.xml`, `values-iw/localized_ui_strings.xml`
- `docs/privacy-policy.md`, `specs/rfes/privacy-policy.md`
