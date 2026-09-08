# Warranty Tracker REST API - Requirements Specification

## Introduction

The Warranty Tracker REST API is a Spring Boot-based REST service that enables users to record products and track their warranty expiration dates. The system maintains a catalog of product categories and products, calculates warranty status dynamically, and provides endpoints to query products by warranty expiration timeline. This portfolio project demonstrates clean REST API design principles with Spring Boot, including proper validation, error handling, and data persistence using Spring Data JPA with an H2 in-memory database.

## Glossary

- **API**: Application Programming Interface; the collection of REST endpoints that clients interact with
- **Category**: A separate entity that classifies products (e.g., Kitchen Appliances, Electronics); modeled with one-to-many relationship: `Category 1 ──── * Product`
- **Category_ID**: The unique identifier for a Category entity
- **Product**: An item owned by the user with warranty tracking information, belonging to exactly one Category
- **Warranty_Status**: The calculated state of a product's warranty (ACTIVE, EXPIRING_SOON, or EXPIRED)
- **Warranty_End_Date**: The date when a product's warranty expires, calculated by the system as `purchaseDate + warrantyMonths`
- **Sample_Data**: Pre-loaded categories and products available when the application starts
- **HTTP_Status_Code**: Standard HTTP response codes (200, 201, 400, 404, 500)

## Requirements

### Requirement 1: Product Categories Storage

**User Story:** As a user, I want the API to provide a fixed set of product categories, so that I can classify my products consistently.

#### Acceptance Criteria

1. THE API SHALL provide a predefined set of five categories: Kitchen Appliances, Bedroom, Beauty, Electronics, and Home Appliances.
2. WHEN the application starts, THE System SHALL load all categories into the database.
3. THE Category_Entity SHALL contain at least an id field and a name field.

---

### Requirement 2: Retrieve All Categories

**User Story:** As a user, I want to retrieve all available product categories via an API endpoint, so that I can see valid category options.

#### Acceptance Criteria

1. WHEN a GET request is made to the /api/categories endpoint, THE API SHALL return a 200 HTTP status code.
2. THE API SHALL return a JSON array containing all available categories.
3. EACH category in the response SHALL include an id and name field.
4. IF no categories exist, THE API SHALL return an empty array.

---

### Requirement 3: Create a Product

**User Story:** As a user, I want to create a new product record with purchase date and warranty duration, so that I can start tracking its warranty.

#### Acceptance Criteria

1. WHEN a POST request is made to the /api/products endpoint with valid product data, THE API SHALL create a new product record and return a 201 HTTP status code.
2. THE API SHALL accept the following input fields: name, categoryId, purchaseDate, and warrantyMonths.
3. THE API SHALL calculate the warranty end date as `purchaseDate + warrantyMonths` and store it in the database.
4. THE API SHALL store the product in the database with a system-generated unique id.
5. THE API SHALL return the created product in the response, including the generated id, calculated warrantyEndDate, and category information.
6. IF the request body is missing required fields, THE API SHALL return a 400 HTTP status code with a descriptive error message.
7. IF the categoryId provided does not reference a valid Category, THE API SHALL return a 400 HTTP status code indicating an invalid category.
8. IF the purchaseDate is in the future, THE API SHALL return a 400 HTTP status code with an error message.
9. IF warrantyMonths is less than or equal to 0, THE API SHALL return a 400 HTTP status code with an error message.

---

### Requirement 4: Retrieve All Products

**User Story:** As a user, I want to retrieve all products I have recorded, so that I can see my complete product inventory.

#### Acceptance Criteria

1. WHEN a GET request is made to the /api/products endpoint, THE API SHALL return a 200 HTTP status code.
2. THE API SHALL return a JSON array containing all products.
3. EACH product in the response SHALL include: id, name, categoryId, purchaseDate, warrantyMonths, and warrantyEndDate.
4. EACH product response SHALL include complete Category information (id and name) associated with the product.
5. THE warrantyEndDate for each product SHALL be the calculated value `purchaseDate + warrantyMonths`.
6. IF no products exist, THE API SHALL return an empty array.

---

### Requirement 5: Retrieve a Product by ID

