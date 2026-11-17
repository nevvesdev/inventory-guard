package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.view.StockView;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(UUID productId, int quantity, Instant updatedAt) {

    public static StockResponse from(StockView view) {
        return new StockResponse(view.productId(), view.quantity(), view.updatedAt());
    }
}