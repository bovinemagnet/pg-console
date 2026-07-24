# m68 — ~50 endpoints copy-paste the common template .data(...) block

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/DashboardResource.java` (throughout); `resource/DiagnosticsResource.java:149` vs `resource/DiagnosticsApiResource.java:84`

The instances/currentInstance/securityEnabled/schemaEnabled/inMemoryMinutes/toggles block is duplicated across ~50 endpoints; getDefaultInstance()/getToggles() duplicated verbatim across resources. Extract a withCommon(TemplateInstance, instance) helper / shared base.
