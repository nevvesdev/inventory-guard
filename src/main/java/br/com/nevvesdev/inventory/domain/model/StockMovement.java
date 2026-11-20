package br.com.nevvesdev.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private StockMovementType type;

    @Column(name = "quantity_delta", nullable = false, updatable = false)
    private int quantityDelta;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    private StockMovement(UUID productId, StockMovementType type, int quantityDelta, UUID orderId) {
        this.productId = productId;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.orderId = orderId;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public static StockMovement initial(UUID productId, int quantity) {
        return new StockMovement(requireProduct(productId), StockMovementType.INITIAL, requirePositive(quantity), null);
    }

    public static StockMovement replenishment(UUID productId, int quantity) {
        return new StockMovement(
                requireProduct(productId),
                StockMovementType.REPLENISHMENT,
                requirePositive(quantity),
                null
        );
    }

    public static StockMovement sale(UUID productId, int quantity, UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("O pedido é obrigatório para uma venda");
        }
        return new StockMovement(
                requireProduct(productId),
                StockMovementType.SALE,
                -requirePositive(quantity),
                orderId
        );
    }

    private static UUID requireProduct(UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("O produto é obrigatório");
        }
        return productId;
    }

    private static int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        return quantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public StockMovementType getType() {
        return type;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}