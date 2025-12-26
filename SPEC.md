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

### Phase 10 — Security & Compliance Monitoring (Planned)
- [ ] **Role & Permission Auditing**
  - Database role hierarchy visualisation
  - Permission matrix (role × object × privilege)
  - Superuser and elevated privilege warnings
  - Role membership changes over time
  - Password policy compliance checks
- [ ] **Connection Security Analysis**
  - SSL/TLS connection status for all clients
  - Authentication method breakdown (md5, scram-sha-256, cert, etc.)
  - Failed login attempt tracking
  - Connection source IP analysis with geolocation
  - Suspicious connection pattern detection
- [ ] **Data Access Patterns**
  - Table access frequency by role
  - Sensitive table monitoring (PII indicators)
  - After-hours access alerts
  - Unusual query pattern detection
  - Row-level security policy overview
- [ ] **Compliance Dashboards**
  - GDPR data subject access tracking
  - SOC 2 relevant metrics (access controls, encryption)
  - Audit trail completeness scoring
  - Data retention policy monitoring
  - Encryption-at-rest verification
- [ ] **Security Recommendations**
  - Weak password detection (if accessible)
  - Overly permissive role warnings
  - Public schema exposure alerts
  - Extension security review
  - pg_hba.conf analysis recommendations

### Phase 11 — Intelligent Insights & Automation (Planned)
- [ ] **Anomaly Detection**
  - Statistical baseline learning for key metrics
  - Automatic anomaly alerts (connections, query times, errors)
  - Seasonal pattern recognition (daily, weekly cycles)
  - Correlation detection between metrics
  - Root cause suggestion for anomalies
- [ ] **Predictive Analytics**
  - Storage growth forecasting
  - Connection pool exhaustion prediction
  - Query performance degradation trends
  - Maintenance window recommendations
  - Capacity planning projections
- [ ] **Automated Recommendations Engine**
  - Priority-ranked action items dashboard
  - One-click fix suggestions with SQL preview
  - Impact estimation for recommendations
  - Recommendation history and effectiveness tracking
  - Configuration tuning suggestions (work_mem, shared_buffers, etc.)
- [ ] **Natural Language Queries**
  - "Show me slow queries from yesterday"
  - "Which tables are growing fastest?"
  - "Why is the database slow right now?"
  - Query intent parsing to dashboard navigation
  - Plain English explanations for technical metrics
- [ ] **Runbook Integration**
  - Predefined incident response playbooks
  - Step-by-step guided troubleshooting
  - Automated diagnostic data collection
  - Integration with ticketing systems (Jira, ServiceNow)
  - Post-incident report generation
- [ ] **Scheduled Maintenance Automation**
  - Intelligent vacuum scheduling based on table activity
  - Automatic index rebuild recommendations
  - Off-peak maintenance window detection
  - Pre/post maintenance metric comparison
  - Rollback capabilities for configuration changes

### Phase 12 — Schema Comparison & Migration (Planned)
- [ ] **Cross-Instance Schema Comparison**
  - Compare schemas between different PostgreSQL instances (e.g., dev vs prod)
  - Source and destination instance selection from configured instances
  - Schema/namespace selection for comparison scope
  - Side-by-side visual diff view with colour-coded changes
- [ ] **Comprehensive Object Comparison**
  - Tables: columns, data types, nullability, defaults, identity/serial
  - Indexes: type, columns, unique, partial, expression indexes
  - Constraints: primary keys, foreign keys, check constraints, unique constraints
  - Views and materialised views with definition comparison
  - Functions and procedures with body/signature comparison
  - Triggers with timing, events, and function references
  - Sequences with start, increment, min/max values
  - Custom types (enums, composites, domains)
  - Extensions with version comparison
- [ ] **Flexible Filtering System**
  - Table name pattern exclusions (e.g., `zz_*`, `temp_*`, `_backup`)
  - Schema exclusions (e.g., `pg_catalog`, `information_schema`)
  - Object type filters (include/exclude specific object types)
  - Configurable filter patterns in text input fields
  - Regex support for advanced pattern matching
  - Filter presets for common exclusion patterns
- [ ] **Difference Categorisation**
  - Missing objects (exists in source, not in destination)
  - Extra objects (exists in destination, not in source)
  - Modified objects (exists in both but differs)
  - Severity levels: Breaking (drops), Warning (alters), Info (additions)
  - Summary statistics (X tables differ, Y indexes missing, etc.)
- [ ] **DDL Migration Script Generation**
  - Generate CREATE statements for missing objects
  - Generate ALTER statements for modified objects
  - Optional DROP statements for extra objects (with safety warnings)
  - Dependency-aware script ordering (create referenced tables first)
  - Transaction wrapping options (single transaction vs individual statements)
  - Script preview before download
  - Copy to clipboard functionality
- [ ] **Comparison Profiles**
  - Save named profiles with source, destination, and filter configurations
  - Quick re-run of saved comparisons
  - Profile sharing via export/import (JSON format)
  - Default profile per instance pair
  - Profile history with last run timestamp and result summary
- [ ] **Comparison History & Audit**
  - Log of all schema comparisons performed
  - Comparison result snapshots for trend analysis
  - Schema drift detection over time
  - Scheduled comparison runs with email notifications
- [ ] **Interactive Diff Viewer**
  - Expandable/collapsible object tree
  - Inline SQL definition diffs (unified or split view)
  - Search and filter within diff results
  - Export diff report as HTML, PDF, or Markdown
  - Shareable comparison result URLs

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

Phase 10 — Security & Compliance Monitoring (Planned)

Deliverables:

Role and permission auditing dashboard

Connection security analysis (SSL, auth methods, failed logins)

Data access pattern monitoring with sensitive table tracking

Compliance dashboards (GDPR, SOC 2 metrics)

Security recommendations engine

Acceptance Criteria:

Role hierarchy visualised with permission matrix

SSL/TLS status visible for all connections

Sensitive table access tracked with alerts

Compliance scoring visible with actionable recommendations

Security warnings displayed for common misconfigurations

Phase 11 — Intelligent Insights & Automation (Planned)

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

Phase 12 — Schema Comparison & Migration (Planned)

Deliverables:

Cross-instance schema comparison with source/destination selector

Comprehensive object comparison (tables, indexes, constraints, views, functions, triggers, sequences, types, extensions)

Flexible filtering system with pattern exclusions (e.g., `zz_*`, `temp_*`)

Visual diff viewer with colour-coded changes and expandable object tree

DDL migration script generation with dependency ordering

Saveable comparison profiles for repeated use

Comparison history and schema drift detection

Acceptance Criteria:

Users can select source and destination instances from configured list

All major database objects compared with detailed attribute-level diffs

Table exclusion patterns configurable via text input (supports wildcards and regex)

Side-by-side diff view shows additions (green), deletions (red), modifications (yellow)

Generated DDL scripts are syntactically correct and dependency-ordered

Profiles persist and can be re-run with one click

Comparison results exportable as HTML, PDF, or Markdown

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
