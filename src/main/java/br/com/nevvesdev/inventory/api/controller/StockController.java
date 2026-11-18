package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.dto.ReplenishStockRequest;
import br.com.nevvesdev.inventory.api.dto.StockResponse;
import br.com.nevvesdev.inventory.application.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public StockResponse getByProductId(@PathVariable UUID productId) {
        return StockResponse.from(stockService.getByProductId(productId));
    }

    @PostMapping("/replenishments")
    public StockResponse replenish(
            @PathVariable UUID productId,
            @Valid @RequestBody ReplenishStockRequest request
    ) {
        return StockResponse.from(stockService.replenish(productId, request.quantity()));
    }
}