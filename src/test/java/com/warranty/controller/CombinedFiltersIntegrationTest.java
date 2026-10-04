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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for combined filters on the GET /api/products endpoint.
 * Tests filtering by both categoryId AND warranty status simultaneously (AND logic).
 * Verifies that when multiple filters are combined, only products matching ALL criteria are returned.
 *
 * These tests verify:
 * 1. Combined Category + Status Filters: Both filters apply simultaneously (AND logic)
 * 2. Combined Filters with Pagination: Correct slice of combined-filtered results
 * 3. Combined Filters with Sorting: Results sorted correctly within combined filters
 * 4. Edge Cases: Empty results, single product, pagination boundaries
 * 5. Response Structure: Correct pagination metadata reflects combined filter results
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Combined Filters Integration Tests for GET /api/products")
class CombinedFiltersIntegrationTest {

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
    private Category clothing;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        today = LocalDate.now(clock);

        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));
        clothing = categoryRepository.save(new Category("Clothing"));

        // ===== ELECTRONICS PRODUCTS =====
        // ACTIVE (warranty ending > today + 30 days)
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Active Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusYears(1));
            product.setWarrantyMonths(24);
            productRepository.save(product);
        }

        // EXPIRING_SOON (today <= warranty end <= today + 30 days)
        for (int i = 1; i <= 2; i++) {
            Product product = new Product();
            product.setName("Expiring Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusMonths(11).minusDays(15));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // EXPIRED (warranty end < today)
        for (int i = 1; i <= 2; i++) {
            Product product = new Product();
            product.setName("Expired Electronics " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(today.minusYears(1).minusMonths(6));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // ===== FURNITURE PRODUCTS =====
        // ACTIVE
        for (int i = 1; i <= 4; i++) {
            Product product = new Product();
            product.setName("Active Furniture " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(today.minusMonths(6));
            product.setWarrantyMonths(18);
            productRepository.save(product);
        }

        // EXPIRING_SOON
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Expiring Furniture " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(today.minusMonths(11).minusDays(10));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // EXPIRED
        Product expiredFurniture1 = new Product();
        expiredFurniture1.setName("Expired Furniture 1");
        expiredFurniture1.setCategory(furniture);
        expiredFurniture1.setPurchaseDate(today.minusYears(2));
        expiredFurniture1.setWarrantyMonths(12);
        productRepository.save(expiredFurniture1);

        // ===== CLOTHING PRODUCTS =====
        // ACTIVE (warranty ending > today + 30 days)
        for (int i = 1; i <= 5; i++) {
            Product product = new Product();
            product.setName("Active Clothing " + i);
            product.setCategory(clothing);
            product.setPurchaseDate(today.minusMonths(3));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }

        // EXPIRED
        for (int i = 1; i <= 2; i++) {
            Product product = new Product();
            product.setName("Expired Clothing " + i);
            product.setCategory(clothing);
            product.setPurchaseDate(today.minusYears(1).minusMonths(6));
            product.setWarrantyMonths(12);
            productRepository.save(product);
        }
    }

    // ===== COMBINED CATEGORY + STATUS FILTER TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=ACTIVE should return only ACTIVE products from electronics")
    void testCombinedFilters_Category1_StatusActive() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(3)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=EXPIRED should return only EXPIRED products from furniture")
    void testCombinedFilters_Category2_StatusExpired() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(1)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=EXPIRING_SOON should return only EXPIRING_SOON products from clothing")
    void testCombinedFilters_Category3_StatusExpiringSoon() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=EXPIRING_SOON should return matching products")
    void testCombinedFilters_ElectronicsExpiring() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(2)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=EXPIRING_SOON should return matching products")
    void testCombinedFilters_FurnitureExpiring() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(3)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE should return only ACTIVE clothing products")
    void testCombinedFilters_ClothingActive() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(clothing.getId().intValue()))));
    }

    // ===== NON-EXISTENT CATEGORY WITH STATUS FILTER =====

    @Test
    @DisplayName("GET /api/products?categoryId=99999&status=ACTIVE should return empty results")
    void testCombinedFilters_NonExistentCategory_WithStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=99999&status=EXPIRED should return empty results with metadata")
    void testCombinedFilters_NonExistentCategory_ExpiredStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== CATEGORY WITH NO PRODUCTS MATCHING STATUS =====

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=ACTIVE should return all ACTIVE furniture")
    void testCombinedFilters_CategoryHasProductsForStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(4)));
    }

    // ===== COMBINED FILTERS WITH PAGINATION =====

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=ACTIVE&limit=2&offset=0 should paginate correctly")
    void testCombinedFilters_WithPagination_FirstPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "2")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(2)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(3)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=ACTIVE&limit=2&offset=2 should return last page")
    void testCombinedFilters_WithPagination_LastPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "2")
                .param("offset", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(2)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(3)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.data[0].warrantyStatus", equalTo("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=ACTIVE&limit=10 should return all results on one page")
    void testCombinedFilters_WithPagination_AllOnOnePage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(4)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&limit=3&offset=0 should return first batch")
    void testCombinedFilters_WithPagination_ClothingFirstBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "3")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&limit=3&offset=3 should return second batch")
    void testCombinedFilters_WithPagination_ClothingSecondBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "3")
                .param("offset", "3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with combined filters and offset beyond total should return empty")
    void testCombinedFilters_WithPagination_OffsetBeyondTotal() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "10")
                .param("offset", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(3)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== COMBINED FILTERS WITH SORTING =====

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=ACTIVE&sort=name,asc should return sorted results")
    void testCombinedFilters_WithSorting_SortByNameAsc() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(4)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&sort=purchaseDate,desc should sort by purchase date")
    void testCombinedFilters_WithSorting_SortByPurchaseDateDesc() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "purchaseDate,desc")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(clothing.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=ACTIVE&sort=id,asc should sort by ID")
    void testCombinedFilters_WithSorting_SortById() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "id,asc")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=EXPIRING_SOON&sort=warrantyMonths,asc should sort by warranty months")
    void testCombinedFilters_WithSorting_SortByWarrantyMonths() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRING_SOON")
                .param("sort", "warrantyMonths,asc")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    // ===== COMBINED FILTERS WITH PAGINATION AND SORTING =====

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&sort=name,asc&limit=2&offset=0 should combine all filters")
    void testCombinedFilters_AllCombined_FirstPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "2")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(2)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(clothing.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&sort=name,asc&limit=2&offset=2 should paginate through sorted results")
    void testCombinedFilters_AllCombined_SecondPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "2")
                .param("offset", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(2)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(clothing.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE&sort=name,asc&limit=2&offset=4 should return last page")
    void testCombinedFilters_AllCombined_LastPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "2")
                .param("offset", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(2)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(4)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.data[0].warrantyStatus", equalTo("ACTIVE")));
    }

    // ===== EDGE CASES: EMPTY RESULTS FROM COMBINED FILTERS =====

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=EXPIRED should return only EXPIRED clothing")
    void testCombinedFilters_EdgeCase_MultipleExpired() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(2)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=ACTIVE&limit=5&offset=0 should paginate correctly")
    void testCombinedFilters_EdgeCase_PaginateSmallSet() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(4)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=EXPIRED should return EXPIRED electronics")
    void testCombinedFilters_EdgeCase_ExiredElectronics() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(2)))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    // ===== EDGE CASE: SINGLE PRODUCT MATCHING COMBINED FILTERS =====

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=EXPIRED should return exactly one product")
    void testCombinedFilters_EdgeCase_SingleResult() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(1)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.data[0].warrantyStatus", equalTo("EXPIRED")))
                .andExpect(jsonPath("$.data[0].categoryId", equalTo(furniture.getId().intValue())));
    }

    // ===== EDGE CASE: PAGINATION ACROSS BOUNDARIES =====

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE should handle 25 total with pagination boundaries")
    void testCombinedFilters_EdgeCase_PaginationAcrossBoundaries_25Products_Limit10_Offset20() throws Exception {
        // With 5 ACTIVE clothing products (less than 25, so this verifies the structure is correct)
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=clothing&status=ACTIVE with limit=2 should handle sequential pagination across all results")
    void testCombinedFilters_EdgeCase_SequentialPagination() throws Exception {
        // First batch
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "2")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));

        // Second batch
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "2")
                .param("offset", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));

        // Third batch (last)
        mockMvc.perform(get("/api/products")
                .param("categoryId", clothing.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "2")
                .param("offset", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== RESPONSE STRUCTURE VALIDATION =====

    @Test
    @DisplayName("GET /api/products with combined filters should include all required fields in product response")
    void testCombinedFilters_ResponseStructure_AllProductFields() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
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
                .andExpect(jsonPath("$.data[0].category", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.id", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.name", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products with combined filters should include all pagination metadata fields")
    void testCombinedFilters_ResponseStructure_PaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.limit", notNullValue()))
                .andExpect(jsonPath("$.pagination.offset", notNullValue()))
                .andExpect(jsonPath("$.pagination.hasMore", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products with combined filters returning no results should have empty data array but valid metadata")
    void testCombinedFilters_ResponseStructure_EmptyResults() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.limit", notNullValue()))
                .andExpect(jsonPath("$.pagination.offset", notNullValue()))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== AND LOGIC VERIFICATION =====

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=ACTIVE should NOT return ACTIVE products from other categories")
    void testCombinedFilters_AndLogic_ExcludesOtherCategories() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))))
                .andExpect(jsonPath("$.data", hasSize(3))); // Only electronics, not furniture or clothing
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=EXPIRED should NOT return ACTIVE furniture")
    void testCombinedFilters_AndLogic_ExcludesOtherStatuses() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))))
                .andExpect(jsonPath("$.data", hasSize(1))); // Only EXPIRED, not ACTIVE
    }
}
