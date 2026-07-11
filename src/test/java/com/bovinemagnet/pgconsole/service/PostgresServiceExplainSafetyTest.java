package com.bovinemagnet.pgconsole.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the EXPLAIN safety guard, in particular that writable CTEs
 * (WITH ... INSERT/UPDATE/DELETE/MERGE) are rejected so EXPLAIN ANALYZE
 * cannot be used to execute DML.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("PostgresService.isExplainSafe")
class PostgresServiceExplainSafetyTest {

    private final PostgresService service = new PostgresService();

    @Test
    @DisplayName("Plain SELECT is allowed")
    void selectIsAllowed() {
        assertThat(service.isExplainSafe("SELECT * FROM ORDERS")).isTrue();
    }

    @Test
    @DisplayName("Read-only CTE is allowed")
    void readOnlyCteIsAllowed() {
        assertThat(service.isExplainSafe("WITH X AS (SELECT 1) SELECT * FROM X")).isTrue();
    }

    @Test
    @DisplayName("VALUES is allowed")
    void valuesIsAllowed() {
        assertThat(service.isExplainSafe("VALUES (1), (2)")).isTrue();
    }

    @Test
    @DisplayName("Writable CTE with DELETE is rejected")
    void writableCteDeleteIsRejected() {
        assertThat(service.isExplainSafe(
            "WITH D AS (DELETE FROM ORDERS RETURNING 1) SELECT * FROM D")).isFalse();
    }

    @Test
    @DisplayName("Writable CTE with UPDATE is rejected")
    void writableCteUpdateIsRejected() {
        assertThat(service.isExplainSafe(
            "WITH U AS (UPDATE T SET A = 1 RETURNING *) SELECT 1")).isFalse();
    }

    @Test
    @DisplayName("Writable CTE with INSERT is rejected")
    void writableCteInsertIsRejected() {
        assertThat(service.isExplainSafe(
            "WITH I AS (INSERT INTO T VALUES (1) RETURNING *) SELECT 1")).isFalse();
    }

    @Test
    @DisplayName("Writable CTE with MERGE is rejected")
    void writableCteMergeIsRejected() {
        assertThat(service.isExplainSafe(
            "WITH M AS (MERGE INTO T USING S ON T.ID = S.ID WHEN MATCHED THEN DELETE) SELECT 1")).isFalse();
    }

    @Test
    @DisplayName("SELECT containing a column name that embeds a DML keyword is allowed")
    void keywordAsSubstringIsAllowed() {
        assertThat(service.isExplainSafe("SELECT DELETED_AT, UPDATED_BY FROM AUDIT_LOG")).isTrue();
    }

    @Test
    @DisplayName("Top-level DML is rejected")
    void topLevelDmlIsRejected() {
        assertThat(service.isExplainSafe("DELETE FROM ORDERS")).isFalse();
        assertThat(service.isExplainSafe("UPDATE T SET A = 1")).isFalse();
        assertThat(service.isExplainSafe("INSERT INTO T VALUES (1)")).isFalse();
        assertThat(service.isExplainSafe("DROP TABLE T")).isFalse();
    }
}
