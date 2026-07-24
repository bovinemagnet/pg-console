# m48 — ExportReportCommand unescaped JSON/markdown interpolation

- **Severity:** Minor
- **Area:** CLI
- **Locations:** `cli/ExportReportCommand.java:348,650,707-708,732`

--instance, current_database(), current_user, activity state emitted without escapeJson (which exists, used only for version); markdown tables broken by `|`/newlines. Escape all interpolated values.
