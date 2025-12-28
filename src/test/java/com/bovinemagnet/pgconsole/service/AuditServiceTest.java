package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditService}.
 * <p>
 * Tests audit logging functionality including log writing, retrieval,
 * and summary statistics.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

    @InjectMocks
    AuditService auditService;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Nested
    @DisplayName("log method")
    class LogMethodTests {

        @Test
        @DisplayName("Writes audit log entry to database")
        void writesAuditLogEntry() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            auditService.log(
                "default",
                "admin",
                AuditLog.Action.CANCEL_QUERY,
                "query",
                "12345",
                null,
                "192.168.1.1",
                true,
                null
            );

            verify(preparedStatement).setString(2, "default");
            verify(preparedStatement).setString(3, "admin");
            verify(preparedStatement).setString(4, "CANCEL_QUERY");
            verify(preparedStatement).setString(5, "query");
            verify(preparedStatement).setString(6, "12345");
            verify(preparedStatement).setString(8, "192.168.1.1");
            verify(preparedStatement).setBoolean(9, true);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("Serialises details map to JSON")
        void serialisesDetailsToJson() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            Map<String, Object> details = new HashMap<>();
            details.put("reason", "query timeout");
            details.put("duration", 300);

            auditService.log(
                "default",
                "admin",
                AuditLog.Action.CANCEL_QUERY,
                "query",
                "12345",
                details,
                "192.168.1.1",
                true,
                null
            );

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(preparedStatement).setString(eq(7), jsonCaptor.capture());

            String json = jsonCaptor.getValue();
            assertThat(json).contains("\"reason\"");
            assertThat(json).contains("\"query timeout\"");
            assertThat(json).contains("\"duration\"");
        }

        @Test
        @DisplayName("Handles null details gracefully")
        void handlesNullDetails() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            auditService.log(
                "default",
                "admin",
                AuditLog.Action.VIEW_SENSITIVE,
                "queries",
                null,
                null,
                "127.0.0.1",
                true,
                null
            );

            verify(preparedStatement).setNull(eq(7), anyInt());
        }

        @Test
        @DisplayName("Logs failure with error message")
        void logsFailureWithErrorMessage() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            auditService.log(
                "default",
                "admin",
                AuditLog.Action.TERMINATE_CONNECTION,
                "connection",
                "5678",
                null,
                "192.168.1.1",
                false,
                "Connection not found"
            );

            verify(preparedStatement).setBoolean(9, false);
            verify(preparedStatement).setString(10, "Connection not found");
        }

        @Test
        @DisplayName("Handles SQLException gracefully")
        void handlesSQLException() throws SQLException {
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection error"));

            // Should not throw - logs the error instead
            auditService.log(
                "default",
                "admin",
                AuditLog.Action.CANCEL_QUERY,
                "query",
                "12345",
                null,
                "192.168.1.1",
                true,
                null
            );

            // No exception should be thrown
        }
    }

    @Nested
    @DisplayName("Convenience logging methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("logSuccess calls log with success=true")
        void logSuccessCallsLogWithTrue() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            auditService.logSuccess(
                "default",
                "admin",
                AuditLog.Action.VIEW_SENSITIVE,
                "locks",
                null,
                "127.0.0.1"
            );

            verify(preparedStatement).setBoolean(9, true);
            verify(preparedStatement).setString(10, null);
        }

        @Test
        @DisplayName("logSuccess with details includes details")
        void logSuccessWithDetailsIncludesDetails() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            Map<String, Object> details = Map.of("count", 5);

            auditService.logSuccess(
                "default",
                "admin",
                AuditLog.Action.VIEW_SENSITIVE,
                "locks",
                null,
                details,
                "127.0.0.1"
            );

            verify(preparedStatement).setBoolean(9, true);
            verify(preparedStatement).setString(eq(7), contains("\"count\""));
        }

        @Test
        @DisplayName("logFailure calls log with success=false")
        void logFailureCallsLogWithFalse() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            auditService.logFailure(
                "default",
                "admin",
                AuditLog.Action.TERMINATE_CONNECTION,
                "connection",
                "1234",
                "127.0.0.1",
                "Permission denied"
            );

            verify(preparedStatement).setBoolean(9, false);
            verify(preparedStatement).setString(10, "Permission denied");
        }
    }

    @Nested
    @DisplayName("getLogs methods")
    class GetLogsTests {

        @Test
        @DisplayName("getRecentLogs returns list of audit entries")
        void getRecentLogsReturnsList() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            setupMockResultSetRow();

            List<AuditLog> logs = auditService.getRecentLogs(10);

            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getUsername()).isEqualTo("admin");
            assertThat(logs.get(0).getAction()).isEqualTo("CANCEL_QUERY");
        }

        @Test
        @DisplayName("getLogsForInstance filters by instance")
        void getLogsForInstanceFilters() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            auditService.getLogsForInstance("production", 10);

            ArgumentCaptor<String> instanceCaptor = ArgumentCaptor.forClass(String.class);
            verify(preparedStatement).setObject(eq(1), instanceCaptor.capture());
            assertThat(instanceCaptor.getValue()).isEqualTo("production");
        }

        @Test
        @DisplayName("getLogs with all filters applies all conditions")
        void getLogsWithAllFilters() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            auditService.getLogs("default", "CANCEL_QUERY", "admin", 50);

            // Should have 4 parameters: instance, action, username, limit
            verify(preparedStatement).setObject(1, "default");
            verify(preparedStatement).setObject(2, "CANCEL_QUERY");
            verify(preparedStatement).setObject(3, "admin");
            verify(preparedStatement).setObject(4, 50);
        }

        @Test
        @DisplayName("getLogs handles null filters")
        void getLogsHandlesNullFilters() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            auditService.getLogs(null, null, null, 10);

            // Only limit parameter should be set
            verify(preparedStatement).setObject(1, 10);
        }

        @Test
        @DisplayName("getLogs handles SQLException gracefully")
        void getLogsHandlesSQLException() throws SQLException {
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));

            List<AuditLog> logs = auditService.getLogs(null, null, null, 10);

            assertThat(logs).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSummary method")
    class GetSummaryTests {

        @Test
        @DisplayName("Returns summary statistics")
        void returnsSummaryStatistics() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("last_24h")).thenReturn(50);
            when(resultSet.getInt("last_7d")).thenReturn(200);
            when(resultSet.getInt("failures_24h")).thenReturn(2);
            when(resultSet.getInt("unique_users_24h")).thenReturn(5);

            AuditService.AuditSummary summary = auditService.getSummary();

            assertThat(summary.getActionsLast24h()).isEqualTo(50);
            assertThat(summary.getActionsLast7d()).isEqualTo(200);
            assertThat(summary.getFailuresLast24h()).isEqualTo(2);
            assertThat(summary.getUniqueUsersLast24h()).isEqualTo(5);
        }

        @Test
        @DisplayName("Returns empty summary on error")
        void returnsEmptySummaryOnError() throws SQLException {
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));

            AuditService.AuditSummary summary = auditService.getSummary();

            assertThat(summary.getActionsLast24h()).isZero();
            assertThat(summary.getActionsLast7d()).isZero();
        }
    }

    private void setupMockResultSetRow() throws SQLException {
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getTimestamp("timestamp")).thenReturn(Timestamp.from(Instant.now()));
        when(resultSet.getString("instance_id")).thenReturn("default");
        when(resultSet.getString("username")).thenReturn("admin");
        when(resultSet.getString("action")).thenReturn("CANCEL_QUERY");
        when(resultSet.getString("target_type")).thenReturn("query");
        when(resultSet.getString("target_id")).thenReturn("12345");
        when(resultSet.getString("details")).thenReturn(null);
        when(resultSet.getString("client_ip")).thenReturn("192.168.1.1");
        when(resultSet.getBoolean("success")).thenReturn(true);
        when(resultSet.getString("error_message")).thenReturn(null);
    }
}
