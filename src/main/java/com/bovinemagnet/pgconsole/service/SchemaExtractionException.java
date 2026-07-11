package com.bovinemagnet.pgconsole.service;

/**
 * Thrown when schema extraction fails partway through.
 * <p>
 * Extraction failures must not be swallowed and reported as an empty schema:
 * a source database timeout would otherwise make every destination object
 * look EXTRA, and a migration with drops enabled would then emit a wall of
 * {@code DROP ... CASCADE} that destroys the destination. Propagating this
 * exception lets the comparison record {@code success=false} instead.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SchemaExtractionException extends RuntimeException {

    /**
     * Creates a new extraction exception.
     *
     * @param message description of what failed
     * @param cause the underlying SQL exception
     */
    public SchemaExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
