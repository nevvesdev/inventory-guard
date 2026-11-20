package br.com.nevvesdev.inventory.application.view;

public record PlacedOrder(OrderView order, boolean replayed) {
}