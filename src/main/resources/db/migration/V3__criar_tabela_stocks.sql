CREATE TABLE stocks (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stocks_product UNIQUE (product_id),
    CONSTRAINT fk_stocks_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_stocks_quantity_non_negative CHECK (quantity >= 0)
);