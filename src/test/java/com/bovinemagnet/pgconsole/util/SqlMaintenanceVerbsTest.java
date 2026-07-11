package com.bovinemagnet.pgconsole.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the allowlist that constrains which SQL verbs a runbook step may
 * auto-execute. Only non-destructive maintenance verbs are permitted; any
 * DROP/TRUNCATE/pg_terminate_backend text is refused.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("SqlMaintenanceVerbs allowlist")
class SqlMaintenanceVerbsTest {

    @Test
    @DisplayName("VACUUM / ANALYZE / REINDEX are allowed")
    void maintenanceVerbsAllowed() {
        assertThat(SqlMaintenanceVerbs.isAllowed("VACUUM ANALYZE public.orders")).isTrue();
        assertThat(SqlMaintenanceVerbs.isAllowed("ANALYZE public.orders")).isTrue();
        assertThat(SqlMaintenanceVerbs.isAllowed("REINDEX TABLE public.orders")).isTrue();
        assertThat(SqlMaintenanceVerbs.isAllowed("  vacuum  freeze  t  ")).isTrue();
    }

    @Test
    @DisplayName("Destructive verbs are refused")
    void destructiveVerbsRefused() {
        assertThat(SqlMaintenanceVerbs.isAllowed("DROP TABLE audit")).isFalse();
        assertThat(SqlMaintenanceVerbs.isAllowed("TRUNCATE orders")).isFalse();
        assertThat(SqlMaintenanceVerbs.isAllowed("DELETE FROM orders")).isFalse();
        assertThat(SqlMaintenanceVerbs.isAllowed("SELECT pg_terminate_backend(123)")).isFalse();
    }

    @Test
    @DisplayName("A statement that merely embeds a verb mid-text is refused")
    void embeddedVerbRefused() {
        assertThat(SqlMaintenanceVerbs.isAllowed("SELECT 'vacuum'; DROP TABLE t")).isFalse();
    }

    @Test
    @DisplayName("Null / blank is refused")
    void nullRefused() {
        assertThat(SqlMaintenanceVerbs.isAllowed(null)).isFalse();
        assertThat(SqlMaintenanceVerbs.isAllowed("   ")).isFalse();
    }
}
