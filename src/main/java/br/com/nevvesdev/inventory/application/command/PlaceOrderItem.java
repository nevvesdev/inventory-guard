package br.com.nevvesdev.inventory.application.command;

import java.util.UUID;

public record PlaceOrderItem(UUID productId, int quantity) {
}