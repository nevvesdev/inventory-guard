package br.com.nevvesdev.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void deveNormalizarSkuENomeAoCriar() {
        Product product = Product.create("  iph-15  ", "  iPhone 15  ", new BigDecimal("5999.9"));

        assertThat(product.getSku()).isEqualTo("IPH-15");
        assertThat(product.getName()).isEqualTo("iPhone 15");
        assertThat(product.getPrice()).isEqualByComparingTo("5999.90");
        assertThat(product.getPrice().scale()).isEqualTo(2);
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isEqualTo(product.getCreatedAt());
    }

    @Test
    void deveRejeitarSkuEmBranco() {
        assertThatThrownBy(() -> Product.create("   ", "iPhone", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarNomeEmBranco() {
        assertThatThrownBy(() -> Product.create("IPH-15", " ", BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarPrecoZero() {
        assertThatThrownBy(() -> Product.create("IPH-15", "iPhone", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarPrecoNegativo() {
        assertThatThrownBy(() -> Product.create("IPH-15", "iPhone", new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}