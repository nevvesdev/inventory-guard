package br.com.nevvesdev.inventory.domain.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final int requestedQuantity;

    public InsufficientStockException(UUID productId, int requestedQuantity) {
        super("Estoque insuficiente para o produto " + productId + " (solicitado: " + requestedQuantity + ")");
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}