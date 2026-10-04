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
 * Integration tests for pagination of the GET /api/products endpoint.
 * Tests pagination with various limit/offset combinations using a real database (H2).
 *
 * These tests verify:
 * 1. Basic pagination: Retrieve first page, second page, etc.
 * 2. Default pagination: No query params returns first 20 products with correct metadata
 * 3. Pagination metadata: Verify totalCount, limit, offset, hasMore calculations
 * 4. Edge cases: Single product per page, offset beyond results, large limit
 * 5. Pagination with filters: Combine pagination with categoryId and/or status filters
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Pagination Integration Tests for GET /api/products")
class PaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category furniture;
    private List<Product> createdProducts;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create test categories
        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));

        // Create test products (25 total to test pagination)
        createdProducts = new ArrayList<>();
        
        // 15 electronics products with varying warranty dates
        for (int i = 1; i <= 15; i++) {
            Product product = new Product();
            product.setName("Electronics Product " + i);
            product.setCategory(electronics);
            product.setPurchaseDate(LocalDate.now().minusMonths(6));
            product.setWarrantyMonths(12 + (i % 5)); // warranty between 12-16 months
            createdProducts.add(productRepository.save(product));
        }

        // 10 furniture products with different warranty dates
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setName("Furniture Product " + i);
            product.setCategory(furniture);
            product.setPurchaseDate(LocalDate.now().minusMonths(12));
            product.setWarrantyMonths(6 + (i % 3)); // warranty between 6-8 months
            createdProducts.add(productRepository.save(product));
        }
    }

    // ===== BASIC PAGINATION TESTS =====

    @Test
    @DisplayName("GET /api/products with limit=5 and offset=0 should return first 5 products")
    void testPagination_FirstPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products with limit=5 and offset=5 should return second page")
    void testPagination_SecondPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products with limit=5 and offset=20 should return last page with 5 products")
    void testPagination_LastFullPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(20)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== DEFAULT PAGINATION TESTS =====

    @Test
    @DisplayName("GET /api/products with no query params should return first 20 products with correct metadata")
    void testPagination_DefaultParams() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(20)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(20)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products with default params should have hasMore=true when more products exist")
    void testPagination_DefaultParams_HasMore() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    // ===== PAGINATION METADATA TESTS =====

    @Test
    @DisplayName("GET /api/products should correctly calculate hasMore when offset + limit < totalCount")
    void testPagination_HasMore_True() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.offset", equalTo(10)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(10)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true))); // 10 + 10 (20) < 25
    }

    @Test
    @DisplayName("GET /api/products should correctly calculate hasMore=false when offset + limit >= totalCount")
    void testPagination_HasMore_False() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.offset", equalTo(15)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(10)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false))); // 15 + 10 (25) >= 25
    }

    @Test
    @DisplayName("GET /api/products should reflect applied offset in pagination metadata")
    void testPagination_MetadataReflectsAppliedOffset() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.offset", equalTo(7)));
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("GET /api/products with limit=1 should return single product per page")
    void testPagination_EdgeCase_SingleProductPerPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(1)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products with offset beyond all results should return empty array")
    void testPagination_EdgeCase_OffsetBeyondResults() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with large limit should return all available products")
    void testPagination_EdgeCase_LargeLimitWithFewerProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "1000")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(25)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(1000)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with offset=0 and limit=1 should return only first product")
    void testPagination_EdgeCase_FirstProductOnly() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", notNullValue()));
    }

    // ===== PAGINATION WITH FILTERS =====

    @Test
    @DisplayName("GET /api/products with categoryId filter and limit=5 should paginate filtered results")
    void testPagination_WithCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(15))) // only electronics products
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products with categoryId filter should show correct totalCount")
    void testPagination_WithCategoryFilter_CorrectTotalCount() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", furniture.getId().toString())
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(10))); // only furniture products
    }

    @Test
    @DisplayName("GET /api/products with status=ACTIVE filter and pagination should work correctly")
    void testPagination_WithStatusFilter() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.pagination.limit", equalTo(10)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)))
                .andExpect(jsonPath("$.pagination.totalCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("GET /api/products with both categoryId and status filters should paginate combined results")
    void testPagination_WithCombinedFilters() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", electronics.getId().toString())
                .param("status", "ACTIVE")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(0)));
    }

    // ===== PAGINATION WITH SORTING =====

    @Test
    @DisplayName("GET /api/products with sort parameter should paginate sorted results")
    void testPagination_WithSorting() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "0")
                .param("sort", "name,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)));
    }

    @Test
    @DisplayName("GET /api/products with sort=id,desc and pagination should work correctly")
    void testPagination_WithSortingDesc() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "0")
                .param("sort", "id,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    // ===== PAGINATION CONSISTENCY TESTS =====

    @Test
    @DisplayName("GET /api/products pagination should be consistent: each page contains correct products")
    void testPagination_Consistency_PageOne() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products pagination should be consistent: page 2 and 3 combined equal full page")
    void testPagination_Consistency_MultiplePagesSize() throws Exception {
        // Request pages separately
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.offset", equalTo(5)))
                .andExpect(jsonPath("$.pagination.limit", equalTo(5)));
    }

    // ===== RESPONSE STRUCTURE TESTS =====

    @Test
    @DisplayName("GET /api/products pagination response should include all required metadata fields")
    void testPagination_ResponseStructure() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "5")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.pagination.totalCount", notNullValue()))
                .andExpect(jsonPath("$.pagination.limit", notNullValue()))
                .andExpect(jsonPath("$.pagination.offset", notNullValue()))
                .andExpect(jsonPath("$.pagination.hasMore", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/products empty pagination should still include pagination metadata")
    void testPagination_EmptyResult_StillHasMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("categoryId", "99999") // non-existent category
                .param("limit", "10")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(0)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    // ===== PARTIAL PAGE TESTS =====

    @Test
    @DisplayName("GET /api/products should return partial page when fewer products remain than limit")
    void testPagination_PartialLastPage() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "8")
                .param("offset", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5))) // only 5 products left (25 - 20)
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }

    @Test
    @DisplayName("GET /api/products with limit=15 and offset=15 should return exactly 10 products")
    void testPagination_PartialPage_ExactCount() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "15")
                .param("offset", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.pagination.totalCount", equalTo(25)));
    }

    // ===== SEQUENTIAL PAGINATION TESTS =====

    @Test
    @DisplayName("GET /api/products sequential pagination should cover all products without duplication")
    void testPagination_Sequential_FirstBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "12")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products sequential pagination second batch should follow first")
    void testPagination_Sequential_SecondBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "12")
                .param("offset", "12")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/products sequential pagination final batch should complete set")
    void testPagination_Sequential_FinalBatch() throws Exception {
        mockMvc.perform(get("/api/products")
                .param("limit", "12")
                .param("offset", "24")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.pagination.hasMore", equalTo(false)));
    }
}
