---
id: TASK-40
title: Remove blank flash after swiping to another month on the calendar screen
status: In Progress
assignee:
  - '@claude'
created_date: '2026-10-29T11:02:00Z'
updated_date: '2026-10-29T11:31:00Z'
labels:
  - calendar
  - ui
dependencies: []
references:
  - app/src/main/java/ninja/doskey/app/levana/ui/calendar/CalendarScreen.kt
  - app/src/main/java/ninja/doskey/app/levana/ui/calendar/CalendarViewModel.kt
priority: medium
type: bug
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After swiping left/right to another month on the main calendar screen (Gregorian or Hebrew mode), the grid briefly blanks/blinks once the swipe settles, even though the month was already visible during the swipe.

Root cause (research, commit abddf8c):
- CalendarScreen.kt GregorianCalendarContent/HebrewCalendarContent: on pagerState.settledPage change they send CalendarIntent.LoadMonth / LoadHebrewMonth.
- CalendarViewModel.loadMonth / loadHebrewMonth (keepContent=false) synchronously set isLoading=true, currentMonth/hebrewYearMonth=target and monthDays=emptyList().
- The pager page renders state.monthDays when pageMonth == currentMonth (otherwise the cache). So the page that was drawn from gregorianMonthCache/hebrewMonthCache during the swipe switches to the now-empty monthDays -> empty Box (+ spinner) -> repopulated when loadJob finishes. That is the flash.
- The header subtitle (CalendarHeaderFormatter.hebrewMonthRange / gregorianRange over state.monthDays) also blanks for the same frame(s).
- The loadJob then replaces the cache with only mapOf(target to days), dropping neighbours until adjacentJob finishes, so a quick second swipe can land on an empty page too.

Suggested direction: when the target month is already in the cache, set monthDays from the cache immediately (no isLoading, no clearing) and keep existing cache entries while the reload/adjacent prefetch runs; only show the empty/loading state when there is genuinely no data. Optionally make the pager page fall back to the cache when monthDays is empty.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 On-device check passes in Gregorian mode: swipe_blank_check.sh (see notes) over 12 alternating swipes reports RESULT 0/12 (every swipe widest_empty_span < 300px). Baseline before fix: 11/19 recorded swipes blank.
- [ ] #2 On-device check passes in Hebrew mode: 0/12 blank. Baseline before fix: 17/18 recorded swipes blank.
- [ ] #3 Neither the incoming nor the outgoing month page ever renders empty (spinner only) during or after a swipe when that month was already loaded; the header subtitle (Hebrew month range / Gregorian date range) never disappears during a swipe
- [ ] #4 Go-to-date / Today jumps to an uncached month still show a loading state and then the correct month
- [ ] #5 ViewModel unit test: LoadMonth/LoadHebrewMonth for a month already in the cache never emits a state with empty monthDays or isLoading=true, and does not drop the neighbouring months from the cache
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. CalendarViewModel.loadMonth/loadHebrewMonth: when the target month is in gregorianMonthCache/hebrewMonthCache, the synchronous step sets monthDays from the cache with isLoading=false; only clear monthDays and set isLoading=true when the month is not cached (Go-to-date/Today to a far month).
2. loadJob merges the reloaded month into the existing cache instead of replacing it with mapOf(target), so neighbours stay available until adjacentJob replaces the cache with the prev/current/next window.
3. Add CalendarMonthSwipeTest (Robolectric-free JVM test like ReactiveCalendarRefreshTest): collect every emitted state while sending LoadMonth/LoadHebrewMonth for a cached neighbour; assert no emission has empty monthDays or isLoading=true and the cache keeps neighbours; plus a test that an uncached jump still goes through the loading state.
4. Build/test/lint, then emulator check with swipe_blank_check.sh (12 Gregorian + 12 Hebrew swipes) and Today/Go-to-date check.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Reproduction on emulator (2026-10-29, commit b18e7b2)

Environment: AVD Levana_Agent_Template_API_36_1 (API 36, 1080x2400), started read-only on emulator-5580; debug APK from assembleDebug; location + notification permissions granted; today = 2026-10-29.

