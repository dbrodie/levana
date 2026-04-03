# Enable R8 for Release Builds

## Summary

Enable R8 minification and shrinking for release builds to significantly reduce APK size.

## Motivation

Release APKs are ~50 MB due to two factors:
- `isMinifyEnabled = false` ships all library code unstripped
- `compose.material.icons.extended` is ~10 MB+ and ships entirely even though only a handful of icons are used

## Changes

- Set `isMinifyEnabled = true` and `isShrinkResources = true` in the release build type
- Add `proguardFiles(...)` pointing to the standard Android rules and `proguard-rules.pro`
- Create `app/proguard-rules.pro` with rules to preserve runtime-needed classes (Koin, Room, Kotlin serialization, KosherJava)

## Expected outcome

APK size reduced from ~50 MB to ~10–15 MB primarily by tree-shaking unused Material icons and stripping dead library code.
