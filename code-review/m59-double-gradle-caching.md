# m59 — Double Gradle caching in CI

- **Severity:** Minor
- **Area:** CI
- **Locations:** `.github/workflows/ci.yml:31,37`

setup-java (cache: gradle) and gradle/actions/setup-gradle both cache Gradle and fight over keys. Drop cache: gradle from setup-java.
