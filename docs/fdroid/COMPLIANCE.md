# F-Droid compliance report: Levana (`ninja.doskey.app.levana`)

State on 2026-11-08, for the fdroiddata submission (TASK-53.6) of v0.4.4. The build and recipe are
described in [README.md](README.md), and the recipe is [ninja.doskey.app.levana.yml](ninja.doskey.app.levana.yml).

## Summary

| Requirement ([F-Droid inclusion policy](https://f-droid.org/docs/Inclusion_Policy/)) | Status |
|---|---|
| Free licence for the app | MIT (`LICENSE`) |
| Free licences for every bundled dependency | yes: Apache-2.0, BSD-3-Clause, LGPL-2.1, MIT (`THIRD_PARTY_NOTICES.md`) |
| Builds from source with free tools only | yes: Gradle, AGP and the Android SDK; no NDK, no prebuilt jars apart from the Gradle wrapper (F-Droid deletes that and uses its own Gradle) |
| No proprietary dependencies (Google Play Services, Firebase, analytics, ads) | none since TASK-53.2 |
| Reproducible, so F-Droid can ship our signed APK | yes; see [Test results](#test-results) |
| Anti-features | none (see [Anti-features](#anti-features)) |

## Licences

- **App:** MIT, copyright Daniel Brodie (`LICENSE`).
- **Bundled libraries:** the release runtime classpath has 168 artifacts:
  - 165 under Apache-2.0: AndroidX, Jetpack Compose, Koin, kotlinx, ACRA and others;
  - 1 under BSD-3-Clause: `androidx.glance:glance-appwidget-external-protobuf`;
  - 1 under MIT: `org.checkerframework:checker-qual`;
  - 1 under LGPL-2.1: KosherJava `com.kosherjava:zmanim`.
  `THIRD_PARTY_NOTICES.md` is generated from the dependency graph
  (`scripts/generate_third_party_notices.py`) and linked from the app's About screen.
- **LGPL-2.1 (KosherJava):** it's used unmodified, as the published Maven artifact. The app's full
  source is public, so the app can be rebuilt against a modified copy of the library. LGPL-2.1
  is compatible with distributing an MIT app.
- **Assets:** icons and graphics are the project's own. Material icons (`material-icons-extended`)
  are Apache-2.0.

## Dependencies and build

- Repositories (`settings.gradle.kts`): libraries come from Google Maven and Maven Central only;
  build plugins also come from the Gradle Plugin Portal. There are no JitPack, snapshot or
  private repositories.
- Every version is exact in `gradle/libs.versions.toml`. The resolved release classpath has no
  dynamic or snapshot versions (TASK-53.3).
- No Google Play Services or Firebase: location uses the platform `LocationManager` (TASK-53.2).
  `dependenciesInfo` is off, so the APK has no Google-encrypted dependency block.
- Native code: only the prebuilt `.so` files inside two AndroidX AARs
  (`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`), built from AOSP source.
  They are packaged unstripped, so an NDK on the build machine doesn't change the APK.
- The F-Droid scanner (`fdroid build` with `--refresh-scanner`) found no problems apart from
  deleting `gradle/wrapper/gradle-wrapper.jar`, which it always does. See
  [Test results](#test-results).

## Anti-features

None apply. Checked against fdroiddata's `config/antiFeatures.yml`:

| Anti-feature | Assessment |
|---|---|
| Ads | none |
| Tracking | none. There are no analytics. Crash reports use ACRA in dialog + email mode: after a crash, the app asks the user, and a report is only sent if the user confirms and sends the prepared email themselves. The report has the version, Android version, device model, crash date and stack trace; no logcat and no app data. |
| NonFreeNet | none. The app works fully offline. It makes one network request itself, the update check below; the location name comes from the platform geocoder (below). |
| NonFreeDep / NonFreeAdd / UpstreamNonFree | none. The same source and the same APK go to every channel. |
| NonFreeAssets | none |
| KnownVuln | none known |
| NSFW | no |
| TetheredNet | no |
| DisabledAlgorithm | no. APK Signature Scheme v2 with an RSA-4096 key. |
| NoSourceSince | no |

**Update check:** a daily WorkManager job (`UpdateCheckWorker`) asks the GitHub Releases API for
the latest version and offers a link to the release page. It skips the check when the installer
is Play (`com.android.vending`) or F-Droid (`org.fdroid.fdroid`) (`UpdateSource.kt`), because
those update the app themselves. It sends no identifiers. GitHub is a free-of-charge public
service, and the check is not required for any feature, so we don't expect an anti-feature
for it. Gap: other F-Droid clients (for example F-Droid Basic, `org.fdroid.basic`, or Droid-ify)
aren't recognised yet, so those installs still run the GitHub check. TASK-63 tracks this.

**Location name:** when the user picks *Current location*, the app asks the platform
`android.location.Geocoder` for the place name (`ReverseGeocoder.kt`). The app opens no
connection for this; the device's geocoding provider, if one is installed, does the lookup. If
there is no provider, or no answer within 5 seconds, the app shows "Current Location". Zmanim
only use the coordinates, so nothing depends on the lookup.

**Permissions** (all used only on the device):
- `ACCESS_FINE/COARSE_LOCATION` for zmanim at the current location;
- `READ_CALENDAR` to show device calendar events;
- `READ/WRITE_CONTACTS` for contacts' Hebrew birthdays, stored as a custom data row on the contact;
- `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` for reminders;
- `INTERNET` for the update check.

## Test results

Recipe for v0.4.4 / 404, run on 2026-11-08 (TASK-53.6) against a fresh local fdroiddata checkout
(master `bd34407b`) with fdroidserver master `cdfdce83` (fdroiddata's CI pins `a35fdfdd` and pulls
forward to master, which was `cdfdce83`). Each job was run the way fdroiddata's `.gitlab-ci.yml`
runs it.

| Check | Result |
|---|---|
| `fdroid readmeta` | pass |
| `fdroid rewritemeta` | pass, no changes |
| `fdroid lint` | pass, no warnings |
| `tools/rewrite-git-redirects.py` (*git redirect* job) | pass, no changes |
| JSON schema (`check-jsonschema` with `schemas/metadata.json`) | pass |
| `tools/check-exif-in-images.sh`, `check-localized-metadata.py`, `check-keyalias-collision.py`, `check-metadata-summary-whitespace.py`, `check-for-unattached-signatures.py`, `make-summary-translatable.py` | pass, no findings |
| `fdroid checkupdates --auto`, real `fdroid-version.txt` URL | up to date at 0.4.4 / 404, recipe unchanged |
| `fdroid build --test --refresh-scanner --on-server --no-tarball ninja.doskey.app.levana:404` | pass: *compared built binary to supplied reference binary successfully*; *supplied reference binary has allowed signer ab02841e…5117* |
| `apksigcopier compare --unsigned` of `levana-v0.4.4.apk` against F-Droid's build | exit 0; both APKs `ninja.doskey.app.levana` 404 / 0.4.4 |

The F-Droid build was made by OpenJDK 21.0.13 (Debian 13, as on F-Droid's buildserver). The
GitHub APK was made by Temurin 17.0.21. The two match. The scanner found nothing apart from the
Gradle wrapper jar, which it always deletes. v0.4.4 built without any `prebuild`, so the
`findByName` fix works with F-Droid's signing-config removal.

The build ran in an arm64 Debian 13 container set up like F-Droid's
`buildserver-trixie` image, not in that amd64 image itself: under emulation on Apple silicon,
Gradle was too slow to finish. The MR's GitLab CI pipeline will run the real image.

The v0.4.3 recipe passed the same checks on 2026-11-03/04 (TASK-53.5), with a `prebuild` for the
`getByName` signing config.

### Store metadata (`fastlane/metadata/android/en-US/`)

The owner approved the listing in TASK-55 (2026-11-03). The v0.4.4 tag's copy was checked on 2026-11-08 against
fdroidserver's own reader (`fdroid update`'s `insert_localized_app_metadata`, with F-Droid's
`char_limits`). Nothing is truncated:

| File | Length | F-Droid limit |
|---|---|---|
| `title.txt` | 23 | 50 |
| `short_description.txt` | 70 | 80 |
| `full_description.txt` | 671 | 4000 |
| `changelogs/401.txt` … `404.txt` | 471, 360, 417, 432 | 500 |

The images follow F-Droid's layout:
- `images/icon.png`, 512×512;
- `images/featureGraphic.png`, 1024×500;
- eight `images/phoneScreenshots/<n>_<name>.png`, each 1080×1920. F-Droid sorts them by file
  name, so the number prefix sets their order.

## Remaining blockers and owner actions

1. **Submit the merge request** to https://gitlab.com/fdroid/fdroiddata (TASK-53.6, owner
   authorization required): a GitLab account, a public fork, and the branch with
   `metadata/ninja.doskey.app.levana.yml`. Its pipeline must pass `fdroid build` on the real
   buildserver image. If a newer release exists by then, replace the build entry with it.
2. Follow-up, not blocking: TASK-63 (update check with other F-Droid clients).

`fdroid-version.txt` is published for v0.4.4, and v0.4.4 has both the `findByName` fix and the
approved listing, so those earlier blockers are resolved.
