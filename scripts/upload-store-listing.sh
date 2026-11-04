#!/usr/bin/env bash
# Uploads the Play store listing (fastlane/metadata/android/en-US: title, short and full
# description, icon, feature graphic, phone screenshots) with fastlane supply. Never the APK,
# the AAB or the changelogs: those go with each release (.github/workflows/release.yml).
# The owner runs this by hand; see specs/STORE_LISTING.md and specs/rfes/release-process.md.
#
#   scripts/upload-store-listing.sh                  # dry run: Google Play validates, nothing is published
#   scripts/upload-store-listing.sh --upload         # real upload, after a typed confirmation
#   scripts/upload-store-listing.sh --version-code 404
#
# Needs PLAY_SERVICE_ACCOUNT_JSON_PATH: the service-account JSON key, kept outside the repo.
set -euo pipefail

FASTLANE_VERSION=2.241.2   # needs Ruby >= 3.2
PACKAGE_NAME=ninja.doskey.app.levana
TRACK=internal
TAG=store-listing-published
LISTING=fastlane/metadata/android/en-US

usage() {
  cat <<EOF
Usage: scripts/upload-store-listing.sh [--upload] [--version-code N] [-h|--help]

Uploads the en-US Play listing in $LISTING (text and images, not
changelogs) with fastlane supply $FASTLANE_VERSION.

  (no flag)          Dry run: Google Play validates the edit, then it is discarded.
  --upload           Publish the listing. Asks you to type 'upload' to confirm.
  --version-code N   A release on the '$TRACK' track. supply needs one even for a
                     listing-only upload. Default: the newest vX.Y.Z tag's versionCode.

Environment:
  PLAY_SERVICE_ACCOUNT_JSON_PATH   Path to the Play service-account JSON key, outside
                                   this repo. Never commit or print it.

Setup, once: Ruby >= 3.2, then  gem install fastlane -v $FASTLANE_VERSION
EOF
}

die() { echo "error: $*" >&2; exit 1; }

