# m40 — /insights/ask NPEs on missing query

- **Severity:** Minor
- **Area:** Web
- **Locations:** `resource/InsightsResource.java:305`

@FormParam('query') passed straight to parseNaturalLanguageQuery with no null/blank check (unlike the recommendations endpoint). Guard and return 400.
