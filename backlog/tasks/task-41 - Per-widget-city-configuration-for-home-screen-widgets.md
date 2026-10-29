---
id: TASK-41
title: Per-widget city configuration for home-screen widgets
status: To Do
assignee: []
created_date: '2026-10-29T20:06:00Z'
labels: []
dependencies:
  - TASK-9
priority: low
type: feature
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Future enhancement (owner request 2026-10-29). By default, all home-screen widgets (Today, Month, Zmanim list) show times for the device location, per the TASK-5 rule. Let the user optionally configure an individual widget instance to show a chosen saved city instead, e.g. through a Glance widget configuration activity when placing or reconfiguring it. This is display-only: it must not affect notifications, dates or Israel/Diaspora, and it must not change the app's selected city. Define what happens when the configured city is deleted (fall back to the device location) and label the widget with the city, plus a zone label when its zone differs from the device's, matching the Zmanim screen.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each widget instance can be set to the device location (default) or a saved city, and the choice persists per instance
- [ ] #2 A configured city affects only that widget's displayed times; notifications, dates, Israel/Diaspora and the app's selected city are unchanged
- [ ] #3 Deleting the configured city falls back to the device location; the city name and a zone label (when zones differ) are shown; English/Hebrew verified
<!-- AC:END -->
