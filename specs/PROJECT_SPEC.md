# Levana — Product Specification

## Purpose

Levana is an open-source Android Hebrew calendar for Orthodox Jewish users in Israel and the Diaspora. Core calendar calculations and locally stored data work offline. Android 14 (API 34) or later is required.

This document describes the current product and its core requirements. Planned work and acceptance criteria live in Backlog.md.

## Calendar and Halachic Times

- Monthly Hebrew-primary and Gregorian-primary calendars, with day details.
- An agenda: holidays, Shabbat times, birthdays, yahrzeits, personal and device-calendar events in one chronological list, with search over the past and next 12 months and a jump to any date. Month is the start screen; Month and Agenda are both in the sidebar.
- Jewish holidays, fasts, Rosh Chodesh, weekly parsha, special-Shabbat indications, Omer counts and molad information on Shabbat Mevarchim.
- Israel/Diaspora observance: Auto-detect (the default: Israel exactly when the device's time zone is Israel's), Israel or Diaspora; modern Israeli holidays can be hidden.
- Location-based zmanim, including dawn, sunrise, Shema and prayer deadlines, midday, Mincha, Plag, sunset, nightfall and halachic midnight.
- Users choose which zmanim appear in day details. Calculation methods are currently fixed; both GRA and MGA Shema times are available.
- Candle lighting and havdalah follow consecutive Shabbat/Yom Tov transitions. The pre-sunset candle-lighting offset is adjustable, defaulting to 18 minutes.

## Device and Selected City

The device decides everything; the selected city is a display lens only.

- **Device:** the device clock, the device time zone and the device position (the current location; without location permission, a bundled city in the device's time zone, or else the one with the closest UTC offset). It decides today's date everywhere (calendar highlight, theme, launcher icon, quick tile, home-screen widget, including the widget's times and place name), every date calculation, when each notification fires and the times its text shows, and Israel/Diaspora detection.
- **Selected city:** chosen only in the sidebar; adding a city in Settings does not select it. It changes only the zmanim on the Zmanim screen, the day panel's times and the agenda's candle-lighting and havdalah times, which use the device's date and are shown in the city's own clock, labelled (e.g. "Jerusalem time") when its time zone differs from the device's. It never affects notifications, holiday dates or Israel/Diaspora.
- **Developer date override (testing only):** a fixed "as if" date. Until it is cleared the device is on that date at the real time of day, every day; the date does not advance at midnight. Everything that reads the device, notifications and their alarms included, behaves as if it were that date.

## Locations

- Device location and bundled city search; multiple saved cities, one of them (or the current location) selected for display.
- Persist coordinates, elevation and timezone for calculations.
- Calendar calculations remain available without location permission or connectivity. Device positioning and place-name lookup depend on available platform services.

## Events and Integrations

- Local Hebrew-date personal events, including birthdays and yahrzeits, with annual recurrence.
- Explicit yahrzeit rules for leap-year Adar, the first anniversary, 30 Adar I and missing 30 Cheshvan/Kislev dates.
- Hebrew birthdays can be read and edited in Android Contacts using a custom field, with permission.
- Selected Android calendars appear alongside local events through read-only access, with permission.
- JSON/CSV import and export of local personal events; this is not a full app backup.

## Reminders

Notifications are enabled by category and require Android notification permission:

- Candle lighting: a morning notice or a configurable number of hours before lighting.
- Holidays: configurable advance days; fasts: the day before.
- Local personal events and contact birthdays: on the occurrence date.
- Omer: at nightfall at the device position and/or a configurable device-local morning time.

Reminder times and texts follow the device: candle lighting and nightfall at the device position, morning notices and all displayed times in the device time zone, dates by the device clock.

Delivery follows Android scheduling restrictions and is not guaranteed at an exact time.

## Presentation and Accessibility

- Material 3 with system fonts. Theme: Light, Dark or System (default System). Optional Material You wallpaper colors (off by default) and optional holiday accent colors; during a holiday, holiday colors take priority over wallpaper colors.
- Hebrew or system language, with English fallback and RTL layout for Hebrew.
- Accessibility requirements: meaningful TalkBack labels, logical focus, scalable text, adequate contrast and touch targets of at least 48 dp.
- Hebrew-date launcher icon and Quick Settings tile.
- Feature walkthrough: one screen per major feature, each shown once. A new install sees them all; an update shows only the screens it adds. Location, notification, contacts and calendar permissions are offered on their screens and can always be declined. It never opens over a notification, widget or tile tap, and can be replayed from About.

## Data and Privacy

- Personal events and settings remain local; Contacts and Calendar access require permission.
- Calendar calculations require no external API. GitHub update checks use the network for applicable installation sources.
- Crash reports are offered through user-confirmed email; they are not sent automatically.
- App upgrades must preserve personal events, saved locations and preferences.
