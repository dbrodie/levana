---
id: TASK-25
title: Improve the add-event form and date-entry UI
status: To Do
assignee: []
created_date: '2026-10-22T10:04:32Z'
updated_date: '2026-10-29T21:29:00Z'
labels: []
dependencies: []
references:
  - levana-oub
  - 'discovered-from: TASK-24 (levana-v57)'
priority: low
type: feature
ordinal: 25000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Split from levana-v57 at the user's request. Improve the UI for adding an event, particularly Hebrew date/year entry: users should be able to reach a distant valid year without thousands of plus/minus taps. Review the form and choose a focused interaction improvement (for example direct year entry or a faster picker), keeping Hebrew/English, RTL, accessibility, and validation coherent. Reuse shared birthday-editor controls where appropriate. This is a low-priority usability enhancement; year-range policy, historical-date preservation, and correctness remain in levana-v57. Do not broaden this into a full app redesign.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The add-event form provides a practical way to enter distant valid years without repeated single-year stepping. Inputs follow the year-range policy established by levana-v57. Hebrew/English, RTL, keyboard and accessibility behavior are verified. Existing event fields, save behavior and shared birthday controls remain usable.
<!-- AC:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-29T21:29:00Z

---

Closed 2026-10-29 by owner decision: superseded by TASK-45 (shared dual-calendar Hebrew date picker), which covers the date-entry improvement.

---
<!-- COMMENTS:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-oub",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependencies": [
    {
      "issue_id": "levana-oub",
      "depends_on_id": "levana-v57",
      "type": "discovered-from",
      "created_at": "2026-10-22T10:04:32Z",
      "created_by": "Daniel Brodie",
      "metadata": "{}"
    }
  ],
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```
