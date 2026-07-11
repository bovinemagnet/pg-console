package com.bovinemagnet.pgconsole.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that the {@code --database} filter is bound as a JDBC parameter
 * rather than concatenated into the SQL text, so a value containing a
 * quote cannot alter the statement.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("ResetStatsCommand SQL injection hardening")
class ResetStatsCommandTest {

    @Test
    @DisplayName("Database name is bound as a parameter, never concatenated")
    void databaseNameIsBoundAsParameter() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);

        String hostile = "x'); DROP TABLE audit; --";
        ResetStatsCommand.resetForDatabase(conn, hostile);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("datname = ?").doesNotContain(hostile);
        verify(ps).setString(1, hostile);
        verify(ps).execute();
    }
}
