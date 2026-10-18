# Native UI review

The implementation uses the connector's `Jewish Calendar App.dc.html`, etag
`1788938696112158`. The reference was rechecked after the reported day-sheet
mismatch. Its sheet starts with a handle, optional badges and three summary cards;
the app's extra date heading was removed.

## Changes

- Bundled Manrope and Frank Ruhl Libre, neutral Material 3 surfaces, consistent
  shape and spacing tokens, restrained native transitions and holiday accents.
- Calendar header and cells, localized event labels, selected/today states,
  accessible day descriptions and Gregorian/Hebrew mode controls.
- Native draggable day sheet with a 48dp expandable handle, compact zmanim cards,
  ordered detail sections, and Back-to-collapse behavior.
- Consistent drawer/city selection, Settings, Zmanim, events, onboarding and forms.
- Date jumps now move the pager; changing calendar mode retains selection;
  superseded day requests are cancelled before newer results are published.

## Review method

Three Terra Medium agents split calendar, ancillary screens and independent visual
criticism. The compiled APK was installed on `Medium_Phone_API_36.1`. Reviews used
both screenshots and `uiautomator` accessibility XML, followed by root review.
The local evidence and detailed findings are in `build/design-qa/REPORT.md`.

The review covered light/dark Zmanim, Hebrew RTL, enlarged Settings text, date
navigation, day-sheet gestures and touch semantics. The final event picker was
checked at default and 130% text size. Switching from Jerusalem to Tel Aviv changed
the displayed zmanim, and switching back restored the Jerusalem values. Prototype sample dates/times
were never copied into runtime data; existing offline calendar calculations remain
in use. Bilingual Jewish-calendar names remain available, with Hebrew primary in
Hebrew UI. Native 48dp touch targets adapt the more compact web prototype.

## Checks

- Debug APK builds and installs on the emulator.
- All 11 unit tests pass, covering calendar naming, Diaspora/Israel differences,
  modern-holiday preferences, zmanim, event serialization and imports.
- Android lint reports 0 errors and 88 warnings; warnings remain visible in
  `app/build/reports/lint-results-debug.html` rather than being suppressed.
- `git diff --check` verifies patch whitespace.

This is a native implementation of the reference's visual language, not a claim
of pixel identity with its browser device frame. Changes remain on
`codex/claude-design-refresh`; the main branch is not merged.
