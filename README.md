# pg-console

A lightweight, self-hosted web dashboard for PostgreSQL operational insight and performance monitoring.

## Features

### Monitoring Dashboards
- **Overview Dashboard** - Live widgets showing connections, active/blocked queries, cache hit ratio, database size, and top tables/indexes
- **Slow Queries** - Query performance from `pg_stat_statements` with sortable columns, hover tooltips, and detailed statistics
- **Activity Monitor** - Real-time view of current database connections, running queries, and wait events
- **Lock Analysis** - Blocking tree visualisation, lock contention detection, and idle-in-transaction highlighting
- **Wait Events** - Session wait state breakdown with type summaries and detailed event table
- **Table Statistics** - Table sizes, sequential vs index scans, bloat indicators, and tuple counts
- **Database Metrics** - Per-database statistics from `pg_stat_database` with comparison view and detailed breakdowns
- **Replication Dashboard** - Streaming replication monitoring, replica lag, replication slot status, and WAL configuration
- **Infrastructure** - Vacuum progress, background processes, checkpoint statistics, and storage insights

### Advanced Diagnostics
- **Query Fingerprinting** - Group similar queries by normalised structure for aggregated analysis
- **Explain Plan** - Generate EXPLAIN/ANALYZE/BUFFERS plans for queries directly from the Query Detail page
- **Incident Reports** - Capture point-in-time snapshots of database state for troubleshooting
- **Alerting** - Webhook notifications when thresholds are exceeded (connections, blocked queries, cache hit ratio)

### Query & Index Optimisation
- **Index Advisor** - Recommendations for missing indexes (high seq scan ratio), unused indexes, and duplicate indexes
- **Query Regression Detection** - Compare query performance across time windows, flag significant slowdowns with severity levels
- **Table Maintenance** - Vacuum/Analyse recommendations based on dead tuple ratios and bloat estimation
- **Statements Baselines** - Period-over-period query comparisons with top movers report

### User Experience
- **Dark Mode** - Toggle between light and dark themes (persisted in browser)
- **Auto-Refresh** - Configurable refresh intervals (Off/5s/10s/30s/60s)
- **Sortable Tables** - Click column headers to sort data
- **Query Tooltips** - Hover to view full SQL queries
- **pg_stat_statements Indicator** - Visual badge showing extension availability
- **SVG Sparklines** - Inline trend charts showing historical metrics (connections, queries, cache hit ratio)

### Multi-Instance Support
- **Instance Selector** - Monitor multiple PostgreSQL instances from one dashboard
- **Instance-aware History** - Historical metrics stored per instance

### Security & Admin Actions
- **HTTP Basic Authentication** - Optional security via Quarkus Elytron
- **Role-based Access** - Admin role required for dangerous actions
- **Cancel Query** - Cancel running queries (admin only)
- **Terminate Connection** - Force-terminate backend connections (admin only)
- **Confirmation Dialogs** - Dangerous actions require confirmation

### Data Export
- **CSV Export** - Download slow queries as CSV files
- **Incident Reports** - Point-in-time database state snapshots

### Enterprise Features
- **REST API** - Full JSON API at `/api/v1/*` for external integrations
- **Prometheus Metrics** - Metrics endpoint at `/q/metrics` for Grafana dashboards
- **Audit Logging** - Track admin actions (cancel/terminate queries, settings changes)
- **Query Bookmarks** - Bookmark slow queries with notes, tags, priority, and status
- **Instance Comparison** - Side-by-side comparison of metrics across instances
- **Scheduled Reports** - Automated daily/weekly email reports with top queries and recommendations

### Data Control & Schema Management
- **Logical Replication** - Publication browser, subscription status, replication origins, and statistics
- **Change Data Capture** - Table change activity tracking (INSERT/UPDATE/DELETE rates), high-churn table identification, WAL generation estimates
- **Data Lineage** - Foreign key relationship display, view dependencies, function dependencies
- **Event Triggers** - DDL event trigger monitoring (CREATE, ALTER, DROP events)
- **Table Partitioning** - Partition tree visualisation, size distribution analysis, orphan partition detection, imbalance warnings

## Technology Stack

- **Quarkus 3.16.3** + **Java 21**
- **Qute** templating engine (server-side rendering)
- **htmx** for dynamic interactions (no Node.js required)
- **Bootstrap 5** for CSS styling (CDN)
- **Plain JDBC** for database connectivity
- **AsciiDoc** + **Antora** for documentation

## Prerequisites

- Java 21 or higher
- PostgreSQL 12+ with `pg_stat_statements` extension enabled
- Docker and Docker Compose (optional, for local testing)

## Quick Start

### Using Docker Compose (Recommended for Development)

