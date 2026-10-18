# Design provenance

The source HTML was downloaded read-only through the Claude Design connector from [Jewish Calendar App Design](https://claude.ai/design/p/fe15c347-4eef-410e-9639-d561a1ff21b0). It is a reference artifact, not an Android runtime dependency. Its sample dates and times are never used by the app.

- Source: `Jewish Calendar App.dc.html`
- ETag: `1788938696112158`
- Bytes: `118707`
- SHA-256: `576a3e3b8888e2674ec7c76605d90f2b918d36fa1a75d632ca2f4baea3c6a329`

The native implementation adapts the reference's Material 3 colors, spacing, typography and components. Offline font files come from the official [Google Fonts repository](https://github.com/google/fonts), with SIL Open Font License texts included in Android assets:

- `ofl/manrope/Manrope[wght].ttf`: SHA-256 `d0639be45d0af36e798172419d7bd173c4bd4f29e2b76cbb69db1d11bf8b0a40`
- `ofl/frankruhllibre/FrankRuhlLibre[wght].ttf`: SHA-256 `f9bf26966681037aae894b031bd0dcf2c1bfdfa128dd0640cf276fe62a338a43`

Both use explicit native variable-font weight settings. Android supplies a sans-serif Hebrew fallback for body text; Hebrew display text uses Frank Ruhl Libre.
