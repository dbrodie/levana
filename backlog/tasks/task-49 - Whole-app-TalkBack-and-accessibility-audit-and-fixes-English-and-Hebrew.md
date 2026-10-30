---
id: TASK-49
title: Whole-app TalkBack and accessibility audit and fixes (English and Hebrew)
status: To Do
assignee: []
created_date: '2026-10-30T06:11:00Z'
labels: []
dependencies: []
priority: medium
type: task
ordinal: 52000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner request 2026-10-30: one task covering TalkBack and accessibility for the **whole app**, replacing the scattered follow-ups. It absorbs TASK-31 (archived) and TASK-26 (archived earlier). Their remaining items are listed below so nothing is lost.

**Goal:** a user relying on TalkBack, keyboard/switch access, large text or high contrast can use every Levana surface in English and Hebrew (RTL).

**Already done** (don't redo; regression-check only): checkbox rows (TASK-22, TASK-27), notification steppers (TASK-28), delete-button labels (TASK-29), calendar-mode selector semantics (TASK-30), and "Today"/state descriptions added to widget cells and rows (TASK-9.2/9.3).

**Surfaces in scope:**
- **Calendar:**
  - Hebrew and Gregorian modes, month swipe/pager, day cells (both dates, holiday, today/selected state);
  - day panel, Go-to-date;
  - the drawer, including city selection;
  - the top bar and its actions.
- **Zmanim screen,** including the zone label.
- **Events:**
  - My Events (both tabs, overflow actions);
  - add/edit event, contact birthday editor, import/export flows.
- **Settings:** all sections and dialogs, including Israel/Diaspora, appearance (TASK-13, if merged), Locations and the city picker, system calendars, Halachic Times, Developer, About.
- **Dialogs and prompts:** permission prompts, the time-zone mismatch pop-up (TASK-46, if merged), and the new date picker (TASK-45, if merged).
- **Outside the app:**
  - the home-screen widgets (Today, Month, Zmanim list);
  - the Quick Settings tile, the launcher icon label, and notifications (text and actions).

**Checks for each surface:**
- **TalkBack spoken output in English and Hebrew,** from real announcements, not just semantics dumps:
  - meaningful labels;
  - correct role and state (selected/checked/disabled/expanded);
  - no duplicate or empty focus stops;
  - sensible grouping, e.g. one stop per day cell or list row;
  - logical focus order in RTL and LTR;
  - headings for section titles;
  - announcements for meaningful changes (live regions where appropriate).
- **Gestures** (explore-by-touch, double-tap, swipe navigation, custom actions where needed) on a real device or an emulator with TalkBack. adb can't inject touch exploration, so record which checks were manual.
- **Keyboard and switch access:** every action is reachable, the focus indicator is visible, and focus is not lost after state changes. Carried over from TASK-31: after switching calendar mode, keyboard focus must return to the mode selector rather than restarting at the menu button. The cause is the Crossfade recomposing the header; hoist it or restore focus with a FocusRequester.
- **Touch targets** at least 48dp.
- **Text scaling up to 200%:** no clipping or overlap. This covers day cells, widgets and dialogs.
- **Colour contrast** to WCAG AA for text and essential icons in light, dark and every holiday theme (and Material You, if merged). Information is never conveyed by colour alone, e.g. holiday dots, dimmed past zmanim, today highlights.
- **Carried over from TASK-26:** on Halachic Times settings, `SettingsCheckboxRow` gives one focus stop per setting, announces its label, checkbox role and checked state, double-tap toggles exactly once, and RTL order is correct. Check disabled behaviour where a suitable surface exists.

**Approach:**
1. Audit first. Use Accessibility Scanner and TalkBack on an emulator or device, plus Compose semantics inspection. Record findings per surface with severity and evidence (screenshots or TalkBack logs outside the repo).
2. Split the fixes into subtasks by area (e.g. calendar, events/editors, settings, widgets/tile/notifications), so they can be fixed and reviewed independently and in parallel without conflicts.
3. Add automated guards where practical: Compose semantics tests for key components and ATF checks in Robolectric tests. Keep the Roborazzi baselines unchanged, unless a visible fix intentionally changes them (review every changed image).
4. Document the app's accessibility conventions (labels, grouping, headings, targets, contrast) in ARCHITECTURE_SPEC so future features follow them.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every surface listed is audited with TalkBack in English and Hebrew (plus keyboard, 200% text, contrast and touch targets); the findings, evidence and environment versions (Android, TalkBack, device or emulator) are recorded in the task
- [ ] #2 Fix subtasks are created by area and completed; no remaining high-severity findings; lower-severity leftovers are explicitly tracked
- [ ] #3 TASK-31's keyboard focus restoration after a calendar mode switch and TASK-26's SettingsCheckboxRow TalkBack checks are done
- [ ] #4 Automated semantics/ATF checks guard key components; existing tests and the Roborazzi baselines pass (intentional image changes reviewed)
- [ ] #5 ARCHITECTURE_SPEC documents the app's accessibility conventions
<!-- AC:END -->
