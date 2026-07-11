package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that excluding DROP statements also excludes the destructive
 * {@code ALTER TABLE ... DROP COLUMN/CONSTRAINT} statements (M39).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("MigrationScript DROP exclusion")
class MigrationScriptDropTest {

    private MigrationScript.MigrationStatement alterDropColumn() {
        return MigrationScript.MigrationStatement.builder()
                .ddl("ALTER TABLE \"public\".\"orders\" DROP COLUMN IF EXISTS \"qty\"")
                .objectType(ObjectDifference.ObjectType.COLUMN)
                .objectName("orders.qty")
                .severity(ObjectDifference.Severity.BREAKING)
                .build();
    }

    @Test
    @DisplayName("ALTER ... DROP COLUMN is classified as a DROP statement")
    void alterDropColumnIsDrop() {
        assertThat(alterDropColumn().isDropStatement()).isTrue();
    }

    @Test
    @DisplayName("With DROPs excluded, no ALTER ... DROP appears in the script")
    void alterDropExcludedWhenDropsOff() {
        MigrationScript script = new MigrationScript();
        script.setWrapOption(MigrationScript.WrapOption.INDIVIDUAL_STATEMENTS);
        script.setIncludeDropStatements(false);
        script.addStatement(alterDropColumn());

        assertThat(script.getFullScript()).doesNotContain("DROP COLUMN");
    }

    @Test
    @DisplayName("With DROPs included, the ALTER ... DROP is present")
    void alterDropIncludedWhenDropsOn() {
        MigrationScript script = new MigrationScript();
        script.setWrapOption(MigrationScript.WrapOption.INDIVIDUAL_STATEMENTS);
        script.setIncludeDropStatements(true);
        script.addStatement(alterDropColumn());

        assertThat(script.getFullScript()).contains("DROP COLUMN");
    }
}
