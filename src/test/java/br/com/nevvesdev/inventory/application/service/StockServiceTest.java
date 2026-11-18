package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.view.StockView;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Stock;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void deveRetornarEstoqueDoProduto() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findByProductId(productId))
                .thenReturn(Optional.of(Stock.create(productId, 4)));

        StockView view = stockService.getByProductId(productId);

        assertThat(view.productId()).isEqualTo(productId);
        assertThat(view.quantity()).isEqualTo(4);
    }

    @Test
    void deveFalharQuandoProdutoNaoPossuiEstoque() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getByProductId(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deveReporEstoqueEDevolverSaldoAtualizado() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.increment(eq(productId), eq(5), any(Instant.class))).thenReturn(1);
        when(stockRepository.findByProductId(productId))
                .thenReturn(Optional.of(Stock.create(productId, 8)));

        StockView view = stockService.replenish(productId, 5);

        assertThat(view.quantity()).isEqualTo(8);
    }

    @Test
    void deveFalharAoReporEstoqueDeProdutoInexistente() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.increment(eq(productId), eq(5), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> stockService.replenish(productId, 5))
                .isInstanceOf(ProductNotFoundException.class);
    }
}