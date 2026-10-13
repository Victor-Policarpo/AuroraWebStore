-- Update product_keys to track supplier origin and acquisition cost.
ALTER TABLE product_keys
    ADD COLUMN supplier_id UUID,
    ADD COLUMN cost_price BIGINT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL;

-- Preserve the key if its supplier is deleted.
ALTER TABLE product_keys
    ADD CONSTRAINT fk_product_keys_supplier
        FOREIGN KEY (supplier_id)
            REFERENCES suppliers (id)
            ON DELETE SET NULL;

-- Optimize checkout queries that search available keys by product and status.
CREATE INDEX idx_product_keys_checkout
    ON product_keys (product_id, status);