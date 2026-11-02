---
id: TASK-59
title: 'Agenda two-pane: only the date rail selects the panel''s day'
status: Done
assignee:
  - '@claude'
created_date: '2026-11-02T10:57:00Z'
updated_date: '2026-11-02T12:46:00Z'
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
- [x] #1 In two panes, only tapping a date rail changes the panel's day; card taps behave as on phones and leave the selection unchanged
- [x] #2 Today, Go to date and search only scroll the agenda without changing the panel; tests, baselines and Figma updated; emulator check recorded
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation (2026-11-02, tested commit 048d9cf)
- AgendaViewModel: GoToToday, GoToDate and SelectSearchResult only scroll (jumpTo; the picker/search close as before). Only SelectDay (the date rail) changes selectedDate. KDoc of AgendaState.selectedDate / AgendaIntent.SelectDay updated.
- AgendaScreen.openItem: no longer takes the selection; every card opens what it opens on a phone (holiday -> Month on the day, candle lighting/Shabbat/havdalah -> Zmanim, event/birthday -> editor, device event -> calendar app) in both layouts. The rail's onSelectDay, the two-pane highlight and the SavedStateHandle key are unchanged, so continuity (fold, rotation, split, process death) is as in TASK-52.6.
- Phones / below 840dp: no code path changed (openItem was already the phone behaviour without a selection).

## Tests
- AgendaViewModelTest: 'the day panel's day follows taps, jumps and today' replaced by 'only a date rail tap changes the day panel's day, which survives the process' (SelectDay sets/clears; GoToDate inside and outside the window, GoToToday and SelectSearchResult scroll but keep it; process death keeps it).
- New AgendaTapTest (Compose on AgendaContent, parameterised two panes / phone, a fixed day with every card type): rail tap -> SelectDay only in two panes, nothing on phones, highlight follows the state; holiday -> Month; candle lighting, Shabbat, havdalah -> Zmanim; event, birthday -> editor; device event -> ACTION_VIEW on the event; Today and Go to date -> GoToToday / OpenDatePicker only; a search result -> SelectSearchResult only; never SelectDay from a card.
- New AgendaTwoPaneSelectionTest (the app at FOLD8_INNER_LANDSCAPE): rail-select Shabbat 21 Nov, then Today and Go to date (picker OK) keep the selection; a search result (Chanukah 15 Dec 2025) scrolls while the panel still shows Vayetzei and nothing in view is selected; the Chanukah card opens Month on 15 Dec 2025; candle lighting -> Zmanim and an event -> editor, back on the Agenda the selection is unchanged.
- The new tests fail against the TASK-52.6 code (5 of 22 in AgendaTapTest+AgendaViewModelTest, 3 of 3 in AgendaTwoPaneSelectionTest) and pass now. FoldContinuityTest.theAgendaKeepsItsSelectedDay (rail tap) unchanged and passing.

## Baselines
No Roborazzi baseline changed (git shows no image modified; verify passed). ListDetailScreenshotTest selects its day by a rail tap, so the two-pane Agenda images are the same; phone baselines unchanged.

## Verification
rm -rf app/build/test-results && set -a && . ./.env && set +a && ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug --rerun-tasks: BUILD SUCCESSFUL, 1219 tests, 0 failures/errors/skipped, Roborazzi verify passed, lint 0 errors / 83 warnings (same as before). Log: /tmp/levana-task59-evidence/gradle-verify.log.
Emulator (Levana_Fold8_API_36_1, inner landscape 933dp, debug build of 048d9cf): rail tap selects 7 Nov (highlight + panel Chayei Sara); event card -> editor, candle lighting -> Zmanim, Shabbat card -> Zmanim, each back to the Agenda with 7 Nov still selected; Today, Go to date (28 Nov) and a search result (Chanukah, 15 Dec 2025) scroll the list while the panel keeps 7 Nov; the Chanukah card opens Month on 15 Dec 2025; rotate to portrait (phone layout) and back keeps 7 Nov. No crashes. Evidence: /tmp/levana-task59-evidence/ (README.md, 18 screenshots, ui.py).
Limitations: birthday cards (no contacts on the emulator) and standalone havdalah cards (only after Yom Tov, none near the emulator date) were not tapped on the device; AgendaTapTest covers both. Emulator times show AM/PM swapped because of the device-zone/location mismatch (TASK-5/46 notice), unrelated.

## Figma
Checked the Option E frames on Screens — Light (section 08, 95-14327), Dark (95-23824) and Hebrew (95-28198), their annotations, and the Components descriptions (Agenda / Date rail 20-54, Agenda / Card 20-131, Pane divider 78-7553): nothing describes the old selection rule, and the Agenda frames show a day selected by its date rail, which the new rule still allows. No Figma change needed; nothing created.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-02T12:46:00Z

---

Coordinator integration 2026-11-02: merged as 689c745. Clean run: 1219 tests, 0 failures; Roborazzi 420 unchanged (no baseline changes). New AgendaTapTest and AgendaTwoPaneSelectionTest fail on the old code. Emulator (Fold8 landscape) checked; birthday and standalone havdalah cards covered by tests only. Figma needed no change.

---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
In the two-pane Agenda only a tap on a day's date rail changes the day panel. Cards now open what they open on a phone (Month, Zmanim, editor, calendar app) without changing the panel, and Today, Go to date and a search result only scroll the list. Phones are unchanged; the selection still survives fold/rotation/split/process death. Tests: AgendaViewModelTest updated, new AgendaTapTest (every tap type, two panes and phone) and AgendaTwoPaneSelectionTest (app-level); all fail on the old code. 1219 tests green, lint clean, no Roborazzi baseline changed. Emulator check on the Fold8 AVD recorded in /tmp/levana-task59-evidence. Figma needed no change (no frame or annotation describes the old rule).
<!-- SECTION:FINAL_SUMMARY:END -->
