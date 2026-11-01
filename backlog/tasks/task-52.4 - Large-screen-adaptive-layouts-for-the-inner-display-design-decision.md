---
id: TASK-52.4
title: Large-screen adaptive layouts for the inner display (design decision)
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
updated_date: '2026-11-01T15:36:00Z'
labels: []
dependencies:
  - TASK-52.2
parent_task_id: TASK-52
priority: low
type: feature
ordinal: 59000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Decide how Levana should use the inner display's width, then implement it with WindowSizeClass / Compose adaptive APIs. Candidates:
- two-pane Month (calendar grid and day panel side by side);
- Agenda list with a detail pane;
- Settings list/detail;
- a navigation rail instead of the drawer at expanded width;
- wider but capped content in editors.

Start with a short proposal and mockups (Claude Design) for owner approval. Phones must stay unchanged, and every pane must keep the TASK-5 device/display-city rules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Owner-approved adaptive design recorded; implemented for the agreed screens with phone layouts unchanged; baselines at compact, medium and expanded widths
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Findings from TASK-52.2 (layout audit, 2026-10-30)
TASK-52.2 fixed only what needed no design decision: single-column content is capped at 640dp and centred above phone width (ui/LargeScreenLayout.kt), and text in fixed-size cells is capped at 1.3x. Evidence: /tmp/levana-task52.2-evidence/ (README, before/after per size). Open design questions for this task:
- Month (both modes): the grid fills the full width. On the inner displays the cells grow to ~120-135dp but stay near-square and readable, so 52.2 left them alone. Meanwhile the day sheet is capped at 640dp (Material) and centred, with empty gutters either side. Landscape inner (933/954dp): the sheet peeks under a wide grid, which looks unbalanced. This is the main two-pane candidate (grid | day panel side by side).
- Ultra inner portrait (859x954) and Fold8 inner portrait: the six-week grid takes ~800dp of height; the sheet peeks only the zmanim cards, and a blank band sits below the last week in five-week months.
- Agenda, Settings, My Events, Zmanim and the editors are now 640dp columns with empty gutters on the inner display. Candidates: list/detail (Agenda -> day or event; My Events -> editor; Settings -> sub-screen), or a wider cap for the agenda.
- Navigation: the modal drawer at expanded width; a navigation rail (or a permanent drawer) is the standard alternative.
- The top bars and the Agenda/Calendar headers (title, Hebrew/Gregorian toggle, actions) stay full width while their content is capped. Decide whether headers align with the content column.
- The cap value (640dp, matching the bottom sheet) is a judgement call; revisit it together with these layouts.
- Text at 200%: cell text in the month grid, date picker and walkthrough illustrations is capped at 1.3x. Alternatives are taller cells or fewer labels per cell, if the owner wants full scaling there.

## Month view design exploration (2026-11-01, design agent, branch task-52.4-month-view-design)

Scope: the Month view on the Fold8 / Fold8 Ultra inner display. This is design only: no app code changed and no existing Figma page was touched. Owner constraints: the slide-in drawer stays (no rail, no permanent drawer), and phone layouts are unchanged.

- Figma page: "Explorations — Foldable Month (TASK-52.4)": https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=63-73. Everything there is built from instances of the existing Levana components, variables and text styles. Local "Explore /" building blocks are in its Building blocks section.
- PNG exports, the README and the full research report: /tmp/levana-task52.4-month-design/ (README.md, options/, comparison.png, annotations/, research.md).

### Research summary
Full report with every link and confidence level: /tmp/levana-task52.4-month-design/research.md. "Inferred" means not seen directly.

- **Window size classes:** width is compact <600, medium 600–839 and expanded ≥840 dp ([docs](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)).
  - Fold8 inner portrait (704) is medium width.
  - Fold8 inner landscape (933) and both Ultra postures are expanded. Ultra portrait (859) is only 19dp above the boundary.
- **Canonical layouts** ([docs](https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts)):
  - The supporting pane goes in a bottom sheet or below the content at compact width, 50/50 at medium and 70/30 at expanded.
  - `SupportingPaneScaffold`'s default directive shows one pane at medium width ([guide](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-a-supporting-pane-layout)). A two-panes-at-medium directive exists, but Google warns it can look packed.
