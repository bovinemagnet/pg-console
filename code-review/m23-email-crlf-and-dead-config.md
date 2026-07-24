# m23 — Email subject CRLF-injectable; several email config flags dead

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `service/notification/EmailNotificationSender.java:114-128`

Subject built from unescaped alert fields (header-injection risk depending on mailer); digestMode/digestIntervalMinutes/useHtmlTemplate/attachReport are never read; the class builds an unused HttpClient. Strip CR/LF from subject; remove or implement the flags.
