package com.bovinemagnet.pgconsole.testutil;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 condition that skips tests when Docker is not available.
 * <p>
 * Use with {@code @ExtendWith(DockerAvailableCondition.class)} to skip
 * integration tests that require Docker/Testcontainers.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker is available");

    private static final ConditionEvaluationResult DISABLED =
        ConditionEvaluationResult.disabled("Docker is not available - skipping integration test");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            return PostgresTestContainer.isDockerAvailable() ? ENABLED : DISABLED;
        } catch (Throwable e) {
            // Any error checking Docker availability means Docker isn't usable
            return ConditionEvaluationResult.disabled(
                "Docker check failed: " + e.getMessage() + " - skipping integration test");
        }
    }
}
