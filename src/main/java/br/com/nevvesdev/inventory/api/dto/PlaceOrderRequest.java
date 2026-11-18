package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty
        @Size(max = 50)
        List<@Valid OrderItemRequest> items
) {

    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(
                items.stream()
                        .map(item -> new PlaceOrderItem(item.productId(), item.quantity()))
                        .toList()
        );
    }
}