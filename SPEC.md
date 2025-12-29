Product Specification: Postgres Insight Dashboard

## Implementation Status

### Phase 0 — Foundations ✅ COMPLETE
- [x] Quarkus Gradle project
- [x] Qute layout template with nav
- [x] Static assets setup (htmx + CSS framework)
- [x] Dark mode toggle working (persisted in localStorage)
- [x] DB connectivity config for one instance
- [x] Basic "About" page showing app + DB version

### Phase 1 — MVP Monitoring Dashboards ✅ COMPLETE
- [x] Overview page (live widgets: connections, active/blocked queries, cache hit ratio, DB size, top tables/indexes)
- [x] Activity page (active sessions table, auto-refresh)
- [x] Locks/Blocking page (blocking tree, lock list, idle-in-transaction highlighting)
- [x] Slow Queries page using pg_stat_statements
  - [x] Sortable table (by calls, totalTime, meanTime, maxTime, rows)
  - [x] Hover full query (CSS tooltip)
  - [x] Query detail page with copy button and full statistics
- [x] Auto-refresh dropdown (Off/5s/10s/30s/60s with localStorage persistence)
- [x] Tables page with bloat indicators
- [x] Databases page (per-database metrics from pg_stat_database)
  - [x] Comparison view showing all databases
  - [x] Detailed view for individual database metrics
  - [x] All pg_stat_database columns (transactions, cache, tuples, sessions, I/O, temp files, deadlocks)

### Phase 2 — History Sampling + SVG Sparklines ✅ COMPLETE
- [x] Dashboard schema + migrations (Flyway with pgconsole schema)
- [x] Scheduled sampler job (configurable interval, default 60s)
- [x] Trend panels with sparklines (Overview dashboard and Query Detail page)
- [x] Retention cleanup job (daily at 3 AM, configurable retention days)

### Phase 3 — Polishing + Safety + Multi-Instance ✅ COMPLETE
- [x] Multi-instance selector (full implementation with configurable named datasources)
- [x] Authentication integration (Quarkus Elytron Security with HTTP Basic auth, configurable enable/disable)
- [x] Authorization for dangerous actions (admin role required for cancel/terminate)
- [x] Cancel/Terminate query buttons (with confirmation modals)
- [x] Export: download CSV (slow queries export with timestamp)

### Phase 4 — Advanced Diagnostics ✅ COMPLETE
- [x] Wait-event breakdown summary (Wait Events page with type summaries and detailed table)
- [x] Query fingerprint grouping (normalisation service, grouped view on Slow Queries page)
- [x] Explain Plan integration (EXPLAIN/ANALYZE/BUFFERS on Query Detail page)
- [x] Incident report snapshot export (text-based report with full state capture)
- [x] Alerting integration (webhook support, configurable thresholds, cooldown)

### Phase 5 — Query & Index Optimisation (Complete)
- [x] **Index Advisor** - Recommendations based on sequential scan patterns and query analysis
  - Identify tables with high sequential scan ratios
  - Detect duplicate/redundant indexes
  - Highlight unused indexes (with safety warnings about recent usage)
  - Summary cards showing missing indexes, unused indexes, and wasted space
- [x] **Query Regression Detection** - Compare query performance across time windows
  - Flag queries with significant mean_time increases
  - Show before/after comparisons with percentage changes
  - Configurable regression threshold and time window (6h/12h/24h/48h)
  - Severity levels (Critical >100%, High >50%, Medium >25%)
  - Also detects query improvements (faster queries)
- [x] **Table Maintenance Recommendations**
  - Vacuum/Analyse recommendations based on dead tuple ratios
  - Bloat estimation with pg_stat_user_tables data
  - Last vacuum/analyse timestamps with "overdue" warnings
  - Support for VACUUM, ANALYSE, and VACUUM FULL recommendations
- [x] **pg_stat_statements Management**
  - Query baseline comparisons (current period vs previous period)
  - Top movers report (queries with biggest delta in execution time)
  - New, removed, increased, and decreased query detection
  - Configurable comparison windows (6h/12h/24h/48h)

### Phase 6 — Replication & Infrastructure Monitoring (Complete)
- [x] **Replication Dashboard**
  - Streaming replication lag monitoring (pg_stat_replication)
  - Replication slot status and retained WAL size
  - Logical replication subscriber status
  - WAL configuration and statistics
- [x] **Vacuum Progress Monitoring**
  - Active vacuum operations (pg_stat_progress_vacuum)
  - Estimated completion percentage with progress bars
  - Phase tracking (heap scan, index vacuum, cleanup, etc.)
  - Autovacuum worker identification
- [x] **Background Process Monitoring**
  - Autovacuum launcher/worker status and counts
  - Background writer statistics (buffers cleaned)
  - Checkpointer activity and timing (timed vs requested)
  - Buffer allocation statistics with warnings
- [x] **Storage Insights**
  - Tablespace usage and sizes
  - Database size breakdown
  - WAL directory size and segment count
  - Temp file usage warnings and recommendations
- [ ] **Connection Pool Integration** (optional - future)
  - PgBouncer stats integration (if accessible)
  - Pool saturation warnings
  - Client wait time metrics

### Phase 7 — Enterprise & Collaboration Features ✅ COMPLETE
- [x] **REST API for Metrics Export**
  - Full REST API at `/api/v1/*` with JSON responses
  - Endpoints for overview, activity, slow queries, locks, wait events
  - Endpoints for tables, databases, index advisor, query regressions
  - Health check and instance listing endpoints
- [x] **Prometheus Metrics Endpoint**
  - Micrometer integration with Prometheus registry
  - Metrics available at `/q/metrics` for Grafana integration
- [x] **Audit Logging**
  - Comprehensive action audit log (who, what, when)
  - Tracks query cancellations, terminations, and other admin actions
  - Audit log viewer with summary statistics
- [x] **Saved Queries & Bookmarks**
  - Bookmark slow queries for tracking
  - Add notes/annotations and tags to queries
  - Priority levels (Critical, High, Medium, Low)
  - Status tracking (Active, Investigating, Resolved, Ignored)
- [x] **Comparison Views**
  - Side-by-side instance comparison (overview stats)
  - Cross-instance query analysis (same query on different instances)
  - Performance variance detection and reporting
- [x] **Scheduled Reports**
  - Daily/weekly summary email reports
  - HTML report generation with top queries and recommendations
  - Configurable report recipients per instance
  - Quarkus Mailer integration
- [ ] **Custom Dashboards** (deferred to future phase)
  - User-defined metric panels
  - Custom SQL widget support (read-only queries)
  - Dashboard templates (OLTP, OLAP, mixed workload)

### Phase 8 — Change Data Control & Schema Management ✅ COMPLETE
- [x] **Logical Replication Management**
  - Publication browser (tables, actions, row filters)
  - Subscription status and lag monitoring
  - Replication origin tracking
  - Subscription statistics with error counts
- [x] **Schema Change Detection**
  - DDL event trigger monitoring (CREATE, ALTER, DROP)
  - Foreign key relationship tracking
  - Object dependency analysis (views, functions)
- [x] **Change Data Capture Dashboard**
  - Table change activity (INSERT/UPDATE/DELETE rates)
  - Row change velocity trends (from pg_stat_user_tables)
  - High-churn table identification with churn ratios
  - Estimated WAL generation by table
- [x] **Event Trigger Management**
  - List and status of DDL event triggers
  - Event trigger function display
  - Tag filtering for event triggers
- [x] **Table Partitioning Insights**
  - Partition tree visualisation with nested partitions
  - Partition size distribution and balance analysis
  - Orphan partition detection
  - Imbalance detection and warnings
  - Empty partition identification
- [x] **Data Lineage**
  - Foreign key relationship display with cascade rules
  - View dependencies (which tables views reference)
  - Function dependencies (which tables functions reference)
  - Summary statistics for schema objects

### Phase 9 — Responsive UI & Navigation Redesign ✅ COMPLETE

**Framework Decision:** Enhance Bootstrap 5 (no build step required)
- Keep Bootstrap 5 as base framework (CDN)
- Add Bootstrap Icons for navigation (CDN)
- Custom CSS for modern aesthetics (glassmorphism, smooth shadows, micro-animations)
- Optional: Animate.css for transitions (CDN)
- Optional: Bootswatch theme or Tabler UI kit for refreshed look
- No Tailwind/Node.js build pipeline required

