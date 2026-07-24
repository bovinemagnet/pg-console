# m32 — SAVEPOINT_PER_OBJECT emits SAVEPOINT but never ROLLBACK/RELEASE

- **Severity:** Minor
- **Area:** Migration script
- **Locations:** `model/MigrationScript.java:191-201`

On a statement error the transaction still fully aborts, so the option's promised partial rollback does not exist. Emit ROLLBACK TO/RELEASE around each object, or remove the option.
