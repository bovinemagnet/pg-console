# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Development mode with live reload
gradle21w quarkusDev

# Run tests
gradle21w test

# Build production JAR
gradle21w clean build

# Run production JAR
java -jar build/quarkus-app/quarkus-run.jar

# Build native executable (requires GraalVM)
gradle21w build -Dquarkus.package.type=native
```

## Development Environment

### Prerequisites
- Java 21+
- PostgreSQL 12+ with `pg_stat_statements` extension enabled
- Docker Compose (optional, for local PostgreSQL)

### Local PostgreSQL Setup
```bash
docker-compose up -d
```
Starts PostgreSQL on port 5432 with user `postgres`/`postgres` and sample data.

### Environment Variables
- `POSTGRES_URL` - JDBC URL (default: `jdbc:postgresql://localhost:5432/postgres`)
- `POSTGRES_USER` - Database user (default: `postgres`)
- `POSTGRES_PASSWORD` - Database password (default: `postgres`)
- `PG_CONSOLE_DATABASES` - Comma-separated list of database names to monitor (default: all non-template databases)
  - Example: `PG_CONSOLE_DATABASES=postgres,myapp_prod` - shows only these two databases
  - Example: `PG_CONSOLE_DATABASES=sim_prd_eplus` - shows only this database
  - Leave empty to show all databases
- `PG_CONSOLE_HISTORY_ENABLED` - Enable/disable history sampling (default: `true`)
- `PG_CONSOLE_HISTORY_INTERVAL` - Sampling interval in seconds (default: `60`)
- `PG_CONSOLE_HISTORY_RETENTION` - Days to retain history data (default: `7`)
- `PG_CONSOLE_HISTORY_TOP_QUERIES` - Number of top queries to sample (default: `50`)

## Architecture

### Technology Stack
- **Quarkus 3.16.3** - Java framework with REST and Qute templating
- **Qute** - Server-side HTML templating (no client-side rendering)
- **htmx** - Dynamic interactions without a JavaScript build pipeline
- **Bootstrap 5** - CSS styling (CDN)
- **Plain JDBC** - Direct SQL queries to PostgreSQL system views

### Package Structure
```
com.bovinemagnet.pgconsole/
├── model/        # DTOs (Activity, SlowQuery, TableStats, LockInfo, BlockingTree, OverviewStats, DatabaseInfo, DatabaseMetrics, *History)
├── repository/   # Data access (HistoryRepository)
├── resource/     # JAX-RS REST endpoints (DashboardResource)
└── service/      # Business logic (PostgresService, SparklineService, MetricsSamplerService)
```

### Templates
Located in `src/main/resources/templates/`:
- `base.html` - Layout template with navigation, dark mode toggle, auto-refresh dropdown
- `index.html` - Overview dashboard with live widgets
- `slowQueries.html` - Slow queries with sortable columns
- `queryDetail.html` - Individual query statistics with copy button
- `activity.html` - Current database activity
- `locks.html` - Lock contention and blocking tree
- `tables.html` - Table statistics and bloat indicators
- `databases.html` - Per-database metrics comparison
- `databaseDetail.html` - Detailed metrics for a single database
- `about.html` - Application and database information

### Key Design Decisions
1. **Server-side rendering**: All pages work without JavaScript; htmx adds progressive enhancement
2. **Plain SQL over ORM**: Direct queries to `pg_stat_statements`, `pg_stat_activity`, `pg_locks`
3. **SVG sparklines**: Generated server-side by `SparklineService` from historical data
4. **Allowlist-based sorting**: Column sort parameters validated against allowlist to prevent SQL injection
5. **Auto-refresh**: Configurable polling (Off/5s/10s/30s/60s) persisted in localStorage
6. **Dark mode**: Bootstrap `data-bs-theme` with localStorage persistence
7. **History sampling**: Scheduled job samples metrics and stores in `pgconsole` schema via Flyway migrations
8. **Retention cleanup**: Daily job purges old history data (configurable retention period)

### Dashboard Routes
- `/` - Overview dashboard with live widgets (connections, active/blocked queries, cache hit ratio, DB size, top tables/indexes)
- `/slow-queries` - Query performance from `pg_stat_statements` with sortable columns
- `/slow-queries/{queryId}` - Detailed query statistics with copy button
- `/activity` - Current database connections and queries
- `/locks` - Lock contention, blocking tree, and idle-in-transaction detection
- `/tables` - Table statistics and bloat indicators
- `/databases` - Per-database metrics comparison (all pg_stat_database columns)
- `/databases/{dbName}` - Detailed metrics for a single database
- `/about` - Application version and PostgreSQL server information
- `/api/sparkline` - SVG sparkline generation endpoint
