# m41 — RunbookService swallows exceptions and returns null/empty

- **Severity:** Minor
- **Area:** Runbooks
- **Locations:** `service/RunbookService.java:863-865,893-895`

getRunbook/getExecution/saveExecution/updateExecution catch Exception and only log; a failed saveExecution leaves id=0 and the caller proceeds as if persisted (silent no-op). Propagate or surface the failure.
