# m24 — Teams: dead config + rejects new Workflows URLs

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/TeamsNotificationSender.java:87,91`

themeColour computed but unused; mentionUsers/mentionUserIds dead; javadoc promises action buttons that aren't implemented; new `*.logic.azure.com` Workflows URLs are rejected by validateConfig. Update validation; remove dead config.
