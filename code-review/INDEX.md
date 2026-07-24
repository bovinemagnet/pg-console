# pg-console code review — findings backlog

Generated 2026-07-07 from a parallel multi-agent review of `src/main`, templates, CLI, migrations, build and the new CI workflows. One file per finding: **C** = Critical, **M** = Major, **m** = Minor / improvement opportunity.

> **Coverage gap:** the deep dive into `PostgresService` internals (~3,588 lines) was cut short by a session limit. Its externally-reachable input paths were reviewed (sort allowlist and prepared statements confirmed clean), but the internal SQL/null-handling was not exhaustively swept. That slice should be re-run — see `m65` for the related test-coverage gap.

## Critical (11)
- [`C01-security-toggle-does-not-gate-dashboards.md`](C01-security-toggle-does-not-gate-dashboards.md) — Enabling security does not actually protect the dashboards
- [`C02-jdbc-url-injection-database-name.md`](C02-jdbc-url-injection-database-name.md) — JDBC URL injection via unvalidated database name (RCE/SSRF/file-write)
- [`C03-explain-analyze-writable-cte.md`](C03-explain-analyze-writable-cte.md) — EXPLAIN ANALYZE executes writable CTEs (arbitrary DML)
- [`C04-runbook-auto-execute-arbitrary-sql.md`](C04-runbook-auto-execute-arbitrary-sql.md) — Runbook auto-execute runs arbitrary SQL and ignores confirmation flags
- [`C05-reset-stats-sql-injection.md`](C05-reset-stats-sql-injection.md) — `reset-stats --database` SQL injection
- [`C06-ssrf-notification-webhooks.md`](C06-ssrf-notification-webhooks.md) — SSRF via notification channel webhooks (with response readback)
- [`C07-schema-diff-swallowed-extraction-failures.md`](C07-schema-diff-swallowed-extraction-failures.md) — Schema extraction failures are swallowed but comparison reports success
- [`C08-pg-get-functiondef-aggregate-error.md`](C08-pg-get-functiondef-aggregate-error.md) — `pg_get_functiondef` on aggregates aborts function extraction
- [`C09-ddl-generator-no-identifier-quoting.md`](C09-ddl-generator-no-identifier-quoting.md) — Generated DDL performs no identifier quoting (broken DDL + injection laundering)
- [`C10-active-alert-unique-constraint-suppresses-incidents.md`](C10-active-alert-unique-constraint-suppresses-incidents.md) — `active_alert.alert_id` UNIQUE constraint suppresses recurring incidents
- [`C11-release-jar-ships-thin-jar.md`](C11-release-jar-ships-thin-jar.md) — Release workflow ships a non-runnable thin JAR (and docs never embedded)

