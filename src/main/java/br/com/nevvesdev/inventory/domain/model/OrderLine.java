package br.com.nevvesdev.inventory.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID productId, int quantity, BigDecimal unitPrice) {
}