# Fix: City Picker Leaves Keyboard Open

## Problem

On a fresh install, onboarding → Pick a City → type "New" in the search field → tap "New York".
The app navigates to the calendar but the soft keyboard stays open, covering the bottom half of
the calendar while it loads. The same `CityPickerScreen` is used from Settings → Manage cities →
Add location, where the keyboard likewise lingers over the Manage cities screen.

Root cause: `CityPickerScreen` never clears focus or hides the IME. Selecting a city fires
`CityPickerIntent.SelectCity`, the ViewModel saves the location and emits `LocationSaved`, and the
screen navigates away while the search `OutlinedTextField` still holds focus and an active text
input session. The keyboard is only torn down once the old destination leaves composition (after
the navigation transition), if at all.

## Fix

In `CityPickerScreen`:

1. Obtain `LocalSoftwareKeyboardController` and `LocalFocusManager`.
2. When a city row is tapped, call `focusManager.clearFocus()` and `keyboardController?.hide()`
   before dispatching `SelectCity`, so the keyboard is gone before navigation starts.
3. Add a `LifecycleEventEffect(Lifecycle.Event.ON_PAUSE)` that does the same. The nav back stack
   entry is paused as soon as navigation away begins, so this covers every other exit path
   (top-bar back arrow, system back/gesture, app backgrounding).

No ViewModel or navigation changes.

## Test Plan

- `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
- Emulator, onboarding entry: `pm clear`, launch, Pick a City, type "New", tap "New York" →
  calendar shows with no keyboard (`dumpsys input_method` reports `mInputShown=false`).
- Emulator, settings entry: Settings → Manage cities → Add location, type a query, tap a result →
  back on Manage cities with no keyboard.
- Emulator, back-arrow exit: open the picker from Manage cities, type, tap the top-bar back arrow →
  no keyboard on Manage cities.
