package com.warranty.controller;

import com.warranty.dto.response.PaginationMetadata;
import com.warranty.dto.response.ProductListResponse;
import com.warranty.dto.response.ProductResponse;
import com.warranty.dto.response.CategoryResponse;
import com.warranty.exception.GlobalExceptionHandler;
import com.warranty.exception.ValidationException;
import com.warranty.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProductController GET /api/products endpoint.
 * Tests parameter validation, default values, and error handling.
 * Mocks ProductService to isolate HTTP layer testing from business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController GET /api/products Unit Tests")
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    private ProductListResponse testProductListResponse;
    private ProductResponse testProductResponse1;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc with the controller and global exception handler
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Initialize test data
        CategoryResponse testCategory = new CategoryResponse(1L, "Electronics");
        testProductResponse1 = new ProductResponse(
                1L,
                "Laptop",
                1L,
                LocalDate.of(2023, 1, 15),
                24,
                LocalDate.of(2025, 1, 15),
                "ACTIVE",
                testCategory
        );

        PaginationMetadata pagination = new PaginationMetadata(100L, 20, 0, true);
        testProductListResponse = new ProductListResponse(
                List.of(testProductResponse1),
                pagination
        );
    }

    // ===== LIMIT PARAMETER VALIDATION TESTS =====

    @Test
    @DisplayName("GET /api/products with limit=0 should return 400 Bad Request with specific message")
    void testListProducts_LimitZero_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("limit", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Limit must be greater than zero")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with limit=-1 should return 400 Bad Request")
    void testListProducts_LimitNegative_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("limit", "-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Limit must be greater than zero")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with limit=1 should return 200 OK")
    void testListProducts_LimitOne_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(1), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(1), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with valid limit=50 should return 200 OK")
    void testListProducts_ValidLimit_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(50), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("limit", "50")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(50), eq(0));
    }

    // ===== OFFSET PARAMETER VALIDATION TESTS =====

    @Test
    @DisplayName("GET /api/products with offset=-1 should return 400 Bad Request with specific message")
    void testListProducts_OffsetNegative_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("offset", "-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Limit and offset must be non-negative integers")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with offset=0 should return 200 OK")
    void testListProducts_OffsetZero_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with valid offset=100 should return 200 OK")
    void testListProducts_ValidOffset_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(100)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("offset", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(100));
    }

    // ===== STATUS PARAMETER VALIDATION TESTS =====

    @Test
    @DisplayName("GET /api/products with status=ACTIVE should return 200 OK")
    void testListProducts_StatusActive_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), eq("ACTIVE"), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), eq("ACTIVE"), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with status=EXPIRING_SOON should return 200 OK")
    void testListProducts_StatusExpiringSoon_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), eq("EXPIRING_SOON"), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRING_SOON")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), eq("EXPIRING_SOON"), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with status=EXPIRED should return 200 OK")
    void testListProducts_StatusExpired_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), eq("EXPIRED"), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "EXPIRED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), eq("EXPIRED"), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with status=active (lowercase) should return 400 Bad Request with specific message")
    void testListProducts_StatusLowercase_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Status must be one of: ACTIVE, EXPIRING_SOON, EXPIRED")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with status=INVALID should return 400 Bad Request")
    void testListProducts_StatusInvalid_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Status must be one of: ACTIVE, EXPIRING_SOON, EXPIRED")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with status omitted should return 200 OK")
    void testListProducts_StatusOmitted_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    // ===== SORT PARAMETER VALIDATION TESTS =====

    @Test
    @DisplayName("GET /api/products with sort=id,asc should return 200 OK")
    void testListProducts_SortIdAsc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "id,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=name,asc should return 200 OK")
    void testListProducts_SortNameAsc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("name"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "name,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("name"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=purchaseDate,asc should return 200 OK")
    void testListProducts_SortPurchaseDateAsc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("purchaseDate"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "purchaseDate,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("purchaseDate"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=warrantyMonths,asc should return 200 OK")
    void testListProducts_SortWarrantyMonthsAsc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("warrantyMonths"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "warrantyMonths,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("warrantyMonths"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=id,desc should return 200 OK")
    void testListProducts_SortIdDesc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("desc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "id,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("desc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=name,desc should return 200 OK")
    void testListProducts_SortNameDesc_ReturnsOk() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("name"), eq("desc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "name,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("name"), eq("desc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with sort=invalidField,asc should return 400 Bad Request")
    void testListProducts_SortInvalidField_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "invalidField,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with sort=id,invalid should return 400 Bad Request")
    void testListProducts_SortInvalidDirection_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "id,invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Sort direction must be either 'asc' or 'desc'")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with sort=id (missing comma) should return 400 Bad Request")
    void testListProducts_SortMissingComma_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Sort parameter must be in format")));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("GET /api/products with sort=id, (trailing comma) should return 400 Bad Request")
    void testListProducts_SortTrailingComma_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("sort", "id,")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Sort parameter must be in format")));

        verifyNoInteractions(productService);
    }

    // ===== DEFAULT PARAMETERS TESTS =====

    @Test
    @DisplayName("GET /api/products with no query parameters should return 200 OK with defaults")
    void testListProducts_NoQueryParams_ReturnsOkWithDefaults() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with no query parameters should use default limit=20")
    void testListProducts_DefaultLimit_Is20() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with no query parameters should use default offset=0")
    void testListProducts_DefaultOffset_Is0() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with no query parameters should use default sort=id,asc")
    void testListProducts_DefaultSort_IsIdAsc() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    // ===== COMBINED PARAMETER TESTS =====

    @Test
    @DisplayName("GET /api/products with limit and offset should pass both to service")
    void testListProducts_LimitAndOffset_PassedToService() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(10), eq(50)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("limit", "10")
                .param("offset", "50"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(10), eq(50));
    }

    @Test
    @DisplayName("GET /api/products with status and sort should pass both to service")
    void testListProducts_StatusAndSort_PassedToService() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), eq("ACTIVE"), eq("name"), eq("desc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("status", "ACTIVE")
                .param("sort", "name,desc"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(isNull(), eq("ACTIVE"), eq("name"), eq("desc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products with categoryId, status, and sort should pass all to service")
    void testListProducts_AllParameters_PassedToService() throws Exception {
        // Arrange
        when(productService.listProducts(eq(1L), eq("ACTIVE"), eq("purchaseDate"), eq("asc"), eq(15), eq(10)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                .param("categoryId", "1")
                .param("status", "ACTIVE")
                .param("sort", "purchaseDate,asc")
                .param("limit", "15")
                .param("offset", "10"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProducts(eq(1L), eq("ACTIVE"), eq("purchaseDate"), eq("asc"), eq(15), eq(10));
    }

    // ===== RESPONSE STRUCTURE TESTS =====

    @Test
    @DisplayName("GET /api/products should return 200 OK status code")
    void testListProducts_HttpStatus200() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(status().is(200));

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products should return response with data array")
    void testListProducts_ResponseHasData() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products should return response with pagination metadata")
    void testListProducts_ResponseHasPagination() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination", notNullValue()));

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

    @Test
    @DisplayName("GET /api/products should return content type application/json")
    void testListProducts_ContentTypeJson() throws Exception {
        // Arrange
        when(productService.listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0)))
                .thenReturn(testProductListResponse);

        // Act & Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(productService, times(1)).listProducts(isNull(), isNull(), eq("id"), eq("asc"), eq(20), eq(0));
    }

}