## Major (43)
- [`M01-secrets-echoed-notification-config.md`](M01-secrets-echoed-notification-config.md) — Notification channel secrets echoed to the UI
- [`M02-raw-jdbc-url-logged-unredacted.md`](M02-raw-jdbc-url-logged-unredacted.md) — Raw JDBC URL (with credentials) printed unredacted
- [`M03-exportconfig-masks-name-only.md`](M03-exportconfig-masks-name-only.md) — Config export masks by key name only, leaking credentials in values
- [`M04-log-redaction-email-noop.md`](M04-log-redaction-email-noop.md) — Email PII masking is a no-op
- [`M05-log-redaction-misses-userinfo-url.md`](M05-log-redaction-misses-userinfo-url.md) — Redaction misses userinfo-style connection URLs
- [`M06-csv-formula-injection.md`](M06-csv-formula-injection.md) — CSV formula injection in all exports
- [`M07-json-injection-webhook-payloads.md`](M07-json-injection-webhook-payloads.md) — JSON injection into webhook payloads
- [`M08-reflected-xss-insights-instance.md`](M08-reflected-xss-insights-instance.md) — Reflected XSS via `instance` param on the Insights page
- [`M09-alert-dedup-broken-uuid.md`](M09-alert-dedup-broken-uuid.md) — Alert deduplication can never match (random UUID)
- [`M10-escalations-bypass-silences-ratelimits.md`](M10-escalations-bypass-silences-ratelimits.md) — Escalations bypass silences, maintenance windows, rate limits, and channel filters
- [`M11-testmode-ignored.md`](M11-testmode-ignored.md) — Channel `testMode` is never honoured
- [`M12-escalation-scheduler-overlap.md`](M12-escalation-scheduler-overlap.md) — Escalation scheduler can overlap itself → duplicate notifications
- [`M13-pagerduty-resolve-npe.md`](M13-pagerduty-resolve-npe.md) — PagerDuty resolution NPEs and 500s the request
- [`M14-escalation-policy-repo-no-transaction.md`](M14-escalation-policy-repo-no-transaction.md) — EscalationPolicyRepository multi-statement writes lack a transaction
- [`M15-custom-dashboard-repo-no-transaction.md`](M15-custom-dashboard-repo-no-transaction.md) — CustomDashboardRepository widget writes lack a transaction
- [`M16-stopwatch-double-running-session.md`](M16-stopwatch-double-running-session.md) — Stopwatch "one running session per instance" is not enforced
- [`M17-find-due-for-escalation-null-timestamp.md`](M17-find-due-for-escalation-null-timestamp.md) — Alerts with NULL `last_notification_at` are excluded from escalation
- [`M18-v1-migration-non-idempotent-indexes.md`](M18-v1-migration-non-idempotent-indexes.md) — V1 migration mixes idempotent tables with non-idempotent indexes
- [`M19-sparkline-locale-broken-svg.md`](M19-sparkline-locale-broken-svg.md) — SparklineService produces malformed SVG on comma-decimal locales
- [`M20-aggregated-metrics-rates-always-zero.md`](M20-aggregated-metrics-rates-always-zero.md) — Five aggregated rate metrics are never populated
- [`M21-anomaly-getdouble-wasnull.md`](M21-anomaly-getdouble-wasnull.md) — Anomaly detection reads nullable columns with getDouble (NULL→0)
- [`M22-anomaly-timezone-mismatch.md`](M22-anomaly-timezone-mismatch.md) — Anomaly hour/DOW uses JVM timezone vs DB-timezone baselines
- [`M23-save-anomaly-no-dedup.md`](M23-save-anomaly-no-dedup.md) — saveAnomaly has no dedup/auto-resolve → unbounded growth + alert spam
- [`M24-forecasting-xaxis-bugs.md`](M24-forecasting-xaxis-bugs.md) — Forecasting x-axis gap compression + off-by-one
- [`M25-refresh-insights-synchronous-scans.md`](M25-refresh-insights-synchronous-scans.md) — refreshInsights runs ~192 percentile scans synchronously on the HTTP thread
- [`M26-unknown-instance-500.md`](M26-unknown-instance-500.md) — Unknown `instance` param throws uncaught → 500 reflecting input
- [`M27-windowcomparison-instant-parse-500.md`](M27-windowcomparison-instant-parse-500.md) — Unvalidated `Instant.parse` on window-comparison params → 500
- [`M28-vacuum-reindex-on-http-thread.md`](M28-vacuum-reindex-on-http-thread.md) — VACUUM/REINDEX/CLUSTER run on the HTTP thread with no statement timeout
- [`M29-scheduled-maintenance-identifier-injection.md`](M29-scheduled-maintenance-identifier-injection.md) — Identifier injection in ScheduledMaintenanceService.buildMaintenanceSQL (latent)
- [`M30-statements-management-dead-reset-sql.md`](M30-statements-management-dead-reset-sql.md) — Dead `sql` variable in getSummary calls pg_stat_statements_reset()
- [`M31-report-service-count-reuse.md`](M31-report-service-count-reuse.md) — ReportService loop-counter reuse empties index recommendations
- [`M32-report-service-html-injection.md`](M32-report-service-html-injection.md) — ReportService HTML-injects unescaped values into emailed reports
- [`M33-cli-overrides-applied-after-boot.md`](M33-cli-overrides-applied-after-boot.md) — CLI overrides are applied after Quarkus boots (no-op) → false validate-config
- [`M34-loglevelmanager-getlevel-npe.md`](M34-loglevelmanager-getlevel-npe.md) — Runtime log-level changes silently fail (getLevel NPE swallowed)
- [`M35-exportconfig-yaml-invalid.md`](M35-exportconfig-yaml-invalid.md) — Config export YAML is structurally invalid
- [`M36-exportconfig-env-shell-unsafe.md`](M36-exportconfig-env-shell-unsafe.md) — Config export `env` format is shell-unsafe
- [`M37-schema-diff-fk-cross-join.md`](M37-schema-diff-fk-cross-join.md) — FK extraction cross-joins pg_attribute → duplicated columns
- [`M38-schema-diff-enum-sequence-double-wrap.md`](M38-schema-diff-enum-sequence-double-wrap.md) — Missing sequence/enum DDL is double-wrapped
- [`M39-isdropstatement-misses-alter-drop.md`](M39-isdropstatement-misses-alter-drop.md) — "Exclude DROP statements" still emits ALTER ... DROP COLUMN/CONSTRAINT
- [`M40-ddl-wrong-schema-qualification.md`](M40-ddl-wrong-schema-qualification.md) — Generated index/trigger/FK DDL keeps the source schema qualification
- [`M41-ddl-generated-columns-as-default.md`](M41-ddl-generated-columns-as-default.md) — Generated (STORED) columns rendered as static DEFAULT
- [`M42-ddl-fk-ordering-single-transaction.md`](M42-ddl-fk-ordering-single-transaction.md) — FK constraints embedded in CREATE with no inter-table ordering
- [`M43-schema-diff-service-duplication.md`](M43-schema-diff-service-duplication.md) — DatabaseDiffService and SchemaComparisonService duplicate ~700 lines

