package br.com.nevvesdev.inventory.application.view;

import br.com.nevvesdev.inventory.domain.model.Stock;

import java.time.Instant;
import java.util.UUID;

public record StockView(UUID productId, int quantity, Instant updatedAt) {

    public static StockView from(Stock stock) {
        return new StockView(stock.getProductId(), stock.getQuantity(), stock.getUpdatedAt());
    }
}