package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(OrderView view) {
        return new OrderResponse(
                view.id(),
                view.status(),
                view.totalAmount(),
                view.createdAt(),
                view.items().stream().map(OrderItemResponse::from).toList()
        );
    }
}