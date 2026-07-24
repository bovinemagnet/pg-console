# m51 — Unbounded inbound X-Correlation-ID; dead duration_ms MDC

- **Severity:** Minor
- **Area:** Logging
- **Locations:** `logging/CorrelationIdFilter.java:125,138-147`

Inbound X-Correlation-ID is accepted verbatim (unbounded length, any chars) into MDC/logs/response header — a log-forging/noise vector; duration_ms is put and immediately cleared (never logged). Length-cap/format-check the ID; remove or use duration_ms.
