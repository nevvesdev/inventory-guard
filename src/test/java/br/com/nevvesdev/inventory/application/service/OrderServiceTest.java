package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void deveConsolidarItensRepetidosEBaixarEstoqueUmaVez() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        when(stockRepository.decrementIfAvailable(eq(productId), eq(3), any(Instant.class))).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderView view = orderService.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 1),
                new PlaceOrderItem(productId, 2)
        )));

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).quantity()).isEqualTo(3);
        assertThat(view.totalAmount()).isEqualByComparingTo("300.00");
        verify(stockRepository).decrementIfAvailable(eq(productId), eq(3), any(Instant.class));
    }

    @Test
    void deveBaixarEstoqueEmOrdemDeterministicaDeProduto() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID menor = a.compareTo(b) < 0 ? a : b;
        UUID maior = a.compareTo(b) < 0 ? b : a;
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(produto(maior, "10.00"), produto(menor, "10.00")));
        when(stockRepository.decrementIfAvailable(any(UUID.class), anyInt(), any(Instant.class))).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(maior, 1),
                new PlaceOrderItem(menor, 1)
        )));

        InOrder ordem = inOrder(stockRepository);
        ordem.verify(stockRepository).decrementIfAvailable(eq(menor), eq(1), any(Instant.class));
        ordem.verify(stockRepository).decrementIfAvailable(eq(maior), eq(1), any(Instant.class));
    }

    @Test
    void deveRejeitarPedidoQuandoEstoqueEInsuficienteSemSalvarPedido() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        when(stockRepository.decrementIfAvailable(eq(productId), eq(5), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 5)
        )))).isInstanceOf(InsufficientStockException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void deveRejeitarPedidoComProdutoInexistenteSemTocarNoEstoque() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 1)
        )))).isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(stockRepository);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void deveFalharAoBuscarPedidoInexistente() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private Product produto(UUID id, String price) {
        Product product = Product.create("SKU-" + id.toString().substring(0, 8), "Produto", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}