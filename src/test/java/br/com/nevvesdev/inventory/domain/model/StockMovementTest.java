package br.com.nevvesdev.inventory.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementTest {

    @Test
    void deveCriarMovimentacaoInicialComDeltaPositivo() {
        UUID productId = UUID.randomUUID();

        StockMovement movement = StockMovement.initial(productId, 5);

        assertThat(movement.getType()).isEqualTo(StockMovementType.INITIAL);
        assertThat(movement.getProductId()).isEqualTo(productId);
        assertThat(movement.getQuantityDelta()).isEqualTo(5);
        assertThat(movement.getOrderId()).isNull();
        assertThat(movement.getCreatedAt()).isNotNull();
    }

    @Test
    void deveCriarMovimentacaoDeReposicaoComDeltaPositivo() {
        StockMovement movement = StockMovement.replenishment(UUID.randomUUID(), 3);

        assertThat(movement.getType()).isEqualTo(StockMovementType.REPLENISHMENT);
        assertThat(movement.getQuantityDelta()).isEqualTo(3);
        assertThat(movement.getOrderId()).isNull();
    }

    @Test
    void deveCriarMovimentacaoDeVendaComDeltaNegativoEPedido() {
        UUID orderId = UUID.randomUUID();

        StockMovement movement = StockMovement.sale(UUID.randomUUID(), 2, orderId);

        assertThat(movement.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(movement.getQuantityDelta()).isEqualTo(-2);
        assertThat(movement.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void deveRejeitarQuantidadeNaoPositiva() {
        assertThatThrownBy(() -> StockMovement.initial(UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StockMovement.replenishment(UUID.randomUUID(), -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StockMovement.sale(UUID.randomUUID(), 0, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarVendaSemPedido() {
        assertThatThrownBy(() -> StockMovement.sale(UUID.randomUUID(), 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarMovimentacaoSemProduto() {
        assertThatThrownBy(() -> StockMovement.initial(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}