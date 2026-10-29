---
id: TASK-35
title: Replace deprecated track input with tracks in release workflow
status: Done
assignee:
  - Daniel Brodie
created_date: "2026-10-28T20:10:51Z"
updated_date: "2026-10-28T20:18:49Z"
labels: []
dependencies: []
references:
  - levana-69j
priority: low
type: task
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
r0adkll/upload-google-play warns that 'track' is deprecated in favour of 'tracks'. One-line change in .github/workflows/release.yml.
<!-- SECTION:DESCRIPTION:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Pushed master 120b80d; tagged v0.4.0. Release run 40761308105 succeeded: upload step used tracks: internal, 'Validating tracks: internal', uploaded 1 artifact, no 'track is deprecated' warning (v0.3.0 run 39164225135 had it).
<!-- SECTION:NOTES:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-69j",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "started_at": "2026-10-28T20:11:01Z",
  "closed_at": "2026-10-28T20:11:49Z",
  "close_reason": "Merged to master; verified against action source and YAML parse. End-to-end confirmation on next tagged release.",
  "dependency_count": 0,
  "dependent_count": 0,
  "comment_count": 0,
  "original_status": "closed",
  "original_priority": 3
}
```
