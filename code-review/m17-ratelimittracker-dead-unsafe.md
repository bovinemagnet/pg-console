# m17 — rateLimitTracker is dead and thread-unsafe

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/NotificationDispatcher.java:59,359-365,349-357`

Written but never read (isRateLimited uses DB counts); unsynchronised ArrayLists mutated from concurrent dispatches → latent CME. Delete it.
