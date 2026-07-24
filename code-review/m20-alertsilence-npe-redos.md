# m20 — AlertSilence: null-endTime NPE, REGEX ReDoS, matches() side effect

- **Severity:** Minor
- **Area:** Notifications
- **Locations:** `model/AlertSilence.java:93-96,326,320`

isActive() NPEs when endTime is null (no-arg ctor leaves it null); REGEX matcher runs a user pattern via String.matches (ReDoS + full-match surprise); matches() mutates this.value. Null-guard, bound the regex, remove the mutation.
