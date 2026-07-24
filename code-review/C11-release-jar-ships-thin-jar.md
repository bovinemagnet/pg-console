# C11 — Release workflow ships a non-runnable thin JAR (and docs never embedded)

- **Severity:** Critical (release pipeline)
- **Area:** CI / release
- **Locations:** `.github/workflows/release-jar.yml:49-53`, `:43`; `build.gradle:225-227,268-296`

## Problem
The "Locate Quarkus uber-jar" step does `find build -name "*.jar" | sort | head -n 1`, which selects the plain `build/libs/pg-console-<v>.jar` (sorts before the runner/release jars). Separately, `includeDocs` checks command-line task names, but the workflow invokes `prepareReleaseArtifact` (which only `dependsOn buildWithDocs`), so `processResources` never gains the antora output.

## Impact
Every tagged release publishes a thin jar; `java -jar pg-console-<v>.jar` fails with "no main manifest attribute". Embedded docs `/docs/index.html` 404 in every release.

## Recommended fix
- Upload `build/release/pg-console.jar` (already produced correctly by `prepareReleaseArtifact`); delete the "Locate" step.
- Make doc embedding a property (`-PincludeDocs`) that `prepareReleaseArtifact` sets, not a start-parameter task-name check.

## Acceptance criteria
- [ ] A tagged release produces a runnable uber-jar.
- [ ] `/docs/` is served from the released jar.