```bash
# Clone the repository
git clone https://github.com/bovinemagnet/pg-console.git
cd pg-console

# Start PostgreSQL with pg_stat_statements enabled
docker-compose up -d

# Run the application in development mode
./gradlew quarkusDev
```

Open http://localhost:8080 in your browser.

### Connecting to an Existing Database

```bash
# Set environment variables
export POSTGRES_URL=jdbc:postgresql://your-host:5432/your-database
export POSTGRES_USER=your-username
export POSTGRES_PASSWORD=your-password

# Run the application
./gradlew quarkusDev
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/postgres` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `postgres` |
| `PG_CONSOLE_DATABASES` | Comma-separated list of databases to monitor | (all databases) |
| `PG_CONSOLE_HISTORY_ENABLED` | Enable/disable history sampling | `true` |
| `PG_CONSOLE_HISTORY_INTERVAL` | Sampling interval in seconds | `60` |
| `PG_CONSOLE_HISTORY_RETENTION` | Days to retain history data | `7` |
| `PG_CONSOLE_HISTORY_TOP_QUERIES` | Number of top queries to sample | `50` |
| `PG_CONSOLE_INSTANCES` | Comma-separated list of instance names | `default` |
| `PG_CONSOLE_SECURITY_ENABLED` | Enable HTTP Basic authentication | `false` |
| `PG_CONSOLE_ALERTING_ENABLED` | Enable alerting | `false` |
| `PG_CONSOLE_WEBHOOK_URL` | Webhook URL for alerts | (none) |
| `PG_CONSOLE_ALERTING_COOLDOWN` | Seconds between alerts of same type | `300` |
| `PG_CONSOLE_ALERT_CONN_PERCENT` | Connection percentage threshold | `90` |
| `PG_CONSOLE_ALERT_BLOCKED` | Blocked queries threshold | `5` |
| `PG_CONSOLE_ALERT_CACHE_HIT` | Cache hit ratio threshold | `90` |
| `PG_CONSOLE_REPORTS_ENABLED` | Enable scheduled reports | `false` |
| `QUARKUS_MAILER_FROM` | Email sender address for reports | (required if reports enabled) |
| `QUARKUS_MAILER_HOST` | SMTP server host | `localhost` |
| `QUARKUS_MAILER_PORT` | SMTP server port | `25` |
| `PG_CONSOLE_METADATA_DATASOURCE` | Datasource for pgconsole metadata (empty=default, "metadata"=dedicated, or instance name) | (default datasource) |
| `PG_CONSOLE_METADATA_URL` | JDBC URL for dedicated metadata database | (none) |
| `PG_CONSOLE_METADATA_USER` | Username for metadata database | (none) |
| `PG_CONSOLE_METADATA_PASSWORD` | Password for metadata database | (none) |

### Database Filter

Limit which databases appear in the dashboard:

```bash
# Monitor all databases (default)
export PG_CONSOLE_DATABASES=

# Monitor a single database
export PG_CONSOLE_DATABASES=production_db

# Monitor multiple specific databases
export PG_CONSOLE_DATABASES=app_prod,app_staging,postgres
```

Or configure in `application.properties`:

```properties
pg-console.databases=production_db,staging_db
```

### History Sampling

The dashboard samples metrics periodically and stores them for trend visualisation. Sparkline charts show historical trends on the Overview dashboard and Query Detail pages.

```bash
# Disable history sampling
export PG_CONSOLE_HISTORY_ENABLED=false

# Sample every 30 seconds instead of 60
export PG_CONSOLE_HISTORY_INTERVAL=30

# Keep 14 days of history instead of 7
export PG_CONSOLE_HISTORY_RETENTION=14
```

History data is stored in a `pgconsole` schema created automatically via Flyway migrations. By default, this schema is created in the monitored database, but it can be stored separately using the metadata datasource configuration (see below).

### Metadata Datasource Separation

By default, pg-console stores its metadata (history, bookmarks, audit logs) in the same database being monitored. For production environments, you may want to store metadata separately to:

- Enable **read-only monitoring** of production databases
- Centralise metadata storage for multi-instance deployments
- Keep the monitored database clean of pgconsole schema

#### Configuration Options

**Option 1: Default (Backwards Compatible)**

Leave `PG_CONSOLE_METADATA_DATASOURCE` unset. Metadata is stored in the monitored database.

**Option 2: Dedicated Metadata Database**