## Minor / improvements (70)
- [`m01-retention-deletes-index-mismatch.md`](m01-retention-deletes-index-mismatch.md) — Retention DELETEs seq-scan (filter column ≠ leading index column)
- [`m02-comparison-repos-swallow-sqlexception.md`](m02-comparison-repos-swallow-sqlexception.md) — Comparison repos swallow SQLException (return empty)
- [`m03-nullable-columns-primitive-int.md`](m03-nullable-columns-primitive-int.md) — Nullable columns mapped to primitive int
- [`m04-null-filter-lists-empty-arrays.md`](m04-null-filter-lists-empty-arrays.md) — Null filter lists persisted as empty arrays vs documented 'NULL = all'
- [`m05-dead-schema-and-duplicate-indexes.md`](m05-dead-schema-and-duplicate-indexes.md) — Duplicate/non-unique indexes and dead columns
- [`m06-area-sparkline-missing-null-filter.md`](m06-area-sparkline-missing-null-filter.md) — generateAreaSparkline lacks the null-element filter
- [`m07-cache-history-series-mislabelled.md`](m07-cache-history-series-mislabelled.md) — Persisted cache history duplicates buffer ratio / mislabels active connections
- [`m08-window-null-zero-and-hour-snapping.md`](m08-window-null-zero-and-hour-snapping.md) — Window comparison coerces null→0 and snaps boundaries to whole hours
- [`m09-metricname-sql-interpolation.md`](m09-metricname-sql-interpolation.md) — metricName interpolated into SQL (latent SQLi)
- [`m10-deque-size-on-add.md`](m10-deque-size-on-add.md) — O(n) ConcurrentLinkedDeque.size() per insert
- [`m11-fingerprint-doubled-quote.md`](m11-fingerprint-doubled-quote.md) — Query fingerprint mishandles SQL doubled quotes
- [`m12-demo-storage-thresholds-hardcoded.md`](m12-demo-storage-thresholds-hardcoded.md) — Hard-coded 100/120GB storage thresholds feed the health score
- [`m13-sampler-duplication.md`](m13-sampler-duplication.md) — MetricsSamplerService and InMemoryMetricsSampler duplicate ~200 lines
- [`m14-no-caching-insights-pipelines.md`](m14-no-caching-insights-pipelines.md) — Insights/Unified/QueryRegression recompute the full pipeline per call
- [`m15-webhook-url-logged.md`](m15-webhook-url-logged.md) — Webhook URL (with secret token) written to logs
- [`m16-repeatcount-dead.md`](m16-repeatcount-dead.md) — repeatCount is dead / would loop forever
- [`m17-ratelimittracker-dead-unsafe.md`](m17-ratelimittracker-dead-unsafe.md) — rateLimitTracker is dead and thread-unsafe
- [`m18-sendermap-unsafe-lazy-init.md`](m18-sendermap-unsafe-lazy-init.md) — Unsafe lazy init of senderMap
- [`m19-alerting-cooldown-before-send.md`](m19-alerting-cooldown-before-send.md) — Cooldown recorded before send + check-then-put race
- [`m20-alertsilence-npe-redos.md`](m20-alertsilence-npe-redos.md) — AlertSilence: null-endTime NPE, REGEX ReDoS, matches() side effect
- [`m21-resolution-ignores-filters.md`](m21-resolution-ignores-filters.md) — Resolution notices ignore channel filters
- [`m22-channel-health-fiction.md`](m22-channel-health-fiction.md) — Channel success/failure counts are never incremented
- [`m23-email-crlf-and-dead-config.md`](m23-email-crlf-and-dead-config.md) — Email subject CRLF-injectable; several email config flags dead
- [`m24-teams-dead-config-workflows-url.md`](m24-teams-dead-config-workflows-url.md) — Teams: dead config + rejects new Workflows URLs
- [`m25-dead-code-retryfailed-matchesalert.md`](m25-dead-code-retryfailed-matchesalert.md) — Dead code: retryFailed and NotificationChannel.matchesAlert
- [`m26-featuretoggle-fail-open.md`](m26-featuretoggle-fail-open.md) — FeatureToggleService is fail-open and ~900 lines of boilerplate
- [`m27-schema-diff-null-message-success.md`](m27-schema-diff-null-message-success.md) — Null exception message leaves comparison marked successful
- [`m28-constraints-compared-by-presence.md`](m28-constraints-compared-by-presence.md) — Unique/check constraints compared by presence only
- [`m29-not-null-like-wildcard.md`](m29-not-null-like-wildcard.md) — `conname NOT LIKE '%_not_null'` also excludes legit names
- [`m30-filter-not-applied-to-all-object-kinds.md`](m30-filter-not-applied-to-all-object-kinds.md) — Include/exclude filter only applied to tables and views
- [`m31-comment-on-is-null-literal.md`](m31-comment-on-is-null-literal.md) — Removed comment emits COMMENT ON ... IS 'null'
- [`m32-savepoint-without-rollback.md`](m32-savepoint-without-rollback.md) — SAVEPOINT_PER_OBJECT emits SAVEPOINT but never ROLLBACK/RELEASE
- [`m33-unescaped-doc-output.md`](m33-unescaped-doc-output.md) — Generated docs don't escape column/enum values (and markdown pipes)
- [`m34-alter-column-type-no-using.md`](m34-alter-column-type-no-using.md) — ALTER COLUMN ... TYPE generated without USING
- [`m35-dead-compared-counters.md`](m35-dead-compared-counters.md) — Dead *Compared counters and contradictory hasBreakingChanges
- [`m36-index-expression-columns-skipped.md`](m36-index-expression-columns-skipped.md) — Index column aggregation skips expression columns
- [`m37-modified-view-and-enum-edge-cases.md`](m37-modified-view-and-enum-edge-cases.md) — MODIFIED view/matview and ALTER TYPE ADD VALUE edge cases
- [`m38-slow-queries-negative-limit.md`](m38-slow-queries-negative-limit.md) — Negative limit crashes /api/slow-queries
- [`m39-sparkline-numberformat.md`](m39-sparkline-numberformat.md) — /api/sparkline 500s on non-numeric input
- [`m40-insights-ask-null-query.md`](m40-insights-ask-null-query.md) — /insights/ask NPEs on missing query
- [`m41-runbook-swallows-exceptions.md`](m41-runbook-swallows-exceptions.md) — RunbookService swallows exceptions and returns null/empty
- [`m42-runbook-conn-per-table.md`](m42-runbook-conn-per-table.md) — RunbookService opens a fresh connection per table in the SQL_TEMPLATE loop
- [`m43-customdashboard-regex-validation.md`](m43-customdashboard-regex-validation.md) — Custom-SQL validation is denylist regex (currently inert)
- [`m44-querydetail-error-innerhtml.md`](m44-querydetail-error-innerhtml.md) — queryDetail.html puts error.message into innerHTML
- [`m45-unescaped-option-catalog-names.md`](m45-unescaped-option-catalog-names.md) — Unescaped catalog names in <option> builders
- [`m46-run-profile-state-changing-get.md`](m46-run-profile-state-changing-get.md) — GET /run-profile/{id} is state-changing (CSRF-prone)
- [`m47-notification-null-body-500.md`](m47-notification-null-body-500.md) — Notification endpoints 500 on null/partial JSON body
- [`m48-exportreport-unescaped-json.md`](m48-exportreport-unescaped-json.md) — ExportReportCommand unescaped JSON/markdown interpolation
- [`m49-printwriter-not-twr.md`](m49-printwriter-not-twr.md) — PrintWriter not try-with-resources; IOExceptions swallowed
- [`m50-filenames-dup-condition.md`](m50-filenames-dup-condition.md) — Filenames.java has a duplicate dead condition
- [`m51-correlationid-unbounded-inbound.md`](m51-correlationid-unbounded-inbound.md) — Unbounded inbound X-Correlation-ID; dead duration_ms MDC
- [`m52-structuredlogger-mdc-overwrite.md`](m52-structuredlogger-mdc-overwrite.md) — JSON-mode MDC keys overwrite request context
- [`m53-dead-logging-config.md`](m53-dead-logging-config.md) — pgconsole-logging.file.*/async.* config is dead
- [`m54-broad-default-exposure.md`](m54-broad-default-exposure.md) — Broad-by-default exposure (0.0.0.0 + Swagger in prod)
- [`m55-assorted-cli.md`](m55-assorted-cli.md) — Assorted CLI issues
- [`m56-workflow-dispatch-invalid-release.md`](m56-workflow-dispatch-invalid-release.md) — workflow_dispatch cannot produce a valid release
- [`m57-vprefix-branch-treated-as-release.md`](m57-vprefix-branch-treated-as-release.md) — Any branch starting with 'v' is treated as a release version
- [`m58-openhtmltopdf-unmaintained.md`](m58-openhtmltopdf-unmaintained.md) — openhtmltopdf 1.0.10 is unmaintained (pins old PDFBox)
- [`m59-double-gradle-caching.md`](m59-double-gradle-caching.md) — Double Gradle caching in CI
- [`m60-docker-compose-exposure.md`](m60-docker-compose-exposure.md) — docker-compose publishes Postgres on 0.0.0.0 + obsolete version key
- [`m61-e2e-needs-external-app.md`](m61-e2e-needs-external-app.md) — E2E suite depends on an externally running app (flaky)
- [`m62-test-endpoints-script.md`](m62-test-endpoints-script.md) — test-endpoints.sh false-positives on the word 'Exception'; CLAUDE.md typo
- [`m63-apiresourceit-weak-asserts.md`](m63-apiresourceit-weak-asserts.md) — ApiResourceIT tests assert anyOf(200,500)
- [`m64-pin-gh-release-action.md`](m64-pin-gh-release-action.md) — Pin softprops/action-gh-release to a commit SHA
- [`m65-sql-layer-untested.md`](m65-sql-layer-untested.md) — Core SQL layer is untested
- [`m66-two-parallel-alerting-stacks.md`](m66-two-parallel-alerting-stacks.md) — Two parallel alerting stacks
- [`m67-notification-sender-duplication.md`](m67-notification-sender-duplication.md) — The four webhook senders duplicate identical scaffolding
- [`m68-endpoint-common-data-duplication.md`](m68-endpoint-common-data-duplication.md) — ~50 endpoints copy-paste the common template .data(...) block
- [`m69-repository-jdbc-boilerplate.md`](m69-repository-jdbc-boilerplate.md) — ~4,300 lines of repeated JDBC boilerplate across repositories
- [`m70-dead-locals-dashboardresource.md`](m70-dead-locals-dashboardresource.md) — Assigned-but-unused locals in DashboardResource

## Suggested fix order

1. **C01** (auth catch-all) first — it gates the exploitability of C02–C08.
2. Injection quick wins: **C02** (JDBC URL allowlist), **C03** (EXPLAIN read-only), **C05** (reset-stats bind param) — small, testable.
3. Data-loss in the diff tool: **C07/C08/C09** together (a partial extraction must not generate DROPs).
4. **C10** (alert unique constraint) and **C11** (release ships a broken jar) before the next tag.
5. Then Majors by theme; Minors as capacity allows.
