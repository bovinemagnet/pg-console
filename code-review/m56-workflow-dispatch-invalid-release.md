# m56 — workflow_dispatch cannot produce a valid release

- **Severity:** Minor
- **Area:** CI
- **Locations:** `.github/workflows/release-jar.yml:7`

Dispatched from a branch, GITHUB_REF_NAME is the branch name → version 0.0.0-SNAPSHOT and action-gh-release fails on a non-tag ref. Remove the trigger or add a version input wired to -PreleaseVersion.
