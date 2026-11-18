package br.com.nevvesdev.inventory.infrastructure.stock;

import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.model.Stock;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;

import java.util.UUID;

public class OptimisticLockStockReservation implements StockReservation {

    private final StockRepository stockRepository;

    public OptimisticLockStockReservation(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void reserve(UUID productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new InsufficientStockException(productId, quantity));

        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}