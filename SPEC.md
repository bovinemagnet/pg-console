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

### Phase 3 — Polishing + Safety + Multi-Instance ❌ NOT IMPLEMENTED
- [ ] Multi-instance selector
- [ ] Authentication integration
- [ ] Authorization for dangerous actions
- [ ] Cancel/Terminate query buttons
- [ ] Export: download CSV

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

Phase 4 — Advanced Diagnostics (optional, depending on appetite)

Ideas:

“Explain plan” integration (only on non-prod or with strict controls)

wait-event breakdown summary

“query fingerprint” grouping / similarity

alerting integration (email/webhook) based on thresholds

simple incident report snapshot export (HTML/PDF)

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
