# M01 — Notification channel secrets echoed to the UI

- **Severity:** Major (security)
- **Area:** Notifications
- **Locations:** `resource/NotificationResource.java:233-247`; `model/NotificationChannel.java:124`

## Problem
`GET /api/channels` and `/api/channels/{id}` serialise the full `NotificationChannel`, including Slack/Discord/Teams webhook URLs, PagerDuty routing keys, and SMTP credentials, unmasked.

## Impact
Anyone who can view the channels page (anonymous by default — see C01) can exfiltrate every webhook credential.

## Recommended fix
- Mask secret fields in API responses (return presence/last-4 only); never return full webhook URLs or routing keys.

## Acceptance criteria
- [ ] Channel API responses do not contain full secrets.
