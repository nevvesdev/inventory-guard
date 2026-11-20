package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.view.StockMovementView;
import br.com.nevvesdev.inventory.domain.model.StockMovementType;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        StockMovementType type,
        int quantityDelta,
        UUID orderId,
        Instant createdAt
) {

    public static StockMovementResponse from(StockMovementView view) {
        return new StockMovementResponse(
                view.id(),
                view.type(),
                view.quantityDelta(),
                view.orderId(),
                view.createdAt()
        );
    }
}