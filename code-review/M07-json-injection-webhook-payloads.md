# M07 — JSON injection into webhook payloads

- **Severity:** Major (security)
- **Area:** Notifications
- **Locations:** `service/notification/SlackNotificationSender.java:122,161`, `DiscordNotificationSender.java:119,124,159,182`, `TeamsNotificationSender.java:118`; input via `resource/NotificationResource.java:316-321`

## Problem
Attacker-supplied `severity` (and Discord role IDs) are concatenated raw inside hand-built JSON strings. `escapeJson` also misses control characters other than `\n\r\t`.

## Impact
A severity value like `HIGH","channel":"#general","text":"...` rewrites the payload structure (e.g. re-routes the Slack message).

## Recommended fix
- Build payloads with Jackson (already on the classpath) instead of `StringBuilder`; this fixes injection and the control-char bug together.

## Acceptance criteria
- [ ] Payloads are serialised by a JSON library; a `"` in any field cannot break structure.
