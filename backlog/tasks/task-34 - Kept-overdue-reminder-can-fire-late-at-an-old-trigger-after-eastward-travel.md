---
id: TASK-34
title: Kept overdue reminder can fire late at an old trigger after eastward travel
status: To Do
assignee: []
created_date: "2026-10-23T07:25:39Z"
updated_date: "2026-10-23T07:25:39Z"
labels: []
dependencies: []
references:
  - levana-kq3
priority: low
type: bug
ordinal: 34000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Pre-existing finding (BUGFIX_REVIEW.md 'Moving east after the new local morning time preserves an already-overdue pending reminder'), still present after levana-clp: when the current plan treats a reminder identity as overdue (trigger passed in the new zone/location), reconciliation keeps whatever alarm is pending under that identity. If the pending alarm was set for an earlier zone/location its trigger can still be in the future, so it fires late (e.g. a 'Tonight is day N' evening notice the next afternoon). Possible fix: record each scheduled trigger in ScheduledAlarmRegistry and treat a kept-overdue identity whose recorded trigger is still ahead as stale. Not changed under levana-clp to keep the existing overdue-preservation rule.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After an eastward move, an identity the plan considers overdue is not delivered later at a stale old-zone trigger; genuinely undelivered overdue alarms are still kept.
<!-- AC:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-kq3",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "open",
  "original_priority": 3
}
```