Steps: open the app on the month calendar, then swipe a month horizontally (adb shell input swipe 900 900 150 900 250 for next, reverse for prev) while recording with adb screenrecord.

Observed (frame-by-frame from the recordings):
- Incoming page blank: mid-to-late slide, the header flips to the new month (CalendarIntent.LoadMonth fired from settledPage) and the incoming month page goes completely empty, showing only the loading spinner dot, for 1 to 3 recorded frames (~70-150 ms), then the days reappear and the slide finishes. The header subtitle (e.g. 'Cheshvan-Kislev 5787' or 'Nov 11, 2026 - Dec 10, 2026') vanishes during the same frames.
- Outgoing page blank: in other swipes the page sliding off-screen goes empty instead. This happens because loadJob replaces gregorianMonthCache/hebrewMonthCache with mapOf(target to days), which drops the neighbours until adjacentJob finishes.
- In Hebrew mode the incoming page was sometimes already blank before the header flipped, so a neighbour was missing from the cache before the swipe.
- Rates: Gregorian 11/19 recorded swipes showed a blank page; Hebrew 17/18. screenrecord only captures some composed frames, so a 'no' on a single swipe is not proof; use 12+ swipes per mode.

Detector: widest horizontal span of the month grid (y 445..1650) that has no dark pixel (gray < 140). Rendered months, including mid-slide frames: always 66-68px. Blank page: 374-740px. Threshold 300px.

Evidence (outside the worktree): /tmp/levana-task40/evidence/. Key files: gregorian_strip.png (frames 6-11, Oct->Nov, blank Nov page + missing subtitle), g_prev_3_strip.png (Dec->Nov), heb_strip.png (Cheshvan->Kislev), calib.png (incoming vs outgoing blank), *.mp4 raw recordings.

Verification script (copy to a file and chmod +x; requires ffmpeg and python3):

```zsh
#!/bin/zsh
# TASK-40 month-swipe blank check. Usage: swipe_blank_check.sh <adb-serial> <swipes> [outdir]
# Precondition: Levana open on the month calendar (Gregorian or Hebrew), 1080x2400 screen.
# Records each swipe (alternating next/prev) and reports the widest empty (no dark pixel) horizontal span
# in the month grid (y 445..1650). Rendered months never exceed ~70px; a blank page gives 370-740px.
S=$1; N=$2; OUT=${3:-/tmp/levana-task40/run}; mkdir -p $OUT; hits=0
adb(){ ~/Library/Android/sdk/platform-tools/adb -s $S "$@"; }
for i in $(seq 1 $N); do
  adb shell rm -f /sdcard/r.mp4
  adb shell screenrecord --bit-rate 20000000 --time-limit 4 /sdcard/r.mp4 & sleep 1.2
  if (( i % 2 )); then adb shell input swipe 900 900 150 900 250; else adb shell input swipe 150 900 900 900 250; fi
  wait; adb pull /sdcard/r.mp4 $OUT/swipe_$i.mp4 >/dev/null; sleep 1
  r=$(python3 -I - $OUT/swipe_$i.mp4 <<'PY'
import subprocess, sys
W,H=1080,2400
raw=subprocess.run(["ffmpeg","-v","error","-i",sys.argv[1],"-fps_mode","passthrough","-f","rawvideo","-pix_fmt","gray","-"],capture_output=True).stdout
worst=(0,-1)
for i in range(len(raw)//(W*H)):
    b=i*W*H; run=best=0
    for x in range(0,W,2):
        run=0 if any(raw[b+y*W+x]<140 for y in range(445,1650,3)) else run+2; best=max(best,run)
    worst=max(worst,(best,i))
print(worst[0], worst[1])
PY
)
  span=${r%% *}; v=ok; (( span > 300 )) && { v=BLANK; hits=$((hits+1)); }
  echo "swipe $i $([[ $((i%2)) = 1 ]] && echo next || echo prev): widest_empty_span=${span}px (frame ${r##* }) $v"
done
echo "RESULT: $hits/$N swipes showed a blank month page"
```

To run in Hebrew mode, tap the 'Hebrew' segment of the mode selector first (at about 657,285 on this AVD).
<!-- SECTION:NOTES:END -->
