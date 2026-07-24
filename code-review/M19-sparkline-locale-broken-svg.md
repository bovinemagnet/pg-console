# M19 — SparklineService produces malformed SVG on comma-decimal locales

- **Severity:** Major (correctness)
- **Area:** Sparklines
- **Locations:** `service/SparklineService.java:102-118,156-172`

## Problem
`String.format("%.2f", …)` is used for SVG path/attribute values without `Locale.ROOT`.

## Impact
On a JVM whose default locale uses a comma decimal (de, fr, …), paths become `M 0,00 L 3,50 …` and `cx="3,50"` — malformed SVG, blank/garbled sparklines on every dashboard.

## Recommended fix
- Use `String.format(Locale.ROOT, ...)` (or `Locale.US`) for all numeric SVG formatting.

## Acceptance criteria
- [ ] Sparklines render correctly with the JVM default locale set to `de-DE`.
