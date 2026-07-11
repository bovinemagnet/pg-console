package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.TableSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that generated DDL quotes every identifier, so mixed-case /
 * reserved-word names round-trip to valid DDL and hostile identifiers
 * cannot be laundered into executable SQL.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("DdlGeneratorService identifier quoting")
class DdlGeneratorServiceQuotingTest {

    private final DdlGeneratorService service = new DdlGeneratorService();

    @Test
    @DisplayName("Mixed-case table and column names are double-quoted")
    void mixedCaseIdentifiersAreQuoted() {
        TableSchema table = new TableSchema();
        table.setSchemaName("public");
        table.setTableName("Order");

        TableSchema.ColumnDefinition col = new TableSchema.ColumnDefinition();
        col.setColumnName("userId");
        col.setDataType("integer");
        col.setNullable(false);
        table.getColumns().add(col);

        String ddl = service.generateFullTableDdl(table, "public");

        assertThat(ddl).contains("CREATE TABLE \"public\".\"Order\"");
        assertThat(ddl).contains("\"userId\" integer");
    }

    @Test
    @DisplayName("An identifier containing a quote and semicolon is escaped, not executable")
    void hostileIdentifierIsEscaped() {
        TableSchema table = new TableSchema();
        table.setSchemaName("public");
        table.setTableName("x\"; DROP TABLE audit;--");

        TableSchema.ColumnDefinition col = new TableSchema.ColumnDefinition();
        col.setColumnName("id");
        col.setDataType("integer");
        col.setNullable(true);
        table.getColumns().add(col);

        String ddl = service.generateFullTableDdl(table, "public");

        // The embedded quote must be doubled and the whole name wrapped, so the
        // ';' lives inside a quoted identifier rather than terminating a statement.
        assertThat(ddl).contains("\"x\"\"; DROP TABLE audit;--\"");
        assertThat(ddl).doesNotContain("TABLE \"public\".x\"; DROP");
    }

    @Test
    @DisplayName("Enum type DDL quotes the type name")
    void enumTypeNameIsQuoted() {
        com.bovinemagnet.pgconsole.model.TypeSchema type =
                new com.bovinemagnet.pgconsole.model.TypeSchema();
        type.setTypeName("Status");
        type.setSchemaName("public");
        type.getEnumLabels().add("ACTIVE");
        type.getEnumLabels().add("INACTIVE");

        String ddl = service.generateEnumTypeDdl(type, "public");

        assertThat(ddl).contains("CREATE TYPE \"public\".\"Status\" AS ENUM ('ACTIVE', 'INACTIVE')");
    }
}
