package com.warranty.exception;

/**
 * Exception thrown when business rule validation fails.
 * Indicates that input data or business logic constraints are violated.
 */
public class ValidationException extends RuntimeException {

    /**
     * Constructs a ValidationException with the specified message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a ValidationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
