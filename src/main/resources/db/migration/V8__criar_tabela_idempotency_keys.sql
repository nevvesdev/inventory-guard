CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(80) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    order_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_idempotency_keys_order FOREIGN KEY (order_id) REFERENCES orders (id)
);