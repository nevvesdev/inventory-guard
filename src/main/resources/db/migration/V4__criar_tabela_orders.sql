CREATE TABLE orders (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_orders_status CHECK (status IN ('CONFIRMED')),
    CONSTRAINT ck_orders_total_non_negative CHECK (total_amount >= 0)
);