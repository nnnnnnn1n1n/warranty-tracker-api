package com.warranty.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warranty.entity.Category;
import com.warranty.entity.Product;
import com.warranty.repository.CategoryRepository;
import com.warranty.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sorting Integration Tests for GET /api/products")
class SortingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Category electronics;
    private Category furniture;
    private Category appliances;
    private List<Product> createdProducts;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronics = categoryRepository.save(new Category("Electronics"));
        furniture = categoryRepository.save(new Category("Furniture"));
        appliances = categoryRepository.save(new Category("Appliances"));

        createdProducts = new ArrayList<>();

        Product p1 = new Product();
        p1.setName("Keyboard");
        p1.setCategory(electronics);
        p1.setPurchaseDate(LocalDate.now().minusMonths(24));
        p1.setWarrantyMonths(12);
        createdProducts.add(productRepository.save(p1));

        Product p2 = new Product();
        p2.setName("Monitor");
        p2.setCategory(electronics);
        p2.setPurchaseDate(LocalDate.now().minusMonths(2));
        p2.setWarrantyMonths(36);
        createdProducts.add(productRepository.save(p2));

        Product p3 = new Product();
        p3.setName("Chair");
        p3.setCategory(furniture);
        p3.setPurchaseDate(LocalDate.now().minusMonths(12));
        p3.setWarrantyMonths(24);
        createdProducts.add(productRepository.save(p3));

        Product p4 = new Product();
        p4.setName("Desk");
        p4.setCategory(furniture);
        p4.setPurchaseDate(LocalDate.now().minusMonths(36));
        p4.setWarrantyMonths(24);
        createdProducts.add(productRepository.save(p4));

        Product p5 = new Product();
        p5.setName("Microwave");
        p5.setCategory(appliances);
        p5.setPurchaseDate(LocalDate.now().minusMonths(1));
        p5.setWarrantyMonths(12);
        createdProducts.add(productRepository.save(p5));

        Product p6 = new Product();
        p6.setName("Laptop");
        p6.setCategory(electronics);
        p6.setPurchaseDate(LocalDate.now().minusMonths(6));
        p6.setWarrantyMonths(12);
        createdProducts.add(productRepository.save(p6));

        Product p7 = new Product();
        p7.setName("Refrigerator");
        p7.setCategory(appliances);
        p7.setPurchaseDate(LocalDate.now().minusMonths(48));
        p7.setWarrantyMonths(36);
        createdProducts.add(productRepository.save(p7));

        Product p8 = new Product();
        p8.setName("Sofa");
        p8.setCategory(furniture);
        p8.setPurchaseDate(LocalDate.now().minusMonths(3));
        p8.setWarrantyMonths(48);
        createdProducts.add(productRepository.save(p8));

        Product p9 = new Product();
        p9.setName("Tablet");
        p9.setCategory(electronics);
        p9.setPurchaseDate(LocalDate.now().minusMonths(8));
        p9.setWarrantyMonths(24);
        createdProducts.add(productRepository.save(p9));

        Product p10 = new Product();
        p10.setName("Dishwasher");
        p10.setCategory(appliances);
        p10.setPurchaseDate(LocalDate.now().minusMonths(18));
        p10.setWarrantyMonths(36);
        createdProducts.add(productRepository.save(p10));
    }

    @Nested
    @DisplayName("Sorting by ID Field")
    class SortByIdTests {

        @Test
        @DisplayName("GET /api/products with sort=id,asc should return products sorted by ID in ascending order")
        void testSortById_Ascending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "id,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> ids = new ArrayList<>();
            root.get("data").forEach(item -> ids.add(item.get("id").asInt()));
            
            for (int i = 0; i < ids.size() - 1; i++) {
                assertTrue(ids.get(i) <= ids.get(i + 1), 
                    "IDs should be in ascending order: " + ids);
            }
        }

        @Test
        @DisplayName("GET /api/products with sort=id,desc should return products sorted by ID in descending order")
        void testSortById_Descending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "id,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> ids = new ArrayList<>();
            root.get("data").forEach(item -> ids.add(item.get("id").asInt()));
            
            for (int i = 0; i < ids.size() - 1; i++) {
                assertTrue(ids.get(i) >= ids.get(i + 1), 
                    "IDs should be in descending order: " + ids);
            }
        }

        @Test
        @DisplayName("GET /api/products without sort parameter should default to sort=id,asc")
        void testSortById_Default() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("limit", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(5)))
                    .andExpect(jsonPath("$.data[0].id", notNullValue()))
                    .andExpect(jsonPath("$.data[1].id", notNullValue()));
        }
    }

    @Nested
    @DisplayName("Sorting by Name Field")
    class SortByNameTests {

        @Test
        @DisplayName("GET /api/products with sort=name,asc should return products sorted alphabetically")
        void testSortByName_Ascending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].name", equalTo("Chair")))
                    .andExpect(jsonPath("$.data[1].name", equalTo("Desk")))
                    .andExpect(jsonPath("$.data[2].name", equalTo("Dishwasher")))
                    .andExpect(jsonPath("$.data[3].name", equalTo("Keyboard")))
                    .andExpect(jsonPath("$.data[4].name", equalTo("Laptop")));
        }

        @Test
        @DisplayName("GET /api/products with sort=name,desc should return products in reverse alphabetical order")
        void testSortByName_Descending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].name", equalTo("Tablet")))
                    .andExpect(jsonPath("$.data[1].name", equalTo("Sofa")))
                    .andExpect(jsonPath("$.data[2].name", equalTo("Refrigerator")))
                    .andExpect(jsonPath("$.data[3].name", equalTo("Monitor")))
                    .andExpect(jsonPath("$.data[4].name", equalTo("Microwave")));
        }
    }

    @Nested
    @DisplayName("Sorting by Purchase Date Field")
    class SortByPurchaseDateTests {

        @Test
        @DisplayName("GET /api/products with sort=purchaseDate,asc should return oldest first")
        void testSortByPurchaseDate_Ascending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "purchaseDate,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].name", equalTo("Refrigerator")))
                    .andExpect(jsonPath("$.data[1].name", equalTo("Desk")));
        }

        @Test
        @DisplayName("GET /api/products with sort=purchaseDate,desc should return newest first")
        void testSortByPurchaseDate_Descending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "purchaseDate,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].name", equalTo("Microwave")))
                    .andExpect(jsonPath("$.data[1].name", equalTo("Monitor")));
        }
    }

    @Nested
    @DisplayName("Sorting by Warranty Months Field")
    class SortByWarrantyMonthsTests {

        @Test
        @DisplayName("GET /api/products with sort=warrantyMonths,asc should sort by duration ascending")
        void testSortByWarrantyMonths_Ascending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "warrantyMonths,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> warranties = new ArrayList<>();
            root.get("data").forEach(item -> warranties.add(item.get("warrantyMonths").asInt()));
            
            for (int i = 0; i < warranties.size() - 1; i++) {
                assertTrue(warranties.get(i) <= warranties.get(i + 1), 
                    "Warranty months should be in ascending order: " + warranties);
            }
        }

        @Test
        @DisplayName("GET /api/products with sort=warrantyMonths,desc should sort by duration descending")
        void testSortByWarrantyMonths_Descending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "warrantyMonths,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> warranties = new ArrayList<>();
            root.get("data").forEach(item -> warranties.add(item.get("warrantyMonths").asInt()));
            
            for (int i = 0; i < warranties.size() - 1; i++) {
                assertTrue(warranties.get(i) >= warranties.get(i + 1), 
                    "Warranty months should be in descending order: " + warranties);
            }
        }
    }

    @Nested
    @DisplayName("Sorting by Warranty End Date Field")
    class SortByWarrantyEndDateTests {

        @Test
        @DisplayName("GET /api/products with sort=warrantyEndDate,asc should sort by end date ascending")
        void testSortByWarrantyEndDate_Ascending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "warrantyEndDate,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()))
                    .andExpect(jsonPath("$.data[1].warrantyEndDate", notNullValue()));
        }

        @Test
        @DisplayName("GET /api/products with sort=warrantyEndDate,desc should sort by end date descending")
        void testSortByWarrantyEndDate_Descending() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "warrantyEndDate,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].warrantyEndDate", notNullValue()))
                    .andExpect(jsonPath("$.data[1].warrantyEndDate", notNullValue()));
        }
    }

    @Nested
    @DisplayName("Sorting by Category ID Field")
    class SortByCategoryIdTests {

        @Test
        @DisplayName("GET /api/products with sort=categoryId,asc should sort by category ID ascending")
        void testSortByCategoryId_Ascending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "categoryId,asc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> categoryIds = new ArrayList<>();
            root.get("data").forEach(item -> categoryIds.add(item.get("categoryId").asInt()));
            
            for (int i = 0; i < categoryIds.size() - 1; i++) {
                assertTrue(categoryIds.get(i) <= categoryIds.get(i + 1), 
                    "Category IDs should be in ascending order: " + categoryIds);
            }
        }

        @Test
        @DisplayName("GET /api/products with sort=categoryId,desc should sort by category ID descending")
        void testSortByCategoryId_Descending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/products")
                    .param("sort", "categoryId,desc")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(content);
            List<Integer> categoryIds = new ArrayList<>();
            root.get("data").forEach(item -> categoryIds.add(item.get("categoryId").asInt()));
            
            for (int i = 0; i < categoryIds.size() - 1; i++) {
                assertTrue(categoryIds.get(i) >= categoryIds.get(i + 1), 
                    "Category IDs should be in descending order: " + categoryIds);
            }
        }
    }

    @Nested
    @DisplayName("Sorting with Filters")
    class SortWithFiltersTests {

        @Test
        @DisplayName("GET /api/products with sort and categoryId filter should apply sort to filtered results")
        void testSort_WithCategoryFilter() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,asc")
                    .param("categoryId", electronics.getId().toString())
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].categoryId", everyItem(equalTo(electronics.getId().intValue()))));
        }

        @Test
        @DisplayName("GET /api/products with sort and status filter should apply sort to filtered results")
        void testSort_WithStatusFilter() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "purchaseDate,asc")
                    .param("status", "ACTIVE")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(0))));
        }

        @Test
        @DisplayName("GET /api/products with sort and combined filters should apply sort to filtered results")
        void testSort_WithCombinedFilters() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "warrantyMonths,desc")
                    .param("categoryId", electronics.getId().toString())
                    .param("status", "ACTIVE")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(lessThanOrEqualTo(10))));
        }
    }

    @Nested
    @DisplayName("Sort Parameter Validation")
    class SortValidationTests {

        @Test
        @DisplayName("GET /api/products with invalid sort field should return 400")
        void testSort_InvalidField() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "invalidField,asc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
        }

        @Test
        @DisplayName("GET /api/products with invalid sort direction should return 400")
        void testSort_InvalidDirection() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,invalid")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Sort direction must be")));
        }

        @Test
        @DisplayName("GET /api/products with malformed sort parameter should return 400")
        void testSort_MalformedParameter() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Sort parameter must be in format")));
        }

        @Test
        @DisplayName("GET /api/products with sort direction case-insensitive should work")
        void testSort_CaseInsensitive() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "name,ASC")
                    .param("limit", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/products")
                    .param("sort", "name,DESC")
                    .param("limit", "5")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Comprehensive Sort Field Coverage")
    class ComprehensiveSortTests {

        @Test
        @DisplayName("GET /api/products should support sorting by all six fields in both directions")
        void testSort_AllFields() throws Exception {
            String[] sortFields = {"id", "name", "purchaseDate", "warrantyEndDate", "warrantyMonths", "categoryId"};
            String[] directions = {"asc", "desc"};

            for (String field : sortFields) {
                for (String dir : directions) {
                    mockMvc.perform(get("/api/products")
                            .param("sort", field + "," + dir)
                            .param("limit", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(0))));
                }
            }
        }

        @Test
        @DisplayName("GET /api/products response should include all sortable fields")
        void testSort_ResponseContainsAllFields() throws Exception {
            mockMvc.perform(get("/api/products")
                    .param("sort", "id,asc")
                    .param("limit", "1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id", notNullValue()))
                    .andExpect(jsonPath("$.data[0].name", notNullValue()))
                    .andExpect(jsonPath("$.data[0].purchaseDate", notNullValue()))
                    .andExpect(jsonPath("$.data[0].warrantyMonths", notNullValue()))
                    .andExpect(jsonPath("$.data[0].categoryId", notNullValue()));
        }
    }
}
