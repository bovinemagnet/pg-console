# m70 — Assigned-but-unused locals in DashboardResource

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/DashboardResource.java:628,1298` and others

stats/vacuumProgress/bgProcessStats locals in several drilldown/page methods are assigned but never used. Remove.
