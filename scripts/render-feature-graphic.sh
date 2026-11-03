#!/usr/bin/env bash
# Renders docs/store/feature-graphic.svg to the store feature graphic (1024×500 RGB PNG, which
# Play requires) with headless Chrome. The fonts (Helvetica Neue, Arial Hebrew) are macOS's,
# so render on a Mac. See specs/STORE_LISTING.md.
#
#   scripts/render-feature-graphic.sh              # into fastlane/…/images/featureGraphic.png
#   scripts/render-feature-graphic.sh /tmp/fg.png  # anywhere else, e.g. to preview
#
# Nothing under fastlane/ is committed without the owner's approval.
set -euo pipefail
cd "$(dirname "$0")/.."
out="${1:-fastlane/metadata/android/en-US/images/featureGraphic.png}"
case "$out" in /*) ;; *) out="$PWD/$out" ;; esac
chrome="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"

"$chrome" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --window-size=1024,500 --screenshot="$out" "file://$PWD/docs/store/feature-graphic.svg" 2> /dev/null

if ! file "$out" | grep -q '1024 x 500, 8-bit/color RGB,'; then
  echo "Unexpected output: $(file "$out")" >&2
  exit 1
fi
echo "Rendered $out"
