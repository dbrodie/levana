---
id: TASK-26
title: Verify SettingsCheckboxRow with TalkBack in English and Hebrew
status: To Do
assignee: []
created_date: "2026-10-22T17:52:59Z"
updated_date: "2026-10-22T17:52:59Z"
labels: []
dependencies: []
references:
  - levana-bhe
  - 'discovered-from: TASK-22 (levana-8pf)'
priority: low
type: task
ordinal: 26000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up to levana-8pf. The reusable SettingsCheckboxRow implementation passed semantics/interaction tests and emulator hierarchy checks, but TalkBack spoken output was not tested. The owner approved merge with this disclosed limitation. On a device/emulator with TalkBack available, verify one focus stop per Halachic Times setting, announced label/checkbox role/checked state, double-tap activation exactly once, and English/Hebrew RTL focus order. Record Android/TalkBack versions and actual spoken-output observations; hierarchy dumps alone do not satisfy this check. Verify disabled behavior in a suitable test surface if available without adding a product setting solely for testing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Actual TalkBack navigation and spoken announcements are verified in English and Hebrew, with one labeled checkbox focus target, correct state, single activation and sensible RTL order; evidence and environment versions are recorded. Any reproducible defect is fixed or tracked explicitly.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-bhe",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependencies": [
    {
      "issue_id": "levana-bhe",
      "depends_on_id": "levana-8pf",
      "type": "discovered-from",
      "created_at": "2026-10-22T17:52:59Z",
      "created_by": "Daniel Brodie",
      "metadata": "{}"
    }
  ],
  "comments": [
    {
      "id": "ad646c34-be21-530e-9faa-d9c98c99ec2e",
      "issue_id": "levana-bhe",
      "author": "Daniel Brodie",
      "text": "From levana-td7 (calendar rows using SettingsCheckboxRow), TalkBack 16 on emulator: focus announcement OK ('not checked. Personal, me@example.com', 'Double-tap to toggle', ROLE_CHECK_BOX). Gap: toggling via keyboard Enter/Alt+Enter produced no spoken state change (TYPE_WINDOW_CONTENT_CHANGED STATE_DESCRIPTION with eventSrcRole=ROLE_NONE, isSelfOrAncestorFocused=false, ttsOutput={}). Unverified whether Halachic Times rows / switch rows behave the same, and real double-tap gesture untested. Evidence: /tmp/levana-emu-bwxvjnj9/artifacts/tb_en_*.log",
      "created_at": "2026-10-23T06:26:35Z"
    }
  ],
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 1,
  "original_status": "open",
  "original_priority": 3
}
```
