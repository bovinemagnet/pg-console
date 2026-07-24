# m14 — Insights/Unified/QueryRegression recompute the full pipeline per call

- **Severity:** Minor
- **Area:** Performance
- **Locations:** `service/UnifiedRecommendationService.java:48-156`, `service/InsightsService.java:45-86`, `service/QueryRegressionService.java:236-238`

getSummary/getTopRecommendations/bySource/bySeverity each re-run index advisor + maintenance + 2×2 history scans + anomalies + config checks; one page load runs it ≥twice. Cache per request/short TTL.
