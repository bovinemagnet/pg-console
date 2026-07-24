# m65 — Core SQL layer is untested

- **Severity:** Minor
- **Area:** Test coverage
- **Locations:** `service/PostgresService.java`, `repository/HistoryRepository.java`, `service/DatabaseDiffService.java`, `service/SchemaComparisonService.java`, `service/SchemaExtractorService.java`, `service/StatementsManagementService.java`, `service/SecurityAuditService.java`, `service/DataSourceManager.java`

220 main classes vs 43 test files, clustered on DTOs. The SQL layer — including the sort-column allowlist that is the SQL-injection defence, and the write-performing StatementsManagementService — has no test. Testcontainers infra already exists (WriteEndpointsIntegrationTest, PostgresTestResource); extend it to PostgresService and the diff services. 13/15 resource classes are untested.
