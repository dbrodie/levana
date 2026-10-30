# Levana

Levana is an open-source Hebrew calendar app for Android, for Orthodox Jewish users in Israel and the Diaspora. Calendar calculations and locally stored data work offline. It requires Android 14 (API 34) or later.

## Features

- Monthly Hebrew-primary and Gregorian-primary calendars with day details, and an agenda with search.
- Jewish holidays, fasts, Rosh Chodesh, the weekly parsha, special Shabbatot, the Omer count and the molad, with Israel or Diaspora observance.
- Location-based zmanim, candle lighting and havdalah, with a choice of which zmanim to show.
- Personal Hebrew-date events, including birthdays and yahrzeits; optional Android Contacts and Calendar integration.
- Notifications for candle lighting, holidays, fasts, personal events and the Omer.
- Hebrew and English with RTL layout, Material 3 theming, a Hebrew-date launcher icon, a Quick Settings tile and a home-screen widget.

## Install

Signed APKs are published on [GitHub Releases](https://github.com/dbrodie/levana/releases). See the [privacy policy](docs/privacy-policy.md) for how the app handles data.

## Building

The app is built with Gradle (JDK 17 and the Android SDK):

```bash
./gradlew :app:assembleDebug
```

## Licence

Levana is licensed under the [MIT License](LICENSE). It bundles third-party libraries under their own licences, including [KosherJava Zmanim](https://github.com/KosherJava/zmanim) (LGPL-2.1); see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Regenerate that file with `scripts/generate_third_party_notices.py` whenever dependencies change.
