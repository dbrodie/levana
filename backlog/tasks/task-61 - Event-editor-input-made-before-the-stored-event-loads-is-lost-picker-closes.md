---
id: TASK-61
title: 'Event editor: input made before the stored event loads is lost (picker closes)'
status: To Do
assignee: []
created_date: '2026-11-02T11:20:00Z'
labels: []
dependencies: []
priority: low
type: bug
ordinal: 101000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-60 (2026-11-02). Opening a stored event shows the editor at once with the default date (1 Tishrei of the current year) and an empty title. The title bar says Edit event, taken from the route. AddEditEventViewModel.loadEvent then replaces the whole AddEditEventState when Room answers. Anything done before that is lost: an open date picker closes (showDatePicker resets to false), and a typed title or a picked date is overwritten. The picker also opens on the default date rather than the stored one.

On a device the load usually takes milliseconds, so this is rare, but a slow disk or a fast tap can hit it. It was the root cause of the TASK-60 screenshot flake; the tests now wait for the load, which hides it in tests only.

ContactBirthdayViewModel already handles the same case: fields edited while loading win (TASK-51). Options: do not allow opening the picker until the event has loaded (show the fields as loading), or keep the UI-only state and fields the user changed while loading, like the birthday editor does.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Opening the date picker or editing a field before a stored event has loaded never loses the input or closes the picker, and the picker never opens on a date other than the stored one; a ViewModel test covers a slow load
<!-- AC:END -->