- [x] **Left-Hand Icon Navigation** ✅ COMPLETE
  - Collapsible sidebar with icon-based navigation
  - Icon set: Bootstrap Icons (CDN, no build step)
  - Tooltips on collapsed state, labels on expanded
  - Grouped navigation sections (Monitoring, Analysis, Infrastructure, Data Control, Enterprise, System)
  - Persistent expand/collapse preference in localStorage
  - Mobile bottom navigation bar with "More" button to access full sidebar
  - [ ] Keyboard shortcuts for navigation (Ctrl+1, Ctrl+2, etc.) - deferred
- [x] **Mobile-First Responsive Layout**
  - Bottom navigation bar on mobile devices
  - Touch-friendly table interactions (tap to expand rows)
  - Responsive cards instead of tables on small screens
  - Pull-to-refresh on mobile
- [x] **Progressive Web App (PWA)**
  - Service worker for offline capability
  - App manifest for "Add to Home Screen"
  - Cached dashboard shell for instant loading
  - PWA icons (SVG + PNG at all standard sizes)
- [x] **Adaptive Dashboard Layouts**
  - Grid-based widget system (CSS Grid/Flexbox)
  - Responsive breakpoints: mobile (<768px), tablet (768-1024px), desktop (>1024px)
  - Compact vs comfortable view toggle with localStorage persistence
- [x] **Accessibility Improvements**
  - ARIA labels for all interactive elements
  - Keyboard navigation throughout
  - Screen reader announcements for live updates
  - High contrast mode option with localStorage persistence
  - Focus indicators and skip links
- [x] **Modern Aesthetic Enhancements**
  - Glassmorphism effects for cards and topbar/sidebar (backdrop-blur, transparency)
  - Refined shadow system (soft, layered shadows)
  - Smooth micro-animations on interactions (hover, focus, click)
  - Improved colour palette with better contrast ratios
  - Refined typography with improved hierarchy
  - Subtle gradient accents for visual interest
  - Rounded corners and softer visual edges
  - Loading skeletons instead of spinners

### Phase 10 — Security & Compliance Monitoring ✅ COMPLETE
- [x] **Role & Permission Auditing**
  - Database role hierarchy visualisation
  - Permission matrix (role × object × privilege via ACL parsing)
  - Superuser and elevated privilege warnings
  - Password policy compliance checks (expiry monitoring)
  - Note: Role membership changes over time requires history tracking (not implemented)
- [x] **Connection Security Analysis**
  - SSL/TLS connection status for all clients (pg_stat_ssl)
  - Authentication method breakdown (md5, scram-sha-256, trust, etc.)
  - pg_hba.conf rules display (PostgreSQL 10+)
  - Connection source analysis (internal/external/local classification)
  - Note: Failed login tracking not available in PostgreSQL without special logging
  - Note: Geolocation not implemented; uses simple IP classification
- [x] **Data Access Patterns**
  - Sensitive table detection via heuristic PII column analysis
  - PII column indicators (email, phone, SSN, password, DOB, address, financial, etc.)
  - Row-level security policy overview
  - Tables needing RLS protection highlighted
  - Note: Table access by role requires pg_stat_statements or audit extension
- [x] **Compliance Dashboards**
  - SOC 2 relevant metrics (access controls, encryption settings)
  - Security scoring by compliance area (5 areas)
  - Audit trail completeness scoring (pgaudit detection)
  - Encryption status verification (SSL, password encryption method)
  - Note: GDPR data subject access tracking requires application-layer implementation
- [x] **Security Recommendations**
  - Overly permissive role warnings (superuser, bypass RLS)
  - Public schema exposure alerts
  - Extension security review (dangerous extensions)
  - pg_hba.conf analysis recommendations
  - Priority-based categorisation (Critical, High, Medium, Low)
  - Note: Weak password detection not possible (passwords not accessible)

### Phase 11 — Intelligent Insights & Automation ✅ COMPLETE
- [x] **Anomaly Detection**
  - Statistical baseline learning for key metrics
  - Automatic anomaly alerts (connections, query times, errors)
  - Seasonal pattern recognition (daily, weekly cycles)
  - Correlation detection between metrics
  - Root cause suggestion for anomalies
- [x] **Predictive Analytics**
  - Storage growth forecasting
  - Connection pool exhaustion prediction
  - Query performance degradation trends
  - Maintenance window recommendations
  - Capacity planning projections
- [x] **Automated Recommendations Engine**
  - Priority-ranked action items dashboard
  - One-click fix suggestions with SQL preview
  - Impact estimation for recommendations
  - Recommendation history and effectiveness tracking
  - Configuration tuning suggestions (work_mem, shared_buffers, etc.)
- [x] **Natural Language Queries**
  - "Show me slow queries from yesterday"
  - "Which tables are growing fastest?"
  - "Why is the database slow right now?"
  - Query intent parsing to dashboard navigation
  - Plain English explanations for technical metrics
- [x] **Runbook Integration**
  - Predefined incident response playbooks
  - Step-by-step guided troubleshooting
  - Automated diagnostic data collection
  - Integration with ticketing systems (Jira, ServiceNow)
  - Post-incident report generation
- [x] **Scheduled Maintenance Automation**
  - Intelligent vacuum scheduling based on table activity
  - Automatic index rebuild recommendations
  - Off-peak maintenance window detection
  - Pre/post maintenance metric comparison
  - Rollback capabilities for configuration changes

### Phase 12 — Schema Comparison & Migration ✅ COMPLETE
- [x] **Cross-Instance Schema Comparison**
  - Compare schemas between different PostgreSQL instances (e.g., dev vs prod)
  - Source and destination instance selection from configured instances
  - Schema/namespace selection for comparison scope
  - Side-by-side visual diff view with colour-coded changes
- [x] **Comprehensive Object Comparison**
  - Tables: columns, data types, nullability, defaults, identity/serial
  - Indexes: type, columns, unique, partial, expression indexes
  - Constraints: primary keys, foreign keys, check constraints, unique constraints
  - Views and materialised views with definition comparison
  - Functions and procedures with body/signature comparison
  - Triggers with timing, events, and function references
  - Sequences with start, increment, min/max values
  - Custom types (enums, composites, domains)
  - Extensions with version comparison
- [x] **Flexible Filtering System**
  - Table name pattern exclusions (e.g., `zz_*`, `temp_*`, `_backup`)
  - Schema exclusions (e.g., `pg_catalog`, `information_schema`)
  - Object type filters (include/exclude specific object types)
  - Configurable filter patterns in text input fields
  - Regex support for advanced pattern matching
  - Filter presets for common exclusion patterns
- [x] **Difference Categorisation**
  - Missing objects (exists in source, not in destination)
  - Extra objects (exists in destination, not in source)
  - Modified objects (exists in both but differs)
  - Severity levels: Breaking (drops), Warning (alters), Info (additions)
  - Summary statistics (X tables differ, Y indexes missing, etc.)
- [x] **DDL Migration Script Generation**
  - Generate CREATE statements for missing objects
  - Generate ALTER statements for modified objects
  - Optional DROP statements for extra objects (with safety warnings)
  - Dependency-aware script ordering (create referenced tables first)
  - Transaction wrapping options (single transaction vs individual statements)
  - Script preview before download
  - Copy to clipboard functionality
- [x] **Comparison Profiles**
  - Save named profiles with source, destination, and filter configurations
  - Quick re-run of saved comparisons
  - Profile sharing via export/import (JSON format)
  - Default profile per instance pair
  - Profile history with last run timestamp and result summary
- [x] **Comparison History & Audit**
  - Log of all schema comparisons performed
  - Comparison result snapshots for trend analysis
  - Schema drift detection over time
  - [ ] Scheduled comparison runs with email notifications (deferred to future phase)
- [x] **Interactive Diff Viewer**
  - Expandable/collapsible object tree
  - Inline SQL definition diffs (unified or split view)
  - Search and filter within diff results
  - Export diff report as HTML or Markdown
  - [ ] Export as PDF (deferred to future phase)
  - [ ] Shareable comparison result URLs (deferred to future phase)

