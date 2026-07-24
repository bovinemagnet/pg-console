# m58 — openhtmltopdf 1.0.10 is unmaintained (pins old PDFBox)

- **Severity:** Minor
- **Area:** Dependencies
- **Locations:** `gradle.properties:22`, `build.gradle:34-35`

openhtmltopdf 1.0.10 (archived 2021) pins PDFBox 2.0.24, which has since had security fixes. Move to the maintained fork io.github.openhtmltopdf 1.1.x (drop-in).
