package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.StockMovement;
import br.com.nevvesdev.inventory.domain.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StockLedger {

    private final StockMovementRepository stockMovementRepository;

    public StockLedger(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    public void recordInitialStock(UUID productId, int quantity) {
        if (quantity > 0) {
            stockMovementRepository.save(StockMovement.initial(productId, quantity));
        }
    }

    public void recordReplenishment(UUID productId, int quantity) {
        stockMovementRepository.save(StockMovement.replenishment(productId, quantity));
    }

    public void recordSales(UUID orderId, List<OrderLine> lines) {
        List<StockMovement> movements = lines.stream()
                .map(line -> StockMovement.sale(line.productId(), line.quantity(), orderId))
                .toList();
        stockMovementRepository.saveAll(movements);
    }
}