package com.bovinemagnet.pgconsole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that user-supplied database names are validated against the
 * server's database list before being spliced into a JDBC URL, closing
 * the connection-property injection hole (socketFactory, loggerFile, etc.).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("CrossDatabaseConnectionService database-name allowlisting")
class CrossDatabaseConnectionServiceTest {

    private CrossDatabaseConnectionService service;

    @BeforeEach
    void setUp() throws SQLException {
        service = new CrossDatabaseConnectionService();
        service.dataSourceManager = mock(DataSourceManager.class);

        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);

        when(service.dataSourceManager.getDataSource("default")).thenReturn(ds);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        // listDatabases() sees exactly two databases
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("datname")).thenReturn("postgres", "appdb");
    }

    @Test
    @DisplayName("JDBC property injection payload is rejected before connecting")
    void connectionPropertyPayloadIsRejected() {
        String payload = "postgres?socketFactory=evil.Clazz&loggerFile=/tmp/pwn";
        assertThatThrownBy(() -> service.getConnectionToDatabase("default", payload))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("Unknown or inaccessible database");
    }

    @Test
    @DisplayName("Database name absent from pg_database is rejected")
    void unknownDatabaseIsRejected() {
        assertThatThrownBy(() -> service.getConnectionToDatabase("default", "nonexistent"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("Unknown or inaccessible database");
    }
}
