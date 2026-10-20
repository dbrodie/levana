# Parallel emulator sessions for Android debugging

## Objective

Allow concurrent agents to debug Levana using independent disposable instances of
the existing AVD, with automatic port selection and explicit ownership. Leave this
branch unmerged for review.

## Implementation

- Add a Python standard-library session helper with start, attach, status, forward,
  and stop commands. Use private temporary session directories and JSON metadata.
- Bind a Unix reporting socket before starting the emulator with `-read-only` and
  `-report-console`; derive the ADB serial from the reported console port.
- Bound startup and boot waits, retain failure logs, and clean up only owned
  processes after verifying their process-start identity.
- Keep the shared ADB server and explicitly target every device command.
- Build from the selected worktree with assembleDebug and install its APK only on
  the selected serial. Keep evidence under the session artifact directory.
- Allocate debugger forwards with tcp:0 and remove only recorded forwards.
- Preserve explicit existing-device investigations with borrowed sessions.
- Keep persistent AVD cloning out of this increment.

## Validation

Run two independent agents concurrently against the same AVD. Verify distinct
serials, separate guest state and artifacts, concurrent dynamic forwards, and that
stopping one session leaves the other responsive. Exercise failure cleanup,
idempotent stop, and skill validation. Use available agent models because the
requested Opus 5.5 is not exposed in this environment. Record observed limitations.
