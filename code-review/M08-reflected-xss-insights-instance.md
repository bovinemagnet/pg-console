# M08 — Reflected XSS via `instance` param on the Insights page

- **Severity:** Major (security)
- **Area:** Templates / NL query
- **Locations:** `templates/insights.html:387` (innerHTML with `response.redirectUrl`); `model/NaturalLanguageQuery.java:299` (`buildUrl` concatenates `instance` with no encoding); `resource/InsightsResource.java:306`

## Problem
`redirectUrl` is inserted unescaped into `innerHTML`, and `buildUrl` appends the user-controlled `instance` query param with no URL-encoding.

## Impact
`/insights?instance="><img src=x onerror=alert(document.cookie)>` executes injected markup after submitting any recognised query.

## Recommended fix
- URL-encode `instance` (and all params) in `buildUrl`, and/or build the anchor via `document.createElement` + `textContent`/`setAttribute` instead of innerHTML string concatenation.

## Acceptance criteria
- [ ] The payload above renders inert.
