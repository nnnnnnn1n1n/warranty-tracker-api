package com.warranty.controller;

import com.warranty.entity.Category;
import com.warranty.entity.Product;
import com.warranty.repository.CategoryRepository;
import com.warranty.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for warranty status filtering of the GET /api/products endpoint.
 * Tests filtering by warranty status (ACTIVE, EXPIRING_SOON, EXPIRED) using a real database (H2)
 * and verifies that products are correctly filtered based on warranty end date and current date.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Warranty Status Filtering Integration Tests for GET /api/products")
class WarrantyStatusFilteringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private Clock clock;

    private Category electronics;
    private Category furniture;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        today = LocalDate.now(clock);

        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));

        // ACTIVE: warrantyEndDate > today + 30 days (9 products)
        for (int i = 1; i <= 5; i++) {
            Product product = new Product();
            product.setName("Active Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusYears(1));
            product.setWarrantyMonths(24);
            productRepository.save(product);
        }

        for (int i = 1; i <= 4; i++) {
            Product product = new Product();
            product.setName("Active Furniture " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(today.minusMonths(6));
            product.setWarrantyMonths(18);
            productRepository.save(product);
        }

        // EXPIRING_SOON: today <= warrantyEndDate <= today + 30 days (9 products)
        for (int i = 1; i <= 4; i++) {
            Product product = new Product();
            product.setName("Expiring Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusMonths(11).minusDays(15));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Expiring Furniture " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(today.minusMonths(11).minusDays(10));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // Boundary: warranty ends today
        Product boundaryToday = new Product();
        boundaryToday.setName("Boundary Today");
        boundaryToday.setCategory(electronics);
        boundaryToday.setPurchaseDate(today.minusMonths(12));
        boundaryToday.setWarrantyMonths(12);
        productRepository.save(boundaryToday);

        // Boundary: warranty ends in 30 days
        Product boundary30 = new Product();
        boundary30.setName("Boundary 30 Days");
        boundary30.setCategory(electronics);
        boundary30.setPurchaseDate(today.minusMonths(11).minusDays(30));
        boundary30.setWarrantyMonths(12);
        productRepository.save(boundary30);

        // EXPIRED: warrantyEndDate < today (6 products)
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Expired Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusYears(1).minusMonths(6));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        for (int i = 1; i <= 2; i++) {
            Product product = new Product();
            product.setName("Expired Furniture " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(today.minusYears(2));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // Boundary: warranty expired yesterday
        Product boundaryYesterday = new Product();
        boundaryYesterday.setName("Boundary Yesterday");
        boundaryYesterday.setCategory(electronics);
        boundaryYesterday.setPurchaseDate(today.minusMonths(12).minusDays(1));
        boundaryYesterday.setWarrantyMonths(12);
        productRepository.save(boundaryYesterday);
    }

    // ===== ACTIVE STATUS TESTS =====

    @Test
    @DisplayName("GET /api/products?status=ACTIVE should return only products with warranty ending > today + 30 days")
    void testStatusFilter_ACTIVE_ReturnsOnlyActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("GET /api/products?status=ACTIVE should not return EXPIRING_SOON or EXPIRED products")
    void testStatusFilter_ACTIVE_ExcludesOtherStatuses() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("EXPIRING_SOON"))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("EXPIRED"))));
    }

    // ===== EXPIRING_SOON STATUS TESTS =====

    @Test
    @DisplayName("GET /api/products?status=EXPIRING_SOON should return only products with warranty ending within 30 days")
    void testStatusFilter_EXPIRING_SOON_ReturnsOnlyExpiringProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9))) // 4 + 3 + 1 boundary today + 1 boundary 30 days
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRING_SOON should not return ACTIVE or EXPIRED products")
    void testStatusFilter_EXPIRING_SOON_ExcludesOtherStatuses() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("EXPIRED"))));
    }

    // ===== EXPIRED STATUS TESTS =====

    @Test
    @DisplayName("GET /api/products?status=EXPIRED should return only products with warranty ended before today")
    void testStatusFilter_EXPIRED_ReturnsOnlyExpiredProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6))) // 3 + 2 + 1 boundary yesterday
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(6)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRED should not return ACTIVE or EXPIRING_SOON products")
    void testStatusFilter_EXPIRED_ExcludesOtherStatuses() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", not(hasItem("EXPIRING_SOON"))));
    }

    // ===== BOUNDARY CONDITION TESTS =====

    @Test
    @DisplayName("GET /api/products?status=EXPIRING_SOON should include boundary products")
    void testStatusFilter_Boundary_WarrantyEndingToday() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRING_SOON should include product with warranty ending in 30 days")
    void testStatusFilter_Boundary_Warranty30Days() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRED should include product with warranty expired yesterday")
    void testStatusFilter_Boundary_WarrantyExpiredYesterday() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))));
    }

    // ===== PAGINATION WITH STATUS FILTER =====

    @Test
    @DisplayName("GET /api/products?status=ACTIVE&limit=3 should paginate correctly")
    void testStatusFilter_WithPagination_FirstPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("limit", "3")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(3)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRING_SOON&limit=5 should paginate correctly")
    void testStatusFilter_WithPagination_ExpiringPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRED&limit=10 should return all expired on one page")
    void testStatusFilter_WithPagination_AllExpiredOnPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(6)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== COMBINED FILTERS =====

    @Test
    @DisplayName("GET /api/products?status=ACTIVE&categoryId=X should apply both filters")
    void testStatusFilter_WithCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?status=EXPIRED&categoryId=furniture should filter both")
    void testStatusFilter_WithCategoryFilter_Expired() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("categoryId", furniture.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    // ===== RESPONSE STRUCTURE =====

    @Test
    @DisplayName("GET /api/products?status=ACTIVE response should include all required fields")
    void testStatusFilter_ResponseStructure() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id", notNullValue()))
                .andExpect(jsonPath("$.data[0].name", notNullValue()))
                .andExpect(jsonPath("$.data[0].categoryId", notNullValue()))
                .andExpect(jsonPath("$.data[0].purchaseDate", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyMonths", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyStatus", equalTo("ACTIVE")))
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.limit", notNullValue()))
                .andExpect(jsonPath("$.pagination.offset", notNullValue()))
                .andExpect(jsonPath("$.pagination.hasMore", notNullValue()));
    }

    // ===== CLOCK USAGE =====

    @Test
    @DisplayName("GET /api/products?status=ACTIVE should use application Clock for date calculations")
    void testStatusFilter_UsesClock() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(9)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))));
    }

    // ===== NO FILTER =====

    @Test
    @DisplayName("GET /api/products without status filter should return all products")
    void testStatusFilter_NoFilter_ReturnsAll() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(24))) // 9 + 9 + 6
                .andExpect(jsonPath("$.data[*].warrantyStatus", hasItems("ACTIVE", "EXPIRING_SOON", "EXPIRED")));
    }
}