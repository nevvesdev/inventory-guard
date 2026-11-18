package br.com.nevvesdev.inventory.infrastructure.stock;

import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.model.Stock;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationStrategiesTest {

    @Mock
    private StockRepository stockRepository;

    @Test
    void atualizacaoCondicionalDeveReservarQuandoHaSaldo() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.decrementIfAvailable(eq(productId), eq(2), any(Instant.class))).thenReturn(1);

        new ConditionalUpdateStockReservation(stockRepository).reserve(productId, 2);

        verify(stockRepository).decrementIfAvailable(eq(productId), eq(2), any(Instant.class));
    }

    @Test
    void atualizacaoCondicionalDeveRejeitarQuandoNenhumaLinhaForAfetada() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.decrementIfAvailable(eq(productId), eq(2), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> new ConditionalUpdateStockReservation(stockRepository).reserve(productId, 2))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void lockPessimistaDeveReduzirSaldoDaLinhaBloqueada() {
        UUID productId = UUID.randomUUID();
        Stock stock = Stock.create(productId, 5);
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(stock));

        new PessimisticLockStockReservation(stockRepository).reserve(productId, 2);

        assertThat(stock.getQuantity()).isEqualTo(3);
    }

    @Test
    void lockPessimistaDeveRejeitarQuandoSaldoEInsuficiente() {
        UUID productId = UUID.randomUUID();
        Stock stock = Stock.create(productId, 5);
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> new PessimisticLockStockReservation(stockRepository).reserve(productId, 6))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stock.getQuantity()).isEqualTo(5);
    }

    @Test
    void lockPessimistaDeveRejeitarQuandoNaoExisteLinhaDeEstoque() {
        UUID productId = UUID.randomUUID();
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new PessimisticLockStockReservation(stockRepository).reserve(productId, 1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void lockOtimistaDeveReduzirSaldoEForcarFlush() {
        UUID productId = UUID.randomUUID();
        Stock stock = Stock.create(productId, 5);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        new OptimisticLockStockReservation(stockRepository).reserve(productId, 2);

        assertThat(stock.getQuantity()).isEqualTo(3);
        verify(stockRepository).saveAndFlush(stock);
    }

    @Test
    void lockOtimistaNaoDeveGravarQuandoSaldoEInsuficiente() {
        UUID productId = UUID.randomUUID();
        Stock stock = Stock.create(productId, 1);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> new OptimisticLockStockReservation(stockRepository).reserve(productId, 2))
                .isInstanceOf(InsufficientStockException.class);

        verify(stockRepository, never()).saveAndFlush(any(Stock.class));
    }
}