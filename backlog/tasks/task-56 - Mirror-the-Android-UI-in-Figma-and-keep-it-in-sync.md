---
id: TASK-56
title: Mirror the Android UI in Figma and keep it in sync
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-31T22:44:00Z'
updated_date: '2026-11-01T00:42:00Z'
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
- [x] #2 Reusable components for every shared UI element, bound to variables and text styles
- [x] #3 Calendar, Zmanim, date picker, Agenda, My events/forms, System calendars and City picker screens built and verified against the emulator
- [x] #4 Settings (main, Halachic times, Locations, Developer, About) and its dialogs built and verified
- [x] #5 Onboarding walkthrough pages built and verified
- [x] #6 Home-screen widgets (Today, Month, Zmanim), quick tile and dynamic app icon represented
- [x] #7 Foundations page documents colours, type, spacing, radii, elevation and icons
- [ ] #8 Screens linked as user flows (sections and/or prototype connections)
- [x] #9 Dark theme and Hebrew (RTL) variants of key screens
- [x] #10 Process documented for updating Figma when the Compose UI changes
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
## Status (2026-11-01, batch 3)

Figma file: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd ("Levana — Android App UI", drafts of "Daniel's team", now Pro). Built with the official Figma MCP server (use_figma). design-reference/claude-design-reference.html is a prototype, so code + emulator captures are the sources; no html_to_figma tool was available.

### Pages
- **Foundations** — documentation page: colour roles with Light/Dark halves, alpha tints/scrims, holiday accent themes, every text style, spacing, radii, elevation, icon set.
- **Components** — ~70 components/sets; 29 sets carry a `Direction=LTR/RTL` variant (mirrored layout, flipped AutoMirrored icons).
- **Screens — Light** — 01 Calendar (month, Hebrew mode, sheet expanded, drawer ×2), 02 Zmanim, 03 Date picker ×3, 04 Agenda ×4, 05 My events & forms ×10, 06 System calendars & Cities ×3, 07 Settings ×10 (full scroll, viewport, 4 dialogs, Halachic times, Cities, About, Developer).
- **Screens — Dark** — every Light section duplicated with the `Color` collection set to Dark mode (36 frames).
- **Screens — Hebrew (RTL)** — 9 key screens in Hebrew UI: calendar, sheet, drawer, Zmanim, Agenda, My events, Add event, Hebrew date picker, Settings. Strings come from values-iw (316 pairs) plus device-captured dynamic strings.
- **Onboarding** — reference pages 1/9 and 2/9 built from Walkthrough components (page dots, bottom bar, permission panel); spec cards for the template, all 9 pages and the process for adding pages.
- **Widgets** — Today (Full/Compact), Month, Zmanim widget components on home-screen frames (Light + Dark), Quick Settings tile, adaptive launcher icon.

### Verification
- Foundations vs code (script): 36/36 colour roles × 2 modes, 15/15 type styles (weight/size/declared line height), 5/5 shapes match Theme.kt/Type.kt; surfaceTint was missing and was added. 13 sampled emulator pixels match their tokens exactly.
- Light, dark, Hebrew/RTL, Settings, onboarding and widget frames were compared side by side with emulator captures of the same state; gaps found were fixed (list-item line heights, RTL switch thumb, Hebrew segment widths, drawer label size, tint overrides, etc.).
- Device: Levana_Agent_Template_API_36_1 (1080×2400, 420 dpi, 12-hour clock, clock set to 31 Oct 2026 09:47 IST), debug build of commit c0ce8b7. No Android source changes.

### Known differences / limits
- Hebrew geresh/gershayim render slightly smaller in Figma (font fallback).
- In Hebrew UI the device shows standalone times like "PM 4:27" (bidi); Figma keeps "4:27 PM".
- Some batch-2 frames show 1 Nov data (emulator clock rolled over then); batch 3 pinned the clock to 31 Oct.
- Not separately compared: Agenda search, Edit event, Birthdays empty/list, year grid (built from verified components).
- Figma quirks recorded: resize() locks auto-layout sizing; variable-bound paint opacity is lost on instances (use alpha variables); cloning variants drops property bindings (rebind from twin).

### Remaining (open ACs)
- Prototype/flow links between screens (sections only today).
- Written process for updating Figma when Compose UI changes.
- Optional states listed earlier (Omer card, delete/import dialogs, snackbars, contact photo, Hebrew-led Agenda, loading).

### Evidence and working files (untracked, in this worktree)
`.figma-migration/` (excluded via .git/info/exclude; delete when done): `emulator/batch1–3` captures + UI dumps, `figma-compare/` Figma screenshots, `specs/` condensed code specs, `strings_en_he.json` / `rtl_dict.json`, `expected_tokens.json`, `state.json` node-ID ledger, `icons/`, `scripts/`.

Process for keeping Figma in sync: specs/FIGMA.md (linked from AGENTS.md).
<!-- SECTION:NOTES:END -->
