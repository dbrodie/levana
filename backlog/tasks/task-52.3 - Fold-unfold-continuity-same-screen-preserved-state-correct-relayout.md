---
id: TASK-52.3
title: 'Fold/unfold continuity: same screen, preserved state, correct relayout'
status: To Do
assignee: []
created_date: '2026-10-30T10:31:00Z'
labels: []
dependencies:
  - TASK-52.1
parent_task_id: TASK-52
priority: high
type: task
ordinal: 58000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Folding or unfolding is a configuration change (size, and often density and orientation); it can also move the app between displays. Verify and fix it so the user stays exactly where they were and the layout adapts.

Check across fold→unfold and unfold→fold, rotation on the inner display, and entering and leaving split screen:
- **Navigation:** the same route and back stack; Month vs Agenda; deep-linked screens.
- **Screen state:** calendar month and selected day, Agenda scroll position and loaded window, search query and results, the Zmanim date, the drawer's open state.
- **Dialogs and in-progress input:** open dialogs and the date picker (including the year list and the current selection), unsaved editor input (event title or notes, birthday contact and date), the walkthrough page and its permission step, in-flight permission prompts, the TASK-46 mismatch dialog (it must not reappear or duplicate).
- **Nothing reloads or flashes needlessly:** no repeated network or geocoder calls, no alarm rebuilds triggered by the config change, no re-entry into the walkthrough.
- **Process death** while unfolded, then restore.

Add instrumentation or Robolectric tests that simulate configuration changes (e.g. StateRestorationTester or ActivityScenario recreate with new qualifiers) for the key screens.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Fold/unfold, rotation and split-screen keep the same screen, back stack and in-progress state on every screen listed; the layout matches the new size
- [ ] #2 No duplicate dialogs, prompts or walkthrough on config change; tests cover state restoration for key screens; emulator evidence (screen recording or screenshots before and after) recorded
<!-- AC:END -->
