package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @Schema(description = "Itens do pedido, de 1 a 50")
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