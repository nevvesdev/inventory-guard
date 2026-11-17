package br.com.nevvesdev.inventory.domain.repository;

import br.com.nevvesdev.inventory.domain.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    List<Stock> findByProductIdIn(Collection<UUID> productIds);
}