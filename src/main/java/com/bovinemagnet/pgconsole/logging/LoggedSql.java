package com.bovinemagnet.pgconsole.logging;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor binding annotation for SQL query logging.
 * <p>
 * Apply this annotation to methods or classes to enable automatic
 * SQL execution logging with timing and row counts.
 * <p>
 * Example usage:
 * <pre>
 * &#64;LoggedSql
 * public List&lt;User&gt; findAllUsers() {
 *     // SQL operations will be automatically logged
 * }
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see SqlLoggingInterceptor
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LoggedSql {
}
