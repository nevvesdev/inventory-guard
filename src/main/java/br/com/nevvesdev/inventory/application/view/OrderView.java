package br.com.nevvesdev.inventory.application.view;

import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemView> items
) {

    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemView::from).toList()
        );
    }
}