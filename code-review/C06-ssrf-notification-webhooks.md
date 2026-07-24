# C06 — SSRF via notification channel webhooks (with response readback)

- **Severity:** Critical (security)
- **Area:** Notifications
- **Locations:** `resource/NotificationResource.java:264-276` (`updateChannel`, no validation), sender validators `service/notification/DiscordNotificationSender.java:93`, `TeamsNotificationSender.java:87` (substring checks); `service/notification/AbstractNotificationSender.java:65-70,148-156` (returns response body); `service/AlertingService.java:224-231`

## Problem
`updateChannel` never calls `dispatcher.validateChannelConfig`, so any URL is accepted. Create-time validation uses substring checks (`contains("discord.com/api/webhooks/")`), bypassable by `http://169.254.169.254/latest/?x=discord.com/api/webhooks/`. The `test` endpoint POSTs server-side and returns the response code plus the first 1000 chars of the body to the caller.

## Impact
Internal network probing with response readback (cloud metadata endpoints, internal services). `AlertingService.sendWebhook` posts to a completely unvalidated URL.

## Recommended fix
- Validate URLs on both create and update: require https, parse the host, and block private/link-local/loopback ranges (and DNS-rebinding-safe resolution if feasible).
- Do not return remote response bodies to the caller; return only a boolean/opaque status.

## Acceptance criteria
- [ ] Update path validates the URL identically to create.
- [ ] URLs resolving to private/link-local addresses are rejected.
- [ ] The test endpoint no longer echoes the remote body.
