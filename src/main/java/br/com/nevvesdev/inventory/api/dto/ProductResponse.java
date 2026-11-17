package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.view.ProductView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        int quantity,
        Instant createdAt
) {

    public static ProductResponse from(ProductView view) {
        return new ProductResponse(
                view.id(),
                view.sku(),
                view.name(),
                view.price(),
                view.quantity(),
                view.createdAt()
        );
    }
}