### Phase 13 — Dashboard Feature Toggles & Modular Configuration (Completed)
- [x] **Section-Level Dashboard Toggles**
  - Enable/disable entire dashboard sections via configuration
  - Sections: Monitoring, Analysis, Infrastructure, Data Control, Enterprise, Security
  - System section (About) always enabled
- [x] **Individual Page Toggles**
  - Fine-grained control over individual pages within sections
  - Override section-level settings (disable specific pages)
  - 27 individual page toggles across 6 configurable sections
- [x] **Environment Variable Support**
  - All toggles configurable via environment variables
  - Pattern: `PG_CONSOLE_DASH_{FEATURE}` (e.g., `PG_CONSOLE_DASH_MONITORING`)
  - Defaults to enabled (opt-out model)
- [x] **Navigation Integration**
  - Disabled pages hidden from sidebar navigation
  - Disabled sections collapse navigation groups
  - Clean UI without dead links
- [x] **Access Control**
  - Disabled pages return HTTP 404 when accessed directly
  - Disabled API endpoints return HTTP 404
  - Prevents information leakage about disabled features
- [x] **Configuration Hierarchy**
  - Page enabled only if: section enabled AND page enabled
  - Section toggle is master switch for all pages within
  - Individual page toggles provide granular control

### Phase 14 — Command-Line Interface (CLI) Support ✅ COMPLETE
- [x] **Picocli Integration**
  - Add Quarkus Picocli extension for CLI framework
  - Support both server mode and command mode
  - Automatic help generation and bash/zsh completion scripts
- [x] **Standard Flags**
  - `--help` / `-h` - Display usage information with all options
  - `--version` / `-V` - Show application and PostgreSQL client version
  - `--config` / `-c` - Specify alternate configuration file path
  - `--verbose` - Enable verbose logging output
- [x] **Server Startup Flags**
  - `--port` / `-p` - Override HTTP server port
  - `--host` - Override HTTP bind address
  - `--no-history` - Disable history sampling at startup
  - `--no-alerting` - Disable alerting at startup
  - `--instance` / `-i` - Specify default instance to connect to
- [x] **Database Admin Commands**
  - `init-schema` - Initialise pgconsole history schema on target database
  - `reset-stats` - Reset pg_stat_statements counters (with confirmation)
  - `health-check` - Test database connectivity and extension availability
  - `list-instances` - List all configured PostgreSQL instances with status
- [x] **Reporting Commands**
  - `export-report` - Generate incident report snapshot and exit
  - `export-config` - Export current effective configuration as properties/YAML/env
  - `validate-config` - Validate configuration without starting server
- [x] **Configuration Priority**
  - Priority order: CLI arguments > environment variables > application.properties
  - CLI flags map to existing configuration properties
  - Clear precedence documented in --help output

### Phase 15 — Enhanced Logging & Observability ✅ COMPLETE
- [x] **Structured Logging**
  - JSON format for production (machine-readable for log aggregation)
  - Plain text format for development (human-readable)
  - Configurable format switching via environment variable (PG_CONSOLE_LOG_FORMAT)
  - Compatible with ELK Stack, Splunk, CloudWatch, Loki
- [x] **Log Output Destinations**
  - Console output (stdout/stderr)
  - Rotating file appender with configurable size and retention
  - Separate error log file for warnings and above
  - Configurable log directory path
- [x] **MDC Context Propagation**
  - Request correlation IDs (UUID per request)
  - User/principal context in all log entries
  - Instance name context for multi-instance deployments
  - Client IP address tracking
- [x] **Performance Metrics Logging**
  - Database query execution time logging
  - Request/response latency tracking
  - Slow operation warnings (configurable threshold)
  - Memory and resource usage periodic logging
- [x] **SQL Query Logging**
  - Configurable enable/disable for SQL statement logging
  - Query execution time in milliseconds
  - Parameter binding logging (with redaction option)
  - Slow query threshold alerts
- [x] **Security & Redaction**
  - Automatic password/secret redaction in logs
  - Connection string sanitisation
  - Configurable sensitive field patterns
  - PII detection and masking options (email, phone, SSN, credit card)
- [x] **Async Logging**
  - Asynchronous log appender for high-throughput scenarios
  - Configurable buffer size and overflow policy
  - Non-blocking logging to prevent performance impact
