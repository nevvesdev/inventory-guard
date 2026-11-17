package br.com.nevvesdev.inventory.application.view;

import br.com.nevvesdev.inventory.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        int quantity,
        Instant createdAt
) {

    public static ProductView of(Product product, int quantity) {
        return new ProductView(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                quantity,
                product.getCreatedAt()
        );
    }
}