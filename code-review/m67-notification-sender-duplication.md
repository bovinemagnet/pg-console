# m67 — The four webhook senders duplicate identical scaffolding

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/{Slack,Discord,Teams,PagerDuty}NotificationSender.java`, `AbstractNotificationSender.java`

All repeat null-config guard → build payload → POST → status check → log → result. Hoist a doSend(channel, alert, url, payload, acceptedStatuses) template method into AbstractNotificationSender; only payload building differs. Using Jackson for payloads also fixes M07.
