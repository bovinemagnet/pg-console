# m49 — PrintWriter not try-with-resources; IOExceptions swallowed

- **Severity:** Minor
- **Area:** CLI
- **Locations:** `cli/ExportReportCommand.java:162`, `cli/ExportConfigCommand.java:130`, `cli/GenerateCompletionCommand.java:108`

On mid-write exception the file is left partial/unflushed; PrintWriter swallows IOException so 'Report written to: ...' can print after a failed write. Use try-with-resources and checkError().
