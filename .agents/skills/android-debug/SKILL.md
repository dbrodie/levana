---
name: android-debug
description: Debug the Levana Android app on an emulator or connected device using Gradle, ADB, logcat, UI inspection, screenshots, and optional live JVM breakpoints through JDWP/jdb. Use for crashes, hangs, wrong runtime state, emulator reproduction, or inspecting the current app screen.
---

# Android Debug

Debug Levana on a real Android runtime and report evidence: the reproduced behavior, relevant logs or live state, the root cause when established, and how the result was verified.

## Project context

- Project root: `/Users/daniel/projects/levana`
- ADB: `/Users/daniel/Library/Android/sdk/platform-tools/adb`
- Gradle JDK: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Application ID: `ninja.doskey.app.levana`
- Main component: `ninja.doskey.app.levana/.MainActivity`
- Preferred AVD: `Medium_Phone_API_36.1`

Use full paths for ADB and the JDK. Run Gradle from the project root with `JAVA_HOME` supplied through `/usr/bin/env`.

## Choose the lightest useful mode

- **Crash or startup failure:** inspect recent `AndroidRuntime`, fatal, and app-specific logs first.
- **Wrong behavior or user flow:** build/install, clear old logs, launch, reproduce with ADB input, then inspect filtered logs and UI state.
- **Current screen:** capture both a screenshot and UI hierarchy when visual state matters. The screenshot reveals rendering; UI XML reveals text, semantics, IDs, and bounds.
- **Wrong in-memory state, control flow, or a hang:** use live JVM breakpoints only after logs and source inspection cannot validate the hypothesis. Read [references/jvm-breakpoints.md](references/jvm-breakpoints.md) before attaching.
- **Native C/C++ code:** this skill does not provide native debugging; use Android Studio/LLDB.

## Target selection

Run `/Users/daniel/Library/Android/sdk/platform-tools/adb devices -l` first. If exactly one ready emulator is connected, use it without asking. If multiple targets are ready, ask which serial to use. If none is ready, tell the user to start `Medium_Phone_API_36.1` from Android Studio and stop; do not silently choose a physical device.

Use `adb -s <serial>` for every device command.

## Build, install, and launch

Build/install only when the requested investigation requires the current source:

```bash
/usr/bin/env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:installDebug
```

If installation succeeds:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> logcat -c
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> shell am force-stop ninja.doskey.app.levana
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> shell am start -n ninja.doskey.app.levana/.MainActivity
```

Do not launch after a failed build or install. Preserve the Gradle failure output and diagnose that failure instead.

## Logs and crash evidence

For recent fatal errors:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> logcat -d -s AndroidRuntime:E '*:F'
```

For app-relevant errors, start from a bounded dump and filter for `levana`, `AndroidRuntime`, `FATAL EXCEPTION`, `ANR`, `Exception`, or the suspected tag. If the process is running, resolve its PID first with `adb shell pidof ninja.doskey.app.levana`, then use `logcat --pid=<pid>`.

Prefer bounded dumps for diagnosis. Use a long-running logcat session only while actively reproducing a problem, and stop it afterward.

When reporting a crash, identify the exception, the first relevant Levana stack frame, the triggering path, and whether it reproduced on the current build. Do not infer a file/line that is absent from the trace.

## UI and interaction

Inspect the hierarchy with:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> shell uiautomator dump /sdcard/levana-window.xml
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> shell cat /sdcard/levana-window.xml
```

Capture a screenshot to a temporary local path when rendering or layout matters:

```bash
/Users/daniel/Library/Android/sdk/platform-tools/adb -s <serial> exec-out screencap -p > /tmp/levana-screen.png
```

View the image with the environment's image inspection tool when available. Use UI XML for stable selectors and semantic confirmation; do not treat coordinates from a different screen state as reliable.

ADB taps, swipes, text input, back, and key events are allowed for reproducing the user flow. Re-dump the UI after navigation before choosing the next coordinate.

## Verification and cleanup

After a fix, reproduce the same flow on a freshly installed debug build and verify both the user-visible outcome and the absence of the original error. Run focused automated tests when they cover the affected logic.

Stop any long-running logcat or debugger session. If live JVM debugging was used, also remove the JDWP port forward and clear Android's debug-app setting as described in the reference.
