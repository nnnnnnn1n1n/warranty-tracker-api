package com.warranty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI/Swagger UI Configuration for the Warranty Tracker REST API.
 * Defines API metadata, reusable schemas, and the GET /api/products endpoint
 * with full documentation including request/response examples.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure the OpenAPI specification with custom metadata, schemas, and endpoint definitions.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Warranty Tracker REST API")
                        .version("1.0.0")
                        .description("REST API for managing product categories and tracking warranty expiration dates"))
                .schema("PaginationMetadata", createPaginationMetadataSchema())
                .schema("WarrantyStatus", createWarrantyStatusSchema())
                .schema("CategoryResponse", createCategoryResponseSchema())
                .schema("ProductResponse", createProductResponseSchema())
                .schema("ProductListResponse", createProductListResponseSchema());

        openAPI.paths(addProductListEndpoint());

        return openAPI;
    }

    // -------------------------------------------------------------------------
    // Schema definitions
    // -------------------------------------------------------------------------

    /**
     * PaginationMetadata schema — returned in every paginated list response.
     */
    private Schema<?> createPaginationMetadataSchema() {
        return new Schema<>()
                .type("object")
                .description("Pagination metadata for list responses")
                .addProperty("totalCount", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("Total count of items matching all filters before pagination"))
                .addProperty("limit", new Schema<>()
                        .type("integer")
                        .format("int32")
                        .description("Page size (number of items per response)"))
                .addProperty("offset", new Schema<>()
                        .type("integer")
                        .format("int32")
                        .description("Offset applied to the result set"))
                .addProperty("hasMore", new Schema<>()
                        .type("boolean")
                        .description("true if (offset + limit) < totalCount, indicating more results are available"));
    }

    /**
     * WarrantyStatus enum schema.
     */
    private Schema<?> createWarrantyStatusSchema() {
        return new Schema<String>()
                .type("string")
                .description("Product warranty status:\n" +
                        "- ACTIVE: warranty expires more than 30 days from today\n" +
                        "- EXPIRING_SOON: warranty expires within 30 days\n" +
                        "- EXPIRED: warranty end date has passed")
                ._enum(Arrays.asList("ACTIVE", "EXPIRING_SOON", "EXPIRED"));
    }

    /**
     * CategoryResponse schema — nested inside ProductResponse.
     */
    private Schema<?> createCategoryResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Category information")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("Category ID"))
                .addProperty("name", new Schema<>()
                        .type("string")
                        .description("Category name"));
    }

    /**
     * ProductResponse schema — includes calculated warrantyStatus and warrantyEndDate.
     */
    private Schema<?> createProductResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Product information with calculated warranty fields")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("Product ID"))
                .addProperty("name", new Schema<>()
                        .type("string")
                        .description("Product name"))
                .addProperty("categoryId", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("Category ID"))
                .addProperty("purchaseDate", new Schema<>()
                        .type("string")
                        .format("date")
                        .description("Purchase date (ISO 8601 format)"))
                .addProperty("warrantyMonths", new Schema<>()
                        .type("integer")
                        .format("int32")
                        .description("Warranty duration in months"))
                .addProperty("warrantyEndDate", new Schema<>()
                        .type("string")
                        .format("date")
                        .description("Calculated warranty end date (ISO 8601 format)"))
                .addProperty("warrantyStatus", new Schema<>()
                        .type("string")
                        ._enum(Arrays.asList("ACTIVE", "EXPIRING_SOON", "EXPIRED"))
                        .description("Calculated warranty status based on today's date"))
                .addProperty("category", new Schema<>()
                        .$ref("#/components/schemas/CategoryResponse")
                        .description("Category information"));
    }

    /**
     * ProductListResponse schema — paginated wrapper.
     */
    private Schema<?> createProductListResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Paginated product list response")
                .addProperty("data", new Schema<>()
                        .type("array")
                        .items(new Schema<>().$ref("#/components/schemas/ProductResponse"))
                        .description("Array of products matching filters and pagination"))
                .addProperty("pagination", new Schema<>()
                        .$ref("#/components/schemas/PaginationMetadata")
                        .description("Pagination metadata"));
    }

    // -------------------------------------------------------------------------
    // Endpoint definition
    // -------------------------------------------------------------------------

    /**
     * Define the GET /api/products endpoint including parameters, responses, and examples.
     */
    private Paths addProductListEndpoint() {
        Paths paths = new Paths();
        PathItem pathItem = new PathItem();

        Operation getOperation = new Operation()
                .summary("List products with pagination, filtering, and sorting")
                .description("Retrieve a paginated list of products with optional filtering by category or warranty " +
                        "status, and sorting by product attributes. Warranty statuses are calculated at request time " +
                        "based on the current date (Asia/Bangkok timezone).")
                .tags(Arrays.asList("product-controller"));

        getOperation.addParametersItem(createLimitParameter());
        getOperation.addParametersItem(createOffsetParameter());
        getOperation.addParametersItem(createCategoryIdParameter());
        getOperation.addParametersItem(createStatusParameter());
        getOperation.addParametersItem(createSortParameter());

        getOperation.responses(createProductListResponses());

        pathItem.get(getOperation);
        paths.addPathItem("/api/products", pathItem);

        return paths;
    }

    // -------------------------------------------------------------------------
    // Query parameter definitions
    // -------------------------------------------------------------------------

    private Parameter createLimitParameter() {
        return new QueryParameter()
                .name("limit")
                .description("Maximum number of products to return per page (default: 20, must be > 0)")
                .required(false)
                .schema(new Schema<>()
                        .type("integer")
                        .format("int32")
                        .minimum(new java.math.BigDecimal(1))
                        ._default(20)
                        .example(20));
    }

    private Parameter createOffsetParameter() {
        return new QueryParameter()
                .name("offset")
                .description("Number of products to skip before starting to collect results (default: 0, must be >= 0)")
                .required(false)
                .schema(new Schema<>()
                        .type("integer")
                        .format("int32")
                        .minimum(new java.math.BigDecimal(0))
                        ._default(0)
                        .example(0));
    }

    private Parameter createCategoryIdParameter() {
        return new QueryParameter()
                .name("categoryId")
                .description("Filter products by category ID (optional). Non-existent category returns empty results.")
                .required(false)
                .schema(new Schema<>()
                        .type("integer")
                        .format("int64")
                        .example(1));
    }

    private Parameter createStatusParameter() {
        return new QueryParameter()
                .name("status")
                .description("Filter products by warranty status (optional, case-sensitive). " +
                        "Allowed values: ACTIVE, EXPIRING_SOON, EXPIRED")
                .required(false)
                .schema(new Schema<>()
                        .type("string")
                        ._enum(Arrays.asList("ACTIVE", "EXPIRING_SOON", "EXPIRED"))
                        .example("ACTIVE"));
    }

    private Parameter createSortParameter() {
        return new QueryParameter()
                .name("sort")
                .description("Sort results by a field and direction in 'field,direction' format (default: 'id,asc'). " +
                        "Supported fields: id, name, purchaseDate, warrantyEndDate, warrantyMonths, categoryId. " +
                        "Directions: asc, desc.")
                .required(false)
                .schema(new Schema<>()
                        .type("string")
                        .pattern("^(id|name|purchaseDate|warrantyEndDate|warrantyMonths|categoryId),(asc|desc)$")
                        .example("warrantyEndDate,asc"));
    }

    // -------------------------------------------------------------------------
    // Response definitions with examples
    // -------------------------------------------------------------------------

    /**
     * Build the ApiResponses for GET /api/products.
     * Includes 200 OK (with multiple named examples) and 400 Bad Request.
     */
    private ApiResponses createProductListResponses() {
        ApiResponses responses = new ApiResponses();

        // 200 OK
        ApiResponse okResponse = new ApiResponse()
                .description("Successful retrieval of product list. " +
                        "Returns an empty data array when no products match the criteria " +
                        "(including when a non-existent categoryId is provided).")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ProductListResponse"))
                                .addExamples("default", createDefaultListExample())
                                .addExamples("filterByStatus", createFilterByStatusExample())
                                .addExamples("secondPage", createSecondPageExample())
                                .addExamples("emptyResults", createEmptyResultsExample())));
        responses.addApiResponse("200", okResponse);

        // 400 Bad Request
        ApiResponse badRequestResponse = new ApiResponse()
                .description("Invalid query parameters. Possible error messages:\n" +
                        "- `Limit must be greater than zero` — limit ≤ 0\n" +
                        "- `Offset must be greater than or equal to zero` — offset < 0\n" +
                        "- `Invalid warranty status: <value>` — status not in {ACTIVE, EXPIRING_SOON, EXPIRED}\n" +
                        "- `Invalid sort format: <value>` — sort is not 'field,direction'\n" +
                        "- `Invalid sort field: <field>` — field not in whitelist\n" +
                        "- `Invalid sort direction: <dir>` — direction is not asc or desc")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>()
                                        .type("object")
                                        .addProperty("message", new Schema<>()
                                                .type("string")
                                                .description("Human-readable error description")))
                                .addExamples("invalidLimit", createInvalidLimitExample())
                                .addExamples("invalidStatus", createInvalidStatusExample())
                                .addExamples("invalidSort", createInvalidSortExample())));
        responses.addApiResponse("400", badRequestResponse);

        return responses;
    }

    // -------------------------------------------------------------------------
    // Named 200 OK examples
    // -------------------------------------------------------------------------

    /**
     * Default example: first page, no filters, sorted by id,asc.
     * Shows two products in different warranty states.
     */
    private Example createDefaultListExample() {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> products = new ArrayList<>();
        products.add(buildProduct(1, "Dell XPS 13",     1, "2024-06-15", 24, "2026-06-15", "ACTIVE",        1, "Electronics"));
        products.add(buildProduct(2, "Apple AirPods Pro", 1, "2024-09-01", 12, "2025-09-01", "ACTIVE",       1, "Electronics"));
        products.add(buildProduct(3, "Samsung Galaxy S24", 1, "2023-03-10", 24, "2025-03-10", "EXPIRED",     1, "Electronics"));

        response.put("data", products);
        response.put("pagination", buildPagination(150, 20, 0, true));

        return new Example()
                .summary("Default listing — first page, no filters")
                .description("GET /api/products — first 20 products sorted by id ascending (default).")
                .value(response);
    }

    /**
     * Example: filter by status=EXPIRING_SOON, sorted by warrantyEndDate,asc.
     */
    private Example createFilterByStatusExample() {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> products = new ArrayList<>();
        products.add(buildProduct(7,  "LG OLED TV",      2, "2024-10-05", 12, "2025-10-05", "EXPIRING_SOON", 2, "Home Appliances"));
        products.add(buildProduct(11, "Sony WH-1000XM5", 1, "2024-10-20", 12, "2025-10-20", "EXPIRING_SOON", 1, "Electronics"));

        response.put("data", products);
        response.put("pagination", buildPagination(2, 20, 0, false));

        return new Example()
                .summary("Filter by status=EXPIRING_SOON")
                .description("GET /api/products?status=EXPIRING_SOON&sort=warrantyEndDate,asc — " +
                        "products whose warranty expires within 30 days, sorted by end date.")
                .value(response);
    }

    /**
     * Example: second page with limit=5, offset=5.
     */
    private Example createSecondPageExample() {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> products = new ArrayList<>();
        products.add(buildProduct(6,  "Canon EOS R50", 3, "2023-05-20", 24, "2025-05-20", "ACTIVE", 3, "Cameras"));
        products.add(buildProduct(7,  "LG OLED TV",    2, "2024-10-05", 12, "2025-10-05", "EXPIRING_SOON", 2, "Home Appliances"));
        products.add(buildProduct(8,  "iPad Air",      1, "2024-01-10", 12, "2025-01-10", "EXPIRED",       1, "Electronics"));
        products.add(buildProduct(9,  "DJI Mini 4 Pro", 3, "2024-07-22", 12, "2025-07-22", "ACTIVE",       3, "Cameras"));
        products.add(buildProduct(10, "Dyson V15",     2, "2023-11-30", 24, "2025-11-30", "ACTIVE",        2, "Home Appliances"));

        response.put("data", products);
        response.put("pagination", buildPagination(150, 5, 5, true));

        return new Example()
                .summary("Second page — limit=5, offset=5")
                .description("GET /api/products?limit=5&offset=5 — second page of 5 products.")
                .value(response);
    }

    /**
     * Example: non-existent categoryId returns empty results (HTTP 200, not 404).
     */
    private Example createEmptyResultsExample() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", new ArrayList<>());
        response.put("pagination", buildPagination(0, 20, 0, false));

        return new Example()
                .summary("Empty results — non-existent categoryId")
                .description("GET /api/products?categoryId=9999 — non-existent category returns HTTP 200 with empty data.")
                .value(response);
    }

    // -------------------------------------------------------------------------
    // Named 400 Bad Request examples
    // -------------------------------------------------------------------------

    private Example createInvalidLimitExample() {
        return new Example()
                .summary("limit=0 — non-positive value")
                .description("GET /api/products?limit=0 — limit must be greater than zero.")
                .value(Map.of("message", "Limit must be greater than zero"));
    }

    private Example createInvalidStatusExample() {
        return new Example()
                .summary("status=UNKNOWN — unsupported enum value")
                .description("GET /api/products?status=UNKNOWN — status must be ACTIVE, EXPIRING_SOON, or EXPIRED.")
                .value(Map.of("message", "Invalid warranty status: UNKNOWN"));
    }

    private Example createInvalidSortExample() {
        return new Example()
                .summary("sort=price,asc — field not in whitelist")
                .description("GET /api/products?sort=price,asc — 'price' is not a supported sort field.")
                .value(Map.of("message", "Invalid sort field: price"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build a sample product map for use inside example responses.
     */
    private Map<String, Object> buildProduct(int id, String name, int categoryId,
                                              String purchaseDate, int warrantyMonths,
                                              String warrantyEndDate, String warrantyStatus,
                                              int catId, String catName) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", id);
        product.put("name", name);
        product.put("categoryId", categoryId);
        product.put("purchaseDate", purchaseDate);
        product.put("warrantyMonths", warrantyMonths);
        product.put("warrantyEndDate", warrantyEndDate);
        product.put("warrantyStatus", warrantyStatus);

        Map<String, Object> category = new LinkedHashMap<>();
        category.put("id", catId);
        category.put("name", catName);
        product.put("category", category);

        return product;
    }

    /**
     * Build a pagination metadata map.
     */
    private Map<String, Object> buildPagination(long totalCount, int limit, int offset, boolean hasMore) {
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("totalCount", totalCount);
        pagination.put("limit", limit);
        pagination.put("offset", offset);
        pagination.put("hasMore", hasMore);
        return pagination;
    }
}
