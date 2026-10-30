---
name: android-debug
description: Debug the Levana Android app on an emulator or connected device using Gradle, ADB, logcat, UI inspection, screenshots, and optional live JVM breakpoints through JDWP/jdb. Use for crashes, hangs, wrong runtime state, emulator reproduction, or inspecting the current app screen.
---

# Android Debug

Debug Levana on a real Android runtime and report evidence: the reproduced behavior, relevant logs or live state, the root cause when established, and how the result was verified.

## Project context

- Project root: the current Levana worktree (the directory containing `gradlew`)
- ADB: `/Users/daniel/Library/Android/sdk/platform-tools/adb`
- Gradle JDK: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Application ID: `ninja.doskey.app.levana`
- Launcher: the currently enabled MAIN/LAUNCHER activity alias in `ninja.doskey.app.levana`
- Source device configuration: `Medium_Phone_API_36.1`
- Dedicated agent AVD: `Levana_Agent_Template_API_36_1`

Use full paths for ADB and the JDK. Run Gradle from the current worktree root with `JAVA_HOME` supplied through `/usr/bin/env`.

## Choose the lightest useful mode

- **Crash or startup failure:** inspect recent `AndroidRuntime`, fatal, and app-specific logs first.
- **Wrong behavior or user flow:** build/install, clear old logs, launch, reproduce with ADB input, then inspect filtered logs and UI state.
- **Current screen:** capture both a screenshot and UI hierarchy when visual state matters. The screenshot reveals rendering; UI XML reveals text, semantics, IDs, and bounds.
- **Wrong in-memory state, control flow, or a hang:** use live JVM breakpoints only after logs and source inspection cannot validate the hypothesis. Read [references/jvm-breakpoints.md](references/jvm-breakpoints.md) before attaching.
- **Foldables (Galaxy Z Fold8 / Fold8 Ultra cover and inner displays, fold/unfold, postures):** read [references/foldables.md](references/foldables.md) for the AVDs and commands.
- **Native C/C++ code:** this skill does not provide native debugging; use Android Studio/LLDB.

## Target selection

Use an owned disposable emulator for a new investigation. Reuse a live session already
created for this task and worktree. If the user explicitly requests an existing device
or the current emulator screen, attach to that target instead; never silently choose
a physical device. If an explicit target is ambiguous, clarify which target they mean.

Set `HELPER` to the absolute path of `scripts/emulator_session.py` beside this skill.
Run from the current worktree root:

```bash
python3 "$HELPER" start --project-root "$PWD"
```

Add `--headless` when a visible emulator window is unnecessary. The helper launches
`Levana_Agent_Template_API_36_1` with `-read-only`, lets the emulator select its own ports, and
receives the console port through a private Unix socket. Each instance has independent
writable runtime state; its changes are discarded on exit. It cannot save snapshots.
On first use, the helper creates the dedicated agent AVD from the source AVD's static
configuration and installed SDK system image. It starts with fresh app data; it never
copies live disks, snapshots, or locks. This avoids conflicting with a user running the
source AVD in writable mode. Template creation is locked and never overwrites an
unmanaged AVD. It can also be prepared explicitly:

```bash
python3 "$HELPER" prepare-template
```

Do not run the dedicated agent AVD in writable mode while agents are using it.
`--avd <name>` selects a deliberately prepared alternative; it does not bypass an
existing writable instance's lock. Do not modify the shared source AVD or delete its
locks. Persistent cloning is outside this workflow.

The helper returns JSON with `session`, `serial`, `project_root`, and `log`. Record
these values in task context; shell variables do not persist across tool calls. Set
`SESSION` and `SERIAL` from those values in examples below. Evidence belongs under
`$SESSION/artifacts/`. Never discover ownership by selecting the first global device.

```bash
python3 "$HELPER" status --session "$SESSION"
# Only for a user-selected existing device; records a borrowed session:
python3 "$HELPER" attach --serial <explicit-serial> --project-root "$PWD"
```

