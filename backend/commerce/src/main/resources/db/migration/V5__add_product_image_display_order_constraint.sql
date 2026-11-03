-- Add optional trailer URL.
ALTER TABLE products
    ADD COLUMN trailer_url VARCHAR(500);

-- Allow products to be imported before commercial details are defined.
ALTER TABLE products
    ALTER COLUMN platform DROP NOT NULL,
    ALTER COLUMN region DROP NOT NULL,
    ALTER COLUMN base_price DROP NOT NULL,
    ALTER COLUMN base_currency DROP NOT NULL;

-- Enforce required fields introduced by the updated entity.
ALTER TABLE products
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN brand SET NOT NULL,
    ALTER COLUMN igdb_id SET NOT NULL;

-- Enforce uniqueness of Steam application IDs.
ALTER TABLE products
    ADD CONSTRAINT uk_products_steam_appid UNIQUE (steam_appid);

-- Enforce uniqueness of product images by product and display order.
ALTER TABLE product_images
    ADD CONSTRAINT uk_product_images_product_display_order
        UNIQUE (product_id, display_order);