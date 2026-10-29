---
id: TASK-5.3
title: Israel/Diaspora three-way setting with Auto-detect
status: To Do
assignee: []
created_date: '2026-10-29T18:47:00Z'
labels: []
dependencies: []
parent_task_id: TASK-5
priority: medium
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-5 case table row 6. Replace the 'In Israel' switch with Auto-detect (default) / Israel / Diaspora. Auto-detect uses the device position/zone, never the selected city. Migrate users who set it manually (IS_IN_ISRAEL_MANUAL) to their explicit choice; everyone else gets Auto-detect. English and Hebrew strings; accessible single-choice control matching existing settings dialogs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Three-way setting persists; Auto-detect follows the device, not the selected city
- [ ] #2 Migration of existing manual and automatic users is tested
- [ ] #3 English/Hebrew UI and TalkBack semantics verified
<!-- AC:END -->