- **Samsung One UI** ([foldable guidance](https://developer.samsung.com/one-ui/largescreen-and-foldable/designing_for_foldable.html)): resume where the user left off and keep the scroll position across fold/unfold. Flex mode puts content on top and controls below.
- **Google Calendar** (high confidence; [Google blog](https://blog.google/products/android/android-app-redesign-tablet-foldable/), [9to5Google](https://9to5google.com/2024/05/21/google-calendar-tablet-app/)):
  - Month on the left, the selected day's events in a right pane.
  - Top bar, no navigation rail.
  - Resizable side mini-month in the Day/Week views; "smooth" transition from cover to inner screen.
  - Portrait vs landscape is not documented.
- **Samsung Calendar** (medium; [How-To Geek](https://www.howtogeek.com/why-i-use-samsung-calendar-instead-of-google-calendar/), [MakeUseOf](https://www.makeuseof.com/samsung-calendar-is-the-best-calendar-app/)):
  - A bigger grid; swiping down expands the cells.
  - Detail opens in a pop-up over the grid.
  - No confirmed side-by-side month and day list.
- **Outlook** (medium, 2020–21 sources; [Microsoft support](https://support.microsoft.com/en-US/Outlook/what-is-the-two-pane-layout-on-my-android-tablet-foldable-device)): list-detail (agenda | detail) with a one/two-pane toggle. Month could span both screens on the Surface Duo.
- **Fantastical, iPad reference** (medium; [Flexibits](https://www.flexibits.com/fantastical-ios/help/calendar-views)): sidebar mini-month, and an older layout with the list beside the month. Narrow split view falls back to the phone layout.
- **Business Calendar 2:** pop-up only (low). **aCalendar, Proton, DigiCal:** nothing found on large-screen layouts (unverified).
- **Hebrew/Jewish apps** (HebDate, Luach, Pocket Luach, Hebcal, Chabad Hayom, My Zmanim): no documented foldable or tablet month layout. HebDate shows the week view on rotation. Levana would set the precedent.
- **Patterns:**
  - Common: a grid with a persistent day pane beside it (Google, Outlook, Fantastical), and a bigger grid with pop-up detail (Samsung, Business Calendar).
  - Rare: Flex-mode calendar layouts and drag and drop.
  - Narrow windows fall back to the phone layout.

### Options (Figma frames)
Sample data:
- device today is Sun 1 Nov 2026;
- the selected day is Shabbat 7 Nov: Chayei Sara, Shabbat Mevarchim Kislev, molad Monday night 10:27 PM and 3 chalakim (computed);
- Rosh Chodesh Kislev is Tue–Wed 10–11 Nov;
- the display city is Jerusalem;
- times are illustrative.

The Rosh Chodesh and Mevarchim bars in the grid are TASK-57.6's proposal. The day panel keeps today's sheet content and adds a date/place header with ‹ › day stepping (TASK-57.2 N1); its content is 57.2's decision.

Sections: [Read me](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=69-7550) · [Comparison with researched apps](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=70-7509) · [Building blocks](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=63-74)

- **A — Two panes everywhere** ([section](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-2120)): grid | day panel in every inner posture. Panel width:
  - 360dp: [Fold8 landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-772), [Ultra portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-1446)
  - 320dp: [Fold8 portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-1109)
  - 380dp: [Ultra landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-1783)
- **B — Adaptive split, recommended** ([section](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-4514)):
  - Landscape: side by side, as in A.
  - Portrait: a full-width grid with a docked, non-modal two-column day panel below it, draggable like the phone sheet.
  - Frames:
    - [Fold8 landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-2121)
    - [Fold8 portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-2458)
    - [Ultra portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-2801)
    - [Ultra landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-3144)
    - [Dark](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-3481)
    - [Hebrew RTL](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-3818)
    - [Drawer open](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-4155)
- **C — Richer grid, day on demand** ([section](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-5989)):
  - Full-width grid with 11sp labels and up to 4 items per cell, including candle-lighting and havdalah times.
  - A one-line selected-day strip; the full day panel opens as a modal side sheet (landscape) or bottom sheet (portrait).
  - Frames:
    - [Fold8 landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-4549)
    - [Fold8 portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-4821)
    - [Ultra portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-5093)
    - [Ultra landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-5365)
    - [Side sheet open](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-5637)
- **D — Grid + agenda pane** ([section](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-7666)):
  - The pane holds a selected-day zmanim summary, then the agenda from the selected day onwards (Agenda components).
  - Fold8 portrait stacks the pane under the grid.
  - Frames:
    - [Fold8 landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-5990)
    - [Fold8 portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-6409)
    - [Ultra portrait](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-6828)
    - [Ultra landscape](https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=68-7247)

Folding to the cover screen gives the phone layout in every option, with the same selected day and month:
- the panel or strip becomes the collapsed day sheet;
- an open side sheet or expanded panel becomes the expanded sheet.

This needs the selected date, visible month and panel state hoisted in the ViewModel (TASK-52.3).

| | A Two panes | B Adaptive split | C Richer grid | D Agenda pane |
|---|---|---|---|---|
| Fold8 landscape (933) | Good | Good (same as A) | Good, but cells fit ~3 items | Good |
| Fold8 portrait (704, medium) | Cramped: grid ≈376dp, phone-like truncation | Good: full-width grid, panel below | Cells ~93dp truncate | OK (stacked) |
| Ultra portrait (859) | Narrow grid, tall panel | Good (stacked) | Good | OK |
| Zmanim visible without a tap | Yes | Yes | No (strip shows 3–4 times) | Partial (quick zmanim only) |
| Fit with TASK-57.2 day panel | Same content | Same content at two widths | Same content, behind a tap | Conflicts (agenda replaces day cards) |
| Overlap with other screens | None | None | None | Duplicates Agenda / TASK-57.5 |
| Precedent | Google Calendar, Outlook | M3 supporting pane (below at medium, beside at expanded) | Samsung Calendar | Fantastical, Outlook |
| Build cost | Low (one rule) | Medium (two layouts) | High (new rich cell and per-cell data) | Medium |

### Recommendation
Option B. It puts the day panel beside the grid in landscape (the common Google/Outlook pattern) and below a full-width grid in portrait. That uses the inner display's height well, removes the blank band TASK-52.2 found, and avoids A's cramped 376dp grid on the Fold8's medium-width portrait. It also keeps the phone model: the panel and the sheet are one surface with the same content (57.2), shown at three widths.

Implementation sketch for the follow-up:
- Choose the layout from the window size and orientation (WindowSizeClass plus aspect), or with `SupportingPaneScaffold` and a custom directive.
- Reuse the day sheet content composable in both panels.
- Add Roborazzi baselines at compact, medium and expanded widths.

### Open questions for the owner
1. **Ultra portrait (859×954).** It is expanded width, but B stacks it because the window is nearly square and a side pane would leave a phone-width grid. Stack by aspect ratio (as drawn), or follow the width class and use two panes (A's frame)?
2. **Docked panel in portrait.** Should it be draggable to full height (as drawn, like the phone sheet), or fixed? And in six-week months, shrink the panel (~270dp) or shorten the grid rows?
3. **Day panel content is TASK-57.2's decision.** The drawn panel keeps the quick tiles plus the zmanim list, which duplicates times (57.2 Z5), and adds a date/place header and a Mevarchim card. If 57.2 changes the sheet, the wide panels follow it. Should 57.2 also cover the two-column docked arrangement?
4. **Rosh Chodesh and Mevarchim grid bars** (57.6) are drawn as a proposal (secondary and tertiary tones), not decided here.
5. **Header alignment.** The month header stays full width, with the mode selector above the pane in landscape. Should it move into the title row on wide windows (TASK-57.3 explores combining those rows)?

### Limitations
- The 420 dpi density is still an assumption (TASK-52.1).
- Zmanim times are approximate sample values.
- PNGs are exported at 1x (dp = px).
- The research is web-only: four apps are well sourced, others are thin or unverified, and no device was used.
<!-- SECTION:NOTES:END -->
