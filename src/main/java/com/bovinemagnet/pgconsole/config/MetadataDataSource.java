package com.bovinemagnet.pgconsole.config;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * CDI qualifier for the metadata datasource.
 * <p>
 * Use this qualifier to inject the datasource used for storing pgconsole
 * metadata (history, bookmarks, audit logs, alerts, etc.). The actual
 * datasource resolved depends on configuration:
 * <ul>
 *   <li>If {@code pg-console.metadata.datasource} is empty, uses the default datasource</li>
 *   <li>If set to "metadata", uses the dedicated metadata datasource</li>
 *   <li>If set to an instance name, uses that instance's datasource</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * &#64;Inject
 * &#64;MetadataDataSource
 * DataSource metadataDs;
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see MetadataDataSourceProvider
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD, TYPE})
public @interface MetadataDataSource {
}
