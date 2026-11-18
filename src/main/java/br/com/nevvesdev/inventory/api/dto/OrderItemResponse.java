package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.view.OrderItemView;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItemView view) {
        return new OrderItemResponse(view.productId(), view.quantity(), view.unitPrice(), view.subtotal());
    }
}