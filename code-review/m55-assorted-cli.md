# m55 — Assorted CLI issues

- **Severity:** Minor
- **Area:** CLI
- **Locations:** `cli/ExportConfigCommand` (showSources no-op), `cli/ValidateConfigCommand.java:400-409`, `cli/PgConsoleCommand.java:77`, `cli/ExportReportCommand` (topN), `logging/LogLevelManager.java:36`

showSources is a documented no-op option; validate-config's monitoring-toggle conflict check only prints in verbose; version hardcoded '1.0.0' separate from quarkus.application.version; topN unvalidated (negative → SQL error); LogLevelManager.scheduler has no @PreDestroy; pervasive System.exit() bypasses graceful shutdown.
