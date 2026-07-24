# m15 — Webhook URL (with secret token) written to logs

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/AlertingService.java:234,241,247`

The full webhook URL is logged at debug/warn/error. Redact before logging.
