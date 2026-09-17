package com.warranty.service;

import com.warranty.dto.response.CategoryResponse;
import com.warranty.entity.Category;
import com.warranty.exception.CategoryNotFoundException;
import com.warranty.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService class.
 * Tests CRUD operations and validation logic for product categories.
 * Uses Mockito to mock CategoryRepository dependency.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private Category testCategory2;
    private CategoryResponse expectedCategoryResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testCategory = new Category(1L, "Kitchen Appliances", null);
        testCategory2 = new Category(2L, "Electronics", null);
        expectedCategoryResponse = new CategoryResponse(1L, "Kitchen Appliances");
    }

    // ===== GET ALL CATEGORIES TESTS =====

    @Test
    @DisplayName("getAllCategories should return all categories when categories exist")
    void testGetAllCategories_WithCategories() {
        // Arrange
        List<Category> categories = List.of(testCategory, testCategory2);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Kitchen Appliances", result.get(0).getName());
        assertEquals("Electronics", result.get(1).getName());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllCategories should return empty list when no categories exist")
    void testGetAllCategories_NoCategories() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllCategories should return single category when only one exists")
    void testGetAllCategories_SingleCategory() {
        // Arrange
        List<Category> categories = List.of(testCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Kitchen Appliances", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllCategories should return responses with correct structure")
    void testGetAllCategories_ResponseStructure() {
        // Arrange
        List<Category> categories = List.of(testCategory);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        CategoryResponse response = result.get(0);
        assertNotNull(response.getId());
        assertNotNull(response.getName());
        assertEquals(1L, response.getId());
        assertEquals("Kitchen Appliances", response.getName());
    }

    // ===== GET CATEGORY BY ID TESTS =====

    @Test
    @DisplayName("getCategoryById should return category when ID exists")
    void testGetCategoryById_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act
        CategoryResponse result = categoryService.getCategoryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Kitchen Appliances", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getCategoryById should throw CategoryNotFoundException when ID does not exist")
    void testGetCategoryById_NotFound() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategoryById(999L),
                "Expected CategoryNotFoundException to be thrown"
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(categoryRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getCategoryById should throw CategoryNotFoundException with descriptive message")
    void testGetCategoryById_ExceptionMessage() {
        // Arrange
        Long categoryId = 42L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategoryById(categoryId)
        );
        assertEquals("Category with ID " + categoryId + " not found", exception.getMessage());
    }

    @Test
    @DisplayName("getCategoryById should return response with correct ID")
    void testGetCategoryById_CorrectId() {
        // Arrange
        Category category = new Category(5L, "Beauty", null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse result = categoryService.getCategoryById(5L);

        // Assert
        assertEquals(5L, result.getId());
        verify(categoryRepository, times(1)).findById(5L);
    }

    @Test
    @DisplayName("getCategoryById should return response with correct name")
    void testGetCategoryById_CorrectName() {
        // Arrange
        Category category = new Category(3L, "Bedroom", null);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse result = categoryService.getCategoryById(3L);

        // Assert
        assertEquals("Bedroom", result.getName());
        verify(categoryRepository, times(1)).findById(3L);
    }

    @Test
    @DisplayName("getCategoryById with ID 0 should throw CategoryNotFoundException")
    void testGetCategoryById_WithZeroId() {
        // Arrange
        when(categoryRepository.findById(0L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategoryById(0L)
        );
    }

    @Test
    @DisplayName("getCategoryById with negative ID should throw CategoryNotFoundException")
    void testGetCategoryById_WithNegativeId() {
        // Arrange
        when(categoryRepository.findById(-1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategoryById(-1L)
        );
    }

    // ===== SAVE SAMPLE CATEGORIES TESTS =====

    @Test
    @DisplayName("saveSampleCategories should create five predefined categories")
    void testSaveSampleCategories_CreatesAllCategories() {
        // Act
        categoryService.saveSampleCategories();

        // Assert
        verify(categoryRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("saveSampleCategories should persist all five category names")
    void testSaveSampleCategories_CorrectCategoryNames() {
        // Capture the list passed to saveAll
        List<Category> capturedCategories = new ArrayList<>();
        when(categoryRepository.saveAll(any())).then(invocation -> {
            capturedCategories.addAll((List<Category>) invocation.getArgument(0));
            return capturedCategories;
        });

        // Act
        categoryService.saveSampleCategories();

        // Assert
        assertEquals(5, capturedCategories.size());
        
        // Check all five categories are present
        List<String> categoryNames = capturedCategories.stream()
                .map(Category::getName)
                .toList();
        
        assertTrue(categoryNames.contains("Kitchen Appliances"));
        assertTrue(categoryNames.contains("Bedroom"));
        assertTrue(categoryNames.contains("Beauty"));
        assertTrue(categoryNames.contains("Electronics"));
        assertTrue(categoryNames.contains("Home Appliances"));
    }

    @Test
    @DisplayName("saveSampleCategories should call repository saveAll exactly once")
    void testSaveSampleCategories_RepositoryInteraction() {
        // Act
        categoryService.saveSampleCategories();

        // Assert
        verify(categoryRepository, times(1)).saveAll(any());
        verifyNoMoreInteractions(categoryRepository);
    }

    @Test
    @DisplayName("saveSampleCategories should pass a list with exactly 5 categories")
    void testSaveSampleCategories_ListSize() {
        // Capture and validate the argument
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        when(categoryRepository.saveAll(any())).thenReturn(new ArrayList<>());

        // Act
        categoryService.saveSampleCategories();

        // Assert
        verify(categoryRepository).saveAll(captor.capture());
        assertEquals(5, captor.getValue().size());
    }

    // ===== CONVERSION AND DTO TESTS =====

    @Test
    @DisplayName("CategoryResponse should have correct ID after conversion")
    void testCategoryConversion_IdPreserved() {
        // Arrange
        Category category = new Category(7L, "Test Category", null);
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse response = categoryService.getCategoryById(7L);

        // Assert
        assertEquals(7L, response.getId());
    }

    @Test
    @DisplayName("CategoryResponse should have correct name after conversion")
    void testCategoryConversion_NamePreserved() {
        // Arrange
        String testName = "Sports Equipment";
        Category category = new Category(8L, testName, null);
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse response = categoryService.getCategoryById(8L);

        // Assert
        assertEquals(testName, response.getName());
    }

    @Test
    @DisplayName("Multiple getCategoryById calls should return consistent responses")
    void testConsistentCategoryResponses() {
        // Arrange
        Category category = new Category(10L, "Consistent Test", null);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse result1 = categoryService.getCategoryById(10L);
        CategoryResponse result2 = categoryService.getCategoryById(10L);

        // Assert
        assertEquals(result1.getId(), result2.getId());
        assertEquals(result1.getName(), result2.getName());
    }

    // ===== INTEGRATION-STYLE TESTS =====

    @Test
    @DisplayName("getAllCategories should return DTO responses not entities")
    void testGetAllCategories_ReturnsResponsesNotEntities() {
        // Arrange
        List<Category> categories = List.of(testCategory, testCategory2);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.stream().allMatch(r -> r instanceof CategoryResponse));
        result.forEach(r -> {
            assertNotNull(r.getId());
            assertNotNull(r.getName());
        });
    }

    @Test
    @DisplayName("getCategoryById should return DTO response not entity")
    void testGetCategoryById_ReturnsResponseNotEntity() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act
        CategoryResponse result = categoryService.getCategoryById(1L);

        // Assert
        assertTrue(result instanceof CategoryResponse);
        assertNotNull(result.getId());
        assertNotNull(result.getName());
    }

    @Test
    @DisplayName("getAllCategories should preserve order of categories from repository")
    void testGetAllCategories_PreservesOrder() {
        // Arrange
        Category cat1 = new Category(1L, "First", null);
        Category cat2 = new Category(2L, "Second", null);
        Category cat3 = new Category(3L, "Third", null);
        List<Category> categories = List.of(cat1, cat2, cat3);
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertEquals("First", result.get(0).getName());
        assertEquals("Second", result.get(1).getName());
        assertEquals("Third", result.get(2).getName());
    }

    @Test
    @DisplayName("saveSampleCategories should not return anything")
    void testSaveSampleCategories_ReturnType() {
        // Act - should not throw any exception and return void
        assertDoesNotThrow(() -> categoryService.saveSampleCategories());
    }

    @Test
    @DisplayName("getCategoryById should call repository exactly once for each ID")
    void testGetCategoryById_RepositoryCallCount() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act
        categoryService.getCategoryById(1L);

        // Assert
        verify(categoryRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(categoryRepository);
    }

    // ===== EDGE CASES AND BOUNDARY TESTS =====

    @Test
    @DisplayName("getCategoryById should handle null response from repository gracefully")
    void testGetCategoryById_NullFromRepository() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(1L));
    }

    @Test
    @DisplayName("getAllCategories should handle large number of categories")
    void testGetAllCategories_LargeNumberOfCategories() {
        // Arrange
        List<Category> categories = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            categories.add(new Category((long) i, "Category " + i, null));
        }
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertEquals(100, result.size());
        assertEquals("Category 1", result.get(0).getName());
        assertEquals("Category 100", result.get(99).getName());
    }

    @Test
    @DisplayName("getCategoryById with very large ID should throw CategoryNotFoundException when not found")
    void testGetCategoryById_WithVeryLargeId() {
        // Arrange
        Long largeId = Long.MAX_VALUE;
        when(categoryRepository.findById(largeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(largeId));
    }

    @Test
    @DisplayName("getAllCategories should return list not null when empty")
    void testGetAllCategories_NeverReturnsNull() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof List);
    }

    @Test
    @DisplayName("CategoryResponse fields should be accessible and non-null when populated")
    void testCategoryResponse_FieldAccessibility() {
        // Arrange
        Category category = new Category(11L, "Accessible", null);
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(category));

        // Act
        CategoryResponse response = categoryService.getCategoryById(11L);

        // Assert
        assertNotNull(response.getId());
        assertNotNull(response.getName());
        assertEquals(11L, response.getId());
        assertEquals("Accessible", response.getName());
    }
}
