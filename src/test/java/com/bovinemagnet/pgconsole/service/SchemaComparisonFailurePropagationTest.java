package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ComparisonFilter;
import com.bovinemagnet.pgconsole.model.MigrationScript;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that a schema-extraction failure surfaces as {@code success=false}
 * (not an empty-schema diff), and that no migration DDL — least of all DROPs —
 * is generated from such a comparison.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("Schema comparison failure propagation")
class SchemaComparisonFailurePropagationTest {

    @Test
    @DisplayName("Extraction failure yields success=false, not an empty diff")
    void extractionFailureYieldsFailure() {
        SchemaComparisonService service = new SchemaComparisonService();
        service.extractorService = mock(SchemaExtractorService.class);
        service.ddlGeneratorService = new DdlGeneratorService();

        when(service.extractorService.extractTables(anyString(), anyString()))
            .thenThrow(new SchemaExtractionException("source timeout", new RuntimeException()));

        ComparisonFilter filter = new ComparisonFilter();
        SchemaComparisonResult result = service.compare("src", "dst", "public", "public", filter);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getDifferences()).isEmpty();
    }

    @Test
    @DisplayName("No migration statements are generated from a failed comparison")
    void noDdlFromFailedComparison() {
        SchemaComparisonResult failed = new SchemaComparisonResult();
        failed.setSuccess(false);
        failed.setErrorMessage("extraction incomplete");

        MigrationScript script = new DdlGeneratorService()
            .generateMigrationScript(failed, MigrationScript.WrapOption.SINGLE_TRANSACTION, true);

        assertThat(script.getStatements()).isEmpty();
    }
}
