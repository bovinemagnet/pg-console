# m33 — Generated docs don't escape column/enum values (and markdown pipes)

- **Severity:** Minor
- **Area:** Schema docs
- **Locations:** `service/SchemaDocumentationService.java:438,447-449,464,514`

PK/FK/index column lists and enum labels are emitted without escapeHtml (unlike neighbouring fields); a `<script>` in a monitored DB injects into the generated HTML. Markdown/AsciiDoc never escape `|`. Escape all interpolated values.
