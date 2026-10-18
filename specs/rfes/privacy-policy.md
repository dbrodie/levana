# Privacy Policy

**Status:** Approved by the owner

## Problem

Google Play requires a privacy policy at a public URL. Levana collects no user data, but it uses on-device permissions (location, contacts, calendars, notifications) and two outside services (Google location services for the optional GPS lookup, and GitHub for update checks on GitHub-installed builds). The policy must say so accurately and match the Play Data safety form.

## Change

- Add `docs/privacy-policy.md`, the policy itself, and a minimal `docs/index.md` linking to it.
- Publish `docs/` on `master` with GitHub Pages, so the policy is served at `https://dbrodie.github.io/levana/privacy-policy`.
- That URL goes into Play Console → App content → Privacy policy. The Data safety form should declare no data collected or shared.

## Facts the policy relies on (checked in the code)

- No analytics, advertising, crash-reporting or account SDKs. The only network code is `update/UpdateChecker.kt`, which calls the GitHub releases API, and only when the app was not installed from Google Play or F-Droid.
- Location comes from Google Play services' fused location provider, and the city name from Android's `Geocoder`. Results stay on the device.
- Contacts: Hebrew birthdays are read from and written to the device's contacts. Calendars: system calendars are read to show events. Nothing leaves the device.
- Exported files (JSON/CSV) are written only where the user chooses.
