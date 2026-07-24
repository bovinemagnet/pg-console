# m52 — JSON-mode MDC keys overwrite request context

- **Severity:** Minor
- **Area:** Logging
- **Locations:** `logging/StructuredLogger.java:314-341`

Metadata keys named user/instance/correlationId overwrite the filter's MDC values and aren't removed, so wrong values persist for the rest of the request. Namespace metadata keys or restore prior values.
