package br.com.nevvesdev.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderItemRequest(
        @Schema(description = "Identificador do produto", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull
        UUID productId,

        @Schema(description = "Quantidade desejada, de 1 a 1000", example = "2")
        @NotNull
        @Positive
        @Max(1000)
        Integer quantity
) {
}