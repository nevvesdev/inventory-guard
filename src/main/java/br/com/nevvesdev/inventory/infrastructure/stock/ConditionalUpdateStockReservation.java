package br.com.nevvesdev.inventory.infrastructure.stock;

import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ConditionalUpdateStockReservation implements StockReservation {

    private final StockRepository stockRepository;

    public ConditionalUpdateStockReservation(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void reserve(UUID productId, int quantity) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        int updated = stockRepository.decrementIfAvailable(productId, quantity, now);
        if (updated == 0) {
            throw new InsufficientStockException(productId, quantity);
        }
    }
}