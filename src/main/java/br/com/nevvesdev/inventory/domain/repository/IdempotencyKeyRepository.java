package br.com.nevvesdev.inventory.domain.repository;

import br.com.nevvesdev.inventory.domain.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (idempotency_key, request_hash, created_at)
            VALUES (:key, :requestHash, :now)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("key") String key,
            @Param("requestHash") String requestHash,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true)
    @Query("UPDATE IdempotencyKey k SET k.orderId = :orderId WHERE k.idempotencyKey = :key")
    int attachOrder(@Param("key") String key, @Param("orderId") UUID orderId);
}