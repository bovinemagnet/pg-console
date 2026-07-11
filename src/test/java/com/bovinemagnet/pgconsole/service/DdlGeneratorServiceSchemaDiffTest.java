package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.MigrationScript;
import com.bovinemagnet.pgconsole.model.ObjectDifference;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import com.bovinemagnet.pgconsole.model.TableSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers DDL-generation correctness: generated columns (M41) and no
 * double-wrapping of missing sequence/enum definitions (M38).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("DdlGeneratorService schema-diff DDL")
class DdlGeneratorServiceSchemaDiffTest {

    private final DdlGeneratorService service = new DdlGeneratorService();

    @Test
    @DisplayName("A STORED generated column round-trips as GENERATED ALWAYS AS (...) STORED")
    void generatedColumnRoundTrips() {
        TableSchema table = new TableSchema();
        table.setSchemaName("public");
        table.setTableName("invoice");

        TableSchema.ColumnDefinition total = new TableSchema.ColumnDefinition();
        total.setColumnName("total");
        total.setDataType("numeric");
        total.setNullable(true);
        total.setGenerated(true);
        total.setGenerationExpression("STORED");
        total.setDefaultValue("(qty * price)");
        table.getColumns().add(total);

        String ddl = service.generateFullTableDdl(table, "public");

        assertThat(ddl).contains("GENERATED ALWAYS AS ((qty * price)) STORED");
        assertThat(ddl).doesNotContain("DEFAULT (qty * price)");
    }

    @Test
    @DisplayName("A missing sequence emits one CREATE SEQUENCE, not a double-wrapped one")
    void missingSequenceNotDoubleWrapped() {
        String storedDdl = "CREATE SEQUENCE \"public\".\"order_seq\" INCREMENT BY 1 MINVALUE 1 START WITH 1;";

        SchemaComparisonResult result = new SchemaComparisonResult();
        result.setSuccess(true);
        result.setDestinationSchema("public");
        result.addDifference(ObjectDifference.builder()
                .objectType(ObjectDifference.ObjectType.SEQUENCE)
                .objectName("order_seq")
                .differenceType(ObjectDifference.DifferenceType.MISSING)
                .severity(ObjectDifference.Severity.INFO)
                .sourceDefinition(storedDdl)
                .build());

        MigrationScript script = service.generateMigrationScript(
                result, MigrationScript.WrapOption.INDIVIDUAL_STATEMENTS, false);

        assertThat(script.getStatements()).hasSize(1);
        String ddl = script.getStatements().get(0).getDdl();
        assertThat(ddl).isEqualTo(storedDdl);
        // Exactly one CREATE SEQUENCE keyword, i.e. not nested.
        assertThat(ddl.split("CREATE SEQUENCE", -1).length - 1).isEqualTo(1);
    }
}
