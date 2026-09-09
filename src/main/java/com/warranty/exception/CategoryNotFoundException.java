package com.warranty.exception;

/**
 * Exception thrown when a category is not found.
 * Indicates that a requested category by ID does not exist in the database.
 */
public class CategoryNotFoundException extends RuntimeException {

    /**
     * Constructs a CategoryNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public CategoryNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a CategoryNotFoundException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public CategoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
