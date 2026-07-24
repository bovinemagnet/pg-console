# m53 — pgconsole-logging.file.*/async.* config is dead

- **Severity:** Minor
- **Area:** Config
- **Locations:** `config/LoggingConfig.java`; `resource/LoggingResource.java:61-64`

file.* (max-size/backup/file-name/error-log-*) and async.* are only echoed by /api/logging/config; setting file.enabled=true writes no file (only quarkus.log.file.* does). ~40% of LoggingConfig is unused. Wire them to quarkus.log.* or remove.
