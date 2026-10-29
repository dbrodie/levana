# Levana — Product Specification

## Purpose

Levana is an open-source Android Hebrew calendar for Orthodox Jewish users in Israel and the Diaspora. Core calendar calculations and locally stored data work offline. Android 14 (API 34) or later is required.

This document describes the current product and its core requirements. Planned work and acceptance criteria live in Backlog.md.

## Calendar and Halachic Times

- Monthly Hebrew-primary and Gregorian-primary calendars, with day details.
- Jewish holidays, fasts, Rosh Chodesh, weekly parsha, special-Shabbat indications, Omer counts and molad information on Shabbat Mevarchim.
- Israel/Diaspora observance inferred from the active location's timezone, with a manual override; modern Israeli holidays can be hidden.
- Location-based zmanim, including dawn, sunrise, Shema and prayer deadlines, midday, Mincha, Plag, sunset, nightfall and halachic midnight.
- Users choose which zmanim appear in day details. Calculation methods are currently fixed; both GRA and MGA Shema times are available.
- Candle lighting and havdalah follow consecutive Shabbat/Yom Tov transitions. The pre-sunset candle-lighting offset is adjustable, defaulting to 18 minutes.

## Locations

- Device location and bundled city search; multiple saved cities with one active location.
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
- Omer: at location nightfall and/or a configurable device-local morning time.

Delivery follows Android scheduling restrictions and is not guaranteed at an exact time.

## Presentation and Accessibility

- Material 3, system-following light/dark appearance, system fonts and optional holiday accent colors.
- Hebrew or system language, with English fallback and RTL layout for Hebrew.
- Accessibility requirements: meaningful TalkBack labels, logical focus, scalable text, adequate contrast and touch targets of at least 48 dp.
- Hebrew-date launcher icon and Quick Settings tile.

## Data and Privacy

- Personal events and settings remain local; Contacts and Calendar access require permission.
- Calendar calculations require no external API. GitHub update checks use the network for applicable installation sources.
- Crash reports are offered through user-confirmed email; they are not sent automatically.
- App upgrades must preserve personal events, saved locations and preferences.
