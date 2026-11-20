package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.IdempotencyKeyReuseException;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.model.OrderStatus;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String KEY = "chave-de-teste-0001";

    @Mock
    private OrderPlacer orderPlacer;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void deveCriarPedidoSemChaveDelegandoAoOrderPlacer() {
        PlaceOrderCommand command = comando();
        OrderView esperado = pedidoConfirmado();
        when(orderPlacer.place(command, null)).thenReturn(new PlacedOrder(esperado, false));

        OrderView resultado = orderService.place(command);

        assertThat(resultado).isEqualTo(esperado);
        verify(orderPlacer, times(1)).place(command, null);
    }

    @Test
    void devePropagarOIndicadorDeReplayDaChaveDeIdempotencia() {
        PlaceOrderCommand command = comando();
        PlacedOrder esperado = new PlacedOrder(pedidoConfirmado(), true);
        when(orderPlacer.place(command, KEY)).thenReturn(esperado);

        PlacedOrder resultado = orderService.place(command, KEY);

        assertThat(resultado.replayed()).isTrue();
        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void deveRepetirPedidoQuandoHaConflitoDeLockOtimista() {
        PlaceOrderCommand command = comando();
        PlacedOrder esperado = new PlacedOrder(pedidoConfirmado(), false);
        when(orderPlacer.place(command, KEY))
                .thenThrow(new OptimisticLockingFailureException("conflito"))
                .thenReturn(esperado);

        PlacedOrder resultado = orderService.place(command, KEY);

        assertThat(resultado).isEqualTo(esperado);
        verify(orderPlacer, times(2)).place(command, KEY);
    }

    @Test
    void deveDesistirAoEsgotarAsTentativas() {
        PlaceOrderCommand command = comando();
        when(orderPlacer.place(command, KEY)).thenThrow(new OptimisticLockingFailureException("conflito"));

        assertThatThrownBy(() -> orderService.place(command, KEY))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(orderPlacer, times(OrderService.MAX_ATTEMPTS)).place(command, KEY);
    }

    @Test
    void naoDeveRepetirQuandoOEstoqueEInsuficiente() {
        PlaceOrderCommand command = comando();
        when(orderPlacer.place(command, KEY)).thenThrow(new InsufficientStockException(UUID.randomUUID(), 1));

        assertThatThrownBy(() -> orderService.place(command, KEY))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderPlacer, times(1)).place(command, KEY);
    }

    @Test
    void naoDeveRepetirQuandoAChaveEReutilizadaComOutroConteudo() {
        PlaceOrderCommand command = comando();
        when(orderPlacer.place(command, KEY)).thenThrow(new IdempotencyKeyReuseException(KEY));

        assertThatThrownBy(() -> orderService.place(command, KEY))
                .isInstanceOf(IdempotencyKeyReuseException.class);

        verify(orderPlacer, times(1)).place(command, KEY);
    }

    @Test
    void deveFalharAoBuscarPedidoInexistente() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private PlaceOrderCommand comando() {
        return new PlaceOrderCommand(List.of(new PlaceOrderItem(UUID.randomUUID(), 1)));
    }

    private OrderView pedidoConfirmado() {
        return new OrderView(UUID.randomUUID(), OrderStatus.CONFIRMED, BigDecimal.TEN, Instant.now(), List.of());
    }
}