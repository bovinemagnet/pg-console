# M14 — EscalationPolicyRepository multi-statement writes lack a transaction

- **Severity:** Major (data integrity)
- **Area:** Persistence
- **Locations:** `repository/EscalationPolicyRepository.java:126-157` (`save`), `:165-197` (`update`), `:242` (`saveTier`), `:269` (`deleteTiersByPolicyId`)

## Problem
`update` performs a policy UPDATE, a tier DELETE, and one INSERT per tier across separate autocommit connections — no transaction. `save` has the same shape.

## Impact
A crash/DB error after the tier delete leaves the policy with zero tiers → escalation silently never fires for that policy.

## Recommended fix
- Wrap the whole operation in a single connection/transaction with commit/rollback.

## Acceptance criteria
- [ ] A failure mid-update rolls back to the prior tier set.
