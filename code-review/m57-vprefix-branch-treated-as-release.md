# m57 — Any branch starting with 'v' is treated as a release version

- **Severity:** Minor
- **Area:** Build
- **Locations:** `build.gradle:72`

`githubRefName?.startsWith('v')` matches branches like `validate-fix` → version `alidate-fix`. Check GITHUB_REF starts with refs/tags/v.
