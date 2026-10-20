# App review task list

Updated after merging the verified bug fixes. Original task order, priorities, types and descriptions are retained from the consolidated review backlog. P1 covers correctness and data protection, P2 covers reliability and everyday usability, and P3 covers enhancements and maintenance. Status refers to the stated scope; separate follow-ups below remain open.

## Original backlog

| Order | Priority | Type | Task | Description | Status |
| ---: | --- | --- | --- | --- | --- |
| 1 | P1 | Bug fix | Fix Shabbat/Yom Tov transitions | Calculate candle-lighting and Havdalah according to consecutive holy days and the Israel/Diaspora setting. | Done |
| 2 | P1 | Bug fix | Correct evening Omer counts | Use the upcoming Hebrew day’s count for evening reminders and the current day’s count for morning reminders. | Done |
| 3 | P1 | Cleanup | Protect personal data during upgrades | Export Room schemas, replace destructive migration fallback with explicit migrations, and test data preservation. | Open |
| 4 | P1 | Bug fix | Reschedule notifications immediately | Cancel and rebuild scheduled reminders when notification settings, timing, or the active location changes. | Done |
| 5 | P1 | Bug fix | Prevent missed reminders | Schedule reminders ahead so a daily worker running after a reminder’s trigger time doesn’t leave gaps. | Done |
| 6 | P1 | Cleanup | Add CI checks before release | Run unit tests, lint, and screenshot verification on pull requests and require successful checks before publishing. | Open |
| 7 | P1 | Bug fix | Fix all-day and multi-day calendar indicators | Preserve all-day event dates across timezones and display dots on every day a multi-day event covers. | Done |
| 8 | P2 | Cleanup | Expand regression coverage | Add tests for notification scheduling, holiday transitions, DST, calendar event dates, and unavailable zmanim at high latitudes. | Done |
| 9 | P2 | Bug fix | Fix notification collisions | Give each reminder a stable identifier that distinguishes its date, event, and reminder type. | Done |
| 10 | P2 | Bug fix | Move blocking work off the UI thread | Run contact queries, calendar queries, and file import/export on an IO dispatcher to prevent UI freezes. | Done |
| 11 | P2 | Cleanup | Centralize date and timezone handling | Use a shared date provider and explicitly distinguish device time from selected-location time across screens and workers. | Partial |
| 12 | P2 | Bug fix | Make calendar data refresh reactive | Refresh calendar dots and day details when personal events, contact birthdays, or device-calendar events change. | Done |
| 13 | P2 | New feature | Make minhag selection meaningful | Define and verify the defaults each minhag controls, apply them to relevant calculations, and allow individual overrides. | Open |
| 14 | P2 | New feature | Configure zmanim methods | Add calculation profiles, Rabbeinu Tam options, and explanations of the method used for each displayed time. | Open |
| 15 | P2 | New feature | Full backup and restore | Add a versioned backup format for personal events, saved locations, and preferences, with explicit handling of contact birthdays. | Open |
| 16 | P2 | New feature | Home-screen widget | Display the Hebrew/Gregorian date, next zman, upcoming candle lighting, and selected location on the home screen. | Open |
| 17 | P2 | New feature | Upcoming agenda | Show holidays, birthdays, yahrzeits, and selected device-calendar events in one chronological list. | Open |
| 18 | P2 | New feature | Event-specific reminders | Allow users to configure reminder times and advance notice separately for individual birthdays and yahrzeits. | Open |
| 19 | P3 | Cleanup | Split large UI files | Extract settings sections, dialogs, navigation, and drawer components into smaller files with clear responsibilities. | Open |
| 20 | P3 | Cleanup | Reconcile documentation | Update product, architecture, and MVP documents to reflect the implemented frameworks, features, and remaining work. | Open |
| 21 | P3 | New feature | Appearance controls | Add Light/Dark/System selection and optional Material You colors alongside holiday themes. | Open |
| 22 | P3 | New feature | Daily learning schedules | Add offline daily learning information, starting with Daf Yomi in day details. | Open |

All eight original bug-fix tasks are complete. Regression coverage now includes notification plans, consecutive holy days, DST, all-day/multi-day calendar dates and unavailable high-latitude nightfall. Date/timezone centralization is partial: alarm planning uses explicit dates/zones and reads the current device zone on rebuild, but an app-wide date-provider refactor remains open. Provider/file IO is off the UI thread; bounded import parsing and export serialization remain synchronous.

## Follow-up fixes completed

| Priority | Type | Task | Description | Status |
| --- | --- | --- | --- | --- |
| P1 | Bug fix | Preserve post-midnight Omer nightfall dates | Keep the astronomical nightfall instant and source evening's count through planning, including previous-evening reconciliation after midnight. | Done |
| P2 | Bug fix | Rebuild reminders on device time-zone changes | Receive system time/time-zone changes and rebuild alarms using the current device zone without restarting the app. | Done |

Both follow-ups passed fresh independent Opus 5.5 reviews and a combined emulator run, including real alarm delivery and system broadcast handling. The production candidate was `d2832e2`; see [verification summary](../BUGFIX_REVIEW.md).

## Remaining findings from verification

These are recorded for future work; they are not included in this merge. Priorities use the same scale as the original backlog.

| Priority | Type | Task | Description | Status |
| --- | --- | --- | --- | --- |
| P1 | Bug fix | Preserve post-midnight candle-lighting dates | Carry the actual candle-lighting date/instant into hours-before reminders when Yom Tov candle lighting occurs after civil midnight. | Open |
| P2 | Bug fix | Preserve post-midnight havdalah end dates | Keep the actual civil end date for post-midnight havdalah and notification wording. | Open |
| P2 | Bug fix | Avoid repeated Omer notices after westward travel | Track delivered Omer reminders so a westward time-zone change does not re-alert for a count already delivered. | Open |
| P2 | Bug fix | Reconcile alarms outside a shifted planning window | Cancel or update previously scheduled alarms left outside the current window after a date-changing time-zone move, including disabled reminders. | Open |
| P2 | Cleanup | Move reverse geocoding off the UI thread | Run the remaining synchronous geocoder call in LocationService away from the main thread. | Open |
| P3 | Cleanup | Show next-day context for late zmanim | Indicate when a displayed nightfall or fast-end time belongs to the following civil day. | Open |

The user accepted the calendar grid's existing two-chip limit: holiday and parsha chips may hide an event indicator. No UI change is requested for it.

