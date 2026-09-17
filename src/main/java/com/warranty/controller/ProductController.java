package com.warranty.controller;

import com.warranty.dto.request.CreateProductRequest;
import com.warranty.dto.request.UpdateProductRequest;
import com.warranty.dto.response.ProductResponse;
import com.warranty.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

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
     * Retrieve all products.
     *
     * @return ResponseEntity with HTTP 200 status and a list of ProductResponse objects
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.status(HttpStatus.OK).body(products);
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

    /**
     * Retrieve products expiring within a specified number of days.
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
}
