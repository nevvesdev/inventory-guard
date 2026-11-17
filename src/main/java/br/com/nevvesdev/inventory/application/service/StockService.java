package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.view.StockView;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public StockView getByProductId(UUID productId) {
        return stockRepository.findByProductId(productId)
                .map(StockView::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}