# m44 — queryDetail.html puts error.message into innerHTML

- **Severity:** Minor
- **Area:** Templates
- **Locations:** `templates/queryDetail.html:265`

Client-side error text (not attacker-influenced) is concatenated into innerHTML. Not exploitable, but tighten to textContent for consistency.
