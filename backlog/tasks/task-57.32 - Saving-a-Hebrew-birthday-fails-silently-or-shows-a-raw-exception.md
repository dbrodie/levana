---
id: TASK-57.32
title: Saving a Hebrew birthday fails silently or shows a raw exception
status: To Do
assignee: []
created_date: '2026-11-02T14:27:00Z'
labels:
  - design-review
dependencies: []
parent_task_id: TASK-57
priority: medium
type: bug
ordinal: 102000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-57.23 ('contacts import failure'). No crash in either case.
1. Contact deleted between picking and Save: ContactBirthdayRepository.setBirthday returns early when getRawContactId(...) is null (data/ContactBirthdayRepository.kt:183), so the view model reports saveComplete and the screen closes as if saved; nothing is written and no message is shown. Evidence: /Users/daniel/.paseo/evidence/task-57.23/61-save-birthday-deleted-contact.png (list without the new contact).
2. Contacts permission revoked (system Settings) while the Add birthday screen is open: after the process restarts the picked contact is gone (expected), the picker still works, then Save shows the raw SecurityException text 'Permission Denial: reading com.android.providers.contacts.ContactsProvider2 uri … requires android.permission.READ_CONTACTS, or grantUriPermission()'. The existing-birthday check (TASK-51) did not run either, since loading the stored birthday failed. Evidence: /Users/daniel/.paseo/evidence/task-57.23/96-pick-contact-no-permission.png, /Users/daniel/.paseo/evidence/task-57.23/97-save-no-permission.png.

Expected: a user-facing error (contact no longer exists / contacts access needed, with the permission path from TASK-57.29), never a silent success or a raw exception message. The view model shows e.message for every failure (ContactBirthdayViewModel save/load), so other provider errors will look the same.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Saving to a contact that no longer exists shows an error and keeps the screen open
- [ ] #2 Without contacts permission the Add birthday screen asks for it instead of showing an exception message
- [ ] #3 No raw exception text is shown to the user; covered by unit tests on the view model
<!-- AC:END -->
