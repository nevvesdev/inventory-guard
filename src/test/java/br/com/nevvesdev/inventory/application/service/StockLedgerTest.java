package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.StockMovement;
import br.com.nevvesdev.inventory.domain.model.StockMovementType;
import br.com.nevvesdev.inventory.domain.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockLedgerTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Captor
    private ArgumentCaptor<StockMovement> movementCaptor;

    @Captor
    private ArgumentCaptor<Iterable<StockMovement>> movementsCaptor;

    @InjectMocks
    private StockLedger stockLedger;

    @Test
    void naoDeveRegistrarMovimentacaoInicialParaEstoqueZerado() {
        stockLedger.recordInitialStock(UUID.randomUUID(), 0);

        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void deveRegistrarMovimentacaoInicial() {
        UUID productId = UUID.randomUUID();

        stockLedger.recordInitialStock(productId, 5);

        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(StockMovementType.INITIAL);
        assertThat(movementCaptor.getValue().getProductId()).isEqualTo(productId);
        assertThat(movementCaptor.getValue().getQuantityDelta()).isEqualTo(5);
    }

    @Test
    void deveRegistrarReposicao() {
        UUID productId = UUID.randomUUID();

        stockLedger.recordReplenishment(productId, 4);

        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(StockMovementType.REPLENISHMENT);
        assertThat(movementCaptor.getValue().getQuantityDelta()).isEqualTo(4);
    }

    @Test
    void deveRegistrarUmaVendaPorLinhaDoPedido() {
        UUID orderId = UUID.randomUUID();
        UUID produtoA = UUID.randomUUID();
        UUID produtoB = UUID.randomUUID();

        stockLedger.recordSales(orderId, List.of(
                new OrderLine(produtoA, 2, new BigDecimal("10.00")),
                new OrderLine(produtoB, 1, new BigDecimal("20.00"))
        ));

        verify(stockMovementRepository).saveAll(movementsCaptor.capture());
        assertThat(movementsCaptor.getValue())
                .extracting(StockMovement::getQuantityDelta)
                .containsExactly(-2, -1);
        assertThat(movementsCaptor.getValue())
                .extracting(StockMovement::getOrderId)
                .containsOnly(orderId);
    }
}