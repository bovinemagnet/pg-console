# m06 — generateAreaSparkline lacks the null-element filter

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/SparklineService.java:134-139`

`generateSparkline` (line 83) filters null history points; `generateAreaSparkline` does not and NPEs at mapToDouble. Add the same guard.