**User Story:** As a user, I want to retrieve a specific product by its ID, so that I can view detailed information about that product.

#### Acceptance Criteria

1. WHEN a GET request is made to the /api/products/{id} endpoint with a valid product ID, THE API SHALL return a 200 HTTP status code and the product details.
2. EACH product response SHALL include: id, name, categoryId, purchaseDate, warrantyMonths, warrantyEndDate, and complete Category information.
3. IF the product ID does not exist, THE API SHALL return a 404 HTTP status code with an error message.
4. IF the product ID is invalid (non-numeric), THE API SHALL return a 400 HTTP status code with an error message.

---

### Requirement 6: Update a Product

**User Story:** As a user, I want to update an existing product's information, so that I can correct or modify product details.

#### Acceptance Criteria

1. WHEN a PUT request is made to the /api/products/{id} endpoint with valid product data, THE API SHALL update the product and return a 200 HTTP status code.
2. THE API SHALL accept updates to: name, categoryId, purchaseDate, and warrantyMonths.
3. THE API SHALL NOT allow direct updates to warrantyEndDate; it will be recalculated as `purchaseDate + warrantyMonths`.
4. THE API SHALL validate all input fields using the same validation rules as product creation.
5. IF the product ID does not exist, THE API SHALL return a 404 HTTP status code.
6. IF the request body is invalid, THE API SHALL return a 400 HTTP status code with a descriptive error message.
7. THE API SHALL return the updated product in the response, including the recalculated warrantyEndDate and Category information.

---

### Requirement 7: Delete a Product

**User Story:** As a user, I want to delete a product record, so that I can remove products that are no longer relevant.

#### Acceptance Criteria

1. WHEN a DELETE request is made to the /api/products/{id} endpoint with a valid product ID, THE API SHALL delete the product and return a 204 HTTP status code.
2. IF the product ID does not exist, THE API SHALL return a 404 HTTP status code.
3. AFTER a product is deleted, WHEN a GET request is made to retrieve that product, THE API SHALL return a 404 HTTP status code.

---

### Requirement 8: Calculate Warranty Status

**User Story:** As a user, I want the API to automatically calculate warranty status based on the current date, so that I can quickly identify which products need attention.

#### Acceptance Criteria

1. THE Warranty_Calculator SHALL evaluate warranty status for each product based on today's date and the product's warrantyEndDate.
2. THE warranty status SHALL follow these fixed rules (not configurable):
   - `EXPIRED`: `today > warrantyEndDate`
   - `EXPIRING_SOON`: `today <= warrantyEndDate AND warrantyEndDate <= today + 30 days`
   - `ACTIVE`: `warrantyEndDate > today + 30 days`
3. THE warranty status SHALL be calculated dynamically at request time and NOT stored in the database.
4. WHEN a product is retrieved via any endpoint, THE API SHALL include the calculated warranty status in the response.

---

### Requirement 9: Retrieve Products by Warranty Expiration

**User Story:** As a user, I want to query products that will expire within a specified number of days, so that I can proactively manage upcoming warranty expirations.

#### Acceptance Criteria

1. WHEN a GET request is made to the /api/products/expiring-soon endpoint with a query parameter specifying the number of days, THE API SHALL return a 200 HTTP status code.
2. THE API SHALL accept a query parameter named "days" with an integer value representing the number of days from today.
3. THE API SHALL return only products whose warranty expires within the specified number of days from today.
4. EACH product in the response SHALL include: id, name, categoryId, purchaseDate, warrantyMonths, warrantyEndDate, warranty status, and complete Category information.
5. IF no products are expiring within the specified timeframe, THE API SHALL return an empty array.
6. IF the "days" parameter is not provided, THE API SHALL use a default value of 30 days.
7. IF the "days" parameter is a negative number, THE API SHALL return a 400 HTTP status code with an error message.

---

### Requirement 10: Sample Data on Application Startup

**User Story:** As a developer testing the API, I want the application to load sample data on startup, so that I can immediately test endpoints with meaningful data.

#### Acceptance Criteria

