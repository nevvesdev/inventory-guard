package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.IdempotencyKeyReuseException;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.IdempotencyKey;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.IdempotencyKeyRepository;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacerTest {

    private static final String KEY = "chave-de-teste-0001";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservation stockReservation;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockLedger stockLedger;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private OrderPlacer orderPlacer;

    @Test
    void deveConsolidarItensRepetidosEReservarEstoqueUmaVez() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlacedOrder placed = orderPlacer.place(comando(new PlaceOrderItem(productId, 1), new PlaceOrderItem(productId, 2)), null);

        assertThat(placed.replayed()).isFalse();
        assertThat(placed.order().items()).hasSize(1);
        assertThat(placed.order().items().get(0).quantity()).isEqualTo(3);
        assertThat(placed.order().totalAmount()).isEqualByComparingTo("300.00");
        verify(stockReservation).reserve(productId, 3);
        verify(stockLedger).recordSales(any(), any());
        verifyNoInteractions(idempotencyKeyRepository);
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

        orderPlacer.place(comando(new PlaceOrderItem(maior, 1), new PlaceOrderItem(menor, 1)), null);

        InOrder ordem = inOrder(stockReservation);
        ordem.verify(stockReservation).reserve(menor, 1);
        ordem.verify(stockReservation).reserve(maior, 1);
    }

    @Test
    void deveRejeitarPedidoQuandoEstoqueEInsuficienteSemSalvarPedidoNemMovimentacao() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        doThrow(new InsufficientStockException(productId, 5)).when(stockReservation).reserve(productId, 5);

        assertThatThrownBy(() -> orderPlacer.place(comando(new PlaceOrderItem(productId, 5)), null))
                .isInstanceOf(InsufficientStockException.class);

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(stockLedger);
    }

    @Test
    void deveRejeitarPedidoComProdutoInexistenteSemTocarNoEstoque() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> orderPlacer.place(comando(new PlaceOrderItem(productId, 1)), null))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(stockReservation);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void deveCriarPedidoEVincularAChaveQuandoAChaveENova() {
        UUID productId = UUID.randomUUID();
        when(idempotencyKeyRepository.insertIfAbsent(eq(KEY), anyString(), any(Instant.class))).thenReturn(1);
        when(productRepository.findAllById(any())).thenReturn(List.of(produto(productId, "100.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlacedOrder placed = orderPlacer.place(comando(new PlaceOrderItem(productId, 2)), KEY);

        assertThat(placed.replayed()).isFalse();
        verify(stockReservation).reserve(productId, 2);
        verify(idempotencyKeyRepository).attachOrder(eq(KEY), any());
    }

    @Test
    void deveDevolverOPedidoOriginalQuandoAChaveSeRepeteComOMesmoConteudo() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        AtomicReference<String> fingerprint = new AtomicReference<>();
        when(idempotencyKeyRepository.insertIfAbsent(eq(KEY), anyString(), any(Instant.class)))
                .thenAnswer(invocation -> {
                    fingerprint.set(invocation.getArgument(1));
                    return 0;
                });
        when(idempotencyKeyRepository.findById(KEY))
                .thenAnswer(invocation -> Optional.of(chaveArmazenada(fingerprint.get(), orderId)));
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(pedido(productId)));

        PlacedOrder placed = orderPlacer.place(comando(new PlaceOrderItem(productId, 2)), KEY);

        assertThat(placed.replayed()).isTrue();
        assertThat(placed.order().items()).hasSize(1);
        verifyNoInteractions(stockReservation);
        verifyNoInteractions(stockLedger);
    }

    @Test
    void deveRejeitarChaveReutilizadaComOutroConteudo() {
        UUID productId = UUID.randomUUID();
        when(idempotencyKeyRepository.insertIfAbsent(eq(KEY), anyString(), any(Instant.class))).thenReturn(0);
        when(idempotencyKeyRepository.findById(KEY))
                .thenReturn(Optional.of(chaveArmazenada("hash-de-outro-conteudo", UUID.randomUUID())));

        assertThatThrownBy(() -> orderPlacer.place(comando(new PlaceOrderItem(productId, 2)), KEY))
                .isInstanceOf(IdempotencyKeyReuseException.class);

        verifyNoInteractions(stockReservation);
        verifyNoInteractions(orderRepository);
    }

    private PlaceOrderCommand comando(PlaceOrderItem... itens) {
        return new PlaceOrderCommand(List.of(itens));
    }

    private Order pedido(UUID productId) {
        return Order.place(List.of(new OrderLine(productId, 2, new BigDecimal("100.00"))));
    }

    private IdempotencyKey chaveArmazenada(String requestHash, UUID orderId) {
        IdempotencyKey key = BeanUtils.instantiateClass(IdempotencyKey.class);
        ReflectionTestUtils.setField(key, "idempotencyKey", KEY);
        ReflectionTestUtils.setField(key, "requestHash", requestHash);
        ReflectionTestUtils.setField(key, "orderId", orderId);
        return key;
    }

    private Product produto(UUID id, String price) {
        Product product = Product.create("SKU-" + id.toString().substring(0, 8), "Produto", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}