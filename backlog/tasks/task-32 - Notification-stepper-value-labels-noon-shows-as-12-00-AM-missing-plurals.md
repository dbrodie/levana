---
id: TASK-32
title: 'Notification stepper value labels: noon shows as 12:00 AM; missing plurals'
status: To Do
assignee: []
created_date: "2026-10-23T06:33:00Z"
updated_date: "2026-10-23T06:33:00Z"
labels:
  - i18n
  - settings
dependencies: []
references:
  - levana-851
priority: low
type: bug
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while verifying levana-419 (emulator, English). settings_at_morning_time renders 720 minutes (12:00 noon, the upper bound of the candle-lighting and Omer morning steppers) as 'At 12:00 AM'; the 12-hour conversion in SettingsScreen always appends AM. Separately settings_hours_before_sunset / settings_days_before_value are plain strings, so values read '1 hours before sunset' / '1 days before' (lint PluralsCandidate). Pre-existing; levana-419 deliberately kept value text unchanged.
<!-- SECTION:DESCRIPTION:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-851",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```
