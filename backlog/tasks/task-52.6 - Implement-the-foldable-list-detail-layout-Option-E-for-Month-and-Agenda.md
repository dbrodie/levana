---
id: TASK-52.6
title: Implement the foldable list-detail layout (Option E) for Month and Agenda
status: To Do
assignee: []
created_date: '2026-11-02T08:56:00Z'
updated_date: '2026-11-02T09:32:00Z'
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
- [x] #4 Open questions answered by the owner and recorded before implementation
- [ ] #5 Roborazzi baselines at the expanded sizes (EN/HE, light/dark, each anchor); emulator screenshots on the Fold8 and Fold8 Ultra AVDs compared to the Figma frames
- [ ] #6 Figma updated: the implemented Option E screens are on the regular screen pages (Light, Dark, Hebrew) and the TASK-52.4 exploration page and leftovers are deleted; node links recorded
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Owner decisions (2026-11-02), answering the open questions from TASK-52.4
1. **Divider stops:** list ≈40%, 50% (default) and 65%, with minimum sizes grid ≥ 360dp and panel ≥ 300dp, are confirmed. **Also allow full collapse:** a fourth state with the panel hidden and the list or grid at full width. Dragging the divider to the edge collapses it; dragging back or double-tapping restores the last stop, and TalkBack gets an expand/collapse action. The collapsed state is persisted like the other stops.
2. **One header for the whole screen** (overrides the Figma frames, which put the header in the list pane only). The top app bar (drawer button, title, + and Today) and the Hebrew/Gregorian mode row **span the full window width above both panes**, identically on Month and Agenda. The panes sit below the header.
3. **Below 840dp,** the day sheet uses the **full width** instead of TASK-52.2's 640dp cap. This applies to medium-width windows such as the Fold8 inner display in portrait; phones are unaffected because they're narrower than 640dp. The affected foldable Roborazzi baselines change intentionally: list them and inspect them. Phone baselines stay identical.
4. **No new dark or Hebrew Figma frames before implementation;** the implementation's screenshots cover them.

## Figma: keep it in sync and clean up when done (owner requirement)
Follow TASK-56's Figma sync process (see its notes and the docs it added) using the official Figma MCP (`mcp__plugin_figma_figma__*`; load `figma:figma-use` before `use_figma`). File: https://www.figma.com/design/E1IFrxKcldXbXnvXy5Nzhd.
- **What's implemented must be in Figma.** Add the Option E screens as built (including the full-width header, the collapsed state and the full-width sheet below 840dp) to the regular screen pages (Screens — Light, Dark, Hebrew (RTL)) at the foldable sizes, matching the emulator captures. Add the pane divider to Components if it's reusable.
- **What isn't implemented must be removed.** Delete the "Explorations — Foldable Month (TASK-52.4)" page, including options A–D, the comparison, the read-me and the inspiration. Remove any other exploration leftovers created for TASK-52.4. Don't delete anything that belongs to TASK-56/57 work. Keep the PNG exports outside the repo as the historical record.
- Record the final Figma node links in this task's notes.
<!-- SECTION:NOTES:END -->
