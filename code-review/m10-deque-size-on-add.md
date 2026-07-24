# m10 — O(n) ConcurrentLinkedDeque.size() per insert

- **Severity:** Minor
- **Area:** Metrics
- **Locations:** `service/InMemoryMetricsStore.java:322-328,86,164,221`, `service/LiveChartHistoryStore.java:137`

size() is O(n) and evaluated on every add (including eagerly in debug log args) near the 10k cap. Track size with an AtomicInteger or guard the debug logging.
