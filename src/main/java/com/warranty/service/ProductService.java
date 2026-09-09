package com.warranty.service;

import com.warranty.dto.request.CreateProductRequest;
import com.warranty.dto.request.UpdateProductRequest;
import com.warranty.dto.response.CategoryResponse;
import com.warranty.dto.response.ProductResponse;
import com.warranty.entity.Category;
import com.warranty.entity.Product;
import com.warranty.exception.CategoryNotFoundException;
import com.warranty.exception.ProductNotFoundException;
import com.warranty.exception.ValidationException;
import com.warranty.repository.CategoryRepository;
import com.warranty.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class that encapsulates product-related business logic, validation, and warranty calculations.
 * Handles creation, retrieval, updating, and deletion of products with warranty tracking.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Creates a new product with validation and warranty calculation.
     *
     * Validation:
     * - Product name must not be empty
     * - Category must exist
     * - Purchase date must not be in the future
     * - Warranty months must be greater than 0
     *
     * @param request the CreateProductRequest containing product details
     * @return ProductResponse with calculated warrantyEndDate and warrantyStatus
     * @throws ValidationException if any business rule is violated
     * @throws CategoryNotFoundException if the referenced category does not exist
     */
    public ProductResponse createProduct(CreateProductRequest request) {
        // Validate product name (additional defensive check beyond @NotBlank)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty");
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + request.getCategoryId() + " not found"
                ));

        // Validate purchase date is not in the future
        if (request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Purchase date cannot be in the future");
        }

        // Validate warranty months is positive (additional defensive check beyond @Positive)
        if (request.getWarrantyMonths() == null || request.getWarrantyMonths() <= 0) {
            throw new ValidationException("Warranty months must be greater than 0");
        }

        // Create and populate product entity
        Product product = new Product();
        product.setName(request.getName());
        product.setPurchaseDate(request.getPurchaseDate());
        product.setWarrantyMonths(request.getWarrantyMonths());
        product.setCategory(category);

        // Save to repository
        Product savedProduct = productRepository.save(product);

        // Convert to response with calculated fields
        return convertToResponse(savedProduct);
    }

    /**
     * Updates an existing product with validation and warranty recalculation.
     *
     * Validation:
     * - Product must exist
     * - All request fields validated same as createProduct
     *
     * @param id the product ID to update
     * @param request the UpdateProductRequest containing updated product details
     * @return ProductResponse with updated calculated fields
     * @throws ProductNotFoundException if product does not exist
     * @throws ValidationException if any business rule is violated
     * @throws CategoryNotFoundException if the referenced category does not exist
     */
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        // Validate product exists
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + id + " not found"
                ));

        // Validate product name (additional defensive check beyond @NotBlank)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty");
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + request.getCategoryId() + " not found"
                ));

        // Validate purchase date is not in the future
        if (request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Purchase date cannot be in the future");
        }

        // Validate warranty months is positive (additional defensive check beyond @Positive)
        if (request.getWarrantyMonths() == null || request.getWarrantyMonths() <= 0) {
            throw new ValidationException("Warranty months must be greater than 0");
        }

        // Update product fields
        product.setName(request.getName());
        product.setPurchaseDate(request.getPurchaseDate());
        product.setWarrantyMonths(request.getWarrantyMonths());
        product.setCategory(category);

        // Save updated product
        Product updatedProduct = productRepository.save(product);

        // Convert to response with recalculated fields
        return convertToResponse(updatedProduct);
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param id the product ID
     * @return ProductResponse with calculated warrantyEndDate and warrantyStatus
     * @throws ProductNotFoundException if product does not exist
     */
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + id + " not found"
                ));

        return convertToResponse(product);
    }

    /**
     * Retrieves all products from the repository.
     *
     * @return List of ProductResponse objects with calculated fields, or empty list if no products exist
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a product by its ID.
     *
     * @param id the product ID to delete
     * @throws ProductNotFoundException if product does not exist
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product with ID " + id + " not found"
                ));

        productRepository.delete(product);
    }

    /**
     * Retrieves products whose warranty expires within a specified number of days from today.
     *
     * Filtering logic:
     * - Returns products where: today <= warrantyEndDate <= today + days
     * - Sorted by warrantyEndDate in ascending order
     *
     * @param days the number of days from today (default 30 if null)
     * @return List of ProductResponse objects expiring within the specified timeframe
     * @throws ValidationException if days parameter is negative
     */
    public List<ProductResponse> getExpiringProducts(Integer days) {
        // Use default of 30 days if null
        int daysToCheck = (days == null) ? 30 : days;

        // Validate days is non-negative
        if (daysToCheck < 0) {
            throw new ValidationException("Days parameter must be non-negative");
        }

        LocalDate today = LocalDate.now();
        LocalDate endDateTo = today.plusDays(daysToCheck);

        // Query products expiring within the range
        List<Product> expiringProducts = productRepository.findProductsExpiringWithin(endDateTo)
                .stream()
                .filter(product -> {
                    LocalDate warrantyEndDate = product.getPurchaseDate()
                            .plusMonths(product.getWarrantyMonths());
                    // Filter: today <= warrantyEndDate <= endDateTo
                    return !warrantyEndDate.isBefore(today) && !warrantyEndDate.isAfter(endDateTo);
                })
                .sorted((p1, p2) -> {
                    LocalDate date1 = p1.getPurchaseDate().plusMonths(p1.getWarrantyMonths());
                    LocalDate date2 = p2.getPurchaseDate().plusMonths(p2.getWarrantyMonths());
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toList());

        // Convert to response DTOs
        return expiringProducts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Product entity to ProductResponse DTO with calculated fields.
     *
     * Calculated fields:
     * - warrantyEndDate: purchaseDate + warrantyMonths
     * - warrantyStatus: calculated using WarrantyCalculator based on current date
     *
     * @param product the Product entity to convert
     * @return ProductResponse with all fields including calculated values
     */
    private ProductResponse convertToResponse(Product product) {
        // Calculate warranty end date
        LocalDate warrantyEndDate = product.getPurchaseDate()
                .plusMonths(product.getWarrantyMonths());

        // Calculate warranty status using WarrantyCalculator
        String warrantyStatus = WarrantyCalculator.calculateStatus(warrantyEndDate, LocalDate.now())
                .toString();

        // Build category response
        Category category = product.getCategory();
        CategoryResponse categoryResponse = new CategoryResponse(
                category.getId(),
                category.getName()
        );

        // Build and return product response
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getPurchaseDate(),
                product.getWarrantyMonths(),
                warrantyEndDate,
                warrantyStatus,
                categoryResponse
        );
    }
}
