---
id: TASK-59
title: 'Agenda two-pane: only the date rail selects the panel''s day'
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-02T10:57:00Z'
updated_date: '2026-11-02T12:27:00Z'
labels: []
dependencies: []
priority: medium
type: task
ordinal: 99000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Owner decision 2026-11-02, reviewing TASK-52.6. In the two-pane Agenda (window ≥ 840dp), **only tapping a day's date rail selects that day for the day panel.** TASK-52.6 shipped a broader rule; narrow it:
- **Holiday, candle-lighting, Shabbat and havdalah cards** behave exactly as on phones (open Month on that day, or Zmanim) and **do not** change the panel's selected day.
- **Event and birthday cards** open their editor as on phones, **without** changing the panel's selection.
- **Today, Go to date and a search result** only scroll the agenda; they **do not** change the panel's selected day (owner, 2026-11-02).
- The selected-row highlight stays (two panes only), following the date-rail selection.

Keep the TASK-52.3/52.6 continuity behaviour (the selection survives fold, rotation and so on). Update the TASK-52.6 tests and baselines for the narrower rule, and the Figma screens if they depict the old behaviour (TASK-56 sync process). Emulator check on the Fold8 AVD.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 In two panes, only tapping a date rail changes the panel's day; card taps behave as on phones and leave the selection unchanged
- [ ] #2 Today, Go to date and search only scroll the agenda without changing the panel; tests, baselines and Figma updated; emulator check recorded
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Research (code read at 01c946b): the TASK-52.6 rule lives in two places. AgendaViewModel: GoToToday, GoToDate and SelectSearchResult call select(date) besides scrolling. AgendaScreen.openItem: with a panel, every card first selects its day, and holiday/candle-lighting/Shabbat/havdalah cards return without navigating. The date rail's SelectDay, the two-pane highlight and the SavedStateHandle key stay as they are.

1. AgendaViewModel: GoToToday, GoToDate and SelectSearchResult only scroll (jumpTo, closing the picker/search); only SelectDay changes selectedDate. Update the KDoc of selectedDate/SelectDay.
2. AgendaScreen: openItem no longer takes the selection; every card navigates exactly as on phones (Month on the day, Zmanim, editor, device calendar). The date rail keeps onSelectDay; the selection object only feeds the rail and the highlight.
3. Tests: AgendaViewModelTest - replace 'the day panel's day follows taps, jumps and today' with: only SelectDay changes it; GoToToday, GoToDate (inside and outside the window) and SelectSearchResult scroll but keep it; process death keeps it. New AgendaTwoPaneTapTest (Robolectric Compose on AgendaContent with a fixed day holding every card type, two panes and phone): rail tap -> SelectDay only; each card -> its phone navigation and no SelectDay; Today, Go to date (header) and a search result -> their intents only. New two-pane end-to-end test in the app harness (Fold8 inner landscape): select a day by its rail, then Today, Go to date and a search result scroll but the panel and highlight keep the day; a Shabbat card opens Zmanim and an event card opens its editor, and back on the Agenda the selection is unchanged; a holiday card opens Month on that day. FoldContinuityTest.theAgendaKeepsItsSelectedDay stays (rail taps).
4. Roborazzi: expect no baseline changes (ListDetailScreenshotTest selects by rail tap); list and inspect any that change.
5. Verify: rm -rf app/build/test-results, then assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug --rerun-tasks.
6. Emulator (Fold8 AVD, landscape): rail tap, each card type, Today, Go to date, search; screenshots + README in /tmp/levana-task59-evidence.
7. Figma: check the Option E Agenda frames/annotations on the Screens pages (TASK-52.6 section 08) for text describing the old rule; update via the Figma MCP if so. Record results in the notes and commit.
<!-- SECTION:PLAN:END -->
