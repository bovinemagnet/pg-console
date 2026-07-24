# M32 — ReportService HTML-injects unescaped values into emailed reports

- **Severity:** Major (security)
- **Area:** Reports
- **Locations:** `service/ReportService.java:135` (`instanceId`), `:146` (`version`), `:186-206` (rec fields, severity)

## Problem
Several interpolated values (request-influenced `instanceId`, monitored-DB `version`, table names, recommendation text/severity) are appended without `escapeHtml`.

## Impact
Values containing `<`/`>` render as markup in the recipient's mail client.

## Recommended fix
- Wrap all interpolated values in `escapeHtml`.

## Acceptance criteria
- [ ] A `<script>`-containing field renders inert in the emailed report.
