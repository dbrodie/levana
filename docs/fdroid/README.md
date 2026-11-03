# Levana on F-Droid: reproducible, developer-signed builds

F-Droid distributes **our** signed APK, the same file as on GitHub Releases and signed with the
same key as Play. Before it publishes a version, F-Droid builds the tagged source itself, without
our key, and checks that its unsigned APK is byte-for-byte our release APK apart from the
signature (`apksigcopier compare --unsigned`). Our own CI runs the same check on every release
(`.github/workflows/verify-release.yml`).

- The F-Droid recipe and how F-Droid finds new versions: [The F-Droid recipe](#the-f-droid-recipe).
- What happens on each release: [Release flow](#release-flow-with-f-droid).
- Validating the recipe locally with fdroidserver: [Local F-Droid validation](#local-f-droid-validation).
- Licences, dependencies, anti-features and open items: [COMPLIANCE.md](COMPLIANCE.md).
- The submission itself (the fdroiddata merge request) is TASK-53.6 and needs the owner's approval.

## The F-Droid recipe

F-Droid keeps the recipe in its own repository, fdroiddata, as
`metadata/ninja.doskey.app.levana.yml`. The copy in [ninja.doskey.app.levana.yml](ninja.doskey.app.levana.yml)
is the version validated here and proposed in the merge request. Once the merge request is
merged, fdroiddata's copy is the one that counts. F-Droid's bot then adds a build entry for each
new version on its own.

| Field | Value | Why |
|---|---|---|
| `Repo` / `commit` | the GitHub repo; the tag's commit hash (later builds: the tag `v%v`) | F-Droid builds the tagged source |
| `subdir` / `gradle` | `app` / `yes` | `assembleRelease`; without a keystore it writes `app-release-unsigned.apk` |
| `Binaries` | `https://github.com/dbrodie/levana/releases/download/v%v/levana-v%v.apk` | our signed APK, which F-Droid compares with its build and then publishes |
| `AllowedAPKSigningKeys` | `ab02841e…5117` | the only certificate F-Droid accepts on that APK (see [Signing identity](#signing-identity)) |
| `License` | `MIT` | `LICENSE` |
| `Categories` | `Calendar & Agenda` | from fdroiddata's `config/categories.yml` |
| `AntiFeatures` | none | see [COMPLIANCE.md](COMPLIANCE.md) |
| Summary, description, icon, screenshots, changelogs | not in the recipe | read from `fastlane/metadata/android/en-US/` at the tag |

F-Droid builds with its buildserver's default JDK (OpenJDK 21), not Temurin 17. That gives the
same APK (see [Pinned toolchain](#pinned-toolchain)), so the recipe doesn't install a JDK.

### How F-Droid finds new versions

F-Droid's update check (`fdroid checkupdates`) has to find both the versionName and the
versionCode. The versionCode exists only as `major*10000 + minor*100 + patch` computed from the
tag, and fdroidserver can't calculate it: its `Tags` mode can only cut one contiguous number out
of the tag name, and `403` is not in `v0.4.3`. So the recipe uses `UpdateCheckMode: HTTP`, a common
pattern in fdroiddata:

```yaml
AutoUpdateMode: Version v%v
UpdateCheckMode: HTTP
UpdateCheckData: https://github.com/dbrodie/levana/releases/latest/download/fdroid-version.txt|versionCode=(\d+)|.|versionName=([\d.]+)
```

`fdroid-version.txt` is a release asset (`versionCode=403` / `versionName=0.4.3`). It's uploaded by
`verify-release.yml` only after the reproducibility check passes, so F-Droid only sees a version
that it can rebuild. Nothing about releasing changes: the tag stays the only version source, and
no version file is committed. When F-Droid sees a new version, it adds a build entry for
`commit: v<version>` and builds it.

### F-Droid removes signing configs before building

Before it builds, fdroidserver deletes every `signingConfigs { … }` block from `build.gradle.kts`
(`remove_signing_keys`). So the build script must not rely on that block. `app/build.gradle.kts`
uses `signingConfigs.findByName("release")`, which then returns null, and the APK stays
unsigned. Up to v0.4.3, it used `getByName`, which fails once the block is gone. That's why the
recipe's v0.4.3 entry has a `prebuild` that creates an empty `release` config. It doesn't change
the APK. Builds from the first tag with `findByName` don't need it. `checkupdates` copies the
previous entry's fields into new entries, and there the `sed` matches nothing, so it's harmless.
Still, it's best to submit the recipe with the first release that has `findByName`, and drop
the `prebuild` line. That release is also the first tag with the owner-approved store listing
(TASK-55), which F-Droid reads from the built commit's `fastlane/` directory.

## Release flow with F-Droid

1. Release as usual (`specs/rfes/release-process.md`): commit the changelog, then tag `vX.Y.Z`. The
   Release workflow publishes `levana-vX.Y.Z.apk` and uploads the bundle to Play.
2. `verify-release.yml` rebuilds the tag without secrets and compares. On success it adds the ✅
   line to the release notes and uploads `fdroid-version.txt`.
3. F-Droid's checkupdates bot (it runs a few times a day) reads `fdroid-version.txt` from the
   latest release and commits a new build entry to fdroiddata.
4. F-Droid's buildserver builds the tag, downloads `levana-vX.Y.Z.apk`, checks it with
   `apksigcopier compare` and `AllowedAPKSigningKeys`, and publishes our signed APK. It usually
   appears in the F-Droid client a day or a few days after the tag.

If step 2 fails, F-Droid sees nothing new. Fix the cause and make a new patch release; don't
re-tag. If step 4 fails, F-Droid's build log (on monitor.f-droid.org and in fdroiddata) shows the
diff. Usually it's the same failure that `verify-release.yml` would have shown.

## Local F-Droid validation

This repeats fdroiddata's GitLab CI jobs (`fdroid lint`, `fdroid rewritemeta`, `checkupdates`,
schema validation and `fdroid build`) on a local fdroiddata checkout. It doesn't need a GitLab
account or a fork.

```bash
W=/tmp/fdroid-check && mkdir -p $W && cd $W
git clone --depth 1 https://gitlab.com/fdroid/fdroiddata.git
git clone https://gitlab.com/fdroid/fdroidserver.git      # fdroiddata CI uses fdroidserver master
cp /path/to/levana/docs/fdroid/ninja.doskey.app.levana.yml fdroiddata/metadata/
git -C fdroiddata config core.fileMode false
git -C fdroiddata checkout -b levana && git -C fdroiddata add metadata && git -C fdroiddata commit -m 'New app: Levana'

# Metadata checks (as fdroiddata's CI jobs: debian:trixie-slim with the fdroidserver deb for dependencies)
docker run --rm -v $W:/work debian:trixie-slim bash -c '
  apt-get update -q && apt-get install -qy --no-install-recommends fdroidserver git ca-certificates >/dev/null
  git config --global --add safe.directory "*"
  export PATH=/work/fdroidserver:$PATH PYTHONPATH=/work/fdroidserver serverwebroot=/tmp
  cd /work/fdroiddata && chmod 0600 config.yml
  fdroid readmeta && fdroid rewritemeta ninja.doskey.app.levana && git diff --exit-code &&
  fdroid lint ninja.doskey.app.levana && fdroid checkupdates --auto -v ninja.doskey.app.levana &&
  git diff --exit-code'
```

`checkupdates` must leave the recipe unchanged when the newest version is already listed.

For `fdroid build`, use fdroiddata's `fdroid build` CI job: the image
`registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie`, user `vagrant`, and
`fdroid build --verbose --test --refresh-scanner --on-server --no-tarball ninja.doskey.app.levana:<versionCode>`.
A pass ends with `compared built binary to supplied reference binary successfully` and
`supplied reference binary has allowed signer ab02841e…`, and leaves
`tmp/binaries/ninja.doskey.app.levana_<versionCode>.binary.apk`. That image is amd64 only. On Apple
silicon, Gradle under qemu emulation is too slow (lint alone ran for hours). TASK-53.5 instead used
an arm64 Debian 13 container set up the same way (same OpenJDK 21 package, SDK licences,
`gradlew-fdroid` and `bsenv.sh` from the official image). The scripts and logs are listed in
TASK-53.5's notes.

## How the build is made reproducible

- **One build for every channel.** `:app:assembleRelease` from the tag. The release workflow
  signs it with the release key; F-Droid and `verify-release.yml` build it unsigned.
- **Unsigned without a key.** When no release keystore is configured (`KEYSTORE_PATH`/
  `keystore.properties`), `assembleRelease` writes `app-release-unsigned.apk`. To install a local
  release build, add `-Plevana.debugSignRelease=true` to sign it with the debug key instead.
- **Version from the tag.** `versionName` is the tag without `v`; `versionCode` is
  `major*10000 + minor*100 + patch`, from `VERSION_NAME` or else `git describe --tags --abbrev=0`
  (`app/build.gradle.kts`). The APK depends on nothing else from git.
- **Nothing machine-specific in the APK:**
  - no VCS info (`vcsInfo.include = false`, so no `META-INF/version-control-info.textproto`);
  - no dependency-info block in the signing block (`dependenciesInfo`, TASK-53.2);
  - the prebuilt `.so` files from AARs are packaged unstripped (`keepDebugSymbols`), so whether an
    NDK is installed doesn't matter;
  - AGP already writes fixed zip timestamps (1981-01-01) in a fixed order.
  The baseline profile (`assets/dexopt/baseline.prof`/`.profm`) and the R8 output were identical
  in every rebuild, so they need no changes.
- **Pinned toolchain** (below). Every dependency version in `gradle/libs.versions.toml` is exact,
  and the resolved release classpath and build script classpath contain no dynamic versions or
  snapshots.

### Pinned toolchain

| Component | Version | Where |
|---|---|---|
| JDK | Eclipse Temurin **17.0.21+9** | `release.yml`, `verify-release.yml` (`setup-java`) |
| Gradle | 8.13, distribution SHA-256 `20f1b117…0aed78` | `gradle/wrapper/gradle-wrapper.properties` (`distributionSha256Sum`) |
| Gradle wrapper jar | official (Gradle 9.3.1 wrapper jar, works with any distribution) | checked by `gradle/actions/wrapper-validation` in both workflows |
| Android Gradle Plugin | 8.13.2 | `gradle/libs.versions.toml` |
| Kotlin / KSP | 2.1.0 / 2.1.0-1.0.29 | `gradle/libs.versions.toml` |
| compileSdk / build-tools | 36 / 35.0.0 | `app/build.gradle.kts` |
| NDK | none needed | prebuilt `.so` files are kept as they are |

Keep the release JDK and the rebuild JDK on the same 17.0.x release, and update both workflows
together. In testing, the output didn't depend on the JDK: Temurin 17.0.21 and JetBrains Runtime
21.0.9 produced byte-identical APKs, because Kotlin targets JVM 17 and D8/R8 produce the dex.
F-Droid's buildserver (Debian 13) builds with OpenJDK 21. `fdroid build` with OpenJDK 21.0.13
reproduced the v0.4.3 release APK (TASK-53.5), so the recipe uses that JDK and doesn't install one.

Gradle dependency verification (`verification-metadata.xml`) is not used. It would protect
against a tampered repository artifact, not against nondeterminism, which the fixed versions and
the rebuild check already cover. It would also need updating for every dependency, lint and
Robolectric change. Revisit this if supply-chain integrity becomes a goal of its own.

## The CI check (`verify-release.yml`)

- Runs automatically after each successful tag-triggered **Release** run (`workflow_run`), and on
  demand from the Actions tab (**Run workflow**, enter a tag such as `v0.4.3`).
- Uses no secrets. The `verify` job has a read-only token: it checks out the exact tag, sets up
  Temurin 17.0.21+9 with no Gradle cache, and builds `assembleRelease` without a keystore.
- Downloads `levana-v<ver>.apk` from that tag's GitHub Release, then checks:
  - package `ninja.doskey.app.levana`, plus versionCode and versionName derived from the tag, for
    both APKs (`aapt2 dump badging`);
  - exactly one signer, with certificate SHA-256 equal to the release / Play app-signing
    certificate (below);
  - `apksigcopier compare --unsigned signed.apk rebuilt-unsigned.apk`.
- On a mismatch, it runs diffoscope and uploads the HTML/text report and both APKs as the
  `reproducibility-v<ver>` artifact, and the run fails.
- A separate `report` job with `contents: write` runs no repository code. It adds a ✅/❌ line
  linking to the run to the release notes, and fails the run again if verification failed, so the
  result shows on the release page.

The release workflow also refuses to publish an APK whose certificate is not the expected one.

## Verify a release yourself

Requirements: git, Temurin (or any OpenJDK) **17.0.21**, the Android SDK with build-tools 35.0.0
and platform 36, and Python 3. Nothing else from us, and no secrets.

```bash
TAG=v0.4.3                       # the release to check
git clone https://github.com/dbrodie/levana.git && cd levana
git checkout "$TAG"
export JAVA_HOME=/path/to/jdk-17.0.21 ANDROID_HOME=/path/to/android-sdk
VERSION_NAME="${TAG#v}" ./gradlew :app:assembleRelease --no-daemon

curl -LO "https://github.com/dbrodie/levana/releases/download/$TAG/levana-$TAG.apk"
python3 -m venv /tmp/apksig && /tmp/apksig/bin/pip install apksigcopier==1.1.1
export PATH="$ANDROID_HOME/build-tools/35.0.0:$PATH"     # apksigcopier calls apksigner

/tmp/apksig/bin/apksigcopier compare --unsigned "levana-$TAG.apk" \
  app/build/outputs/apk/release/app-release-unsigned.apk && echo REPRODUCIBLE
apksigner verify --print-certs "levana-$TAG.apk" | grep 'SHA-256'
aapt2 dump badging "levana-$TAG.apk" | head -1
```

To see what differs after a failed compare, run `diffoscope levana-$TAG.apk
app/build/outputs/apk/release/app-release-unsigned.apk`. The APK Signing Block always differs;
everything else should not.

Note that signing a rebuilt APK with `apksigner sign` doesn't reproduce our APK. With default
options it adds v1 (JAR) signature files and re-aligns entries. Our APK is signed by AGP during
`assembleRelease` (v2 only), and the compare copies that signature as-is.

## Signing identity

| | |
|---|---|
| Certificate | `CN=Daniel Brodie` |
| Certificate SHA-256 | `ab02841e88825c51c8f0b0f013ff4e312bca47ea1bfbffa894b51890ad5a5117` |
| F-Droid `AllowedAPKSigningKeys` | the same value |
| Signature schemes | v2 (minSdk 34, so no v1) |

GitHub release APKs are signed with this key (checked for v0.4.1 and v0.4.2). Since 2026-10-30
it is also the **Play app-signing key** (uploaded with PEPK), so one APK and one signing identity
serve GitHub, Play and F-Droid. To confirm it, compare the value with the app-signing key
certificate's SHA-256 in Play Console (*App integrity* → *App signing*). Before that date, Play
re-signed uploads with a different, Google-managed key, so builds up to v0.4.1 (versionCode 401)
can't be used on Play.

## Key backup and recovery

The key cannot be replaced. Android only installs updates signed with the same key, F-Droid only
accepts our APKs while the certificate equals `AllowedAPKSigningKeys`, and Play app signing is
bound to it. If the key is lost, no channel can update existing installs: the app has to ship as
a new package, and users must reinstall.

Never commit the key, a password, `keystore.properties` or a decoded keystore. The repo is public,
so Actions logs and artifacts are world-readable too.

- **Where the key is**:
  - the owner's machine, as `levana-release.keystore` (alias `levana`) outside any git checkout;
  - the `KEYSTORE_BASE64` Actions secret, with `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`;
  - the passwords are in the owner's password manager.
- **Backups**:
  - keep at least two offline copies of the keystore file in separate places, e.g. an encrypted
    USB drive and an encrypted cloud or password-manager attachment;
  - keep the passwords somewhere other than the keystore copies;
  - check a backup periodically: `keytool -list -v -keystore <copy> -alias levana` should show
    the SHA-256 above.
- **Recovery**:
  - restore the keystore from a backup and check its fingerprint;
  - re-create the four Actions secrets if needed (`base64 -i levana-release.keystore`, pasted into
    the secret form, never into a log);
  - tag a release and confirm that both the release workflow's certificate check and
    `verify-release.yml` pass.
- **Compromise**: rotating the key would break every channel, as above. Play supports an upgrade
  through key rotation (APK Signature Scheme v3 lineage). F-Droid would need
  `AllowedAPKSigningKeys` updated in fdroiddata. Plan this before acting.
- **Secret exposure in CI**: only the tag-triggered release job references the signing secrets.
  `verify-release.yml` and any future PR workflow must not. To make that a hard rule, move the four
  secrets and `PLAY_SERVICE_ACCOUNT_JSON` into a GitHub **environment** named `release`:
  - add `environment: release` to the `build-release` job;
  - limit the environment's deployment tags to `v*.*.*`.