Use `adb -s "$SERIAL"` for every device command. All sessions use the standard shared
local ADB server on port 5037; leave `ADB_SERVER_SOCKET` and
`ANDROID_ADB_SERVER_PORT` unset in device-command environments. Never run
`adb kill-server`, global disconnects, or `forward --remove-all`.

Startup and boot waits are bounded. On failure, inspect the returned session path and
`emulator.log`; do not issue device commands using failed session metadata. Missing
AVDs, resource exhaustion, or denied runtime permissions should be reported with the
actual diagnostic. Do not replace the requested AVD or retry indefinitely.

## Build, install, and launch

Build/install only when the requested investigation requires the current source:

```bash
/usr/bin/env ANDROID_HOME="/Users/daniel/Library/Android/sdk" JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the install command only after the build succeeds. `installDebug` may install on
multiple connected devices, so use the explicit targeted install above.

If installation succeeds:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" logcat -c
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell am force-stop ninja.doskey.app.levana
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER ninja.doskey.app.levana
# Set LAUNCH_COMPONENT to the returned component on the last line, then:
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell am start -n "$LAUNCH_COMPONENT"
```

Resolve the enabled launcher component before each launch: `.MainActivity` is not
exported, and the enabled launcher alias can change with the app icon. Validate that
the resolver returned a component in `ninja.doskey.app.levana` before launching; stop
and inspect package state if it reports no activity. An implicit `am start` with
MAIN/LAUNCHER does not reliably resolve these aliases because they omit DEFAULT.

Do not launch after a failed build or install. Preserve the Gradle failure output and diagnose that failure instead.

## Logs and crash evidence

For recent fatal errors:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" logcat -d -s AndroidRuntime:E '*:F'
```

For app-relevant errors, start from a bounded dump and filter for `levana`, `AndroidRuntime`, `FATAL EXCEPTION`, `ANR`, `Exception`, or the suspected tag. If the process is running, resolve its PID first with `adb -s "$SERIAL" shell pidof ninja.doskey.app.levana`, then use `logcat --pid=<pid>`.

Prefer bounded dumps for diagnosis. Use a long-running logcat session only while actively reproducing a problem, and stop it afterward.

When reporting a crash, identify the exception, the first relevant Levana stack frame, the triggering path, and whether it reproduced on the current build. Do not infer a file/line that is absent from the trace.

## UI and interaction

Inspect the hierarchy with:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell uiautomator dump /sdcard/levana-window.xml
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" shell cat /sdcard/levana-window.xml
```

Save UI XML under `$SESSION/artifacts/window.xml` when retaining evidence.

Capture a screenshot to the session artifact directory when rendering or layout matters:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s "$SERIAL" exec-out screencap -p > "$SESSION/artifacts/screen.png"
```

View the image with the environment's image inspection tool when available. Use UI XML for stable selectors and semantic confirmation; do not treat coordinates from a different screen state as reliable.

ADB taps, swipes, text input, back, and key events are allowed for reproducing the user flow. Re-dump the UI after navigation before choosing the next coordinate.

## Verification and cleanup

After a fix, reproduce the same flow on a freshly installed debug build and verify both the user-visible outcome and the absence of the original error. Run focused automated tests when they cover the affected logic.

Stop any long-running logcat or debugger session. If live JVM debugging was used,
clear Android's debug-app setting while the original device is still connected, as
described in the reference. Then stop the session:

```bash
python3 "$HELPER" stop --session "$SESSION"
```

The helper removes recorded forwards and terminates only an owned emulator whose
process-start identity still matches. Borrowed devices remain running. Repeated stop
is safe. Logs and artifacts remain available for reporting; remove the exact session
directory only when its evidence is no longer needed. Never terminate other agents'
emulators. Owned sessions should be stopped on task completion or abandoned startup;
keep a session running only when continued investigation requires it.
