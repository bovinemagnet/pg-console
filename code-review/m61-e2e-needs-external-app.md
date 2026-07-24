# m61 — E2E suite depends on an externally running app (flaky)

- **Severity:** Minor
- **Area:** Tests
- **Locations:** `src/test/java/.../PlaywrightTestBase.java:113`, `build.gradle:121-134`

Defaults to http://localhost:8080; the e2eTest task does not start the app or wire @QuarkusTest, so tests fail with connection-refused unless dev mode is up (and then collide with its DB state). Not run in CI. Wire a managed app or document it as a manual smoke suite.
