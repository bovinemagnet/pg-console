# M06 — CSV formula injection in all exports

- **Severity:** Major (security)
- **Area:** Exports
- **Locations:** `resource/DashboardResource.java:2652` (`escapeCsv`), `resource/DiagnosticsApiResource.java:590`; fields in `exportActivity` (`:2503-2509`), `exportLocks`, `exportSlowQueries`, `exportTables`

## Problem
`escapeCsv` applies only RFC-4180 quoting; leading `=`, `+`, `-`, `@` are not neutralised. Client-controlled `application_name`/`user`/`query` flow in unescaped.

## Impact
A client connecting with `application_name` = `=HYPERLINK("http://evil","x")` causes formula execution when the CSV is opened in Excel/Sheets.

## Recommended fix
- Prefix any cell beginning with `= + - @` (and tab/CR) with a `'` or space before quoting.

## Acceptance criteria
- [ ] A field starting with `=` is neutralised in exported CSV.
