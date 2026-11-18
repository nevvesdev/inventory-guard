package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservation stockReservation;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPlacer orderPlacer;

    @Test
    void deveConsolidarItensRepetidosEReservarEstoqueUmaVez() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderView view = orderPlacer.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 1),
                new PlaceOrderItem(productId, 2)
        )));

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).quantity()).isEqualTo(3);
        assertThat(view.totalAmount()).isEqualByComparingTo("300.00");
        verify(stockReservation).reserve(productId, 3);
    }

    @Test
    void deveReservarEstoqueEmOrdemDeterministicaDeProduto() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID menor = a.compareTo(b) < 0 ? a : b;
        UUID maior = a.compareTo(b) < 0 ? b : a;
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(produto(maior, "10.00"), produto(menor, "10.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderPlacer.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(maior, 1),
                new PlaceOrderItem(menor, 1)
        )));

        InOrder ordem = inOrder(stockReservation);
        ordem.verify(stockReservation).reserve(menor, 1);
        ordem.verify(stockReservation).reserve(maior, 1);
    }

    @Test
    void deveRejeitarPedidoQuandoEstoqueEInsuficienteSemSalvarPedido() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        doThrow(new InsufficientStockException(productId, 5)).when(stockReservation).reserve(productId, 5);

        assertThatThrownBy(() -> orderPlacer.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 5)
        )))).isInstanceOf(InsufficientStockException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void deveRejeitarPedidoComProdutoInexistenteSemTocarNoEstoque() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> orderPlacer.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(productId, 1)
        )))).isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(stockReservation);
        verifyNoInteractions(orderRepository);
    }

    private Product produto(UUID id, String price) {
        Product product = Product.create("SKU-" + id.toString().substring(0, 8), "Produto", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}