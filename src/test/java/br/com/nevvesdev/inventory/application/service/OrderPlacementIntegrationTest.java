package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.nevvesdev.inventory.support.IntegrationTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class OrderPlacementIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductRepository productRepository;

    private final List<UUID> productIds = new ArrayList<>();
    private final List<UUID> orderIds = new ArrayList<>();

    @AfterEach
    void limparDadosCriados() {
        orderRepository.deleteAllById(orderIds);
        stockRepository.deleteAll(stockRepository.findByProductIdIn(productIds));
        productRepository.deleteAllById(productIds);
    }

    @Test
    void devePersistirPedidoEBaixarEstoque() {
        UUID productId = criarProduto(5);

        OrderView order = orderService.place(new PlaceOrderCommand(List.of(new PlaceOrderItem(productId, 2))));
        orderIds.add(order.id());

        assertThat(order.id()).isNotNull();
        assertThat(order.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(3);
        assertThat(orderService.getById(order.id()).items()).hasSize(1);
    }

    @Test
    void deveDesfazerBaixasJaFeitasQuandoUmItemFicaSemEstoque() {
        UUID a = criarProduto(5);
        UUID b = criarProduto(5);
        UUID menor = a.compareTo(b) < 0 ? a : b;
        UUID maior = a.compareTo(b) < 0 ? b : a;
        long pedidosAntes = orderRepository.count();

        assertThatThrownBy(() -> orderService.place(new PlaceOrderCommand(List.of(
                new PlaceOrderItem(menor, 2),
                new PlaceOrderItem(maior, 10)
        )))).isInstanceOf(InsufficientStockException.class);

        assertThat(stockService.getByProductId(menor).quantity()).isEqualTo(5);
        assertThat(stockService.getByProductId(maior).quantity()).isEqualTo(5);
        assertThat(orderRepository.count()).isEqualTo(pedidosAntes);
    }

    @Test
    void deveReporEstoqueSomandoAoSaldoAtual() {
        UUID productId = criarProduto(5);

        stockService.replenish(productId, 4);

        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(9);
    }

    private UUID criarProduto(int quantity) {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID id = productService.create(
                new CreateProductCommand(sku, "Produto de teste", new BigDecimal("10.00"), quantity)
        ).id();
        productIds.add(id);
        return id;
    }
}