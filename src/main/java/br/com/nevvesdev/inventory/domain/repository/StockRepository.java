package br.com.nevvesdev.inventory.domain.repository;

import br.com.nevvesdev.inventory.domain.model.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    List<Stock> findByProductIdIn(Collection<UUID> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") UUID productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Stock s
               SET s.quantity = s.quantity - :quantity,
                   s.updatedAt = :now,
                   s.version = s.version + 1
             WHERE s.productId = :productId
               AND s.quantity >= :quantity
            """)
    int decrementIfAvailable(
            @Param("productId") UUID productId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Stock s
               SET s.quantity = s.quantity + :quantity,
                   s.updatedAt = :now,
                   s.version = s.version + 1
             WHERE s.productId = :productId
            """)
    int increment(
            @Param("productId") UUID productId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );
}