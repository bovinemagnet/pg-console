# M04 — Email PII masking is a no-op

- **Severity:** Major (security)
- **Area:** Logging
- **Locations:** `logging/LogRedactionService.java:226`

## Problem
`"[email]@$0".split("@")[1]` evaluates to the literal `"$0"`, so `replaceAll(...)` replaces each email with itself.

## Impact
With `redact.mask-pii=true`, emails pass through logs verbatim while operators believe they are masked.

## Recommended fix
- Use a proper replacement (`$0`/capture-group referencing) or reconstruct the masked value correctly; add a unit test.

## Acceptance criteria
- [ ] A log line with an email is actually masked when mask-pii is on.
