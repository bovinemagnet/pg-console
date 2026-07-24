# m25 — Dead code: retryFailed and NotificationChannel.matchesAlert

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/NotificationDispatcher.java:256-279`, `model/NotificationChannel.java:218-236`

retryFailed has no caller (and would bypass rate limits/silences if wired); matchesAlert duplicates channelMatchesAlert with different null semantics. Remove.
