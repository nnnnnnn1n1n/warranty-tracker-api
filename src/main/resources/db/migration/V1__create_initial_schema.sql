-- Create category table
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Create product table
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    purchase_date DATE NOT NULL,
    warranty_months INTEGER NOT NULL,
    category_id BIGINT NOT NULL REFERENCES category(id)
);

-- Create index on product.category_id for foreign key lookups
CREATE INDEX idx_product_category_id ON product(category_id);
