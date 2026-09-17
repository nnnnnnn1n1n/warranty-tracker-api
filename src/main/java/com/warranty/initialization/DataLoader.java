package com.warranty.initialization;

import com.warranty.dto.request.CreateProductRequest;
import com.warranty.service.CategoryService;
import com.warranty.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * DataLoader component that initializes the database with predefined categories
 * and sample products on application startup.
 * 
 * Implements ApplicationRunner to execute the run() method when the application
 * starts after all beans are initialized.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * Executes on application startup to load sample data.
     * Initializes the database with:
     * - Five predefined categories
     * - Three sample products representing all warranty statuses (ACTIVE, EXPIRING_SOON, EXPIRED)
     *
     * @param args application command line arguments (not used)
     * @throws Exception if an error occurs during data loading
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            loadCategories();
            loadSampleProducts();
        } catch (Exception e) {
            log.error("Error loading sample data", e);
        }
    }

    /**
     * Loads five predefined categories into the database.
     */
    private void loadCategories() {
        log.info("Loading sample categories...");
        categoryService.saveSampleCategories();
        log.info("Sample categories loaded successfully");
    }

    /**
     * Loads three sample products representing all warranty statuses:
     * - EXPIRED: Refrigerator (warranty expired 12 months ago)
     * - EXPIRING_SOON: Microwave (warranty expires in approximately 3 months)
     * - ACTIVE: Laptop (warranty expires in approximately 30 months)
     */
    private void loadSampleProducts() {
        log.info("Loading sample products...");

        LocalDate today = LocalDate.now();

        // EXPIRED Product: Warranty expired 12 months ago
        // PurchaseDate: 12 months ago, WarrantyMonths: 24
        // WarrantyEndDate: now().minusMonths(12) - EXPIRED
        CreateProductRequest refrigerator = new CreateProductRequest(
                "Refrigerator",
                1L, // Kitchen Appliances
                today.minusMonths(24),
                24
        );
        productService.createProduct(refrigerator);
        log.debug("Created EXPIRED product: Refrigerator");

        // EXPIRING_SOON Product: Warranty expires within 30 days
        // PurchaseDate: 27 months ago, WarrantyMonths: 30
        // WarrantyEndDate: now().plusMonths(3) - within 30-day window, EXPIRING_SOON
        CreateProductRequest microwave = new CreateProductRequest(
                "Microwave",
                1L, // Kitchen Appliances
                today.minusMonths(27),
                30
        );
        productService.createProduct(microwave);
        log.debug("Created EXPIRING_SOON product: Microwave");

        // ACTIVE Product: Warranty expires beyond 30 days
        // PurchaseDate: 6 months ago, WarrantyMonths: 36
        // WarrantyEndDate: now().plusMonths(30) - ACTIVE
        CreateProductRequest laptop = new CreateProductRequest(
                "Laptop",
                4L, // Electronics
                today.minusMonths(6),
                36
        );
        productService.createProduct(laptop);
        log.debug("Created ACTIVE product: Laptop");

        log.info("Sample products loaded successfully");
    }
}
