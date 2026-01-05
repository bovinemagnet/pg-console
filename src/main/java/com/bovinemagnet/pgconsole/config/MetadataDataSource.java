package com.bovinemagnet.pgconsole.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * CDI qualifier annotation for identifying the PostgreSQL datasource used for pgconsole metadata storage.
 * <p>
 * This qualifier enables CDI dependency injection of the datasource that stores pgconsole's internal
 * metadata, including historical metrics, query bookmarks, audit logs, and alert configurations. The
 * metadata schema (created by Flyway migrations) contains tables such as {@code query_stats_history},
 * {@code connection_stats_history}, and {@code bloat_history}.
 * <p>
 * The actual datasource resolved by this qualifier is determined by the {@code pg-console.metadata.datasource}
 * configuration property:
 * <ul>
 *   <li>If empty or not set - Uses the default datasource (metadata stored alongside monitored data)</li>
 *   <li>If set to "metadata" - Uses a dedicated metadata datasource (isolated metadata storage)</li>
 *   <li>If set to an instance name - Uses that specific instance's datasource (multi-instance scenario)</li>
 * </ul>
 * <p>
 * This annotation is used in conjunction with {@link MetadataDataSourceProvider}, which implements the
 * producer logic that resolves the correct datasource based on configuration.
 * <p>
 * Example usage in a repository class:
 * <pre>{@code
 * @ApplicationScoped
 * public class HistoryRepository {
 *     @Inject
 *     @MetadataDataSource
 *     DataSource metadataDs;
 *
 *     public void saveQueryHistory(QueryStats stats) {
 *         try (Connection conn = metadataDs.getConnection()) {
 *             // Insert into pgconsole.query_stats_history
 *         }
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Thread Safety:</strong> As a Jakarta CDI qualifier annotation, this type is inherently thread-safe.
 * The datasource instances it qualifies are managed by the CDI container and follow standard JDBC datasource
 * thread-safety guarantees.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see MetadataDataSourceProvider
 * @see jakarta.inject.Qualifier
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD, TYPE })
public @interface MetadataDataSource {}
