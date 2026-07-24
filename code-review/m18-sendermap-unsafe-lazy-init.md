# m18 — Unsafe lazy init of senderMap

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/NotificationDispatcher.java:296-311`

Non-volatile field assigned before population inside the synchronised method while readers check it unsynchronised → a racing thread can see a partial map ('No sender configured'). Initialise in @PostConstruct.
