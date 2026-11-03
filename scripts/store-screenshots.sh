#!/usr/bin/env bash
# Regenerates the Play/F-Droid phone screenshots (StoreScreenshotTest) into
# fastlane/metadata/android/en-US/images/phoneScreenshots/. See specs/STORE_LISTING.md.
#
#   scripts/store-screenshots.sh          # all shots
#   scripts/store-screenshots.sh 2,5      # only shots 2 and 5
#
# Afterwards `git status fastlane/` shows what changed. Nothing under fastlane/ is committed
# without the owner's approval; `git restore fastlane/` discards a regeneration.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; . ./.env; set +a; fi

./gradlew :app:recordRoborazziDebug :app:testDebugUnitTest --rerun \
  --tests '*.screenshots.StoreScreenshotTest' -Plevana.store.shots="${1:-all}"

git status --short fastlane/metadata/android/en-US/images/phoneScreenshots/
