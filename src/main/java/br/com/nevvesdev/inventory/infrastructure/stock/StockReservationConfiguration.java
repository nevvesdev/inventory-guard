package br.com.nevvesdev.inventory.infrastructure.stock;

import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StockReservationConfiguration {

    @Bean
    StockReservation stockReservation(
            @Value("${inventory.stock.strategy:conditional-update}") String strategy,
            StockRepository stockRepository
    ) {
        return switch (strategy) {
            case "conditional-update" -> new ConditionalUpdateStockReservation(stockRepository);
            case "pessimistic-lock" -> new PessimisticLockStockReservation(stockRepository);
            case "optimistic-lock" -> new OptimisticLockStockReservation(stockRepository);
            default -> throw new IllegalArgumentException("Estratégia de estoque desconhecida: " + strategy);
        };
    }
}