package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.TableSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for schema extraction and DDL generation against a real
 * local PostgreSQL ({@code pg_console} database). Skipped automatically when the
 * database is not reachable, so it never breaks a Docker-less CI run.
 * <p>
 * Validates the composite-FK (M37) and generated-column (M41) fixes and drives
 * the schema-requalification (M40) and FK-ordering (M42) work: the acid test is
 * that generated DDL actually applies against a fresh destination schema.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Tag("integration")
@DisplayName("Schema diff against live PostgreSQL")
class SchemaDiffLiveDbTest {

    private static final String URL = "jdbc:postgresql://127.0.0.1:5432/pg_console";
    private static final String USER = "postgres";
    private static final String PASS = "postgres";

    private static final String SRC = "m_diff_src";
    private static final String DST = "m_diff_dst";

    private final SchemaExtractorService extractor = new SchemaExtractorService();
    private final DdlGeneratorService generator = new DdlGeneratorService();

    private Connection conn;

    @BeforeAll
    static void checkReachable() {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            assumeTrue(c.isValid(3), "pg_console not reachable");
        } catch (SQLException e) {
            assumeTrue(false, "pg_console not reachable: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection(URL, USER, PASS);
        exec("DROP SCHEMA IF EXISTS " + SRC + " CASCADE");
        exec("DROP SCHEMA IF EXISTS " + DST + " CASCADE");
        exec("CREATE SCHEMA " + SRC);
        exec("CREATE SCHEMA " + DST);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null) {
            exec("DROP SCHEMA IF EXISTS " + SRC + " CASCADE");
            exec("DROP SCHEMA IF EXISTS " + DST + " CASCADE");
            conn.close();
        }
    }

    private void exec(String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private TableSchema tableNamed(List<TableSchema> tables, String name) {
        return tables.stream().filter(t -> t.getTableName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Composite foreign key extracts the correct, non-duplicated columns (M37)")
    void compositeForeignKeyColumns() throws SQLException {
        exec("CREATE TABLE " + SRC + ".parent (a int, b int, PRIMARY KEY (a, b))");
        exec("CREATE TABLE " + SRC + ".child (x int, y int, "
                + "CONSTRAINT fk_child FOREIGN KEY (x, y) REFERENCES " + SRC + ".parent (a, b))");

        List<TableSchema> tables = extractor.extractTables(conn, SRC);
        TableSchema child = tableNamed(tables, "child");

        assertThat(child.getForeignKeys()).hasSize(1);
        TableSchema.ForeignKeyDefinition fk = child.getForeignKeys().get(0);
        assertThat(fk.getColumns()).containsExactly("x", "y");
        assertThat(fk.getReferencedColumns()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("A generated column extracts and regenerates as GENERATED ... STORED (M41)")
    void generatedColumnRoundTrips() throws SQLException {
        exec("CREATE TABLE " + SRC + ".invoice ("
                + "qty int, price numeric, "
                + "total numeric GENERATED ALWAYS AS (qty * price) STORED)");

        List<TableSchema> tables = extractor.extractTables(conn, SRC);
        TableSchema invoice = tableNamed(tables, "invoice");

        String ddl = generator.generateFullTableDdl(invoice, DST);
        assertThat(ddl).contains("GENERATED ALWAYS AS");
        assertThat(ddl).contains("STORED");

        // The acid test: the generated DDL must actually apply in the dest schema.
        exec(ddl);
        boolean isGenerated = queryBoolean(
                "SELECT is_generated = 'ALWAYS' FROM information_schema.columns "
                + "WHERE table_schema = '" + DST + "' AND table_name = 'invoice' AND column_name = 'total'");
        assertThat(isGenerated).isTrue();
    }

    private boolean queryBoolean(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); var rs = st.executeQuery(sql)) {
            return rs.next() && rs.getBoolean(1);
        }
    }
}
