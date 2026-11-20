package br.com.nevvesdev.inventory.domain.repository;

import br.com.nevvesdev.inventory.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends Repository<StockMovement, UUID> {

    <S extends StockMovement> S save(S movement);

    <S extends StockMovement> List<S> saveAll(Iterable<S> movements);

    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);
}