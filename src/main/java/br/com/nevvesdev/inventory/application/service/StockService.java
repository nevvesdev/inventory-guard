package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.view.StockView;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockLedger stockLedger;

    public StockService(StockRepository stockRepository, StockLedger stockLedger) {
        this.stockRepository = stockRepository;
        this.stockLedger = stockLedger;
    }

    @Transactional(readOnly = true)
    public StockView getByProductId(UUID productId) {
        return stockRepository.findByProductId(productId)
                .map(StockView::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional
    public StockView replenish(UUID productId, int quantity) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        int updated = stockRepository.increment(productId, quantity, now);
        if (updated == 0) {
            throw new ProductNotFoundException(productId);
        }

        stockLedger.recordReplenishment(productId, quantity);

        return getByProductId(productId);
    }
}