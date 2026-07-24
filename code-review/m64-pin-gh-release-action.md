# m64 — Pin softprops/action-gh-release to a commit SHA

- **Severity:** Minor
- **Area:** CI (security)
- **Locations:** `.github/workflows/release-jar.yml:69`

A third-party action runs in a job with contents: write; a compromised/re-pointed v2 tag could exfiltrate GITHUB_TOKEN and tamper with releases. Pin to a full SHA.
