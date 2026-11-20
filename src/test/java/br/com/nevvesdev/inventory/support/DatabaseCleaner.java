package br.com.nevvesdev.inventory.support;

import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(
                "TRUNCATE TABLE order_items, orders, stock_movements, idempotency_keys, stocks, products"
        );
    }
}