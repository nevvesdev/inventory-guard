package br.com.nevvesdev.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void deveCriarEstoqueComQuantidadeInformada() {
        UUID productId = UUID.randomUUID();

        Stock stock = Stock.create(productId, 7);

        assertThat(stock.getProductId()).isEqualTo(productId);
        assertThat(stock.getQuantity()).isEqualTo(7);
        assertThat(stock.getUpdatedAt()).isNotNull();
    }

    @Test
    void devePermitirEstoqueInicialZerado() {
        Stock stock = Stock.create(UUID.randomUUID(), 0);

        assertThat(stock.getQuantity()).isZero();
    }

    @Test
    void deveRejeitarQuantidadeNegativa() {
        assertThatThrownBy(() -> Stock.create(UUID.randomUUID(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarProdutoNulo() {
        assertThatThrownBy(() -> Stock.create(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}