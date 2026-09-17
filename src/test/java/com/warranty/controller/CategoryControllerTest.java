package com.warranty.controller;

import com.warranty.dto.response.CategoryResponse;
import com.warranty.exception.CategoryNotFoundException;
import com.warranty.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CategoryController REST endpoints.
 * Tests all GET endpoints for categories using MockMvc.
 * Mocks CategoryService to isolate HTTP layer testing from business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController Unit Tests")
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    private CategoryResponse testCategoryResponse1;
    private CategoryResponse testCategoryResponse2;
    private CategoryResponse testCategoryResponse3;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc with the controller
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService)).build();
        
        // Initialize test data
        testCategoryResponse1 = new CategoryResponse(1L, "Kitchen Appliances");
        testCategoryResponse2 = new CategoryResponse(2L, "Electronics");
        testCategoryResponse3 = new CategoryResponse(3L, "Bedroom");
    }

    // ===== GET /api/categories (Get All Categories) TESTS =====

    @Test
    @DisplayName("GET /api/categories should return 200 OK with all categories")
    void testGetAllCategories_ReturnsOkWithCategories() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(testCategoryResponse1, testCategoryResponse2);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Kitchen Appliances")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Electronics")));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return 200 OK with empty array when no categories exist")
    void testGetAllCategories_ReturnsOkWithEmptyArray() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(jsonPath("$", emptyIterable()));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return 200 OK with single category")
    void testGetAllCategories_ReturnsOkWithSingleCategory() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(testCategoryResponse1);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Kitchen Appliances")));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return correct content type JSON")
    void testGetAllCategories_ReturnsJsonContentType() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of(testCategoryResponse1));

        // Act & Assert
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return all category fields")
    void testGetAllCategories_ResponseContainsAllFields() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(testCategoryResponse1);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0]", hasKey("id")))
                .andExpect(jsonPath("$[0]", hasKey("name")));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return categories in correct order")
    void testGetAllCategories_PreservesOrder() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(
                testCategoryResponse1,
                testCategoryResponse2,
                testCategoryResponse3
        );
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Kitchen Appliances")))
                .andExpect(jsonPath("$[1].name", is("Electronics")))
                .andExpect(jsonPath("$[2].name", is("Bedroom")));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should return multiple categories with correct structure")
    void testGetAllCategories_MultipleCategories() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1L, "Kitchen Appliances"),
                new CategoryResponse(2L, "Electronics"),
                new CategoryResponse(3L, "Bedroom"),
                new CategoryResponse(4L, "Beauty"),
                new CategoryResponse(5L, "Home Appliances")
        );
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].id", notNullValue()))
                .andExpect(jsonPath("$[*].name", notNullValue()));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories should call service layer exactly once")
    void testGetAllCategories_ServiceInteractionCount() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of(testCategoryResponse1));

        // Act
        mockMvc.perform(get("/api/categories"));

        // Assert
        verify(categoryService, times(1)).getAllCategories();
        verifyNoMoreInteractions(categoryService);
    }

    // ===== GET /api/categories/{id} (Get Category by ID) TESTS =====

    @Test
    @DisplayName("GET /api/categories/{id} should return 200 OK with category when ID exists")
    void testGetCategoryById_ReturnsOkWithCategory() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Kitchen Appliances")));

        verify(categoryService, times(1)).getCategoryById(1L);
    }



    @Test
    @DisplayName("GET /api/categories/{id} should return correct content type JSON")
    void testGetCategoryById_ReturnsJsonContentType() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return response with correct ID")
    void testGetCategoryById_ResponseHasCorrectId() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(5L)).thenReturn(new CategoryResponse(5L, "Beauty"));

        // Act & Assert
        mockMvc.perform(get("/api/categories/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)));

        verify(categoryService, times(1)).getCategoryById(5L);
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return response with correct name")
    void testGetCategoryById_ResponseHasCorrectName() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(3L)).thenReturn(new CategoryResponse(3L, "Bedroom"));

        // Act & Assert
        mockMvc.perform(get("/api/categories/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Bedroom")));

        verify(categoryService, times(1)).getCategoryById(3L);
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return response with both ID and name fields")
    void testGetCategoryById_ResponseHasAllFields() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(2L)).thenReturn(testCategoryResponse2);

        // Act & Assert
        mockMvc.perform(get("/api/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", notNullValue()))
                .andExpect(jsonPath("$", hasKey("id")))
                .andExpect(jsonPath("$", hasKey("name")));

        verify(categoryService, times(1)).getCategoryById(2L);
    }





    @Test
    @DisplayName("GET /api/categories/{id} should call service layer exactly once per request")
    void testGetCategoryById_ServiceInteractionCount() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act
        mockMvc.perform(get("/api/categories/1"));

        // Assert
        verify(categoryService, times(1)).getCategoryById(1L);
        verifyNoMoreInteractions(categoryService);
    }



    @Test
    @DisplayName("GET /api/categories/{id} should pass correct ID to service")
    void testGetCategoryById_PassesCorrectIdToService() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(42L)).thenReturn(new CategoryResponse(42L, "Test"));

        // Act
        mockMvc.perform(get("/api/categories/42"));

        // Assert
        verify(categoryService, times(1)).getCategoryById(42L);
    }

    // ===== ERROR HANDLING TESTS =====





    @Test
    @DisplayName("GET /api/categories/{id} with invalid ID format should return 400 Bad Request")
    void testGetCategoryById_WithInvalidIdFormat_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/categories/invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Service should not be called for invalid format
        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("GET /api/categories/{id} with non-numeric ID should return 400 Bad Request")
    void testGetCategoryById_WithAlphabeticId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/categories/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("GET /api/categories/{id} with special characters in ID should return 400 Bad Request")
    void testGetCategoryById_WithSpecialCharacterId_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/categories/@#$"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    // ===== HTTP STATUS CODE TESTS =====

    @Test
    @DisplayName("GET /api/categories should return HTTP 200 status code")
    void testGetAllCategories_HttpStatus200() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(status().is(200));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return HTTP 200 status code on success")
    void testGetCategoryById_HttpStatus200() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(status().is(200));

        verify(categoryService, times(1)).getCategoryById(1L);
    }



    // ===== RESPONSE BODY TESTS =====

    @Test
    @DisplayName("GET /api/categories should return valid JSON array structure")
    void testGetAllCategories_ValidJsonArrayStructure() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(testCategoryResponse1, testCategoryResponse2);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", hasSize(2)));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return valid JSON object structure")
    void testGetCategoryById_ValidJsonObjectStructure() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", notNullValue()));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("GET /api/categories should return response without any null fields in array items")
    void testGetAllCategories_NoNullFieldsInResponse() throws Exception {
        // Arrange
        List<CategoryResponse> categories = List.of(testCategoryResponse1);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should return response without any null fields")
    void testGetCategoryById_NoNullFieldsInResponse() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", notNullValue()));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    // ===== INTEGRATION-STYLE TESTS =====

    @Test
    @DisplayName("GET /api/categories endpoint should exist and be accessible")
    void testGetAllCategories_EndpointExists() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());

        // Act & Assert - just ensure endpoint doesn't throw 404
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} endpoint should exist and be accessible")
    void testGetCategoryById_EndpointExists() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert - just ensure endpoint doesn't throw 404
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("Both GET endpoints should accept application/json content type")
    void testGetEndpoints_AcceptJsonContentType() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getAllCategories();
        verify(categoryService, times(1)).getCategoryById(1L);
    }

    @Test
    @DisplayName("GET /api/categories should return different results when service returns different data")
    void testGetAllCategories_DifferentDataOnDifferentCalls() throws Exception {
        // Arrange - first call returns empty, second call returns data
        when(categoryService.getAllCategories())
                .thenReturn(new ArrayList<>())
                .thenReturn(List.of(testCategoryResponse1, testCategoryResponse2));

        // Act & Assert - first call
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Act & Assert - second call
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(categoryService, times(2)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should not return 404 when service returns a category")
    void testGetCategoryById_SuccessfulResponse() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    // ===== CONTENT NEGOTIATION TESTS =====

    @Test
    @DisplayName("GET /api/categories should respond with application/json even without explicit Accept header")
    void testGetAllCategories_RespondWithJsonWithoutAcceptHeader() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of(testCategoryResponse1));

        // Act & Assert
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should respond with application/json even without explicit Accept header")
    void testGetCategoryById_RespondWithJsonWithoutAcceptHeader() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(1L)).thenReturn(testCategoryResponse1);

        // Act & Assert
        mockMvc.perform(get("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

    // ===== EDGE CASE TESTS =====

    @Test
    @DisplayName("GET /api/categories should handle service returning very large list")
    void testGetAllCategories_LargeDataSet() throws Exception {
        // Arrange
        List<CategoryResponse> largeList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            largeList.add(new CategoryResponse((long) i, "Category " + i));
        }
        when(categoryService.getAllCategories()).thenReturn(largeList);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/categories/{id} should handle category with very long name")
    void testGetCategoryById_LongCategoryName() throws Exception {
        // Arrange
        String longName = "A".repeat(255);
        when(categoryService.getCategoryById(1L)).thenReturn(new CategoryResponse(1L, longName));

        // Act & Assert
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(longName)));

        verify(categoryService, times(1)).getCategoryById(1L);
    }

}
