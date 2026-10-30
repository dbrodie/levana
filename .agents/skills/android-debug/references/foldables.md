# Foldables: Galaxy Z Fold8 and Fold8 Ultra

Emulators and commands for testing Levana on the Fold8 and Fold8 Ultra cover and inner
displays and across fold/unfold (TASK-52). Display specs, sources and the dp arithmetic are
in TASK-52.1; the Roborazzi sizes are in `specs/ARCHITECTURE_SPEC.md`.

## AVDs

Set `AVDS` to the absolute path of `scripts/foldable_avds.py` beside this skill (next to
`$HELPER`):

```bash
python3 "$AVDS" create   # once per machine; skips existing ones
python3 "$AVDS" list
```

| AVD | Inner (unfolded) | Cover (folded) |
|---|---|---|
| `Levana_Fold8_API_36_1` | 2448 x 1848 px, 933 x 704 dp (landscape) | 1248 x 1972 px, 475 x 751 dp |
| `Levana_Fold8_Ultra_API_36_1` | 2256 x 2504 px, 859 x 954 dp (portrait) | 1080 x 2520 px, 411 x 960 dp |

Both run at 420 dpi, an assumption: Samsung publishes no Android density. On a real device
check it with `adb shell wm density` and `wm size`.

Each AVD has two built-in displays, inner and cover, and the system moves the app between
them on fold, as on a real Samsung foldable. The emulator only does this for its Pixel Fold
device names, so the AVDs set `hw.device.name` to `pixel_fold` / `pixel_9_pro_fold` with
Samsung's sizes. A generic hinge AVD (`hw.sensor.hinge` with a `hw.displayRegion`, such as
Studio's deprecated "7.6in Foldable") reports the posture but keeps the full inner display,
so it does not test the cover. The AVDs copy the agent template's static configuration and
system image and are marked as managed; the script never overwrites an unmanaged AVD.

Start one like any other AVD (headless works; `-read-only` keeps it disposable):

```bash
python3 "$HELPER" start --project-root "$PWD" --avd Levana_Fold8_API_36_1 --headless
```

## Fold, unfold and postures

```bash
adb -s "$SERIAL" shell locksettings set-disabled true   # once per session: no PIN prompt
adb -s "$SERIAL" emu fold                  # device state CLOSED: the app moves to the cover
adb -s "$SERIAL" shell wm dismiss-keyguard # the "Swipe up to continue" screen after a fold
adb -s "$SERIAL" emu unfold                # OPENED: back to the inner display
adb -s "$SERIAL" shell wm dismiss-keyguard
adb -s "$SERIAL" emu posture 2             # HALF_OPENED (tabletop/book); 1 CLOSED, 3 OPENED
adb -s "$SERIAL" shell cmd device_state state   # the committed state
```

Each fold or unfold shows Android's "Swipe up to continue" screen on the new display; the
activity is stopped until it is dismissed, and only then relaunched at the new size. Do not
rely on `settings put system fold_lock_behavior_setting stay_awake_on_fold_key`; on this
image it did not remove the screen.

Rotation of the inner display:

```bash
adb -s "$SERIAL" shell settings put system accelerometer_rotation 0
adb -s "$SERIAL" shell settings put system user_rotation 1   # 0 natural, 1 90°, 2 180°, 3 270°
```

## Evidence

Screenshots need the display: `screencap` warns and picks one when there are two.

```bash
adb -s "$SERIAL" shell dumpsys SurfaceFlinger --display-id   # first line inner, second cover
adb -s "$SERIAL" exec-out screencap -p -d <display-id> > "$SESSION/artifacts/cover.png"
```

The window size the app received, and its relaunches:

```bash
adb -s "$SERIAL" shell dumpsys activity activities | grep -A60 "ninja.doskey.app.levana/.MainActivity" | grep -m1 -o -E "sw[0-9]+dp w[0-9]+dp h[0-9]+dp [0-9]+dpi"
adb -s "$SERIAL" logcat -d -b events | grep "levana.MainActivity" | grep -E "wm_on_(stop|destroy|create|resume)_called"
adb -s "$SERIAL" logcat -d -b crash
```

The manifest declares no `configChanges`, so each fold or unfold recreates `MainActivity`
(`handleRelaunchActivity` in the events log). Folding on the Ultra can trigger TASK-50's
restore crash (two relaunches in quick succession).

## Real devices

Pair over wireless debugging (Settings > Developer options > Wireless debugging > Pair
device with pairing code), then connect and use the printed serial with `attach`:

```bash
adb pair <ip>:<pairing-port>      # enter the pairing code
adb connect <ip>:<port>
python3 "$HELPER" attach --serial <ip>:<port> --project-root "$PWD"
```

On a real device fold and unfold by hand. Samsung's own setting for continuing apps on the
cover screen decides whether the app stays open after a fold; check it before testing.
