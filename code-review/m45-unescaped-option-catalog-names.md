# m45 — Unescaped catalog names in <option> builders

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/DatabaseDiffResource.java:128,181`, `resource/SchemaDocResource.java:96,121`, `resource/SchemaComparisonResource.java:111`

Database/schema names are inserted into `<option>` without HTML-escaping (contrast HtmlText.escape in CustomDashboardResource). Low-risk stored XSS if an object is named with HTML metacharacters. Escape them.
