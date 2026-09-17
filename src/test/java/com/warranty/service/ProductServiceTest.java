package com.warranty.service;

import com.warranty.dto.request.CreateProductRequest;
import com.warranty.dto.request.UpdateProductRequest;
import com.warranty.dto.response.ProductResponse;
import com.warranty.entity.Category;
import com.warranty.entity.Product;
import com.warranty.exception.CategoryNotFoundException;
import com.warranty.exception.ProductNotFoundException;
import com.warranty.exception.ValidationException;
import com.warranty.repository.CategoryRepository;
import com.warranty.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService class.
 * Tests CRUD operations, validation logic, and service interactions with CategoryService.
 * Uses Mockito to mock ProductRepository and CategoryRepository dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category testCategory;
    private Product testProduct;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        // Initialize test data
        today = LocalDate.now();
        testCategory = new Category(1L, "Kitchen Appliances", null);
        
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Microwave");
        testProduct.setPurchaseDate(today.minusMonths(12));
        testProduct.setWarrantyMonths(24);
        testProduct.setCategory(testCategory);

        createRequest = new CreateProductRequest(
                "Laptop",
                1L,
                today.minusMonths(6),
                24
        );

        updateRequest = new UpdateProductRequest(
                "Laptop Pro",
                1L,
                today.minusMonths(6),
                36
        );
    }

    // ===== CREATE PRODUCT TESTS =====

    @Test
    @DisplayName("createProduct should create a valid product successfully")
    void testCreateProduct_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Microwave", response.getName());
        assertEquals(1L, response.getCategoryId());
        assertNotNull(response.getWarrantyEndDate());
        assertNotNull(response.getWarrantyStatus());
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createProduct should calculate warranty end date correctly")
    void testCreateProduct_CalculatesWarrantyEndDate() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2023, 6, 15);
        int warrantyMonths = 24;
        testProduct.setPurchaseDate(purchaseDate);
        testProduct.setWarrantyMonths(warrantyMonths);
        
        createRequest.setPurchaseDate(purchaseDate);
        createRequest.setWarrantyMonths(warrantyMonths);
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertEquals(LocalDate.of(2025, 6, 15), response.getWarrantyEndDate());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when product name is empty")
    void testCreateProduct_EmptyProductName() {
        // Arrange
        createRequest.setName("");

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest),
                "Expected ValidationException for empty product name"
        );
        assertEquals("Product name cannot be empty", exception.getMessage());
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when product name is null")
    void testCreateProduct_NullProductName() {
        // Arrange
        createRequest.setName(null);

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Product name cannot be empty", exception.getMessage());
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when product name is whitespace")
    void testCreateProduct_WhitespaceProductName() {
        // Arrange
        createRequest.setName("   ");

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Product name cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw CategoryNotFoundException when category does not exist")
    void testCreateProduct_CategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setCategoryId(999L);

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> productService.createProduct(createRequest)
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when purchase date is in the future")
    void testCreateProduct_FuturePurchaseDate() {
        // Arrange
        createRequest.setPurchaseDate(today.plusDays(1));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Purchase date cannot be in the future", exception.getMessage());
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("createProduct should accept purchase date equal to today")
    void testCreateProduct_PurchaseDateToday() {
        // Arrange
        createRequest.setPurchaseDate(today);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setPurchaseDate(today);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        verify(productRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when warranty months is zero")
    void testCreateProduct_ZeroWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(0);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Warranty months must be greater than 0", exception.getMessage());
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when warranty months is negative")
    void testCreateProduct_NegativeWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(-5);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Warranty months must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw ValidationException when warranty months is null")
    void testCreateProduct_NullWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.createProduct(createRequest)
        );
        assertEquals("Warranty months must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("createProduct should accept positive warranty months")
    void testCreateProduct_PositiveWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(12);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setWarrantyMonths(12);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        verify(productRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createProduct should return ProductResponse with correct structure")
    void testCreateProduct_ResponseStructure() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setId(5L);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response.getId());
        assertNotNull(response.getName());
        assertNotNull(response.getCategoryId());
        assertNotNull(response.getPurchaseDate());
        assertNotNull(response.getWarrantyMonths());
        assertNotNull(response.getWarrantyEndDate());
        assertNotNull(response.getWarrantyStatus());
        assertNotNull(response.getCategory());
    }

    @Test
    @DisplayName("createProduct should include nested category in response")
    void testCreateProduct_IncludesCategory() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response.getCategory());
        assertEquals(1L, response.getCategory().getId());
        assertEquals("Kitchen Appliances", response.getCategory().getName());
    }

    // ===== UPDATE PRODUCT TESTS =====

    @Test
    @DisplayName("updateProduct should update an existing product successfully")
    void testUpdateProduct_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Laptop Pro");
        updatedProduct.setPurchaseDate(today.minusMonths(6));
        updatedProduct.setWarrantyMonths(36);
        updatedProduct.setCategory(testCategory);
        
        when(productRepository.save(any())).thenReturn(updatedProduct);

        // Act
        ProductResponse response = productService.updateProduct(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Laptop Pro", response.getName());
        assertEquals(36, response.getWarrantyMonths());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("updateProduct should throw ProductNotFoundException when product does not exist")
    void testUpdateProduct_ProductNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(999L, updateRequest)
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("updateProduct should throw ValidationException when product name is empty")
    void testUpdateProduct_EmptyProductName() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        updateRequest.setName("");

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.updateProduct(1L, updateRequest)
        );
        assertEquals("Product name cannot be empty", exception.getMessage());
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("updateProduct should throw CategoryNotFoundException when new category does not exist")
    void testUpdateProduct_CategoryNotFound() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        updateRequest.setCategoryId(999L);

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> productService.updateProduct(1L, updateRequest)
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(productRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("updateProduct should throw ValidationException when purchase date is in future")
    void testUpdateProduct_FuturePurchaseDate() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        updateRequest.setPurchaseDate(today.plusDays(1));

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.updateProduct(1L, updateRequest)
        );
        assertEquals("Purchase date cannot be in the future", exception.getMessage());
    }

    @Test
    @DisplayName("updateProduct should throw ValidationException when warranty months is zero")
    void testUpdateProduct_ZeroWarrantyMonths() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        updateRequest.setWarrantyMonths(0);

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.updateProduct(1L, updateRequest)
        );
        assertEquals("Warranty months must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("updateProduct should recalculate warranty end date")
    void testUpdateProduct_RecalculatesWarrantyEndDate() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        LocalDate newPurchaseDate = LocalDate.of(2024, 1, 1);
        updateRequest.setPurchaseDate(newPurchaseDate);
        updateRequest.setWarrantyMonths(12);
        
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Laptop Pro");
        updatedProduct.setPurchaseDate(newPurchaseDate);
        updatedProduct.setWarrantyMonths(12);
        updatedProduct.setCategory(testCategory);
        
        when(productRepository.save(any())).thenReturn(updatedProduct);

        // Act
        ProductResponse response = productService.updateProduct(1L, updateRequest);

        // Assert
        assertEquals(LocalDate.of(2025, 1, 1), response.getWarrantyEndDate());
    }

    // ===== GET PRODUCT BY ID TESTS =====

    @Test
    @DisplayName("getProductById should return product when ID exists")
    void testGetProductById_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Microwave", response.getName());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getProductById should throw ProductNotFoundException when ID does not exist")
    void testGetProductById_NotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(999L)
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getProductById should return product with calculated warranty status")
    void testGetProductById_IncludesWarrantyStatus() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response.getWarrantyStatus());
        assertTrue("ACTIVE".equals(response.getWarrantyStatus()) || 
                  "EXPIRING_SOON".equals(response.getWarrantyStatus()) ||
                  "EXPIRED".equals(response.getWarrantyStatus()));
    }

    @Test
    @DisplayName("getProductById should return product with calculated warranty end date")
    void testGetProductById_IncludesCalculatedEndDate() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response.getWarrantyEndDate());
        LocalDate expectedEndDate = testProduct.getPurchaseDate()
                .plusMonths(testProduct.getWarrantyMonths());
        assertEquals(expectedEndDate, response.getWarrantyEndDate());
    }

    // ===== GET ALL PRODUCTS TESTS =====

    @Test
    @DisplayName("getAllProducts should return all products when products exist")
    void testGetAllProducts_WithProducts() {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Laptop");
        product2.setPurchaseDate(today.minusMonths(6));
        product2.setWarrantyMonths(24);
        product2.setCategory(testCategory);
        
        List<Product> products = List.of(testProduct, product2);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Microwave", responses.get(0).getName());
        assertEquals("Laptop", responses.get(1).getName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllProducts should return empty list when no products exist")
    void testGetAllProducts_NoProducts() {
        // Arrange
        when(productRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertNotNull(responses);
        assertEquals(0, responses.size());
        assertTrue(responses.isEmpty());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllProducts should return ProductResponse objects not entities")
    void testGetAllProducts_ReturnsResponses() {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertNotNull(responses);
        assertTrue(responses.stream().allMatch(r -> r instanceof ProductResponse));
    }

    @Test
    @DisplayName("getAllProducts should include warranty status for each product")
    void testGetAllProducts_IncludesWarrantyStatus() {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        responses.forEach(r -> assertNotNull(r.getWarrantyStatus()));
    }

    // ===== DELETE PRODUCT TESTS =====

    @Test
    @DisplayName("deleteProduct should delete a product successfully")
    void testDeleteProduct_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    @DisplayName("deleteProduct should throw ProductNotFoundException when product does not exist")
    void testDeleteProduct_NotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(999L)
        );
        assertTrue(exception.getMessage().contains("999"));
        verify(productRepository, times(0)).delete(any());
    }

    @Test
    @DisplayName("deleteProduct should call repository delete exactly once")
    void testDeleteProduct_RepositoryCallCount() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).delete(eq(testProduct));
        verifyNoMoreInteractions(productRepository);
    }

    // ===== GET EXPIRING PRODUCTS TESTS =====

    @Test
    @DisplayName("getExpiringProducts should return products expiring within default 30 days")
    void testGetExpiringProducts_DefaultDays() {
        // Arrange
        Product expiringProduct = new Product();
        expiringProduct.setId(2L);
        expiringProduct.setName("Expiring Product");
        expiringProduct.setPurchaseDate(today.minusMonths(12));
        expiringProduct.setWarrantyMonths(13); // Expires in ~1 month
        expiringProduct.setCategory(testCategory);
        
        List<Product> allProducts = List.of(testProduct, expiringProduct);
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(allProducts);

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(null);

        // Assert
        assertNotNull(responses);
        verify(productRepository, times(1)).findProductsExpiringWithin(any());
    }

    @Test
    @DisplayName("getExpiringProducts should use custom days parameter")
    void testGetExpiringProducts_CustomDays() {
        // Arrange
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(new ArrayList<>());

        // Act
        productService.getExpiringProducts(60);

        // Assert
        verify(productRepository, times(1)).findProductsExpiringWithin(any());
    }

    @Test
    @DisplayName("getExpiringProducts should throw ValidationException when days is negative")
    void testGetExpiringProducts_NegativeDays() {
        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> productService.getExpiringProducts(-1)
        );
        assertEquals("Days parameter must be non-negative", exception.getMessage());
        verify(productRepository, times(0)).findProductsExpiringWithin(any());
    }

    @Test
    @DisplayName("getExpiringProducts should return empty list when no products expiring")
    void testGetExpiringProducts_NoExpiringProducts() {
        // Arrange
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(new ArrayList<>());

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(30);

        // Assert
        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("getExpiringProducts should filter products correctly within range")
    void testGetExpiringProducts_FilteringLogic() {
        // Arrange
        // Product expiring in 15 days (should be included)
        Product expiringProduct = new Product();
        expiringProduct.setId(2L);
        expiringProduct.setName("Expiring Soon");
        expiringProduct.setPurchaseDate(today.minusMonths(11));
        expiringProduct.setWarrantyMonths(12); // Expires in ~1 month
        expiringProduct.setCategory(testCategory);
        
        List<Product> allProducts = List.of(expiringProduct);
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(allProducts);

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(30);

        // Assert
        assertNotNull(responses);
    }

    @Test
    @DisplayName("getExpiringProducts should return ProductResponse objects with warranty status")
    void testGetExpiringProducts_IncludesWarrantyStatus() {
        // Arrange
        Product expiringProduct = new Product();
        expiringProduct.setId(2L);
        expiringProduct.setName("Expiring");
        expiringProduct.setPurchaseDate(today.minusMonths(11));
        expiringProduct.setWarrantyMonths(12);
        expiringProduct.setCategory(testCategory);
        
        List<Product> allProducts = List.of(expiringProduct);
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(allProducts);

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(30);

        // Assert
        responses.forEach(r -> assertNotNull(r.getWarrantyStatus()));
    }

    @Test
    @DisplayName("getExpiringProducts should sort by warranty end date in ascending order")
    void testGetExpiringProducts_SortedByEndDate() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPurchaseDate(today.minusMonths(11));
        product1.setWarrantyMonths(12); // Expires in ~1 month
        product1.setCategory(testCategory);
        
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPurchaseDate(today.minusMonths(10));
        product2.setWarrantyMonths(12); // Expires in ~2 months
        product2.setCategory(testCategory);
        
        List<Product> allProducts = List.of(product2, product1); // Unsorted order
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(allProducts);

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(60);

        // Assert
        assertNotNull(responses);
        // Should be sorted by warranty end date
        if (responses.size() > 1) {
            LocalDate date1 = responses.get(0).getWarrantyEndDate();
            LocalDate date2 = responses.get(1).getWarrantyEndDate();
            assertTrue(date1.isBefore(date2) || date1.isEqual(date2));
        }
    }

    // ===== CATEGORY SERVICE INTERACTION TESTS =====

    @Test
    @DisplayName("createProduct should interact correctly with CategoryService")
    void testCreateProduct_CategoryServiceInteraction() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response.getCategory());
        assertEquals("Kitchen Appliances", response.getCategory().getName());
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("updateProduct should verify category exists before updating")
    void testUpdateProduct_VerifiesCategoryExists() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Updated");
        updatedProduct.setPurchaseDate(today.minusMonths(6));
        updatedProduct.setWarrantyMonths(36);
        updatedProduct.setCategory(testCategory);
        
        when(productRepository.save(any())).thenReturn(updatedProduct);

        // Act
        productService.updateProduct(1L, updateRequest);

        // Assert
        verify(categoryRepository, times(1)).findById(1L);
    }

    // ===== DTO CONVERSION AND RESPONSE TESTS =====

    @Test
    @DisplayName("ProductResponse should contain all required fields from entity")
    void testProductResponse_AllFieldsPresent() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response.getId());
        assertNotNull(response.getName());
        assertNotNull(response.getCategoryId());
        assertNotNull(response.getPurchaseDate());
        assertNotNull(response.getWarrantyMonths());
        assertNotNull(response.getWarrantyEndDate());
        assertNotNull(response.getWarrantyStatus());
        assertNotNull(response.getCategory());
    }

    @Test
    @DisplayName("ProductResponse should preserve product ID")
    void testProductResponse_PreservesId() {
        // Arrange
        testProduct.setId(42L);
        when(productRepository.findById(42L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(42L);

        // Assert
        assertEquals(42L, response.getId());
    }

    @Test
    @DisplayName("ProductResponse should preserve product name")
    void testProductResponse_PreservesName() {
        // Arrange
        testProduct.setName("Test Appliance");
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertEquals("Test Appliance", response.getName());
    }

    @Test
    @DisplayName("ProductResponse should preserve purchase date")
    void testProductResponse_PreservesPurchaseDate() {
        // Arrange
        LocalDate purchaseDate = LocalDate.of(2023, 3, 15);
        testProduct.setPurchaseDate(purchaseDate);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertEquals(purchaseDate, response.getPurchaseDate());
    }

    @Test
    @DisplayName("ProductResponse should preserve warranty months")
    void testProductResponse_PreservesWarrantyMonths() {
        // Arrange
        testProduct.setWarrantyMonths(48);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertEquals(48, response.getWarrantyMonths());
    }

    // ===== EDGE CASES AND BOUNDARY TESTS =====

    @Test
    @DisplayName("createProduct should handle minimum warranty duration (1 month)")
    void testCreateProduct_MinimumWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(1);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setWarrantyMonths(1);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getWarrantyMonths());
    }

    @Test
    @DisplayName("createProduct should handle large warranty duration (120 months)")
    void testCreateProduct_LargeWarrantyMonths() {
        // Arrange
        createRequest.setWarrantyMonths(120);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setWarrantyMonths(120);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(120, response.getWarrantyMonths());
    }

    @Test
    @DisplayName("createProduct should handle very old purchase dates")
    void testCreateProduct_VeryOldPurchaseDate() {
        // Arrange
        LocalDate oldDate = LocalDate.of(2000, 1, 1);
        createRequest.setPurchaseDate(oldDate);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        testProduct.setPurchaseDate(oldDate);
        when(productRepository.save(any())).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(oldDate, response.getPurchaseDate());
    }

    @Test
    @DisplayName("getAllProducts should handle large number of products")
    void testGetAllProducts_LargeNumberOfProducts() {
        // Arrange
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Product product = new Product();
            product.setId((long) i);
            product.setName("Product " + i);
            product.setPurchaseDate(today.minusMonths(6));
            product.setWarrantyMonths(24);
            product.setCategory(testCategory);
            products.add(product);
        }
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertEquals(100, responses.size());
    }

    @Test
    @DisplayName("getExpiringProducts with days=0 should return only today's expiring products")
    void testGetExpiringProducts_ZeroDays() {
        // Arrange
        when(productRepository.findProductsExpiringWithin(any())).thenReturn(new ArrayList<>());

        // Act
        List<ProductResponse> responses = productService.getExpiringProducts(0);

        // Assert
        assertNotNull(responses);
        verify(productRepository, times(1)).findProductsExpiringWithin(any());
    }

    @Test
    @DisplayName("Multiple createProduct calls should not interfere with each other")
    void testCreateProduct_MultipleCallsIndependent() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPurchaseDate(today.minusMonths(6));
        product1.setWarrantyMonths(24);
        product1.setCategory(testCategory);
        
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPurchaseDate(today.minusMonths(12));
        product2.setWarrantyMonths(36);
        product2.setCategory(testCategory);
        
        CreateProductRequest request1 = new CreateProductRequest("Product 1", 1L, today.minusMonths(6), 24);
        CreateProductRequest request2 = new CreateProductRequest("Product 2", 1L, today.minusMonths(12), 36);
        
        when(productRepository.save(any())).thenReturn(product1).thenReturn(product2);

        // Act
        ProductResponse response1 = productService.createProduct(request1);
        ProductResponse response2 = productService.createProduct(request2);

        // Assert
        assertNotEquals(response1.getName(), response2.getName());
        assertEquals("Product 1", response1.getName());
        assertEquals("Product 2", response2.getName());
    }

    @Test
    @DisplayName("ProductResponse should be independent after creation")
    void testProductResponse_Independence() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response1 = productService.getProductById(1L);
        ProductResponse response2 = productService.getProductById(1L);

        // Assert
        assertEquals(response1.getId(), response2.getId());
        assertEquals(response1.getName(), response2.getName());
        verify(productRepository, times(2)).findById(1L);
    }
}
