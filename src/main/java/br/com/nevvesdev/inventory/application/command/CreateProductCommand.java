package br.com.nevvesdev.inventory.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(String sku, String name, BigDecimal price, int initialQuantity) {
}