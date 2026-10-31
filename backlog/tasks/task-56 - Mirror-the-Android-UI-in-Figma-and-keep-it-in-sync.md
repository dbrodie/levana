---
id: TASK-56
title: Mirror the Android UI in Figma and keep it in sync
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-31T22:44:00Z'
updated_date: '2026-10-31T22:44:00Z'
labels: []
dependencies: []
references:
  - https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd
priority: medium
type: task
ordinal: 69000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Maintain an editable Figma Design file that reproduces Levana's shipped Android UI (Jetpack Compose + Material 3) so design work, reviews and the store/walkthrough catalog can start from the real app rather than screenshots. The file must mirror the current visual appearance (no redesign), be built from editable layers, auto layout and reusable components bound to Figma variables/styles that map 1:1 to the Compose theme (Theme.kt, Type.kt, shapes), and be organised by feature and user flow. Each screen is verified against an emulator screenshot of the same state. The Android source is not modified by this work; when the app UI changes, the affected Figma components/screens are updated.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Foundations: colour variables (light + dark), spacing/radius variables, text styles (incl. Hebrew), effect styles mapped to the Compose theme with Android code syntax
- [ ] #2 Reusable components for every shared UI element, bound to variables and text styles
- [x] #3 Calendar, Zmanim, date picker, Agenda, My events/forms, System calendars and City picker screens built and verified against the emulator
- [ ] #4 Settings (main, Halachic times, Locations, Developer, About) and its dialogs built and verified
- [ ] #5 Onboarding walkthrough pages built and verified
- [ ] #6 Home-screen widgets (Today, Month, Zmanim), quick tile and dynamic app icon represented
- [ ] #7 Foundations page documents colours, type, spacing, radii, elevation and icons
- [ ] #8 Screens linked as user flows (sections and/or prototype connections)
- [ ] #9 Dark theme and Hebrew (RTL) variants of key screens
- [ ] #10 Process documented for updating Figma when the Compose UI changes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Work in small batches, each brought to the user for approval before the next.
1. Inventory screens/components from ui/ and capture reference screenshots + uiautomator bounds on a disposable emulator (android-debug skill).
2. Foundations: variables (Color, Color · Dark reference, Dimension), text styles, effect styles mirroring Theme.kt / Type.kt.
3. Components on the Components page, bound to variables; icons imported from Google's Material icon SVGs.
4. Compose screens on the Screens page, one Section per feature/flow, from component instances with auto layout.
5. Screenshot each frame from Figma, compare with the emulator capture of the same state, fix gaps.
Remaining batch order (proposed): Settings + dialogs → Onboarding → Foundations page + flow links → widgets/tile/icon → missing states → dark theme / Hebrew RTL.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Status (2026-11-01)

Figma file: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd ("Levana — Android App UI", in the user's drafts on "Daniel's team"). Pages: Foundations (empty so far), Components, Screens. Built with the official Figma MCP server (use_figma); no html_to_figma tool was available, and design-reference/claude-design-reference.html is a prototype, so the app code and emulator captures were the sources.

### Done
- Foundations: `Color` (35 M3 roles, Light) + `Color · Dark (reference)`; tint/state/scrim alpha variables (event-bar 18%, holiday 16%, onSurface 12/38%, drawer scrim 32%, dialog dim 60% measured on device); `Dimension` (spacing 1–24, radius 4–28 + full); 30+ text styles in Roboto, Hebrew styles in Noto Sans Hebrew; elevation/1, /3, sheet. Variables carry ANDROID code syntax (e.g. MaterialTheme.colorScheme.primary).
- Components (~45): 38 Material icons, icon button, top app bar, calendar month header, mode selector/segments, day cell (State × Mode Gregorian/Hebrew), event bar, weekday header, sheet handle, day badge, quick-zman card, zman rows/cards, sheet event row, buttons, text fields, search field, switch, checkbox, FAB, tab, menu, drawer item/city card, Hebrew date field, agenda rail/card/month header/banner, My events list card, city row, calendar row, date-picker day cell and year pill, system status/gesture bars.
- Screens (25 frames, all verified against emulator captures):
  - 01 Calendar: Gregorian month, Hebrew month, day sheet expanded, drawer, drawer with city list
  - 02 Zmanim: day list
  - 03 Date picker: go-to-date (Gregorian-led), Hebrew-led day grid, year grid
  - 04 Agenda: list + permission banner, search empty, search results, list with device event
  - 05 My events: Birthdays (permission/empty/list), Events (list/overflow menu), Add event (empty/filled), Edit event, Add birthday (no contact/chosen)
  - 06 Calendars & cities: System calendars (permission/list), City picker

### Remaining (see acceptance criteria)
- Settings + sub-screens + dialogs; onboarding; widgets/tile/icon; Foundations page; flow links; dark/RTL variants.
- Missing states: Omer card, device-events sheet section, holiday badges, delete/remove/import dialogs, snackbars, stored-birthday notice, contact photo, picker holiday chip, Gregorian year grid, Hebrew-led agenda, city search query, loading.
- Not separately compared yet (built from verified components): agenda search empty/results, Edit event, Add event empty, Add birthday chosen, Birthdays empty/list, Events list, year grid.

### Findings / known differences
- Compose's default LineHeightStyle trims leading on single lines, so text styles use AUTO line height (declared sp kept in each style description).
- Variable-bound paint opacity is lost on instances; use alpha colour variables instead.
- Hebrew gershayim/geresh render slightly smaller in Figma than on the device.
- Capture date mix: most frames show 31 Oct 2026; Agenda-with-device-event, System calendars and City picker show 1 Nov (emulator clock rolled over).
- Plan limits hit while on Starter (3 pages, 1 variable mode, MCP call cap); the user has since upgraded. Re-check whether Light/Dark can become modes of one collection.

### Evidence and working files (untracked, in this worktree)
Kept in `.figma-migration/` (excluded via .git/info/exclude, not committed; delete when the task is done):
- `emulator/batch1`, `emulator/batch2`: emulator screenshots (`*.png`, `*_s.png` scaled to 915px) and uiautomator dumps (`*.xml`)
- `figma-compare/`: Figma frame screenshots used in comparisons
- `state.json`: node-ID ledger for the Figma file; `icons/`, `icons.json`: Material icon SVGs; `scripts/`: capture/dump/pixel-sampling helpers
Tested against debug build of commit c0ce8b7 on Levana_Agent_Template_API_36_1 (1080×2400, 420dpi, 12h clock). No Android source changes.
<!-- SECTION:NOTES:END -->
