# pg-console

A PostgreSQL Insight Dashboard - lightweight, self-hosted web dashboard for PostgreSQL operational insight and performance monitoring.

## Features

- **Slow/Expensive Queries**: Monitor query performance using `pg_stat_statements`
- **Current Activity**: View active connections, blocking queries, and long-running transactions
- **Connections & Waits**: Analyze connection status and wait events
- **Table/Index Usage**: Track table statistics and identify bloat indicators
- **Server-Side SVG Sparklines**: Visualizations rendered on the server
- **Interactive UI**: 
  - Sortable columns powered by htmx
  - Hover to view full SQL queries
  - Real-time auto-refresh capability

## Technology Stack

- **Gradle** + **Java 21** + **Quarkus 3.16.3**
- **Qute** templating engine
- **htmx** for dynamic interactions (no Node.js toolchain required)
- **Bootstrap 5** for CSS styling
- **PostgreSQL JDBC** for database connectivity
- Minimal vanilla JavaScript

## Prerequisites

- Java 21 or higher
- PostgreSQL 12+ with `pg_stat_statements` extension enabled
- Docker and Docker Compose (optional, for local testing)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/bovinemagnet/pg-console.git
cd pg-console
```

### 2. Start PostgreSQL (with Docker Compose)

```bash
docker-compose up -d
```

This will start a PostgreSQL instance with:
- Port: 5432
- User: postgres
- Password: postgres
- Database: postgres
- Extensions: pg_stat_statements (pre-enabled)
- Sample data loaded

### 3. Configure Database Connection

The application uses the following default configuration (can be overridden with environment variables):

```properties
POSTGRES_URL=jdbc:postgresql://localhost:5432/postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

### 4. Build and Run

Using Gradle wrapper:

```bash
./gradlew quarkusDev
```

Or build a production JAR:

```bash
./gradlew clean build
java -jar build/quarkus-app/quarkus-run.jar
```

### 5. Access the Dashboard

Open your browser and navigate to:

```
http://localhost:8080
```

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
   CREATE EXTENSION pg_stat_statements;
   ```

## Configuration

Edit `src/main/resources/application.properties` or set environment variables:

```properties
# PostgreSQL connection
quarkus.datasource.jdbc.url=${POSTGRES_URL:jdbc:postgresql://localhost:5432/postgres}
quarkus.datasource.username=${POSTGRES_USER:postgres}
quarkus.datasource.password=${POSTGRES_PASSWORD:postgres}

# HTTP server
quarkus.http.port=8080
quarkus.http.host=0.0.0.0
```

## Usage

### Dashboard Views

1. **Home** (`/`): Overview and navigation to all features
2. **Slow Queries** (`/slow-queries`): 
   - View queries sorted by total time, mean time, calls, etc.
   - Click column headers to sort
   - Hover over truncated queries to see full SQL
3. **Activity** (`/activity`): 
   - Monitor active connections and queries
   - Identify blocking queries (highlighted in red)
   - View wait events and query states
4. **Tables** (`/tables`): 
   - Analyze sequential vs index scans
   - Monitor table bloat percentage
   - Track INSERT/UPDATE/DELETE operations

## Development

### Build

```bash
./gradlew clean build
```

### Run in Dev Mode

```bash
./gradlew quarkusDev
```

Dev mode includes:
- Live reload
- Dev UI at http://localhost:8080/q/dev

### Run Tests

```bash
./gradlew test
```

## Production Deployment

1. Build the production artifact:
   ```bash
   ./gradlew clean build
   ```

2. Run the JAR:
   ```bash
   java -jar build/quarkus-app/quarkus-run.jar
   ```

3. Or build a native executable (requires GraalVM):
   ```bash
   ./gradlew build -Dquarkus.package.type=native
   ./build/pg-console-1.0.0-SNAPSHOT-runner
   ```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Credits

Built with:
- [Quarkus](https://quarkus.io/)
- [htmx](https://htmx.org/)
- [Bootstrap](https://getbootstrap.com/)
- [PostgreSQL](https://www.postgresql.org/)
