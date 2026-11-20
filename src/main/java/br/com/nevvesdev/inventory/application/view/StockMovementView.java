package br.com.nevvesdev.inventory.application.view;

import br.com.nevvesdev.inventory.domain.model.StockMovement;
import br.com.nevvesdev.inventory.domain.model.StockMovementType;

import java.time.Instant;
import java.util.UUID;

public record StockMovementView(
        UUID id,
        StockMovementType type,
        int quantityDelta,
        UUID orderId,
        Instant createdAt
) {

    public static StockMovementView from(StockMovement movement) {
        return new StockMovementView(
                movement.getId(),
                movement.getType(),
                movement.getQuantityDelta(),
                movement.getOrderId(),
                movement.getCreatedAt()
        );
    }
}