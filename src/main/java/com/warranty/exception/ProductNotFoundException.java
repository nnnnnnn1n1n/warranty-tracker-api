package com.warranty.exception;

/**
 * Exception thrown when a product is not found.
 * Indicates that a requested product by ID does not exist in the database.
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Constructs a ProductNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public ProductNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a ProductNotFoundException with the specified product ID.
     * Automatically generates message: "Product with ID {productId} not found"
     *
     * @param productId the ID of the product that was not found
     */
    public ProductNotFoundException(Long productId) {
        super("Product with ID " + productId + " not found");
    }

    /**
     * Constructs a ProductNotFoundException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