1. WHEN the application starts, THE System SHALL automatically create the five predefined categories.
2. WHEN the application starts, THE System SHALL populate the database with sample products representing different warranty statuses.
3. THE sample products SHALL include at least one product with ACTIVE warranty, one with EXPIRING_SOON warranty, and one with EXPIRED warranty.

---

### Requirement 11: Validate Product Input Data

**User Story:** As an API consumer, I want the API to validate all product inputs, so that invalid data is rejected with clear error messages.

#### Acceptance Criteria

1. THE Validator SHALL reject product names that are empty or null, returning a 400 HTTP status code.
2. THE Validator SHALL reject categoryId values that do not reference a valid Category ID, returning a 400 HTTP status code.
3. THE Validator SHALL reject purchaseDate values that are in the future, returning a 400 HTTP status code.
4. THE Validator SHALL reject warrantyMonths values that are less than or equal to 0, returning a 400 HTTP status code.
5. THE API SHALL return a descriptive error message for each validation failure.

---

### Requirement 12: Handle Product Not Found

**User Story:** As an API consumer, I want clear error responses when requesting a non-existent product, so that I understand why the request failed.

#### Acceptance Criteria

1. WHEN a GET or PUT or DELETE request is made with a product ID that does not exist, THE API SHALL return a 404 HTTP status code.
2. THE error response SHALL include a descriptive message indicating the product was not found.
3. THE error response SHALL include the requested product ID in the message when possible.

---

### Requirement 13: Handle Invalid Product Data

**User Story:** As an API consumer, I want clear error responses for invalid input data, so that I can correct my requests.

#### Acceptance Criteria

1. WHEN a POST or PUT request is made with invalid or missing fields, THE API SHALL return a 400 HTTP status code.
2. THE error response SHALL describe which field(s) failed validation and why.
3. THE error response SHALL provide guidance on acceptable values when applicable.

---

### Requirement 14: Handle Invalid Category

**User Story:** As an API consumer, I want clear error responses when using an invalid category, so that I can select from valid options.

#### Acceptance Criteria

1. WHEN a POST or PUT request is made with a category that does not exist, THE API SHALL return a 400 HTTP status code.
2. THE error response SHALL indicate that the category is invalid.
3. THE error response MAY include the list of valid categories.

---

### Requirement 15: REST API Compliance

**User Story:** As a developer, I want the API to follow REST conventions, so that it is intuitive and follows industry standards.

#### Acceptance Criteria

1. THE API SHALL use appropriate HTTP methods: GET for retrieval, POST for creation, PUT for updates, DELETE for deletion.
2. THE API SHALL use appropriate HTTP status codes: 200 for successful GET/PUT, 201 for successful POST, 204 for successful DELETE, 400 for bad requests, 404 for not found.
3. THE API SHALL accept and return JSON formatted data for all requests and responses.
4. THE API SHALL use resource-based URL patterns (e.g., /api/products, /api/categories).

---

### Requirement 16: API Documentation

**User Story:** As a developer integrating with the API, I want automated API documentation, so that I can understand the endpoints and their parameters without additional documentation.

#### Acceptance Criteria

1. WHEN the application is running, THE API SHALL expose OpenAPI/Swagger UI documentation at a standard endpoint.
2. THE documentation SHALL describe all available endpoints, request parameters, response formats, and HTTP status codes.
3. THE documentation SHALL allow developers to test endpoints directly through the UI.

---

### Requirement 17: In-Memory Data Storage

**User Story:** As a system, I want to store product and category data during application runtime, so that users can interact with persistent data while the application is running.

#### Acceptance Criteria

1. THE System SHALL use an in-memory H2 database for data storage during runtime.
2. WHEN the application is running, THE System SHALL store all product and category data in the database.
3. WHEN the application restarts, THE System SHALL clear all persisted data, and the database will be reinitialized with sample data on startup.

---

## Summary

This Requirements Specification defines a complete MVP for a personal Warranty Tracker REST API. The system provides CRUD operations for products, calculates warranty status dynamically, and offers query capabilities for products expiring within specified timeframes. All requirements follow EARS patterns and comply with INCOSE quality standards, ensuring clarity, testability, and completeness.
