# m27 — Null exception message leaves comparison marked successful

- **Severity:** Minor
- **Area:** Schema comparison
- **Locations:** `service/SchemaComparisonService.java:100-102`, `service/DatabaseDiffService.java:117-120`, `model/SchemaComparisonResult.java:893-895`

setErrorMessage(null) on an NPE leaves isSuccess() true (setSuccess(false) is a no-op by design). A crashed comparison renders as a successful empty result. Set an explicit failure flag.
