# m21 — Resolution notices ignore channel filters

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/NotificationDispatcher.java:155-180`

Resolutions go to all enabled channels, including ones whose severity/type/instance filters excluded the original alert. Apply the same filter.
