-- Create the commercial product type catalog.
CREATE TABLE product_types (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_product_types_name UNIQUE (name),
    CONSTRAINT chk_product_types_name_not_blank CHECK (btrim(name) <> '')
);

-- Seed the GAME type used by the IGDB import and by existing products.
INSERT INTO product_types (id, name, description, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'GAME',
    'Default product type for games imported from IGDB',
    TRUE,
    NOW(),
    NOW()
);

-- Add the required association. The column starts nullable to allow the backfill.
ALTER TABLE products
    ADD COLUMN product_type_id UUID;

-- Every existing product is a game in the current catalog, so associate it with GAME.
UPDATE products
SET product_type_id = '00000000-0000-0000-0000-000000000001'
WHERE product_type_id IS NULL;

-- Only enforce NOT NULL after all existing products have a valid type.
ALTER TABLE products
    ALTER COLUMN product_type_id SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_product_type
        FOREIGN KEY (product_type_id)
            REFERENCES product_types (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_products_product_type_id ON products (product_type_id);
