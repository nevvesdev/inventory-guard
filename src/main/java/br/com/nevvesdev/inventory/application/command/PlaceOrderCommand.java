package br.com.nevvesdev.inventory.application.command;

import java.util.List;

public record PlaceOrderCommand(List<PlaceOrderItem> items) {
}