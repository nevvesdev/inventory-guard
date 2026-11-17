package br.com.nevvesdev.inventory.api.dto;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        @Size(min = 3, max = 40)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String sku,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        @Max(1_000_000)
        Integer initialQuantity
) {

    public CreateProductCommand toCommand() {
        return new CreateProductCommand(sku, name, price, initialQuantity);
    }
}