```bash
# Configure monitored instance (can be read-only)
export POSTGRES_URL=jdbc:postgresql://prod-db:5432/postgres
export POSTGRES_USER=readonly_monitor
export POSTGRES_PASSWORD=secure_password

# Configure dedicated metadata database
export PG_CONSOLE_METADATA_DATASOURCE=metadata
export PG_CONSOLE_METADATA_URL=jdbc:postgresql://control-db:5432/pgconsole
export PG_CONSOLE_METADATA_USER=pgconsole_admin
export PG_CONSOLE_METADATA_PASSWORD=metadata_password
```

**Option 3: Use Another Instance's Datasource**

```bash
# Monitor multiple instances
export PG_CONSOLE_INSTANCES=production,staging

# Store metadata in the staging instance
export PG_CONSOLE_METADATA_DATASOURCE=staging
```

#### Initialising the Schema

When using a dedicated metadata database, initialise the schema using the CLI:

```bash
# Initialise schema on metadata database
java -jar pg-console.jar init-schema --metadata

# Dry-run to see what migrations would run
java -jar pg-console.jar init-schema --metadata --dry-run
```

### Multi-Instance Configuration

Monitor multiple PostgreSQL instances from a single dashboard:

```properties
# application.properties or environment variables

# List of instance names
pg-console.instances=default,production,staging

# Display names for instances (optional)
pg-console.instance.production.display-name=Production DB
pg-console.instance.staging.display-name=Staging DB

# Named datasources for additional instances
quarkus.datasource.production.db-kind=postgresql
quarkus.datasource.production.jdbc.url=${POSTGRES_PRODUCTION_URL}
quarkus.datasource.production.username=${POSTGRES_PRODUCTION_USER}
quarkus.datasource.production.password=${POSTGRES_PRODUCTION_PASSWORD}

quarkus.datasource.staging.db-kind=postgresql
quarkus.datasource.staging.jdbc.url=${POSTGRES_STAGING_URL}
quarkus.datasource.staging.username=${POSTGRES_STAGING_USER}
quarkus.datasource.staging.password=${POSTGRES_STAGING_PASSWORD}
```

When multiple instances are configured, an instance selector appears in the navigation bar.

### Security Configuration

Enable HTTP Basic authentication to protect the dashboard:

```bash
# Enable security
export PG_CONSOLE_SECURITY_ENABLED=true
```

Users and roles are configured in properties files:

**`users.properties`:**
```properties
admin=admin
viewer=viewer
```

**`roles.properties`:**
```properties
admin=admin,viewer
viewer=viewer
```

The `admin` role is required for dangerous actions like cancelling queries or terminating connections. Update the default credentials before deploying to production.

## PostgreSQL Setup

### Enable pg_stat_statements

The `pg_stat_statements` extension is required for slow query monitoring:

1. Add to `postgresql.conf`:
   ```
   shared_preload_libraries = 'pg_stat_statements'
   pg_stat_statements.track = all
   ```

2. Restart PostgreSQL

3. Create the extension in your database:
   ```sql
   CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
   ```

### Database Permissions

The connecting user needs access to PostgreSQL system views:

```sql
-- Minimum required permissions
GRANT pg_read_all_stats TO your_user;

-- Or for pg_stat_statements specifically
GRANT SELECT ON pg_stat_statements TO your_user;
```

## Dashboard Pages

| Path | Description |
|------|-------------|
| `/` | Overview dashboard with live metrics |
| `/slow-queries` | Query performance from pg_stat_statements |
| `/slow-queries?view=grouped` | Queries grouped by fingerprint |
| `/slow-queries/{id}` | Detailed query statistics with Explain Plan |
| `/activity` | Current database connections and queries |
| `/locks` | Lock contention and blocking tree |
| `/wait-events` | Session wait state breakdown |
| `/tables` | Table statistics and bloat indicators |
| `/databases` | Per-database metrics comparison |
| `/databases/{name}` | Detailed metrics for a single database |
| `/index-advisor` | Index recommendations (missing, unused, duplicates) |
| `/query-regressions` | Query performance regression detection |
| `/table-maintenance` | Vacuum and analyse recommendations |
| `/statements-management` | Query baselines and top movers report |
| `/replication` | Streaming replication status and lag monitoring |
| `/infrastructure` | Vacuum progress, background processes, and storage |
| `/comparison` | Side-by-side instance comparison |
| `/bookmarks` | Query bookmarks and annotations |
| `/audit-log` | Admin action audit log |
| `/schema-docs` | Schema documentation generation ([docs](docs/modules/user-guide/pages/schema-docs.adoc)) |
| `/database-diff` | Cross-database schema comparison ([docs](docs/modules/user-guide/pages/database-diff.adoc)) |
| `/logical-replication` | Publications, subscriptions, and replication origins |
| `/cdc` | Change data capture dashboard with table activity |
| `/data-lineage` | Foreign keys, views, and function dependencies |
| `/partitions` | Table partitioning insights and health |
| `/about` | Application and PostgreSQL server info |
| `/incident-report/export` | Download point-in-time incident report |
| `/api/v1/*` | REST API for metrics (JSON) |
| `/q/metrics` | Prometheus metrics endpoint |

