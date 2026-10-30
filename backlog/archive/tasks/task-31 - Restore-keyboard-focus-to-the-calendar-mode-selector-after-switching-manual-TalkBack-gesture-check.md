---
id: TASK-31
title: >-
  Restore keyboard focus to the calendar mode selector after switching; manual
  TalkBack gesture check
status: To Do
assignee: []
created_date: '2026-10-23T06:18:42Z'
updated_date: '2026-10-30T06:11:00Z'
labels:
  - accessibility
dependencies: []
references:
  - levana-wf2
  - 'discovered-from: TASK-30 (levana-5xh)'
priority: low
type: task
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow-up to levana-5xh (commits 5dbc00b + verification notes). TalkBack SPOKEN OUTPUT IS NOW VERIFIED via VERBOSE TalkBack logs with keyboard navigation (emulator-5556, /tmp/levana-emu-qkq71rin/artifacts): EN 'Not selected, Hebrew, Radio button, 1 of 2, In list, 2 items' / 'Selected, Gregorian, Radio button, 2 of 2'; HE 'נבחר, עברי, Radio button, 1 of 2…' / 'לא נבחר, לועזי, Radio button, 2 of 2…'. The empty Material RadioButton child is never focused or spoken; Enter on the selected option is silent and keeps the mode; Enter on the other option switches once.
Remaining scope: (1) Keyboard focus restoration: after a mode switch, CalendarScreen's Crossfade recomposes the per-mode header and keyboard input focus is lost, so the next Tab starts from the menu button. TalkBack's own focus does return to the newly selected option. Consider hoisting the header out of the Crossfade, or restoring focus to the selector with a FocusRequester. (2) Optional manual check on a real device of TalkBack touch gestures (explore-by-touch + double-tap on each option, including the selected one), which adb can't inject. Related: levana-bhe.
<!-- SECTION:DESCRIPTION:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-10-30T06:11:00Z

---

Archived 2026-10-30 by owner decision: replaced by the single whole-app TalkBack/accessibility task, which carries over this task's remaining items (keyboard focus restoration after a calendar mode switch; manual TalkBack gesture check).

---
<!-- COMMENTS:END -->

## Beads history

```json
{
  "_type": "issue",
  "id": "levana-wf2",
  "owner": "dbrodie@gmail.com",
  "created_by": "Daniel Brodie",
  "dependencies": [
    {
      "issue_id": "levana-wf2",
      "depends_on_id": "levana-5xh",
      "type": "discovered-from",
      "created_at": "2026-10-23T06:18:43Z",
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
