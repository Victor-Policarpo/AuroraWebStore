-- V3__create_suppliers_and_supplier_offers.sql

-- Add external catalog identifiers to products.
ALTER TABLE products
    ADD COLUMN igdb_id BIGINT UNIQUE,
    ADD COLUMN steam_appid BIGINT;

-- Replace the existing active flag with a product lifecycle status.
ALTER TABLE products ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE products ADD CONSTRAINT chk_products_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'OUT_OF_STOCK'));
ALTER TABLE products DROP COLUMN active;

-- Create suppliers.
CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);


-- Create supplier offers.
CREATE TABLE supplier_offers (
     id UUID PRIMARY KEY,
     product_id UUID NOT NULL,
     supplier_id UUID NOT NULL,
     external_product_id VARCHAR(100),
     wholesale_price BIGINT NOT NULL,
     wholesale_currency VARCHAR(3) NOT NULL,
     stock_quantity INT NOT NULL DEFAULT 0,
     active BOOLEAN NOT NULL DEFAULT TRUE,
     created_at TIMESTAMPTZ NOT NULL,
     updated_at TIMESTAMPTZ NOT NULL,

     CONSTRAINT fk_supplier_offers_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
     CONSTRAINT fk_supplier_offers_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT,
     CONSTRAINT uq_supplier_offers_product_supplier UNIQUE (product_id, supplier_id),
     CONSTRAINT chk_supplier_offers_currency CHECK (wholesale_currency IN ('BRL', 'USD', 'EUR')),
     CONSTRAINT chk_supplier_offers_price CHECK (wholesale_price >= 0),
     CONSTRAINT chk_supplier_offers_stock CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_supplier_offers_product_active_price
    ON supplier_offers (product_id, wholesale_price)
    WHERE active = TRUE AND stock_quantity > 0;

CREATE INDEX idx_supplier_offers_supplier
    ON supplier_offers (supplier_id);