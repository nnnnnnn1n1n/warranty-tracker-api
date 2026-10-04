package com.warranty.controller;

import com.warranty.dto.request.CreateProductRequest;
import com.warranty.dto.request.UpdateProductRequest;
import com.warranty.dto.response.ProductResponse;
import com.warranty.dto.response.ProductListResponse;
import com.warranty.exception.ValidationException;
import com.warranty.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * REST Controller for Product endpoints.
 * Handles HTTP requests for CRUD operations on products and warranty status queries.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product.
     *
     * @param request the CreateProductRequest containing product details
     * @return ResponseEntity with HTTP 201 Created status and the created ProductResponse
     * @throws ValidationException if request data fails validation (handled by GlobalExceptionHandler)
     * @throws CategoryNotFoundException if the referenced category does not exist (handled by GlobalExceptionHandler)
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse createdProduct = productService.createProduct(request);
        URI location = URI.create("/api/products/" + createdProduct.getId());
        return ResponseEntity.created(location).body(createdProduct);
    }

    /**
     * Retrieve paginated list of products with optional filtering and sorting.
     * Supports limit/offset pagination, filtering by category and warranty status,
     * and sorting by product attributes.
     *
     * @param limit the maximum number of products to return (query parameter, default 20)
     * @param offset the number of products to skip (query parameter, default 0)
     * @param categoryId filter by category ID (query parameter, optional)
     * @param status filter by warranty status: ACTIVE, EXPIRING_SOON, or EXPIRED (query parameter, optional)
     * @param sort sort criteria in format "field,direction" (query parameter, default "id,asc")
     * @return ResponseEntity with HTTP 200 status and ProductListResponse containing paginated products and pagination metadata
     * @throws ValidationException if any parameters fail validation (handled by GlobalExceptionHandler)
     */
    @GetMapping
    public ResponseEntity<ProductListResponse> listProducts(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "id,asc") String sort) {
        
        // Validate limit > 0
        if (limit <= 0) {
            throw new ValidationException("Limit must be greater than zero");
        }

        // Validate offset >= 0
        if (offset < 0) {
            throw new ValidationException("Limit and offset must be non-negative integers");
        }

        // Validate status is null or one of {ACTIVE, EXPIRING_SOON, EXPIRED}
        if (status != null) {
            Set<String> validStatuses = Set.of("ACTIVE", "EXPIRING_SOON", "EXPIRED");
            if (!validStatuses.contains(status)) {
                throw new ValidationException("Status must be one of: ACTIVE, EXPIRING_SOON, EXPIRED");
            }
        }
        
        // Validate and parse sort parameter
        String sortField;
        String sortDirection;
        
        // Validate sort format: must be "field,direction"
        if (sort == null || !sort.contains(",")) {
            throw new ValidationException("Sort parameter must be in format 'field,direction' (e.g., 'name,asc')");
        }
        
        String[] sortParts = sort.split(",", 2);
        if (sortParts.length != 2 || sortParts[0].trim().isEmpty() || sortParts[1].trim().isEmpty()) {
            throw new ValidationException("Sort parameter must be in format 'field,direction' (e.g., 'name,asc')");
        }
        
        sortField = sortParts[0].trim();
        sortDirection = sortParts[1].trim();
        
        // Validate sort field is in whitelist
        Set<String> validSortFields = Set.of("id", "name", "purchaseDate", "warrantyEndDate", "warrantyMonths", "categoryId");
        if (!validSortFields.contains(sortField)) {
            throw new ValidationException("Invalid sort field: " + sortField + ". Supported fields are: id, name, purchaseDate, warrantyEndDate, warrantyMonths, categoryId");
        }
        
        // Validate sort direction
        Set<String> validDirections = Set.of("asc", "desc");
        if (!validDirections.contains(sortDirection.toLowerCase())) {
            throw new ValidationException("Sort direction must be either 'asc' or 'desc'");
        }
        
        // Normalize direction to lowercase for consistency
        sortDirection = sortDirection.toLowerCase();
        
        ProductListResponse response = productService.listProducts(
                categoryId,
                status,
                sortField,
                sortDirection,
                limit,
                offset
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Retrieve products expiring within a specified number of days.
     * This endpoint MUST be defined before the /{id} endpoint to prevent Spring from interpreting "expiring-soon" as an ID value.
     *
     * @param days the number of days from today (query parameter, optional, default 30)
     * @return ResponseEntity with HTTP 200 status and a list of ProductResponse objects
     * @throws ValidationException if days parameter is negative (handled by GlobalExceptionHandler)
     */
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<ProductResponse>> getExpiringProducts(@RequestParam(required = false) Integer days) {
        List<ProductResponse> expiringProducts = productService.getExpiringProducts(days);
        return ResponseEntity.status(HttpStatus.OK).body(expiringProducts);
    }

    /**
     * Retrieve a specific product by its ID.
     *
     * @param id the product ID (path variable)
     * @return ResponseEntity with HTTP 200 status and the ProductResponse object if found
     * @throws ProductNotFoundException if the product with the given ID does not exist (handled by GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.status(HttpStatus.OK).body(product);
    }

    /**
     * Update an existing product.
     *
     * @param id the product ID (path variable)
     * @param request the UpdateProductRequest containing updated product details
     * @return ResponseEntity with HTTP 200 status and the updated ProductResponse
     * @throws ProductNotFoundException if the product with the given ID does not exist (handled by GlobalExceptionHandler)
     * @throws ValidationException if request data fails validation (handled by GlobalExceptionHandler)
     * @throws CategoryNotFoundException if the referenced category does not exist (handled by GlobalExceptionHandler)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
    }

    /**
     * Delete a product by its ID.
     *
     * @param id the product ID (path variable)
     * @return ResponseEntity with HTTP 204 No Content status
     * @throws ProductNotFoundException if the product with the given ID does not exist (handled by GlobalExceptionHandler)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}