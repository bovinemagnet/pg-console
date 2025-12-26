# pg-console

A lightweight, self-hosted web dashboard for PostgreSQL operational insight and performance monitoring.

## Features

### Monitoring Dashboards
- **Overview Dashboard** - Live widgets showing connections, active/blocked queries, cache hit ratio, database size, and top tables/indexes
- **Slow Queries** - Query performance from `pg_stat_statements` with sortable columns, hover tooltips, and detailed statistics
- **Activity Monitor** - Real-time view of current database connections, running queries, and wait events
- **Lock Analysis** - Blocking tree visualisation, lock contention detection, and idle-in-transaction highlighting
- **Table Statistics** - Table sizes, sequential vs index scans, bloat indicators, and tuple counts
- **Database Metrics** - Per-database statistics from `pg_stat_database` with comparison view and detailed breakdowns

### User Experience
- **Dark Mode** - Toggle between light and dark themes (persisted in browser)
- **Auto-Refresh** - Configurable refresh intervals (Off/5s/10s/30s/60s)
- **Sortable Tables** - Click column headers to sort data
- **Query Tooltips** - Hover to view full SQL queries
- **pg_stat_statements Indicator** - Visual badge showing extension availability
- **SVG Sparklines** - Inline trend charts showing historical metrics (connections, queries, cache hit ratio)

## Technology Stack

- **Quarkus 3.16.3** + **Java 21**
- **Qute** templating engine (server-side rendering)
- **htmx** for dynamic interactions (no Node.js required)
- **Bootstrap 5** for CSS styling (CDN)
- **Plain JDBC** for database connectivity

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

History data is stored in a `pgconsole` schema created automatically via Flyway migrations.

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
| `/slow-queries/{id}` | Detailed query statistics with copy button |
| `/activity` | Current database connections and queries |
| `/locks` | Lock contention and blocking tree |
| `/tables` | Table statistics and bloat indicators |
| `/databases` | Per-database metrics comparison |
| `/databases/{name}` | Detailed metrics for a single database |
| `/about` | Application and PostgreSQL server info |

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
