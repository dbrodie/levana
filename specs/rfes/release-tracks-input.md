# Release workflow: `track` → `tracks` (levana-69j)

## Problem

The Google Play upload step in `.github/workflows/release.yml` uses
`r0adkll/upload-google-play@v1` with `track: internal`. The action logs a
deprecation warning: `track` will be removed in favour of `tracks`.

## Change

Replace `track: internal` with `tracks: internal`. `tracks` is a
comma-separated list; a single value uploads to the internal track only.

Setting both inputs is an error, and setting neither falls back to the
`production` track, so `track` must be replaced (not supplemented) and
`tracks` must not be dropped.

## Verification

- Action source (`src/main.ts`, `src/input-validation.ts` at `v1`):
  `tracks: internal` → `['internal']`, no deprecation warning.
- Workflow YAML parses; the upload step's inputs are
  `tracks: internal`, `status: completed`, unchanged otherwise.
- Not exercised end to end: the workflow only runs on `v*.*.*` tag pushes
  and publishes a release, so it is confirmed on the next real release.