## Building

### Development Mode (with live reload)

```bash
./gradlew quarkusDev
```

Dev mode includes:
- Live reload on code changes
- Dev UI at http://localhost:8080/q/dev

### Production Build

```bash
# Build the JAR
./gradlew clean build

# Run the production JAR
java -jar build/quarkus-app/quarkus-run.jar
```

### Native Executable (requires GraalVM)

```bash
./gradlew build -Dquarkus.package.type=native
./build/pg-console-1.0.0-runner
```

### Run Tests

```bash
./gradlew test
```

## Visual Indicators

- **Cache Hit Ratio** - Green (≥90%), Yellow (<90%) - indicates buffer cache effectiveness
- **Commit Ratio** - Percentage of committed vs rolled back transactions
- **Deadlocks** - Red highlighting when deadlocks detected
- **Blocking Queries** - Red highlighting in activity view
- **pg_stat_statements Badge** - Green icon indicates the extension is available

## Documentation

Comprehensive documentation is available in [AsciiDoc](https://asciidoc.org/) format, built with [Antora](https://antora.org/).

### Documentation Modules

| Module | Description | Location |
|--------|-------------|----------|
| **User Guide** | Installation, configuration, and dashboard usage | [docs/modules/user-guide](docs/modules/user-guide/pages/index.adoc) |
| **Admin Guide** | Deployment, security, alerting, and multi-instance setup | [docs/modules/admin-guide](docs/modules/admin-guide/pages/index.adoc) |
| **Developer Guide** | Architecture, database schema, and testing | [docs/modules/developer-guide](docs/modules/developer-guide/pages/index.adoc) |
| **API Reference** | REST API endpoints and response formats | [docs/modules/api-reference](docs/modules/api-reference/pages/index.adoc) |

### Key Documentation Pages

- [Installation Guide](docs/modules/user-guide/pages/installation.adoc) - Getting started with pg-console
- [Configuration](docs/modules/user-guide/pages/configuration.adoc) - Environment variables and settings
- [Using Dashboards](docs/modules/user-guide/pages/dashboards.adoc) - Dashboard features, keyboard shortcuts, and custom dashboards
- [Advanced Diagnostics](docs/modules/user-guide/pages/diagnostics.adoc) - Diagnostic tools and analysis
- [Deployment Guide](docs/modules/admin-guide/pages/deployment.adoc) - Production deployment options
- [Alerting Configuration](docs/modules/admin-guide/pages/alerting.adoc) - Webhook and email notifications
- [Multi-Instance Setup](docs/modules/admin-guide/pages/multi-instance.adoc) - Monitoring multiple PostgreSQL instances
- [REST API Endpoints](docs/modules/api-reference/pages/endpoints.adoc) - Complete API reference
- [Architecture Overview](docs/modules/developer-guide/pages/architecture.adoc) - System design and components
- [Schema Documentation](docs/modules/user-guide/pages/schema-docs.adoc) - Generate data dictionaries from database schemas
- [Database Diff](docs/modules/user-guide/pages/database-diff.adoc) - Compare schemas across databases and instances
- [Runbooks](docs/modules/user-guide/pages/runbooks.adoc) - Automated diagnostic workflows
- [XID Wraparound](docs/modules/user-guide/pages/xid-wraparound.adoc) - Transaction ID monitoring and prevention
- [Troubleshooting](docs/modules/user-guide/pages/troubleshooting.adoc) - Common issues and solutions
- [Security Configuration](docs/modules/admin-guide/pages/security.adoc) - Authentication and authorisation
- [CLI Reference](docs/modules/admin-guide/pages/cli-reference.adoc) - Command-line options and init-schema
- [Schema-Free Mode](docs/modules/admin-guide/pages/schema-free-mode.adoc) - Read-only monitoring without pgconsole schema
- [Database Schema](docs/modules/developer-guide/pages/database-schema.adoc) - pgconsole internal schema design
- [Testing Guide](docs/modules/developer-guide/pages/testing.adoc) - Running and writing tests

### Building Documentation

Generate the documentation site locally:

```bash
./gradlew antora
```

The generated site is available at `build/site/index.html`.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Licence

This project is licensed under the MIT Licence.

## Author

Paul Snow

## Credits

Built with:
- [Quarkus](https://quarkus.io/)
- [htmx](https://htmx.org/)
- [Bootstrap](https://getbootstrap.com/)
- [PostgreSQL](https://www.postgresql.org/)
