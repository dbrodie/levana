---
id: TASK-52.6
title: Implement the foldable list-detail layout (Option E) for Month and Agenda
status: To Do
assignee: []
created_date: '2026-11-02T08:56:00Z'
labels: []
dependencies:
  - TASK-52.4
parent_task_id: TASK-52
priority: medium
type: feature
ordinal: 98000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implementation of the owner's chosen foldable layout, **Option E: list-detail with a draggable divider**. Decided 2026-11-02; the full design, rationale and Figma links are in TASK-52.4's notes. Figma section: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd/?node-id=78-9942. PNGs: /tmp/levana-task52.4-month-design/options/E1–E7 and annotations/E_annotation.png. Re-export from Figma if /tmp was cleared.

**Rule:** two panes when the **window width is ≥ 840dp** (Material "expanded"): the Fold 8 inner display in landscape and both Fold 8 Ultra orientations. Narrower windows (Fold 8 inner portrait 704dp, covers, split halves) keep today's phone layout unchanged. It's decided by window width, not orientation.

**Layout:**
- Month screen = month grid | day panel; Agenda screen = agenda list | the same day panel.
- The month header and mode selector sit in the list pane.
- The day panel is the **current expanded day sheet content**, unchanged (redesigning it belongs to TASK-57.2), showing the selected day. No drag handle in the side pane.
- Navigation stays the slide-in drawer.

**Draggable divider:**
- a 16dp gutter with a 4×48dp handle;
- snaps to anchors of list ≈40%, 50% (the default) and 65%;
- minimum sizes: grid ≥ 360dp, panel ≥ 300dp;
- the position is remembered (DataStore) and shared by Month and Agenda;
- double-tapping the handle and TalkBack custom actions cycle through the anchors;
- it must not clash with horizontal month swipes.

**Continuity (with TASK-52.3):** the selected day, visible month or agenda scroll position, and divider position survive fold, unfold, rotation, split and process death. Below 840dp the layout falls back to the phone screen on the same selected day, with the panel content shown in the day sheet.

**Constraints:**
- Phone and below-840dp layouts stay pixel-identical (the existing Roborazzi baselines must not change).
- Respect the TASK-5 device/display-city rule: the panel times use the display city.
- Use WindowSizeClass or the Compose adaptive APIs. MVI throughout.
- English and Hebrew (RTL mirrors the panes); light and dark.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 At window width ≥ 840dp, Month and Agenda show list | day panel per Option E; below 840dp the existing layouts are unchanged (existing baselines unchanged)
- [ ] #2 Draggable divider with 40/50/65% anchors, minimum sizes, persisted and shared position, double-tap and TalkBack actions, no conflict with month swipes
- [ ] #3 Selected day, scroll and divider survive fold/unfold/rotation/split/process death; folding to the cover returns to the phone layout on the same day
- [ ] #4 Open questions answered by the owner and recorded before implementation
- [ ] #5 Roborazzi baselines at the expanded sizes (EN/HE, light/dark, each anchor); emulator screenshots on the Fold8 and Fold8 Ultra AVDs compared to the Figma frames
<!-- AC:END -->
