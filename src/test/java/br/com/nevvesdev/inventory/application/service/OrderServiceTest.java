package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.model.OrderStatus;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderPlacer orderPlacer;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void deveDelegarCriacaoDoPedidoAoOrderPlacer() {
        PlaceOrderCommand command = new PlaceOrderCommand(List.of(new PlaceOrderItem(UUID.randomUUID(), 1)));
        OrderView esperado = pedidoConfirmado();
        when(orderPlacer.place(command)).thenReturn(esperado);

        OrderView resultado = orderService.place(command);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void deveFalharAoBuscarPedidoInexistente() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private OrderView pedidoConfirmado() {
        return new OrderView(UUID.randomUUID(), OrderStatus.CONFIRMED, BigDecimal.TEN, Instant.now(), List.of());
    }
}