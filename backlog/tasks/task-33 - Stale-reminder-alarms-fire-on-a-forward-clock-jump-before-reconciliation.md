---
id: TASK-33
title: Stale reminder alarms fire on a forward clock jump before reconciliation
status: To Do
assignee: []
created_date: "2026-10-23T07:25:38Z"
updated_date: "2026-10-23T07:25:38Z"
labels: []
dependencies: []
references:
  - levana-vdq
priority: low
type: bug
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while verifying levana-clp on emulator-5554 (session /tmp/levana-emu-h3rx7pgl, evidence artifacts/commands.log and 11-omer-delivered-tokyo.txt). Setting the device clock forward from 2026-07-17 to 2027-05-24 made a still-pending 2026-07-24 candle-lighting alarm already past; AlarmManager delivered it immediately, before the TIME_SET rebuild could cancel it, and it posted 'candle_lighting/2026-07-24' ten months late. The registry reconciliation cannot win this race. A receiver-side lateness guard (e.g. drop a reminder whose source evening is well before both the device's and the location's today, while keeping Doze-delayed same-evening deliveries) would fix it. Out of scope for levana-clp.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After a forward manual clock or date jump, reminder alarms for evenings that are long past are not posted; Doze-delayed alarms for the current or previous evening still deliver.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-vdq",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```
