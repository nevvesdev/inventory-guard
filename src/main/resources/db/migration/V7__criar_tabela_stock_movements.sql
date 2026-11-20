CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    order_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_stock_movements_type CHECK (type IN ('INITIAL', 'REPLENISHMENT', 'SALE')),
    CONSTRAINT ck_stock_movements_delta_not_zero CHECK (quantity_delta <> 0),
    CONSTRAINT ck_stock_movements_sale_has_order CHECK ((type = 'SALE') = (order_id IS NOT NULL)),
    CONSTRAINT ck_stock_movements_delta_sign CHECK (
        (type = 'SALE' AND quantity_delta < 0) OR (type <> 'SALE' AND quantity_delta > 0)
    )
);

CREATE INDEX idx_stock_movements_product_created ON stock_movements (product_id, created_at DESC);

CREATE FUNCTION prevent_stock_movement_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'stock_movements é imutável: operação % não permitida', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_movements_immutable
    BEFORE UPDATE OR DELETE ON stock_movements
    FOR EACH ROW EXECUTE FUNCTION prevent_stock_movement_change();