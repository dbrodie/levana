# RFE: Add Roborazzi Screenshot Testing Support [DONE]

## Context

We need a way to visually verify Compose UI rendering without an emulator. Roborazzi uses Robolectric + Android's layoutlib to render Compose on the JVM and capture screenshots for comparison. The immediate use case is verifying the day-of-week calendar header renders correctly in both Gregorian (LTR, English) and Hebrew Primary (RTL, Hebrew) modes.

## Branch

`rfe_add_roborazzi_support`

## Changes

### 1. Version catalog (`gradle/libs.versions.toml`)

Add versions:
```toml
roborazzi = "1.59.0"
robolectric = "4.14.1"
junit = "4.13.2"
```

Add libraries:
```toml
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule = { group = "io.github.takahirom.roborazzi", name = "roborazzi-junit-rule", version.ref = "roborazzi" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

Add plugin:
```toml
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

### 2. Root `build.gradle.kts`

Add to plugins block:
```kotlin
alias(libs.plugins.roborazzi) apply false
```

### 3. App `app/build.gradle.kts`

Add plugin:
```kotlin
alias(libs.plugins.roborazzi)
```

Add `testOptions` inside `android` block:
```kotlin
testOptions {
    unitTests {
        isIncludeAndroidResources = true
        all {
            it.systemProperty("robolectric.graphicsMode", "NATIVE")
        }
    }
}
```

Add test dependencies:
```kotlin
testImplementation(platform(libs.compose.bom))
testImplementation(libs.junit)
testImplementation(libs.robolectric)
testImplementation(libs.roborazzi)
testImplementation(libs.roborazzi.compose)
testImplementation(libs.roborazzi.junit.rule)
testImplementation(libs.compose.ui.test.junit4)
debugImplementation(libs.compose.ui.test.manifest)
```

### 4. Make `DayOfWeekHeader` testable

In `app/src/main/java/com/levana/app/ui/calendar/CalendarScreen.kt`, change:
```kotlin
private fun DayOfWeekHeader(hebrewPrimary: Boolean)
```
to:
```kotlin
@VisibleForTesting
internal fun DayOfWeekHeader(hebrewPrimary: Boolean)
```

### 5. Create Robolectric properties

**File:** `app/src/test/resources/robolectric.properties`
```properties
sdk=34
```

### 6. Create screenshot test

**File:** `app/src/test/java/com/levana/app/ui/calendar/DayOfWeekHeaderScreenshotTest.kt`

Two test cases:
- `dayOfWeekHeader_gregorian_ltr` — `hebrewPrimary = false`, no RTL wrapper, verifies Sun-Sat left-to-right in English
- `dayOfWeekHeader_hebrew_rtl` — `hebrewPrimary = true`, wrapped in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`, verifies Hebrew day names right-to-left

Use `@Config(qualifiers = "en-rUS-w400dp-h800dp-mdpi")` for locale determinism. Screenshots saved to `app/src/test/screenshots/`.

## Usage

```bash
# Record baseline screenshots:
./gradlew recordRoborazziDebug

# Verify against baselines:
./gradlew verifyRoborazziDebug

# Generate diff images for debugging:
./gradlew compareRoborazziDebug
```

## Notes

- `isIncludeAndroidResources = true` is required for Robolectric to load Android resources
- `testImplementation(platform(libs.compose.bom))` is needed so Compose test deps resolve versions from the BOM
- `debugImplementation(compose-ui-test-manifest)` provides the `ComponentActivity` needed by Compose test rules
- First run downloads Robolectric's instrumented SDK jar for SDK 34 (one-time, cached after)
- Recorded `.png` files should be committed to git; add `*_compare.png` to `.gitignore`
