package br.com.nevvesdev.inventory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull
        UUID productId,

        @NotNull
        @Positive
        @Max(1000)
        Integer quantity
) {
}