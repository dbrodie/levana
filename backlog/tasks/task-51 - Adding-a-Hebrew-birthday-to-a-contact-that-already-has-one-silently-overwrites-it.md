---
id: TASK-51
title: >-
  Adding a Hebrew birthday to a contact that already has one silently overwrites
  it
status: To Do
assignee: []
created_date: '2026-10-30T10:07:00Z'
labels: []
dependencies: []
priority: medium
type: bug
ordinal: 54000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner report, 2026-10-30. In My Events → Birthdays → Add, picking a contact that already has a Hebrew birthday doesn't show the existing birthday. The editor stays in 'add' mode with the default date, and Save overwrites the stored birthday without warning.

Code check (master bbdca65): picking a contact only sets the contact (ContactBirthdayViewModel.selectContact / resolvePickedContact). The existing birthday is loaded only when the screen is opened for editing (loadBirthday, which sets isEditing = true). save() always calls repository.setBirthday, replacing whatever is stored.

Expected behaviour:
- After a contact is picked in add mode, check whether it already has a Hebrew birthday.
- If it does, switch to editing that birthday: load its date into the editor, as when opening it from the list, including the TASK-24 non-destructive handling of out-of-range stored years.
- Make the switch clearly visible on the screen. The title changes from 'Add birthday' to 'Edit birthday', and a short notice says that this contact already has a Hebrew birthday (showing its date) and that you are now editing it, not adding a new one. The Save button wording follows the mode.
- If the user had already changed the date before picking the contact, decide in the plan between keeping their choice with the notice or loading the stored date. Never lose the stored birthday silently.
- Picking a different contact without a birthday returns to add mode.
- English and Hebrew strings, RTL, TalkBack announcement of the mode change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Picking a contact that already has a Hebrew birthday loads it and switches the screen to edit mode, with a visible notice and edit title; no silent overwrite
- [ ] #2 Picking a contact without a birthday stays in (or returns to) add mode; switching contacts updates the mode correctly
- [ ] #3 Unit tests for both paths, including a pending load and an out-of-range stored year; emulator screenshots in English and Hebrew; Roborazzi baselines for the notice
<!-- AC:END -->
