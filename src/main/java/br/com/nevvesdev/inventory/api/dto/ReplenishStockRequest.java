package br.com.nevvesdev.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplenishStockRequest(
        @Schema(description = "Quantidade a somar ao saldo atual", example = "5")
        @NotNull
        @Positive
        @Max(1_000_000)
        Integer quantity
) {
}