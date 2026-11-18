package br.com.nevvesdev.inventory.domain.model;

import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Stock() {
    }

    private Stock(UUID productId, int quantity, Instant now) {
        this.productId = productId;
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public static Stock create(UUID productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("O produto é obrigatório");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("A quantidade não pode ser negativa");
        }
        return new Stock(productId, quantity, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        if (quantity < amount) {
            throw new InsufficientStockException(productId, amount);
        }
        quantity -= amount;
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}