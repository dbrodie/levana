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
- Live tests found an existing writable source AVD blocks same-AVD read-only
  launches on this runtime. Provision a dedicated agent template from static config
  and the installed SDK system image only, with fresh userdata; leave the user
  emulator running. Lock provisioning and reject unmanaged existing templates.
- Verify the owned child actually listens on its reported console port before
  accepting readiness; do not accidentally target an existing racing emulator.
- Stop owned processes even if ADB forward cleanup fails; retain cleanup warnings.

## Validation

Run two independent agents concurrently against the same AVD. Verify distinct
serials, separate guest state and artifacts, concurrent dynamic forwards, and that
stopping one session leaves the other responsive. Exercise failure cleanup,
idempotent stop, and skill validation. Use available agent models because the
requested Opus 5.5 is not exposed in this environment. Record observed limitations.

## Observed validation (2026-10-20)

- Two available-model agents completed two live parallel rounds. Opus 5.5 was not
  available through the agent API.
- Round 1 used a freshly provisioned, warmed config-only temporary template. Agents
  owned emulator-5556 and emulator-5558; writing different values to the same guest
  file remained isolated. Each installed the worktree APK and captured its own UI.
- Round 2 used the actual default managed template, with no warmup. Emulator launches
  started two seconds apart and both booted successfully. Both agents installed the
  APK and completed actual jdb attach, thread listing, and detach on dynamic ports.
- Stopping A preserved B's device, marker, and forward. Repeated stop was safe.
- Missing-AVD and forced startup-timeout tests failed with retained diagnostics and
  no surviving owned emulator.
- Twelve focused ownership, forwarding, provisioning, and cleanup regression tests
  passed. Skill validator and `git diff --check` passed. assembleDebug succeeded with
  ANDROID_HOME explicitly supplied for the fresh worktree.
- Testing corrected two older skill assumptions: MainActivity is not exported
  (resolve the enabled launcher alias), and adb jdwp streams indefinitely (use bounded
  app PID lookup before forwarding, then verify availability with actual jdb attach).
- Original user emulator remained running. No source userdata, snapshots, or locks
  were copied or deleted. Keep the dedicated managed template for future sessions.
- Evidence: /tmp/levana-emu-iv8q_hav/artifacts/,
  /tmp/levana-emu-71p4tqc7/artifacts/jdb.txt, and tester B's
  /tmp/levana-emu-f64zkwie/artifacts/.

The branch is intentionally unmerged. Sandbox auto-review delays affected test
scheduling; they did not prevent the completed live checks.
