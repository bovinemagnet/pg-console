# m22 — Channel success/failure counts are never incremented

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `model/NotificationChannel.java:247-271`

getSuccessRate()/getHealthCssClass() always show 100%/green because no repository writes the counters. Either wire them up or remove the health UI.
