package br.com.nevvesdev.inventory.application.port;

import java.util.UUID;

public interface StockReservation {

    void reserve(UUID productId, int quantity);
}