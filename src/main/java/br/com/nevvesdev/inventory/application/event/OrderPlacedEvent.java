package br.com.nevvesdev.inventory.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        List<Item> items,
        BigDecimal totalAmount,
        Instant occurredAt
) {

    public OrderPlacedEvent {
        items = List.copyOf(items);
    }

    public record Item(UUID productId, int quantity) {
    }
}