- [x] **Log Level Management**
  - Runtime log level adjustment via API endpoint (/api/v1/logging/*)
  - Per-package log level configuration
  - Temporary debug mode with auto-revert
  - Log level presets (minimal, standard, verbose, debug)

### Phase 16 — Notification Channels & Alerting Integration ✅ COMPLETE
- [x] **Slack Integration**
  - Native Slack webhook support
  - Rich message formatting with alert severity colours
  - Channel routing based on alert type/severity
  - Block Kit support for structured messages
- [x] **Microsoft Teams Integration**
  - Teams webhook connector support
  - Adaptive card formatting for alerts
  - Channel and user mention support
- [x] **PagerDuty Integration**
  - PagerDuty Events API v2 integration
  - Incident creation and resolution
  - Service routing and escalation policies
  - Deduplication key support
- [x] **Discord Integration**
  - Discord webhook support
  - Embedded message formatting
  - Role mention support for critical alerts
- [x] **Email Enhancements**
  - HTML email templates with branding
  - Digest mode (batch multiple alerts)
  - Per-recipient preferences
  - Attachment support for reports
- [x] **Escalation Policies**
  - Multi-tier escalation chains
  - Time-based escalation (escalate after N minutes)
  - Repeat cycles for unacknowledged alerts
  - Fallback notification channels
- [x] **Alert Management**
  - Alert acknowledgement and silencing
  - Maintenance windows (suppress alerts)
  - Alert grouping and deduplication
  - Alert history and analytics
  - Note: On-call schedule integration deferred to future phase

### Phase 17 — Connection Pool Monitoring (Planned)
- [ ] **PgBouncer Integration**
  - Connect to PgBouncer admin console
  - Pool statistics (active, waiting, idle connections)
  - Database and user pool metrics
  - Client and server connection counts
  - Average query time per pool
- [ ] **Pgpool-II Integration**
  - Pgpool-II PCP interface support
  - Backend node status monitoring
  - Load balancing statistics
  - Connection pool utilisation
  - Watchdog cluster status
- [ ] **Pool Health Monitoring**
  - Pool saturation warnings (configurable threshold)
  - Client wait time alerts
  - Connection churn detection
  - Pool exhaustion prediction
- [ ] **Pool Configuration Display**
  - Current pool settings visualisation
  - Configuration recommendations
  - Pool sizing suggestions based on workload
- [ ] **Multi-Pooler Support**
  - Support multiple pooler instances
  - Pooler-to-database mapping
  - Aggregate pool statistics across instances

### Phase 18 — Testing Framework ✅ COMPLETE
- [x] **Unit Testing**
  - JUnit 5 test framework with Quarkus Test
  - Service layer unit tests with mocking (AlertingServiceTest, FeatureToggleServiceTest, AuditServiceTest)
  - Model/DTO validation tests (14 test classes covering all model classes)
  - 369+ unit tests with comprehensive coverage
- [x] **Integration Testing**
  - Quarkus @QuarkusTest integration tests
  - PostgreSQL support via Testcontainers (PostgresTestResource)
  - REST endpoint testing with RestAssured (ApiResourceIT)
  - Test profiles for isolated test execution
- [x] **End-to-End Testing** (Infrastructure Ready)
  - Playwright dependency configured in build.gradle
  - E2E test task configured (`gradle21w e2eTest`)
  - Test base classes and infrastructure ready
  - Note: Full E2E tests deferred until Playwright browser setup
- [x] **Test Infrastructure**
  - CI/CD pipeline integration (GitHub Actions .github/workflows/ci.yml)
  - Test result reporting and trends (Gradle test reports)
  - Code coverage reporting (JaCoCo with 60% initial target)
  - Test data factories (TestDataFactory with model builders)
  - Testcontainers for PostgreSQL (PostgresTestResource, PostgresTestContainer)

### Phase 19 — Documentation Generation ✅ COMPLETE
- [x] **Antora Documentation Site**
  - Full Antora documentation structure
  - Multiple modules (user guide, admin guide, API reference, developer guide)
  - Navigation hierarchy with nav.adoc files
  - Cross-module references and linking
  - Versioned documentation support
- [x] **AsciiDoc Content**
  - Comprehensive user documentation
  - Installation and configuration guides
  - Feature documentation with examples
  - Troubleshooting guides
  - Best practices and recommendations
- [x] **Mermaid Diagram Generation**
  - Architecture diagrams (component, deployment)
  - Entity-relationship diagrams from schema
  - Sequence diagrams for key workflows
  - Flowcharts for decision processes
  - Diagrams externalised to examples directory
- [ ] **Schema Documentation** (deferred)
  - Automated data dictionary generation
  - Table and column descriptions
  - Foreign key relationship documentation
  - Index documentation with usage statistics
  - Export as AsciiDoc tables
- [x] **API Documentation**
  - OpenAPI/Swagger specification generation
  - REST endpoint documentation
  - Request/response examples
  - Authentication documentation
  - Rate limiting and error codes
- [x] **Interactive Examples**
  - Code snippets with syntax highlighting
  - Copy-to-clipboard functionality
  - Runnable curl examples
  - Configuration file templates

### Phase 20 — Plugin & Extension System (Planned)
- [ ] **Plugin Architecture**
  - Plugin discovery and loading mechanism
  - Plugin lifecycle management (install, enable, disable, uninstall)
  - Plugin isolation and sandboxing
  - Plugin dependency resolution
  - Hot reload support for development
- [ ] **Custom Dashboard Widgets**
  - Widget plugin API for custom metrics
  - Custom visualisation components
  - Widget configuration UI
  - Widget placement and sizing
  - Widget data refresh management
- [ ] **Custom Metric Collectors**
  - Metric collector plugin interface
  - Custom SQL query metrics
  - External data source integration
  - Metric aggregation and transformation
  - Metric storage and retention
- [ ] **Third-Party Integrations**
  - Integration plugin framework
  - OAuth/API key management for integrations
  - Webhook receiver plugins
  - External alerting system connectors
  - Cloud provider integrations (AWS RDS, Azure, GCP)
- [ ] **Plugin Marketplace**
  - Plugin registry and discovery
  - Plugin versioning and updates
  - Plugin ratings and reviews
  - Official vs community plugins
  - Plugin security scanning
- [ ] **Plugin Development Kit**
  - Plugin SDK with TypeScript/Java support
  - Plugin scaffolding CLI tool
  - Development mode with hot reload
  - Plugin testing utilities
  - Documentation and examples

### Phase 21 — Enhanced Database Diagnostics & Interactive Charts ✅ COMPLETE
- [x] **Pipeline/Queue Risk Monitoring**
  - Oldest row age tracking for event/queue tables
  - Queue depth and staleness warnings
  - Configurable table patterns for queue detection
- [x] **TOAST Bloat Analysis**
  - TOAST table size vs main table ratio
  - TOAST-specific bloat percentage
  - Large object storage efficiency metrics
- [x] **Index Redundancy Detection**
  - Duplicate/overlapping index identification
  - Index-to-table size ratio warnings
  - Missing foreign key index detection
- [x] **Statistical Freshness Monitoring**
  - Percentage modified since last ANALYZE
  - Auto-analyze prediction
  - Priority ranking for stale tables
- [x] **Write/Read Ratio Analysis**
  - Per-table DML vs scan breakdown
  - Workload pattern classification
  - Dominant operation indicators
- [x] **HOT Update Efficiency**
  - HOT update ratio tracking
  - Fill factor recommendations
  - Bloat candidate identification
- [x] **Column Correlation Statistics**
  - Physical vs logical ordering analysis
  - CLUSTER recommendations
  - Range query optimisation hints
- [x] **Interactive Live Charts**
  - Adjustable refresh intervals (3s/5s/10s/30s)
  - Pause/resume controls
  - Connection, commit, tuple, cache trends
  - WAL and checkpoint monitoring
- [x] **Enhanced XID Wraparound Monitoring**
  - Percentage to wraparound with visual indicators
  - Per-database XID age tracking
  - Emergency vacuum recommendations
- [ ] **Tooltip Drill-downs** (deferred to future phase)
  - Hover tooltips with top-N details
  - Context-sensitive metric explanations
  - Quick action links

---

1. Purpose

Provide a lightweight, self-hosted web dashboard for PostgreSQL operational insight and performance monitoring, focusing on:

slow / expensive queries

current activity and blocking

connections, waits, and long transactions

table/index usage and bloat-ish indicators (best-effort)

optional historical sampling for trends

Non-goals

Not a full DB admin IDE (no arbitrary SQL editor in v1)

Not a replacement for pgAdmin/DBeaver

No heavy charting pipeline or frontend build toolchain

2. Target Users

Solution architects / engineers owning a Postgres-backed app

Ops/DBA-lite users needing quick visibility into query hotspots and lock contention

Dev teams troubleshooting production incidents

3. Deployment Assumptions

Runs as a single service (JVM) with static assets packaged in the jar

Connects to one or more target Postgres instances using configured connection strings

Optional: stores its own monitoring history in a “dashboard schema” on a separate DB or on the same DB (configurable)

4. Core Design Principles

Server rendered by default (Qute). Pages are accessible without JS.

htmx for partial updates (auto-refresh, sorting, modal fragments).

Minimal vanilla JS only for:

dark mode toggle + persistence

tooltip/hover helpers if needed

copy-to-clipboard for query text (optional)

No Node/npm build; use CDN or vendored minified JS/CSS.

Safe by default: read-only insight, with “dangerous actions” disabled unless explicitly enabled and permissioned.

User Experience (High Level)
Global Layout

Top nav: Overview | Activity | Slow Queries | Locks | Tables/Indexes | Settings/About

Instance selector (if multiple DBs): dropdown at top-right (or left of nav)

Footer: Postgres version, dashboard version, last refresh time

Theme

Light/dark mode toggle

Theme stored in localStorage, applied early to avoid flicker

CSS variables (or Bootstrap with a small override stylesheet)

Interactions

Column sorting by clicking column headers (server-side sort)

Filter input on key lists (server-side search)

Hover to show full query text in slow query list (tooltip)

“Details” action on row opens a modal (htmx) or navigates to detail page (fallback)

Functional Requirements
1) Database Instance Management

FR-1.1 Support one or more configured instances:

instanceId, displayName, JDBC URL, username, password/secret reference

Optional tags (env=prod, team=payments)

FR-1.2 Instance health check

on dashboard startup and periodically, validate connectivity and record status

FR-1.3 Security

credentials via env vars or secret provider (Kubernetes/HashiCorp/etc)

never log passwords, never log full JDBC URLs with secrets

2) Overview Dashboard

Goal: “Is the DB OK right now?”

Widgets:

Connections used / max

Active queries count

Blocked queries count

Longest running query duration

Cache hit ratio (display with disclaimer)

DB size

Top tables by size (top 10)

Top indexes by size (top 10)

Sources (typical):

pg_stat_database

pg_stat_activity

pg_settings

pg_database_size()

pg_class, pg_stat_user_tables

Refresh: partial sections auto-refresh every 10–30 seconds (configurable)

3) Activity Dashboard

Goal: “What’s running and why is it slow?”

Table: active sessions
Columns:

pid

user

app_name

state

query duration

wait_event_type/wait_event

client addr

truncated query + hover for full
Actions:

“Cancel” (optional)

“Terminate” (optional)

Filters:

min duration

show only “active”

show only waiting/blocked

search query text

Refresh: every 5–10 seconds

Safety controls:

Cancel/Terminate disabled by default

If enabled, require “admin role” or config token, and double-confirm via modal

4) Blocking / Locks Dashboard

Goal: Find lock contention quickly.

Views:

blocking tree: blocked_pid → blocker_pid with lock modes

list of locks (grouped by relation)

highlight “idle in transaction” blockers

Refresh: every 5–10 seconds

5) Slow Queries Dashboard (Key Feature)

This is the “product heart”.

Prerequisite

Requires pg_stat_statements extension enabled on the target DB.

Table: Top Queries

Sortable by column headers (server-side):

total_time (descending)

mean_time

calls

rows

shared_blks_hit/read

temp_blks_written (if available)

% of total time (optional)

Row includes:

queryid (or hash)

normalized query (truncated in cell)

hover tooltip shows full query text

“Details” button

Filters:

search query text (contains)

min calls

min mean_time

time window: “since last reset” (default), plus if history exists: last 1h/24h/7d

“Details” view for a query

full normalized query text (copy button)

stats summary (calls, total_time, mean_time, stddev, rows)

sparkline trend of time/calls (if history sampling enabled)

optionally: show “similar queries” by shared prefix/fingerprint (phase 3)

Tooltip requirement: hover to full query text

Implementation options:

Pure CSS tooltip (preferred if it behaves well)

Minimal JS tooltip helper (if you want nicer positioning)

HTML title="" attribute fallback

Recommendation: Use title as fallback plus a small CSS tooltip for better formatting (monospace, line breaks).

6) Tables / Indexes Dashboard

Tables:

top by size

sequential scan heavy tables

dead tuples approximation (if you choose to expose)

last vacuum/analyze (if available in stats views)

Indexes:

top by size

unused indexes (caution text)

index scan counts

This page should include warnings that these are heuristics.

7) Optional History Sampling (Strongly Recommended in Phase 2)

Without this, you can’t do trends.

Approach:

Quarkus scheduler job runs every 30s/60s:

snapshot key metrics (connections, active, blocked)

snapshot top N queries from pg_stat_statements (by total_time or mean_time)

store aggregated counters in dashboard tables

Storage location:

configurable:

same Postgres instance (dashboard schema)

separate Postgres DB (recommended for safety)

Data retention:

keep 7 days by default (configurable)

daily cleanup job

Notes:

sampling should be light and bounded (top N, not full scan)

avoid heavy introspection queries on busy prod

Non-Functional Requirements
Performance

Page loads under 1–2 seconds for typical datasets

All “live refresh” endpoints must be bounded and efficient (limit, filter, index where applicable)

Avoid rendering massive query lists; default to top 50 / 100 with pagination

Reliability

If target DB unreachable: show “degraded” state and last known snapshot

Never crash entire app for a single instance failing

Security

Authentication: at minimum HTTP basic or reverse-proxy SSO (preferred)

Authorization: role-based for dangerous actions

Prevent SSRF-style “connect to arbitrary DB” (instances must be preconfigured)

Audit log for admin actions (cancel/terminate, reset stats)

Usability

Accessible tables (keyboard-friendly)

Monospace code blocks for SQL

Obvious refresh indicators (last updated timestamp)

Technical Architecture
Backend Stack

Quarkus (Gradle)

Qute templating

htmx (vendored or CDN)

JDBC (Agroal) + plain SQL (recommended)

Flyway or Liquibase (only for dashboard’s own schema; optional)

Small in-memory cache for some expensive stats (short TTL)

Opinionated call: use plain SQL + lightweight row mapping, not ORM. You’re reading system views; ORM adds friction without benefit.

Module/Package Layout (suggested)

config/ instance configs, feature flags, auth config

db/ query DAO layer

service/ aggregation & business logic

web/ routes/controllers

web/fragments/ fragment endpoints for htmx

render/ SVG render helpers (sparklines)

model/ DTOs

Static Assets

META-INF/resources/css/app.css

META-INF/resources/js/app.js

META-INF/resources/vendor/htmx.min.js (or CDN)

optional: Bootstrap CSS (CDN) + small custom overrides

SVG Rendering

Generate SVG strings server-side:

sparklines: small (e.g., 120x24)

bars: top-N bars

Embed inline in HTML for crisp rendering in both themes

UI Details: Slow Query Table
Sorting

Clickable column headers with hx-get="/fragments/slow-queries?sort=mean_time&dir=desc"

hx-push-url="true" so back button works

Server validates sort column against allowlist to avoid SQL injection

Hover Full Query

Display truncated query in table cell (CSS text-overflow: ellipsis)

Tooltip contains full query, monospaced, wrapped

Fallback:

title="full query text" (cheap and robust)
Enhanced:

custom tooltip with data-tooltip="..." for better formatting (CSS)

Query Text Handling

Normalized query from pg_stat_statements may be long

Store and render safely:

HTML-escape everything

optionally limit tooltip length (e.g., 10k chars) to avoid huge DOM

Phased Implementation Plan
Phase 0 — Repo + Foundations (1–2 days)

Deliverables:

Quarkus Gradle project

Qute layout template with nav

Static assets setup (htmx + CSS framework + app.css + app.js)

Dark mode toggle working (persisted)

DB connectivity config for one instance

Basic “About” page showing app + DB version

Acceptance Criteria:

Running locally with ./gradlew quarkusDev

Theme toggle persists after refresh

Landing page renders using Qute

Phase 1 — MVP Monitoring Dashboards (core value) (3–7 days)

Deliverables:

Overview page (live widgets)

Activity page (active sessions table, auto-refresh)

Locks/Blocking page (blocked/blocker list)

Slow Queries page using pg_stat_statements

sortable table

hover full query

query detail page or modal fragment

Acceptance Criteria:

Works against a real DB with pg_stat_statements enabled

Sorting and filtering are server-side and safe (allowlist)

No Node build pipeline

htmx partial refresh works reliably

Phase 2 — History Sampling + SVG Sparklines (5–10 days)

Deliverables:

Dashboard schema + migrations (Flyway/Liquibase)

Scheduled sampler job (30–60s) storing:

connection counts

active/blocked counts

top N query metrics deltas or snapshots

Trend panels:

Overview shows last 1h sparkline for connections/active/blocked

Slow query details show sparkline of mean_time or total_time

Retention cleanup job

Acceptance Criteria:

Trend charts appear as inline SVG

Sampling bounded (top N) and does not overload DB

Can disable history entirely with config

Phase 3 — Polishing + Safety + Multi-Instance (1–2 weeks)

Deliverables:

Multi-instance selector

Authentication integration (basic auth or reverse proxy headers)

Authorization for dangerous actions

Optional actions:

cancel query

terminate backend

reset pg_stat_statements (admin-only)

“Copy query” button

Export: download CSV for slow query list

Heuristic pages: tables/indexes insights

Acceptance Criteria:

Production-friendly guardrails

Clear audit log for admin actions

UI feels cohesive

Phase 4 — Advanced Diagnostics ✅ COMPLETE

Deliverables:

Wait-event breakdown summary page

Query fingerprint grouping with normalised view

Explain Plan integration on Query Detail page

Incident report snapshot export

Alerting integration with webhook support

Acceptance Criteria:

Wait events page shows type summaries and detailed breakdown

Slow queries can be viewed grouped by fingerprint

EXPLAIN/ANALYZE can be run from Query Detail (SELECT only)

Incident reports capture full database state

Alerts fire when thresholds exceeded with cooldown

Phase 5 — Query & Index Optimisation (Complete)

Deliverables:

Index Advisor page with recommendations for missing, unused, and duplicate indexes

Query regression detection with configurable thresholds and time windows

Table maintenance recommendations (vacuum/analyse) with bloat estimation

pg_stat_statements baseline comparisons and top movers report

Acceptance Criteria:

Index suggestions based on sequential scan patterns - DONE

Regression candidates flagged with performance deltas and severity levels - DONE

Maintenance recommendations shown with urgency levels and SQL commands - DONE

Query baselines enable period-over-period comparison with movement types - DONE

Phase 6 — Replication & Infrastructure Monitoring ✅ COMPLETE

Deliverables:

Replication Dashboard (streaming and logical) - DONE

Vacuum progress monitoring - DONE

Background process status page - DONE

Storage insights (tablespaces, WAL) - DONE

Optional PgBouncer integration - Deferred to future phase

Acceptance Criteria:

Replication lag visible with sync state badges - DONE

Active vacuums show progress with phase tracking - DONE

Background workers status shown with buffer statistics - DONE

Storage breakdown with tablespace, database, and WAL sizes - DONE

Phase 7 — Enterprise & Collaboration Features ✅ COMPLETE

Deliverables:

REST API for metrics export at /api/v1/* - DONE

Prometheus metrics endpoint (/q/metrics) - DONE

Audit logging with viewer page - DONE

Query bookmarks with annotations and tags - DONE

Cross-instance comparison views - DONE

Scheduled email reports - DONE

Custom dashboard builder - Deferred to future phase

Acceptance Criteria:

REST API returns JSON for all major dashboard data - DONE

Prometheus metrics available for external consumption - DONE

All admin actions logged with user attribution - DONE

Bookmarks persist with priority, status, and tags - DONE

Instances compared side-by-side with variance detection - DONE

Reports scheduled with configurable recipients - DONE

Phase 8 — Change Data Control & Schema Management ✅ COMPLETE

Deliverables:

Logical replication management dashboard (publications, subscriptions, origins) - DONE

Schema change detection with DDL event monitoring - DONE

Change data capture dashboard with table change rates - DONE

Event trigger management interface - DONE

Table partitioning insights with balance analysis - DONE

Data lineage visualisation - DONE

Acceptance Criteria:

Publications and subscriptions visible with status and statistics - DONE

DDL event triggers listed with function information - DONE

Table change rates displayed with activity levels - DONE

Event triggers listed with enabled status - DONE

Partition trees displayed with size distribution and imbalance detection - DONE

Foreign key relationships shown with cascade rules - DONE

Phase 9 — Responsive UI & Navigation Redesign ✅ COMPLETE

Deliverables:

Left-hand collapsible sidebar with icon-based navigation - DONE

Mobile-responsive layouts with bottom navigation on small screens - DONE

Progressive Web App (PWA) support with offline capability - DONE

Adaptive dashboard layouts with responsive breakpoints - DONE

Accessibility improvements (ARIA, keyboard navigation, screen reader support) - DONE

Modern aesthetic enhancements (glassmorphism, animations, high contrast mode) - DONE

Acceptance Criteria:

Sidebar collapses to icons with tooltips on hover - DONE

Mobile users see bottom navigation bar and card-based layouts - DONE

PWA installable with "Add to Home Screen" functionality - DONE

Dashboard adapts gracefully across mobile, tablet, and desktop - DONE

WCAG 2.1 AA compliance for core functionality - DONE

Phase 10 — Security & Compliance Monitoring ✅ COMPLETE

Deliverables:

Role and permission auditing dashboard - DONE

Connection security analysis (SSL, auth methods, pg_hba.conf) - DONE

Data access pattern monitoring with PII detection and RLS policies - DONE

Compliance dashboards (SOC 2 metrics, security scoring) - DONE

Security recommendations engine with priority categorisation - DONE

Acceptance Criteria:

Role hierarchy visualised with permission matrix and membership tree - DONE

SSL/TLS status visible for all connections via pg_stat_ssl - DONE

Sensitive tables detected via heuristic PII column analysis - DONE

Compliance scoring visible across 5 security areas with grades - DONE

Security recommendations displayed by priority with suggested actions - DONE

Known Limitations:

Failed login tracking requires PostgreSQL log parsing (not implemented)

Geolocation uses simple IP classification (internal/external/local)

GDPR data subject tracking requires application-layer implementation

Weak password detection not possible (passwords not accessible)

Phase 11 — Intelligent Insights & Automation ✅ COMPLETE

Deliverables:

Anomaly detection with statistical baseline learning

Predictive analytics for storage, connections, and performance

Automated recommendations engine with priority ranking

Natural language query interface

Runbook integration with guided troubleshooting

Scheduled maintenance automation

Acceptance Criteria:

Anomalies detected and alerted with root cause suggestions

Storage and capacity forecasts displayed with confidence intervals

Recommendations ranked by impact with one-click SQL preview

Natural language queries parsed to relevant dashboard views

Runbooks executed with step-by-step guidance

Maintenance windows suggested based on activity patterns

Phase 12 — Schema Comparison & Migration ✅ COMPLETE

Deliverables:

Cross-instance schema comparison with source/destination selector - DONE

Comprehensive object comparison (tables, indexes, constraints, views, functions, triggers, sequences, types, extensions) - DONE

Flexible filtering system with pattern exclusions (e.g., `zz_*`, `temp_*`) - DONE

Visual diff viewer with colour-coded changes and expandable object tree - DONE

DDL migration script generation with dependency ordering - DONE

Saveable comparison profiles for repeated use - DONE

Comparison history and schema drift detection - DONE

Acceptance Criteria:

Users can select source and destination instances from configured list - DONE

All major database objects compared with detailed attribute-level diffs - DONE

Table exclusion patterns configurable via text input (supports wildcards and regex) - DONE

Side-by-side diff view shows additions (green), deletions (red), modifications (yellow) - DONE

Generated DDL scripts are syntactically correct and dependency-ordered - DONE

Profiles persist and can be re-run with one click - DONE

Comparison results exportable as HTML or Markdown - DONE

Known Limitations:

Scheduled comparison runs with email notifications deferred to future phase

PDF export deferred to future phase

Shareable comparison result URLs deferred to future phase

Phase 13 — Dashboard Feature Toggles & Modular Configuration (Completed)

Deliverables:

DashboardConfig.java - SmallRye Config mapping interface for dashboard toggles

FeatureToggleService.java - Service for checking page enablement with hierarchical logic

Section-level toggles for Monitoring, Analysis, Infrastructure, Data Control, Enterprise, Security

Individual page toggles (27 pages) with environment variable support

Navigation conditional rendering in base.html using Qute {#if} directives

Route guards in DashboardResource.java, ApiResource.java, and SchemaComparisonResource.java returning 404

Acceptance Criteria:

[x] Section toggles disable all pages within that section

[x] Individual page toggles can override section settings

[x] Disabled pages return HTTP 404 when accessed directly

[x] Disabled pages hidden from sidebar navigation

[x] Disabled API endpoints return HTTP 404

[x] All features enabled by default (opt-out model)

[x] Configuration via application.properties and environment variables

[x] About page always accessible regardless of settings

Configuration Properties:

```properties
# Section toggles
pg-console.dashboards.monitoring.enabled=${PG_CONSOLE_DASH_MONITORING:true}
pg-console.dashboards.analysis.enabled=${PG_CONSOLE_DASH_ANALYSIS:true}
pg-console.dashboards.infrastructure.enabled=${PG_CONSOLE_DASH_INFRASTRUCTURE:true}
pg-console.dashboards.data-control.enabled=${PG_CONSOLE_DASH_DATA_CONTROL:true}
pg-console.dashboards.enterprise.enabled=${PG_CONSOLE_DASH_ENTERPRISE:true}
pg-console.dashboards.security.enabled=${PG_CONSOLE_DASH_SECURITY:true}

# Individual page toggles (examples)
pg-console.dashboards.monitoring.slow-queries=${PG_CONSOLE_DASH_SLOW_QUERIES:true}
pg-console.dashboards.analysis.index-advisor=${PG_CONSOLE_DASH_INDEX_ADVISOR:true}
```

Phase 14 — Command-Line Interface (CLI) Support ✅ COMPLETE

Deliverables:

Picocli integration via Quarkus Picocli extension

Standard flags (--help, --version, --config, --verbose)

Server startup flags (--port, --host, --no-history, --no-alerting, --instance)

Database admin commands (init-schema, reset-stats, health-check, list-instances)

Reporting commands (export-report, export-config, validate-config)

Bash/zsh completion script generation

Acceptance Criteria:

Application supports both server mode and command mode

CLI arguments take highest priority over env vars and properties

--help displays all available options with descriptions

--version shows application version and build info

Admin commands can run without starting web server

health-check validates database connectivity and pg_stat_statements availability

validate-config exits with appropriate return codes (0 = valid, 1 = invalid)

Shell completion scripts generated for bash and zsh

Example Usage:

```bash
# Start server with CLI overrides
java -jar pg-console.jar --port 9090 --no-alerting

# Run admin commands
java -jar pg-console.jar health-check --instance production
java -jar pg-console.jar list-instances
java -jar pg-console.jar export-report --output incident-2024.txt
java -jar pg-console.jar validate-config --config /etc/pg-console/app.properties

# Get help
java -jar pg-console.jar --help
java -jar pg-console.jar health-check --help
```

CLI Flags Reference:

| Flag | Short | Description |
|------|-------|-------------|
| `--help` | `-h` | Display usage information |
| `--version` | `-v` | Show application version |
| `--config` | `-c` | Alternate config file path |
| `--verbose` | | Enable verbose logging |
| `--port` | `-p` | HTTP server port |
| `--host` | | HTTP bind address |
| `--no-history` | | Disable history sampling |
| `--no-alerting` | | Disable alerting |
| `--instance` | `-i` | Default instance name |

Phase 15 — Enhanced Logging & Observability ✅ COMPLETE

Deliverables:

Structured JSON logging for production environments - DONE

Plain text logging for development with configurable switching - DONE

Rotating file appender with size limits and retention policies - DONE

MDC context propagation with correlation IDs and user context - DONE

Performance metrics logging for database operations and requests - DONE

Configurable SQL query logging with execution times - DONE

Sensitive data redaction (passwords, connection strings, PII) - DONE

Async logging appender for high-throughput scenarios - DONE

Runtime log level management via API endpoint - DONE

Acceptance Criteria:

JSON and plain text formats switchable via PG_CONSOLE_LOG_FORMAT - DONE

File logging writes to configurable directory with rotation - DONE

Each request gets unique correlation ID visible in all related logs - DONE

Database query times logged when SQL logging enabled - DONE

Passwords and secrets automatically redacted from all log output - DONE

Async logging does not block request processing - DONE

Log levels adjustable at runtime without restart - DONE

Slow operation warnings triggered above configurable threshold - DONE

Configuration Properties:

```properties
# Log format (json or plain)
pg-console.logging.format=${PG_CONSOLE_LOG_FORMAT:plain}

# File logging
pg-console.logging.file.enabled=${PG_CONSOLE_LOG_FILE_ENABLED:false}
pg-console.logging.file.path=${PG_CONSOLE_LOG_FILE_PATH:/var/log/pg-console}
pg-console.logging.file.max-size=${PG_CONSOLE_LOG_FILE_SIZE:10M}
pg-console.logging.file.max-backup=${PG_CONSOLE_LOG_FILE_BACKUP:5}

# SQL logging
pg-console.logging.sql.enabled=${PG_CONSOLE_LOG_SQL_ENABLED:false}
pg-console.logging.sql.slow-threshold-ms=${PG_CONSOLE_LOG_SQL_SLOW_MS:1000}

# Async logging
pg-console.logging.async.enabled=${PG_CONSOLE_LOG_ASYNC:true}
pg-console.logging.async.queue-size=${PG_CONSOLE_LOG_ASYNC_QUEUE:1024}

# Redaction
pg-console.logging.redact.enabled=${PG_CONSOLE_LOG_REDACT:true}
pg-console.logging.redact.patterns=${PG_CONSOLE_LOG_REDACT_PATTERNS:password,secret,token,key}
```

Example Log Output (JSON):

```json
{
  "timestamp": "2024-12-27T10:15:30.123Z",
  "level": "INFO",
  "logger": "c.b.p.service.PostgresService",
  "message": "Query executed successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "user": "admin",
  "instance": "production",
  "duration_ms": 45,
  "thread": "executor-1"
}
```

Phase 16 — Notification Channels & Alerting Integration ✅ COMPLETE

Deliverables:

Slack webhook integration with rich message formatting - DONE

Microsoft Teams adaptive card support - DONE

PagerDuty Events API v2 integration with incident management - DONE

Discord webhook support with embedded messages - DONE

Enhanced email templates with HTML formatting and digests - DONE

Multi-tier escalation policies with time-based escalation - DONE

Alert acknowledgement, silencing, and maintenance windows - DONE

Acceptance Criteria:

Slack alerts display with severity colours using Block Kit - DONE

Teams messages render as adaptive cards with proper formatting - DONE

PagerDuty incidents created with deduplication and service routing - DONE

Discord embeds include all relevant alert information - DONE

Email notifications support HTML templates and digest mode - DONE

Escalation chains progress after configurable timeout periods - DONE

Maintenance windows suppress alerts for specified duration - DONE

Known Limitations:

On-call schedule integration deferred to future phase

Interactive acknowledgement buttons in Slack/Teams deferred to future phase

Configuration Properties:

```properties
# Slack
pg-console.notifications.slack.enabled=${PG_CONSOLE_SLACK_ENABLED:false}
pg-console.notifications.slack.webhook-url=${PG_CONSOLE_SLACK_WEBHOOK}
pg-console.notifications.slack.channel=${PG_CONSOLE_SLACK_CHANNEL:#alerts}

# PagerDuty
pg-console.notifications.pagerduty.enabled=${PG_CONSOLE_PAGERDUTY_ENABLED:false}
pg-console.notifications.pagerduty.routing-key=${PG_CONSOLE_PAGERDUTY_KEY}

# Teams
pg-console.notifications.teams.enabled=${PG_CONSOLE_TEAMS_ENABLED:false}
pg-console.notifications.teams.webhook-url=${PG_CONSOLE_TEAMS_WEBHOOK}

# Escalation
pg-console.notifications.escalation.timeout-minutes=${PG_CONSOLE_ESCALATION_TIMEOUT:15}
```

Phase 17 — Connection Pool Monitoring (Planned)

Deliverables:

PgBouncer admin console integration

Pgpool-II PCP interface support

Pool health monitoring with saturation warnings

Pool configuration display and recommendations

Multi-pooler support with aggregate statistics

Acceptance Criteria:

PgBouncer stats displayed (active, waiting, idle, server connections)

Pgpool-II backend status and load balancing visible

Pool saturation warnings trigger at configurable threshold

Configuration recommendations based on observed workload

Multiple pooler instances configurable and monitored

Configuration Properties:

```properties
# PgBouncer
pg-console.pooler.pgbouncer.enabled=${PG_CONSOLE_PGBOUNCER_ENABLED:false}
pg-console.pooler.pgbouncer.host=${PG_CONSOLE_PGBOUNCER_HOST:localhost}
pg-console.pooler.pgbouncer.port=${PG_CONSOLE_PGBOUNCER_PORT:6432}

# Pgpool-II
pg-console.pooler.pgpool.enabled=${PG_CONSOLE_PGPOOL_ENABLED:false}
pg-console.pooler.pgpool.pcp-host=${PG_CONSOLE_PGPOOL_PCP_HOST:localhost}
pg-console.pooler.pgpool.pcp-port=${PG_CONSOLE_PGPOOL_PCP_PORT:9898}

# Thresholds
pg-console.pooler.saturation-warning-percent=${PG_CONSOLE_POOL_WARN_PERCENT:80}
```

Phase 18 — Testing Framework ✅ COMPLETE

Deliverables:

JUnit 5 unit test suite with Quarkus Test - DONE

Integration tests using Testcontainers for PostgreSQL - DONE

End-to-end test infrastructure with Playwright - DONE (infrastructure ready)

CI/CD pipeline with GitHub Actions - DONE

Code coverage reporting with JaCoCo (60% initial target) - DONE

Acceptance Criteria:

369+ unit tests with comprehensive coverage - DONE

Service classes have unit tests (AlertingServiceTest, FeatureToggleServiceTest, AuditServiceTest) - DONE

Integration tests verify REST endpoints (ApiResourceIT) - DONE

Test data factories provide consistent test data (TestDataFactory) - DONE

CI pipeline configured to run on push and PR (.github/workflows/ci.yml) - DONE

Coverage reports generated via JaCoCo with configurable thresholds - DONE

Test Commands:

```bash
# Run unit tests
gradle21w test

# Run integration tests
gradle21w integrationTest

# Run E2E tests (when Playwright configured)
gradle21w e2eTest

# Generate coverage report
gradle21w jacocoTestReport

# Check coverage threshold
gradle21w jacocoTestCoverageVerification
```

Phase 19 — Documentation Generation ✅ COMPLETE

Deliverables:

Antora documentation site with versioned modules - DONE

AsciiDoc content for user, admin, and developer guides - DONE

Mermaid diagrams for architecture, ERD, and workflows - DONE

Automated schema documentation (data dictionary) - Deferred

OpenAPI/Swagger API specification - DONE

Acceptance Criteria:

Antora site builds with `gradle21w antora` command - DONE

Documentation modules: user-guide, admin-guide, api-reference, developer-guide - DONE

Mermaid diagrams render in documentation and externalised to examples/ - DONE

Schema documentation auto-generated from database metadata - Deferred

OpenAPI spec available at /q/openapi endpoint - DONE

Known Limitations:

Automated schema documentation (data dictionary) deferred to future phase

Documentation Structure:

```
docs/
├── antora.yml
├── modules/
│   ├── ROOT/
│   │   ├── nav.adoc
│   │   └── pages/
│   │       ├── index.adoc
│   │       └── quick-start.adoc
│   ├── user-guide/
│   │   ├── nav.adoc
│   │   └── pages/
│   │       ├── index.adoc
│   │       ├── installation.adoc
│   │       ├── configuration.adoc
│   │       ├── dashboards.adoc
│   │       └── troubleshooting.adoc
│   ├── admin-guide/
│   │   ├── nav.adoc
│   │   └── pages/
│   │       ├── index.adoc
│   │       ├── deployment.adoc
│   │       ├── configuration.adoc
│   │       ├── security.adoc
│   │       ├── multi-instance.adoc
│   │       ├── alerting.adoc
│   │       └── cli-reference.adoc
│   ├── api-reference/
│   │   ├── nav.adoc
│   │   └── pages/
│   │       ├── index.adoc
│   │       ├── rest-api.adoc
│   │       └── endpoints.adoc
│   └── developer-guide/
│       ├── nav.adoc
│       ├── examples/
│       │   ├── architecture.mmd
│       │   ├── erd-schema.mmd
│       │   └── workflow-alerting.mmd
│       └── pages/
│           ├── index.adoc
│           ├── architecture.adoc
│           ├── database-schema.adoc
│           └── testing.adoc
└── examples/
    ├── architecture.mmd
    ├── erd-schema.mmd
    └── workflow-alerting.mmd
```

Phase 20 — Plugin & Extension System (Planned)

Deliverables:

Plugin architecture with discovery and lifecycle management

Custom dashboard widget API

Custom metric collector interface

Third-party integration framework

Plugin marketplace with registry

Plugin development kit (SDK)

Acceptance Criteria:

Plugins discovered from designated directory on startup

Widget plugins render custom visualisations on dashboards

Metric collectors execute custom SQL and store results

Integration plugins connect to external services (AWS RDS, etc.)

Plugin registry lists available plugins with versions

SDK scaffolds new plugin projects with templates

Plugin API Example:

```java
@Plugin(id = "custom-metrics", version = "1.0.0")
public class CustomMetricsPlugin implements MetricCollector {

    @Override
    public List<Metric> collect(DataSource ds) {
        // Custom metric collection logic
        return List.of(
            new Metric("custom.connection.count", queryConnectionCount(ds))
        );
    }
}
```

Configuration Properties:

```properties
# Plugin system
pg-console.plugins.enabled=${PG_CONSOLE_PLUGINS_ENABLED:false}
pg-console.plugins.directory=${PG_CONSOLE_PLUGINS_DIR:./plugins}
pg-console.plugins.auto-update=${PG_CONSOLE_PLUGINS_AUTO_UPDATE:false}

# Marketplace
pg-console.plugins.marketplace.enabled=${PG_CONSOLE_MARKETPLACE_ENABLED:false}
pg-console.plugins.marketplace.url=${PG_CONSOLE_MARKETPLACE_URL:https://plugins.pg-console.io}
```

Phase 21 — Enhanced Database Diagnostics & Interactive Charts ✅ COMPLETE

Deliverables:

Pipeline/Queue Risk Monitoring with oldest row age tracking - DONE

TOAST Bloat Analysis separate from table bloat - DONE

Index Redundancy Detection with duplicate and missing FK indexes - DONE

Statistical Freshness Monitoring with auto-analyze predictions - DONE

Write/Read Ratio Analysis for workload classification - DONE

HOT Update Efficiency tracking for fill factor recommendations - DONE

Column Correlation Statistics for CLUSTER recommendations - DONE

Interactive Live Charts with adjustable refresh and pause/resume - DONE

Enhanced XID Wraparound Monitoring with visual indicators - DONE

Tooltip Drill-downs for quick access to details - Deferred to future phase

Acceptance Criteria:

Queue tables display oldest row age with staleness warnings - DONE

TOAST bloat percentage shown separately from table bloat - DONE

Duplicate indexes identified with wasted space calculation - DONE

Tables ranked by statistical staleness (% modified since ANALYZE) - DONE

Write-heavy vs read-heavy tables classified automatically - DONE

HOT update ratio displayed with fill factor suggestions - DONE

Column correlations shown with CLUSTER recommendations - DONE

Charts support 3s/5s/10s/30s refresh with pause/resume controls - DONE

XID wraparound shows percentage to threshold per database - DONE

Tooltips show top-5 details on hover without page navigation - Deferred

Configuration Properties:

```properties
# Diagnostics section toggle
pg-console.dashboards.diagnostics.enabled=${PG_CONSOLE_DASH_DIAGNOSTICS:true}

# Individual page toggles
pg-console.dashboards.diagnostics.pipeline-risk=${PG_CONSOLE_DASH_PIPELINE_RISK:true}
pg-console.dashboards.diagnostics.toast-bloat=${PG_CONSOLE_DASH_TOAST_BLOAT:true}
pg-console.dashboards.diagnostics.index-redundancy=${PG_CONSOLE_DASH_INDEX_REDUNDANCY:true}
pg-console.dashboards.diagnostics.statistical-freshness=${PG_CONSOLE_DASH_STAT_FRESHNESS:true}
pg-console.dashboards.diagnostics.write-read-ratio=${PG_CONSOLE_DASH_WRITE_READ:true}
pg-console.dashboards.diagnostics.hot-efficiency=${PG_CONSOLE_DASH_HOT_EFFICIENCY:true}
pg-console.dashboards.diagnostics.correlation=${PG_CONSOLE_DASH_CORRELATION:true}
pg-console.dashboards.diagnostics.live-charts=${PG_CONSOLE_DASH_LIVE_CHARTS:true}
pg-console.dashboards.diagnostics.xid-wraparound=${PG_CONSOLE_DASH_XID_WRAPAROUND:true}

# Queue/pipeline table patterns (comma-separated)
pg-console.diagnostics.queue-patterns=${PG_CONSOLE_QUEUE_PATTERNS:*_queue,*_event,*_job,*_task}

# Thresholds
pg-console.diagnostics.queue-stale-hours=${PG_CONSOLE_QUEUE_STALE_HOURS:24}
pg-console.diagnostics.toast-bloat-warn-percent=${PG_CONSOLE_TOAST_BLOAT_WARN:30}
pg-console.diagnostics.hot-efficiency-warn-percent=${PG_CONSOLE_HOT_WARN:50}
pg-console.diagnostics.xid-warn-percent=${PG_CONSOLE_XID_WARN:50}
pg-console.diagnostics.xid-critical-percent=${PG_CONSOLE_XID_CRITICAL:75}
```

New Dashboard Routes:

- `/diagnostics` - Enhanced diagnostics overview
- `/diagnostics/pipeline-risk` - Queue/pipeline table monitoring
- `/diagnostics/toast-bloat` - TOAST table bloat analysis
- `/diagnostics/index-redundancy` - Duplicate and missing index detection
- `/diagnostics/statistical-freshness` - Table statistics staleness
- `/diagnostics/write-read-ratio` - Workload pattern analysis
- `/diagnostics/hot-efficiency` - HOT update efficiency
- `/diagnostics/correlation` - Column correlation statistics
- `/diagnostics/live-charts` - Interactive real-time charts
- `/diagnostics/xid-wraparound` - Transaction ID wraparound monitoring

Configuration Specification (example)
Required

instances[0].id

instances[0].jdbcUrl

instances[0].username

instances[0].password (or secret ref)

Feature flags

features.historySampling=true|false

features.adminActions=false|true

features.multiInstance=true|false

Sampling

sampling.intervalSeconds=60

sampling.topNQueries=50

sampling.retentionDays=7

UI

ui.defaultTheme=dark|light

Initial Dashboard Pages (v1)

/ Overview

/activity Activity

/locks Locks & Blocking

/slow-queries Slow Queries

/slow-queries/{queryId} Query Detail

/about About / Diagnostics

Risks and Mitigations

pg_stat_statements not enabled → show guided setup instructions + degrade gracefully

Permissions → document required grants/roles; run with least privilege

Performance overhead → cache results briefly; bound queries (LIMIT); avoid heavy catalog scans

Security → hard disable admin actions unless explicitly configured

Definition of Done for “Initial Setup + Initial Monitoring Dashboards”

One configured instance

Overview/Activity/Locks/Slow Queries pages

Sorting + filtering server-side

Hover full query works

Dark mode toggle works and persists

No Node/npm pipeline

Deployment as single runnable artifact
