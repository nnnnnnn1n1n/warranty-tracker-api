package com.warranty.controller;

import com.warranty.dto.response.ProductListResponse;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for category filtering of the GET /api/products endpoint.
 * Tests filtering by categoryId using a real database (H2) and verifies that
 * products are correctly filtered, paginated, and returned with warranty status.
 *
 * These tests verify:
 * 1. Valid category filtering: Only products from the requested category are returned
 * 2. Non-existent category: Returns empty results with HTTP 200
 * 3. Category with no products: Returns empty results correctly
 * 4. No filter: Returns products from all categories
 * 5. Category filter with pagination: Correct slice of filtered results
 * 6. Category filter with sorting: Sorted results within category
 * 7. Combined filters: Category + status filters work together
 * 8. Response structure: Warranty status calculated correctly for filtered products
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Category Filtering Integration Tests for GET /api/products")
class CategoryFilteringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category furniture;
    private Category emptyCategory;
    private List<Product> electronicsProducts;
    private List<Product> furnitureProducts;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test categories
        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));
        emptyCategory = categoryRepository.save(new Category("Empty Category"));

        // Create electronics products (15 total, with varying warranty dates)
        electronicsProducts = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Product product = new Product();
            product.setName("Laptop " + i);
            product.setCategory(electronics);
            // Vary purchase dates to create different warranty statuses
            product.setPurchaseDate(LocalDate.now().minusMonths(6 + (i % 5)));
            product.setWarrantyMonths(12 + (i % 8)); // warranty between 12-19 months
            electronicsProducts.add(productRepository.save(product));
        }

        // Create furniture products (12 total, with different warranty dates)
        furnitureProducts = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Product product = new Product();
            product.setName("Chair " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(LocalDate.now().minusMonths(12 + (i % 3)));
            product.setWarrantyMonths(6 + (i % 4)); // warranty between 6-9 months
            furnitureProducts.add(productRepository.save(product));
        }

        // emptyCategory has no products
    }

    // ===== BASIC CATEGORY FILTERING TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X should return only products from that category")
    void testCategoryFilter_ValidCategoryId_ReturnsOnlyProductsFromCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(15)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(15)))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X should verify all returned products belong to the category")
    void testCategoryFilter_VerifyAllProductsInCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(12)))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X should populate category object in each product")
    void testCategoryFilter_CategoryObjectPopulated() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category.id", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.name", notNullValue()))
                .andExpect(jsonPath("$.data[0].category.name", equalTo("Electronics")));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X should calculate warrantyStatus for filtered products")
    void testCategoryFilter_WarrantyStatusCalculated() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(
                        anyOf(equalTo("ACTIVE"), equalTo("EXPIRING_SOON"), equalTo("EXPIRED"))
                )));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X should include warrantyEndDate for filtered products")
    void testCategoryFilter_WarrantyEndDateIncluded() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()))
                .andExpect(jsonPath("$.data[0].warrantyMonths", notNullValue()));
    }

    // ===== NON-EXISTENT CATEGORY TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=99999 (non-existent) should return empty array with HTTP 200")
    void testCategoryFilter_NonExistentCategory_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=99999 should still include pagination metadata")
    void testCategoryFilter_NonExistentCategory_StillHasMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=99999 should not return HTTP 404")
    void testCategoryFilter_NonExistentCategory_NotFound() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // HTTP 200, not 404
    }

    // ===== EMPTY CATEGORY TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X (empty category) should return empty array with HTTP 200")
    void testCategoryFilter_EmptyCategory_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", emptyCategory.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X (empty category) should have correct metadata")
    void testCategoryFilter_EmptyCategory_HasCorrectMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", emptyCategory.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)));
    }

    // ===== NO FILTER (ALL CATEGORIES) TESTS =====

    @Test
    @DisplayName("GET /api/products without categoryId should return products from all categories")
    void testCategoryFilter_NoFilter_ReturnsAllCategories() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(20))) // default limit
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(27))); // 15 + 12 products
    }

    @Test
    @DisplayName("GET /api/products without filter should include products from multiple categories")
    void testCategoryFilter_NoFilter_MultipleCategories() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "27") // get all products
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(27)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(27)));
    }

    // ===== CATEGORY FILTER WITH PAGINATION TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X&limit=5 should paginate filtered results correctly")
    void testCategoryFilter_WithPagination_FirstPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(15)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&limit=5&offset=5 should return second page of filtered results")
    void testCategoryFilter_WithPagination_SecondPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "5")
                .param("offset", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(15)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&limit=5&offset=10 should return third page of filtered results")
    void testCategoryFilter_WithPagination_ThirdPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "5")
                .param("offset", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false))); // only 5 more products
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&limit=20 should return all filtered products on one page")
    void testCategoryFilter_WithPagination_AllOnOnePage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("limit", "20")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(12)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X should show hasMore=false on last page")
    void testCategoryFilter_WithPagination_HasMoreFalseOnLastPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("limit", "8")
                .param("offset", "8")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4))) // 12 - 8 = 4 remaining
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&offset=beyond_total should return empty array")
    void testCategoryFilter_WithPagination_OffsetBeyondTotal() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("limit", "10")
                .param("offset", "50")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(12)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== CATEGORY FILTER WITH SORTING TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X&sort=name,asc should return products sorted by name")
    void testCategoryFilter_WithSorting_SortByNameAsc() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("sort", "name,asc")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(15)))
                .andExpect(jsonPath("$.data[0].name", startsWith("Laptop")));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&sort=purchaseDate,desc should sort by purchase date descending")
    void testCategoryFilter_WithSorting_SortByPurchaseDateDesc() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("sort", "purchaseDate,desc")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].purchaseDate", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&sort=warrantyMonths,asc should sort by warranty months")
    void testCategoryFilter_WithSorting_SortByWarrantyMonths() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("sort", "warrantyMonths,asc")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(15)))
                .andExpect(jsonPath("$.data[0].warrantyMonths", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&sort=id,asc should sort by ID within filtered category")
    void testCategoryFilter_WithSorting_SortById() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("sort", "id,asc")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].id", notNullValue()));
    }

    // ===== CATEGORY FILTER WITH COMBINED FILTERS TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=ACTIVE should apply both filters (AND logic)")
    void testCategoryFilter_WithStatusFilter() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(15))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=EXPIRING_SOON should filter by both category and status")
    void testCategoryFilter_WithExpiringStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(12))))
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(furniture.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=EXPIRED should filter by category and EXPIRED status")
    void testCategoryFilter_WithExpiredStatus() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=Y&sort=Z should combine all three filters")
    void testCategoryFilter_WithStatusAndSort() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))))
                .andExpect(jsonPath("$.data[*].warrantyStatus", everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X&status=Y&limit=5&offset=0 should combine all filters with pagination")
    void testCategoryFilter_AllFiltersWithPagination() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("sort", "name,asc")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)));
    }

    // ===== RESPONSE STRUCTURE TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X response should include all required product fields")
    void testCategoryFilter_ResponseStructure_AllProductFields() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
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
    @DisplayName("GET /api/products?categoryId=X response should include pagination metadata")
    void testCategoryFilter_ResponseStructure_PaginationMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.limit", notNullValue()))
                .andExpect(jsonPath("$.pagination.offset", notNullValue()))
                .andExpect(jsonPath("$.pagination.hasMore", notNullValue()));
    }

    // ===== SEQUENTIAL CATEGORY FILTERING TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=X sequential pagination should cover all filtered products")
    void testCategoryFilter_Sequential_FirstBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "7")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(7)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X sequential pagination second batch should follow first")
    void testCategoryFilter_Sequential_SecondBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "7")
                .param("offset", "7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(7)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X sequential pagination final batch should complete set")
    void testCategoryFilter_Sequential_FinalBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "7")
                .param("offset", "14")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1))) // 15 - 14 = 1 remaining
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== MULTIPLE CATEGORY ISOLATION TESTS =====

    @Test
    @DisplayName("GET /api/products?categoryId=electronics should not return furniture products")
    void testCategoryFilter_IsolatesCategories() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", everyItem(containsString("Laptop"))));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=furniture should not return electronics products")
    void testCategoryFilter_FurnitureIsolation() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", everyItem(containsString("Chair"))));
    }

    // ===== DEFAULT BEHAVIOR WITH CATEGORY FILTER =====

    @Test
    @DisplayName("GET /api/products?categoryId=X with no limit/offset should use defaults (limit=20, offset=0)")
    void testCategoryFilter_WithDefaults() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.data", hasSize(15))); // all 15 electronics products fit in default limit
    }

    @Test
    @DisplayName("GET /api/products?categoryId=X with only offset should use default limit=20")
    void testCategoryFilter_DefaultLimitWithOffset() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("offset", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(5)))
                .andExpect(jsonPath("$.data", hasSize(10))); // 15 - 5 = 10 remaining
    }
}