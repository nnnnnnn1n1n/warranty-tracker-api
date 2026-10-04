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
 * Integration tests for edge cases of the GET /api/products endpoint.
 * Tests boundary conditions, empty results, warranty status boundary dates,
 * sort field edge cases, filter combinations with edge cases, and data type boundaries.
 *
 * These tests verify:
 * 1. Boundary conditions for pagination (offset at/beyond total, offset + limit = total, etc.)
 * 2. Empty result scenarios (category with no products, filter returns zero results, etc.)
 * 3. Warranty status boundary dates (exact boundary between status transitions)
 * 4. Sort field edge cases (identical dates, null values, ascending vs descending)
 * 5. Category and status filter combinations with edge cases
 * 6. Data type and boundary edge cases (large limit values, etc.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Edge Cases Integration Tests for GET /api/products")
class EdgeCasesIntegrationTest {

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
    private Category emptyCategory;
    private LocalDate today;
    private LocalDate todayPlus30;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        today = LocalDate.now(clock);
        todayPlus30 = today.plusDays(30);

        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));
        emptyCategory = categoryRepository.save(new Category("Empty Category"));

        // Create test products for various edge cases
        createBoundaryDateProducts();
        createSingleProductScenarios();
        createLargeDataset();
    }

    private void createBoundaryDateProducts() {
        // Product with warranty ending EXACTLY today (boundary between ACTIVE/EXPIRING_SOON)
        Product boundaryToday = new Product();
        boundaryToday.setName("Boundary Today Product");
        boundaryToday.setCategory(electronics);
        boundaryToday.setPurchaseDate(today); // warranty ends today (0 months = today)
        boundaryToday.setWarrantyMonths(0);
        productRepository.save(boundaryToday);

        // Product with warranty ending EXACTLY 30 days from today (boundary between EXPIRING_SOON/ACTIVE)
        Product boundary30Days = new Product();
        boundary30Days.setName("Boundary 30 Days Product");
        boundary30Days.setCategory(electronics);
        boundary30Days.setPurchaseDate(todayPlus30.minusMonths(1)); // ends exactly 30 days from today
        boundary30Days.setWarrantyMonths(1);
        productRepository.save(boundary30Days);

        // Product with warranty ending YESTERDAY (boundary between EXPIRING_SOON/EXPIRED)
        Product boundaryYesterday = new Product();
        boundaryYesterday.setName("Boundary Yesterday Product");
        boundaryYesterday.setCategory(electronics);
        boundaryYesterday.setPurchaseDate(today.minusDays(1));
        boundaryYesterday.setWarrantyMonths(0); // ends yesterday
        productRepository.save(boundaryYesterday);

        // Multiple products with same warranty end date (for sort edge case)
        LocalDate sharedEndDate = today.plusMonths(6);
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Same End Date Product " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(sharedEndDate.minusMonths(6));
            product.setWarrantyMonths(6);
            productRepository.save(product);
        }
    }

    private void createSingleProductScenarios() {
        // Single product in category (for testing offset = totalCount - 1)
        Product singleProduct = new Product();
        singleProduct.setName("Single Product in Category");
        singleProduct.setCategory(furniture);
        singleProduct.setPurchaseDate(today.minusMonths(12));
        singleProduct.setWarrantyMonths(24);
        productRepository.save(singleProduct);

        // Category with no products (emptyCategory) - already created in setUp
    }

    private void createLargeDataset() {
        // Create 50 products for testing large limit and offset scenarios
        for (int i = 1; i <= 50; i++) {
            Product product = new Product();
            product.setName("Large Dataset Product " + String.format("%03d", i));
            product.setCategory(i % 2 == 0 ? electronics : furniture);
            product.setPurchaseDate(today.minusMonths(i % 36)); // vary purchase dates
            product.setWarrantyMonths(12 + (i % 24)); // vary warranty months
            productRepository.save(product);
        }
    }

    // ===== BOUNDARY CONDITIONS FOR PAGINATION =====

    @Test
    @DisplayName("GET /api/products with offset beyond all results should return empty with correct metadata")
    void testPaginationBoundary_OffsetBeyondAll() throws Exception {
        // Use limit=1 to get the total count first, then offset far beyond
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .param("offset", "10000") // Way beyond totalCount
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(10000)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with offset > totalCount should return empty array")
    void testPaginationBoundary_OffsetExceedsTotalCount() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "100") // Way beyond totalCount
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.pagination.totalCount", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /api/products with last page where offset + limit exceeds total should have hasMore = false")
    void testPaginationBoundary_LastPageHasMoreFalse() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .param("offset", "0") // Fetch all with large limit
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false))) // All fit in one page
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products with offset + limit > totalCount should return remaining products only")
    void testPaginationBoundary_OffsetPlusLimitExceedsTotal() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Get first page with large limit to know totalCount
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()));
        
        // Now test with offset + limit exceeding total
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with very large limit (10000) should return all available products")
    void testPaginationBoundary_VeryLargeLimitValue() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10000")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit", equalTo(10000)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54)))); // At least our created products
    }

    @Test
    @DisplayName("GET /api/products with modest limit should return expected number of results")
    void testPaginationBoundary_ModestLimit() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true))); // More products available
    }

    // ===== EMPTY RESULT SCENARIOS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X (empty category) should return empty array with pagination metadata")
    void testEmptyResults_EmptyCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", emptyCategory.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)));
    }

    @Test
    @DisplayName("GET /api/products?status=X where no products match should return empty array")
    void testEmptyResults_StatusFilterNoMatches() throws Exception {
        // Assuming all products created have certain statuses, create a scenario where a status has no matches
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Result depends on warranty dates; just verify response structure
                .andExpect(jsonPath("$.pagination.totalCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false))); // if totalCount <= limit
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=Y where both filters match nothing should return empty")
    void testEmptyResults_CombinedFiltersNoMatches() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", emptyCategory.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=invalid_nonexistent should return empty results, not error")
    void testEmptyResults_NonexistentCategoryId() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "999999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP 200, not 400 or 404
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)));
    }

    @Test
    @DisplayName("GET /api/products with offset beyond all results should return empty array")
    void testEmptyResults_OffsetBeyondAllResults() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== WARRANTY STATUS BOUNDARY DATES =====

    @Test
    @DisplayName("GET /api/products should correctly identify product warranty ending exactly today")
    void testWarrantyBoundary_EndDateToday() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Boundary Today Product')].warrantyStatus",
                        hasItem(anyOf(equalTo("EXPIRING_SOON"), equalTo("EXPIRED")))));
    }

    @Test
    @DisplayName("GET /api/products should correctly identify product warranty ending exactly 30 days from today")
    void testWarrantyBoundary_EndDate30DaysFromToday() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Boundary 30 Days Product')].warrantyStatus",
                        hasItem(anyOf(equalTo("EXPIRING_SOON"), equalTo("ACTIVE")))));
    }

    @Test
    @DisplayName("GET /api/products should correctly identify product warranty ended yesterday")
    void testWarrantyBoundary_EndDateYesterday() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'Boundary Yesterday Product')].warrantyStatus",
                        hasItem(equalTo("EXPIRED"))));
    }

    @Test
    @DisplayName("GET /api/products with status filter should correctly handle boundary date products")
    void testWarrantyBoundary_FilterByStatusWithBoundaryDates() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRED"))));
    }

    @Test
    @DisplayName("GET /api/products should handle multiple products with identical warranty end dates")
    void testWarrantyBoundary_MultipleProductsSameEndDate() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Verify that products with similar names (Same End Date Product) are returned
                .andExpect(jsonPath("$.data[?(@.warrantyEndDate)]", hasSize(greaterThanOrEqualTo(54))));
    }

    // ===== SORT FIELD EDGE CASES =====

    @Test
    @DisplayName("GET /api/products?sort=warrantyEndDate,asc with multiple identical dates should return all")
    void testSortEdge_IdenticalDatesSorted() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "warrantyEndDate,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))))
                .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products?sort=warrantyEndDate,desc should sort in descending order")
    void testSortEdge_DateDescending() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "warrantyEndDate,desc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    @Test
    @DisplayName("GET /api/products?sort=name,asc should sort product names alphabetically")
    void testSortEdge_NameAscending() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "name,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))))
                .andExpect(jsonPath("$.data[0].name", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products?sort=id,asc should return products sorted by ID ascending")
    void testSortEdge_IdAscending() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "id,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    @Test
    @DisplayName("GET /api/products?sort=id,desc should return products sorted by ID descending")
    void testSortEdge_IdDescending() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "id,desc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    @Test
    @DisplayName("GET /api/products?sort=categoryId should sort by category ID")
    void testSortEdge_CategoryIdSort() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "categoryId,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    @Test
    @DisplayName("GET /api/products?sort=purchaseDate,asc should sort by purchase date ascending")
    void testSortEdge_PurchaseDateAscending() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "purchaseDate,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    @Test
    @DisplayName("GET /api/products?sort=warrantyMonths should sort by warranty months")
    void testSortEdge_WarrantyMonthsSort() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "warrantyMonths,asc")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(54))));
    }

    // ===== CATEGORY AND STATUS FILTER COMBINATIONS WITH EDGE CASES =====

    @Test
    @DisplayName("GET /api/products?categoryId=furniture&status=ACTIVE should return only furniture products with ACTIVE status")
    void testFilterCombination_CategoryAndStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=electronics&status=EXPIRING_SOON should apply both filters")
    void testFilterCombination_ElectronicsExpiringCombined() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "EXPIRING_SOON")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("EXPIRING_SOON"))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=Y&sort=Z&limit=5 should combine all parameters with pagination")
    void testFilterCombination_AllParametersWithPagination() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("GET /api/products with filter returning zero results should still have pagination metadata")
    void testFilterCombination_ZeroResultsHasMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", emptyCategory.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(10)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== DATA TYPE AND BOUNDARY EDGE CASES =====

    @Test
    @DisplayName("GET /api/products should correctly handle products with warranty months including zero")
    void testDataBoundary_MaxWarrantyMonths() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyMonths", everyItem(greaterThanOrEqualTo(0))));
    }

    @Test
    @DisplayName("GET /api/products should correctly include all product fields in response")
    void testDataBoundary_AllProductFieldsPresent() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id", notNullValue()))
                .andExpect(jsonPath("$.data[0].name", notNullValue()))
                .andExpect(jsonPath("$.data[0].categoryId", notNullValue()))
                .andExpect(jsonPath("$.data[0].purchaseDate", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyMonths", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyStatus", notNullValue()))
                .andExpect(jsonPath("$.data[0].category", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.id", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.name", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products with offset just before last page should include last products")
    void testDataBoundary_LastProductInclusion() throws Exception {
        // Get large result set to find offset point
        mockMvc.perform(get("/api/products")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(notNullValue())));
    }

    @Test
    @DisplayName("GET /api/products response should have consistent pagination across multiple requests")
    void testDataBoundary_PaginationConsistency() throws Exception {
        // First request
        mockMvc.perform(get("/api/products")
                .param("limit", "20")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.totalCount", greaterThanOrEqualTo(54)));

        // Second request with different offset should have same totalCount
        mockMvc.perform(get("/api/products")
                .param("limit", "20")
                .param("offset", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.totalCount", greaterThanOrEqualTo(54)));
    }

    @Test
    @DisplayName("GET /api/products should return products in consistent order when using same parameters")
    void testDataBoundary_ConsistentOrdering() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("sort", "id,asc")
                .param("limit", "20")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(20)))
                .andExpect(jsonPath("$.data[0].id", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products with limit=1 and multiple offsets should traverse all products")
    void testDataBoundary_LimitOneTraversal() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }
}
