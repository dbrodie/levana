---
id: TASK-51
title: >-
  Adding a Hebrew birthday to a contact that already has one silently overwrites
  it
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-30T10:07:00Z'
updated_date: '2026-10-30T10:21:00Z'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Product choice (date changed before the pick): load the stored date. The stored birthday is the persistent data, so it must never be replaced unseen; the user's unsaved add-mode date is a few taps to re-pick. The notice then also says that the date they chose was replaced by the saved one, so nothing changes without the user seeing it. If they switch to a contact without a birthday, their add-mode date comes back (kept as a snapshot), so it is not lost either.

1. ViewModel (ui/birthday/ContactBirthdayViewModel.kt), reusing the load path:
   - Split loadBirthday into the request guard (route key, rotation) and a shared load(key, fromPick). LoadBirthday keeps today's behaviour exactly (editedWhileLoading, preservedDate, token/supersede).
   - After a contact is chosen (ContactPicked once resolved, and ContactSelected), run load(key, fromPick = true) under the same token/loadJob, so a later choice, a route load or Save supersedes it.
   - fromPick, contact has a birthday: the stored date wins (editedWhileLoading ignored), editorDate + TASK-24 preservedDate exactly as the edit path; isEditing = true; state.storedBirthdayNotice = stored (day, month, year as stored) + replacedChosenDate (the user had changed the add-mode date and it differs from the loaded one). The add-mode date (and whether the user chose it) is snapshotted at the first switch.
   - fromPick, no birthday: back to add mode: isEditing = false, notice cleared, preservedDate cleared, add-mode snapshot restored (so another contact's date is never saved to this one).
   - Save is refused while the chosen contact's stored birthday has not been checked (state.storedBirthdayUnchecked: set on a contact choice, cleared when the lookup succeeds). A failed lookup shows the error and keeps Save off rather than risking an unseen overwrite.
2. Screen (MVI): notice card above the date field (info icon, secondaryContainer) "<name> already has a Hebrew birthday: <date>. You are now editing it, not adding a new one." (+ "The date you chose was replaced by the saved one." when applicable), date via formatHebrewDate (year-less for unknown years), name/date bidi-wrapped. A persistent polite live region around it so TalkBack announces the switch. Save button disabled while unchecked; label already follows isEditing (Save/Update).
3. Title: new strings design_add_birthday / design_edit_birthday (EN + HE). topBarTitleFor gets birthdayEditing; LevanaTopBar reads it from the route key (edit from the list) or the route's ContactBirthdayViewModel state (same instance via the back-stack entry). TopBarTitlesTest updated.
4. Tests (ContactBirthdayViewModelTest / IoTest): pick with existing birthday -> edit mode, date, notice; pick without -> add mode; switching A(birthday)->B(none)->C(birthday) incl. snapshot restore; pre-pick date replaced (flag); pick while a route load is pending and a check pending when another pick arrives / Save refused while unchecked; out-of-range stored year (5500, 6100, year 0) preserved on save after a pick; lookup failure keeps Save off. Existing tests that save right after ContactSelected wait for the check.
5. Roborazzi: new AppShell test picking a contact with a stored birthday through the picker result -> app_contact_birthday_existing (EN/HE x light/dark). Expected change: app_contact_birthday_* (4) title "Birthday" -> "Add birthday", required by the task; any other change is investigated.
6. Verify: assemble, unit tests, lint, verifyRoborazziDebug --rerun-tasks from a clean test-results; emulator (android-debug) EN + HE: contact with birthday -> edit mode + notice, contact without -> add, saved values on the contact; screenshots + README outside the worktree.
<!-- SECTION:PLAN:END -->
