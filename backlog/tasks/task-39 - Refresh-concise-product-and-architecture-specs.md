---
id: TASK-39
title: Refresh concise product and architecture specs
status: Done
assignee: []
created_date: '2026-10-29T10:50:00Z'
updated_date: '2026-10-29T10:53:00Z'
labels: []
dependencies: []
documentation:
  - specs/PROJECT_SPEC.md
  - specs/ARCHITECTURE_SPEC.md
type: docs
ordinal: 38000
---

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Replace stale specifications with concise current product capabilities and architectural commitments; verify against implementation and run required checks.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Rewrote both specs against the current implementation: product 476 to 55 lines; architecture 109 to 48. Corrected stack, cities, tests, location and appearance capabilities, provider ownership, reminders and offline boundaries. Removed duplicate inventories, speculative features and stale draft boilerplate. Reviewed against build dependencies, preferences/settings, repositories, notification code and tests. git diff --check, backlog doctor, assembleDebug, testDebugUnitTest and lintDebug passed. Documentation-only change; no runtime check required. Evidence: /tmp/levana-spec-refresh-checks.log. Awaiting integration.

Merged and pushed to origin/master at d0af209. Combined assembleDebug, testDebugUnitTest and lintDebug passed; backlog doctor passed. Evidence: /tmp/levana-spec-integration-checks.log.
<!-- SECTION:NOTES:END -->