UPLOAD=false
VERSION_CODE=
while [ $# -gt 0 ]; do
  case "$1" in
    --upload) UPLOAD=true ;;
    --version-code)
      [ $# -ge 2 ] || die "--version-code needs a value"
      VERSION_CODE=$2; shift ;;
    --version-code=*) VERSION_CODE=${1#*=} ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; die "unknown argument: $1" ;;
  esac
  shift
done

cd "$(dirname "$0")/.."
REPO=$(pwd -P)

# supply looks up a release on the track even when it uploads only the listing, and fails on
# a track without one ("Could not find release for version code ''", v0.4.4 on production).
# Every release goes to internal, so the newest release tag names a release there.
if [ -z "$VERSION_CODE" ]; then
  LATEST=$(git tag -l 'v*.*.*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n 1 || true)
  [ -n "$LATEST" ] || die "no vX.Y.Z tag found; pass --version-code"
  IFS='.' read -r MAJ MIN PAT <<< "${LATEST#v}"
  VERSION_CODE=$(( MAJ * 10000 + MIN * 100 + PAT ))
fi
[[ "$VERSION_CODE" =~ ^[0-9]+$ ]] || die "--version-code must be a number, got '$VERSION_CODE'"

# Local summary, before anything talks to Google Play
echo "Listing: $LISTING (changelogs excluded)"
for field in title:30 short_description:80 full_description:4000; do
  name=${field%%:*} limit=${field##*:}
  [ -s "$LISTING/$name.txt" ] || die "$LISTING/$name.txt is missing or empty"
  length=$(printf '%s' "$(cat "$LISTING/$name.txt")" | LC_ALL=en_US.UTF-8 wc -m | tr -d ' ')
  [ "$length" -le "$limit" ] || die "$name.txt is $length characters; Play allows $limit"
  printf '  %-22s %5s / %s characters\n' "$name.txt" "$length" "$limit"
done
for image in icon featureGraphic; do
  [ -f "$LISTING/images/$image.png" ] || die "$LISTING/images/$image.png is missing"
  echo "  images/$image.png"
done
echo "  images/phoneScreenshots: $(find "$LISTING/images/phoneScreenshots" -name '*.png' | wc -l | tr -d ' ') screenshots"
echo "Track for the release lookup: $TRACK, versionCode $VERSION_CODE"

if git rev-parse -q --verify "refs/tags/$TAG" > /dev/null; then
  PATHS=("$LISTING" ":(exclude)$LISTING/changelogs")
  if git diff --quiet "$TAG" -- "${PATHS[@]}"; then
    echo "Unchanged since the last upload ($TAG = $(git rev-parse --short "$TAG^{commit}"))."
  else
    echo "Changed since the last upload ($TAG = $(git rev-parse --short "$TAG^{commit}")):"
    git diff --stat "$TAG" -- "${PATHS[@]}" | sed 's/^/  /'
  fi
else
  echo "No local $TAG tag, so no diff against the last upload (git fetch origin --tags)."
fi

DIRTY=$(git status --porcelain -- "$LISTING")
if [ -n "$DIRTY" ]; then
  $UPLOAD && die "uncommitted changes in $LISTING; upload only committed, owner-approved listings"
  echo "warning: uncommitted changes in $LISTING are included in this dry run" >&2
fi
echo

if [ -z "${PLAY_SERVICE_ACCOUNT_JSON_PATH:-}" ]; then
  die "PLAY_SERVICE_ACCOUNT_JSON_PATH is not set.
  Point it at the Play service-account JSON key, stored outside this repo (TASK-58), e.g.
    PLAY_SERVICE_ACCOUNT_JSON_PATH=~/secrets/levana-play.json scripts/upload-store-listing.sh"
fi
KEY=$PLAY_SERVICE_ACCOUNT_JSON_PATH
[ -f "$KEY" ] && [ -r "$KEY" ] || die "PLAY_SERVICE_ACCOUNT_JSON_PATH does not name a readable file"
KEY=$(cd "$(dirname "$KEY")" && pwd -P)/$(basename "$KEY")
case "$KEY" in
  "$REPO"/*) die "the service-account key is inside the repo; move it outside $REPO" ;;
esac

command -v gem > /dev/null || die "Ruby is not installed; fastlane $FASTLANE_VERSION needs Ruby >= 3.2"
gem list -i fastlane -v "$FASTLANE_VERSION" > /dev/null ||
  die "fastlane $FASTLANE_VERSION is not installed: gem install fastlane -v $FASTLANE_VERSION (needs Ruby >= 3.2)"

if $UPLOAD; then
  [ -r /dev/tty ] || die "--upload needs a terminal to confirm"
  printf "Publish this listing to Google Play for %s? Type 'upload' to continue: " "$PACKAGE_NAME"
  read -r answer < /dev/tty
  [ "$answer" = upload ] || die "not confirmed; nothing uploaded"
fi

# supply uploads every language folder under metadata_path, so it gets a copy of en-US only
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
mkdir "$STAGE/en-US"
cp "$LISTING"/*.txt "$STAGE/en-US/"
cp -R "$LISTING/images" "$STAGE/en-US/"

VALIDATE_ONLY=true
if $UPLOAD; then VALIDATE_ONLY=false; fi

# supply reads the key from the path; its summary table shows the path, never the key
SUPPLY_JSON_KEY=$KEY \
SUPPLY_PACKAGE_NAME=$PACKAGE_NAME \
SUPPLY_METADATA_PATH=$STAGE \
SUPPLY_TRACK=$TRACK \
SUPPLY_VERSION_CODE=$VERSION_CODE \
SUPPLY_SKIP_UPLOAD_APK=true \
SUPPLY_SKIP_UPLOAD_AAB=true \
SUPPLY_SKIP_UPLOAD_CHANGELOGS=true \
SUPPLY_SYNC_IMAGE_UPLOAD=true \
SUPPLY_VALIDATE_ONLY=$VALIDATE_ONLY \
  fastlane "_${FASTLANE_VERSION}_" supply

if $UPLOAD; then
  echo
  echo "Uploaded. Record it by moving $TAG to this commit:"
  echo "  git tag -f $TAG $(git rev-parse HEAD) && git push --force origin refs/tags/$TAG"
else
  echo
  echo "Dry run passed; nothing was published. Run again with --upload to publish."
fi
