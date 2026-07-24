# m47 — Notification endpoints 500 on null/partial JSON body

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/NotificationResource.java:320,253,362`

fireAlert/createChannel/createSilence dereference body fields without validation → 500 instead of 400. Validate and return 400.
