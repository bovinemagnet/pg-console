# m63 — ApiResourceIT tests assert anyOf(200,500)

- **Severity:** Minor
- **Area:** Tests
- **Locations:** `src/test/java/.../resource/ApiResourceIT.java:74,88,98`

Three of four tests accept 200 or 500 (or 200/404), so a broken dashboard returning 500 still passes CI; the class is tagged integration but runs on H2. Assert 200 and run against Testcontainers.
