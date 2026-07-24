# M33 — CLI overrides are applied after Quarkus boots (no-op) → false validate-config

- **Severity:** Major (correctness)
- **Area:** CLI
- **Locations:** `cli/PgConsoleCommand.java:229-250`, `cli/ValidateConfigCommand.java:160`

## Problem
With quarkus-picocli and no custom `@QuarkusMain`, the runtime (HTTP server, config) is fully initialised before `run()` executes, so option overrides do nothing.

## Impact
`--port 9090` still binds the configured port; `validate-config -c other.properties` prints "Validating: other.properties" but validates the *default* config — false PASS/FAIL in CI.

## Recommended fix
- Apply overrides via system properties/`@QuarkusMain` before boot, or document that these options are informational; make validate-config load the named file explicitly.

## Acceptance criteria
- [ ] `--port` changes the bound port; validate-config validates the file it names.
