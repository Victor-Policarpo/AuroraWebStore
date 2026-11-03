-- Preserve each existing product/category assignment before removing the old FK column.
CREATE TABLE product_categories (
    product_id UUID NOT NULL,
    category_id UUID NOT NULL,
    CONSTRAINT pk_product_categories PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_categories_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_categories_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);

INSERT INTO product_categories (product_id, category_id)
SELECT id, category_id FROM products WHERE category_id IS NOT NULL;

CREATE INDEX idx_product_categories_category_id ON product_categories (category_id);

ALTER TABLE products DROP CONSTRAINT fk_products_category;
DROP INDEX IF EXISTS idx_products_category_id;
ALTER TABLE products DROP COLUMN category_id;
