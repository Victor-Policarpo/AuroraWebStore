-- 1. users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

-- 2. categories
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
);

-- 3. products
CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    brand VARCHAR(100),
    platform VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    base_price BIGINT NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_base_currency CHECK (base_currency IN ('BRL', 'USD', 'EUR')),
    CONSTRAINT chk_products_base_price CHECK (base_price >= 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);

-- 4. product_images
CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- 5. carts
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_carts_user_id UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 6. cart_items
CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);

-- 7. coupons
CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    discount_type VARCHAR(30) NOT NULL,
    discount_value BIGINT NOT NULL,
    minimum_amount BIGINT,
    max_uses INTEGER,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_coupons_discount_value_range CHECK (
        (discount_type = 'PERCENTAGE' AND discount_value BETWEEN 1 AND 100) OR
        (discount_type = 'FIXED_AMOUNT' AND discount_value > 0)
    ),
    CONSTRAINT chk_coupons_minimum_amount CHECK (minimum_amount IS NULL OR minimum_amount >= 0),
    CONSTRAINT chk_coupons_max_uses CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT chk_coupons_date_order CHECK (
        starts_at IS NULL OR
        expires_at IS NULL OR
        expires_at > starts_at
    )
);

-- 8. orders
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    coupon_id UUID,
    status VARCHAR(30) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_orders_currency CHECK (currency IN ('BRL', 'USD', 'EUR'))
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_coupon_id ON orders (coupon_id);

-- 9. order_items
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    platform VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_order_items_subtotal CHECK (subtotal > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- 10. product_keys
CREATE TABLE product_keys (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    order_item_id UUID,
    key_value_encrypted TEXT NOT NULL,
    key_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reserved_at TIMESTAMPTZ,
    sold_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_product_keys_fingerprint
        UNIQUE (key_fingerprint),

    CONSTRAINT fk_product_keys_product
        FOREIGN KEY (product_id)
        REFERENCES products (id),

    CONSTRAINT fk_product_keys_order_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items (id),

    CONSTRAINT chk_product_keys_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD')),

    CONSTRAINT chk_product_keys_status_lifecycle
        CHECK (
            (
                status = 'AVAILABLE'
                AND order_item_id IS NULL
                AND reserved_at IS NULL
                AND sold_at IS NULL
            )
            OR
            (
                status = 'RESERVED'
                AND order_item_id IS NOT NULL
                AND reserved_at IS NOT NULL
                AND sold_at IS NULL
            )
            OR
            (
                status = 'SOLD'
                AND order_item_id IS NOT NULL
                AND sold_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_product_keys_product_id ON product_keys (product_id);
CREATE INDEX idx_product_keys_order_item_id ON product_keys (order_item_id);

-- 11. payments
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_payments_provider_payment_id UNIQUE (provider_payment_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_payments_currency CHECK (currency IN ('BRL', 'USD', 'EUR')),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id ON payments (order_id);

-- 12. coupon_usages
CREATE TABLE coupon_usages (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    used_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_coupon_usages_coupon_user UNIQUE (coupon_id, user_id),
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_usages_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_coupon_usages_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages (coupon_id);
CREATE INDEX idx_coupon_usages_user_id ON coupon_usages (user_id);
CREATE INDEX idx_coupon_usages_order_id ON coupon_usages (order_id);