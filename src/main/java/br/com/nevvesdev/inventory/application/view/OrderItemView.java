package br.com.nevvesdev.inventory.application.view;

import br.com.nevvesdev.inventory.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemView(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    public static OrderItemView from(OrderItem item) {
        return new OrderItemView(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}