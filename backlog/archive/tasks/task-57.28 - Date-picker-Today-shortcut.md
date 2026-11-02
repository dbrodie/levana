---
id: TASK-57.28
title: 'Date picker: Today shortcut'
status: To Do
assignee: []
created_date: '2026-11-02T09:31:00Z'
updated_date: '2026-11-02T10:11:00Z'
labels:
  - design-review
dependencies: []
references:
  - specs/reviews/2026-11-01-figma-design-review.md
  - https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=91-6399
parent_task_id: TASK-57
priority: low
type: feature
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**Problem.** The shared dual-calendar date picker (ui/datepicker/HebrewDatePickerDialog.kt, TASK-45 design 1b) has no quick way back to today. If the user browses months with the chevrons, opens the year grid or opens an event stored years away, getting back to today takes many taps. The picker is used by Go-to-date, the agenda, the event and birthday editors, and the developer date override.

**Finding.** Design review 2026-11-01 (specs/reviews/2026-11-01-figma-design-review.md), D3 (Medium): "No 'Today' shortcut; no Yom Tov/Shabbat markers in Hebrew-led grid." The reviewer suggested adding a "Today" text button. TASK-57.14 covered the holiday-dot half of D3 and left Today for a separate decision. This task is that decision.

**How the picker behaves today (relevant facts).**
- It opens with the initial date selected and that date's month shown. Today has a ring marker (DatePickerState.today, from the device context, including the developer override).
- The chevrons step months in the leading calendar. Tapping the title opens a 3-column year grid. Picking a year moves the selection to the same day and month in that year and closes the grid.
- Days are selectable when their Hebrew year is in SupportedHebrewYears (5600-6000), or is the year of an out-of-range initial date (extraHebrewYear).
- Nothing is committed until OK. The footer has the selection summary (both calendars, holiday chip) and Cancel/OK. The developer override adds a "Clear" action there.

**Proposed behaviour.**
- **Action:** tapping Today *selects* today and shows today's month in the leading calendar (re-anchoring both hebrewMonth and gregorianMonth). If the year grid is open, it closes. It does not confirm: OK still commits and Cancel discards. In most call sites the user's intent is "pick today", so only navigating would leave a second tap. Selecting is also harmless because nothing is committed. New intent `DatePickerIntent.GoToToday`, reduced like SelectDay(today) plus showSelection.
- **Enabled/disabled:** shown in both Hebrew-led and Gregorian-led layouts and in the year grid. Disabled when there is nothing to do: today is already selected, its month is shown and the year grid is closed.
- **Hidden** when today is unknown (state.today == null) or not selectable (outside SupportedHebrewYears). This is near-impossible in practice but must not produce a dead button.
- **Placement (recommended Option 1):** the footer's start-aligned neutral action. The selection summary moves to its own line above an actions row: [Today] ... [Cancel] [OK]. In RTL the row mirrors: היום on the right, ביטול/אישור on the left. M3 text button style, the same as Cancel/OK.
- **Text:** "Today" / "היום", reusing the existing R.string.calendar_today.
- **TalkBack:** a normal labelled button ("Today, button"; "היום, לחצן"). Give it an onClickLabel such as "Select today" / "בחירת היום" so the action is clear. After the tap, the selection change is announced the same way as a day tap, through the footer summary. When disabled, it reads as disabled.

**Options designed in Figma** (file E1IFrxKcldXbXnvXy5Nzhd, page "Screens — Light", section "Proposal — Date picker Today shortcut" https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=91-6399, next to "03 Date picker" 28:1751). The mock-ups assume today is Wed 21 Oct 2026 (10 Cheshvan, ring) and the selection is 31 Oct, so the button is enabled.
- **Option 1, recommended: Today in the footer.**
  - Hebrew-led: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=92-5228
  - Gregorian-led: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=92-5506
  - Hebrew-led year grid: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=92-6039
  - Hebrew UI RTL: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=92-6190
- **Option 2: Today in the month header, before the chevrons.**
  - Hebrew-led: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=91-6400
  - Gregorian-led: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=91-6578
- Notes card: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd?node-id=92-6479

**Why Option 1.**
- With Option 2, the month title gets only about 146dp on a 412dp phone. "October 2026" just fits, but "September 2026" (179dp at headlineSmall) and other long English Gregorian months would truncate. The title is already the tap target for the year grid.
- The footer start is the M3 slot for a dialog's neutral action. It is visually separate from Cancel/OK, stays in the same place in both modes and in the year grid, and leaves room for the developer override's "Clear".
- Cost: the dialog grows by about 40dp because the summary gets its own line. The content already scrolls on short windows.
- Option 2 keeps the dialog height and puts Today next to the other navigation controls, but it needs a fix for the title width (ellipsis or a smaller title style) before it could be used.

Screenshots: /Users/daniel/.paseo/evidence/picker-today-shortcut/: section-overview.png, option1-footer-{hebrew-led-light,gregorian-led-light,year-grid-light,hebrew-led-rtl}.png, option2-header-{hebrew-led-light,gregorian-led-light}.png.

Status: proposal awaiting the owner's choice of option and confirmation of the select-vs-navigate behaviour. Not implemented. After the decision, update the existing picker frames in Figma (light, dark, Hebrew RTL) to the chosen design before coding, as specs/FIGMA.md requires.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Owner has chosen the placement (Option 1 footer or Option 2 header) and confirmed that Today selects today rather than only navigating; the chosen design is applied to the existing date picker frames in Figma (light, dark, Hebrew RTL; Hebrew-led, Gregorian-led and year grid)
- [ ] #2 A Today button in the shared picker selects today and shows its month in the leading calendar, closing the year grid; nothing is committed until OK
- [ ] #3 It works in both Hebrew-led and Gregorian-led layouts and from the year grid; it is disabled when today is already selected and shown, and hidden when today is unknown or outside the selectable range
- [ ] #4 Label is 'Today' / 'היום' (R.string.calendar_today) with correct RTL placement; the developer override's Clear action still fits
- [ ] #5 Reducer unit tests cover GoToToday (from another month, from the year grid, in both leading calendars, leap Adar, already-today no-op, today unselectable)
- [ ] #6 Roborazzi snapshots cover the picker with the Today button in English and Hebrew, light and dark, both layouts
- [ ] #7 Emulator verification at every call site, in English/Hebrew and light/dark, plus TalkBack announcing the button and its action and the disabled state
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Proposal created on 2026-11-02 by the design agent. No code changes were made.
- Figma: the proposal frames are copies of the current picker frames. Light 28:2059, 28:1752 and 28:2312 were copied after TASK-57.14, and Hebrew RTL 44:2712 was copied in its state at that time. The originals and components were not modified. Reused: the Button component set 19:46 (Style=Text, LTR/RTL) and the Date picker / Day cell State=Today variant.
- Observation, not fixed here: the existing Hebrew RTL frame 44:2712, and therefore its copy 92:6190, shows the לועזי (Gregorian) segment checked while the grid is Hebrew-led. That frame needs correcting when the Figma frames are next updated.

Dropped by the owner (2026-11-02): Android's own Material date picker has no Today shortcut either, so this isn't important. The Figma proposal section (91:6399) was deleted; the existing picker frames were not changed.
<!-- SECTION:NOTES:END -->
