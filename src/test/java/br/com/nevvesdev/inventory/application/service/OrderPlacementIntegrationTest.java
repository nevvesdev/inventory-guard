package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.IdempotencyKeyReuseException;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.model.StockMovement;
import br.com.nevvesdev.inventory.domain.model.StockMovementType;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.StockMovementRepository;
import br.com.nevvesdev.inventory.support.DatabaseCleaner;
import br.com.nevvesdev.inventory.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
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
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limparDadosCriados() {
        DatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void devePersistirPedidoEBaixarEstoque() {
        UUID productId = criarProduto(5);

        OrderView order = orderService.place(pedido(productId, 2));

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
        assertThat(movimentacoes(menor)).extracting(StockMovement::getType)
                .containsExactly(StockMovementType.INITIAL);
    }

    @Test
    void deveReporEstoqueSomandoAoSaldoAtual() {
        UUID productId = criarProduto(5);

        stockService.replenish(productId, 4);

        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(9);
    }

    @Test
    void deveManterOLivroRazaoReconciliadoComOSaldo() {
        UUID productId = criarProduto(5);

        orderService.place(pedido(productId, 2));
        stockService.replenish(productId, 3);

        List<StockMovement> movimentacoes = movimentacoes(productId);
        int soma = movimentacoes.stream().mapToInt(StockMovement::getQuantityDelta).sum();

        assertThat(movimentacoes).hasSize(3);
        assertThat(movimentacoes).extracting(StockMovement::getType)
                .containsExactlyInAnyOrder(
                        StockMovementType.INITIAL,
                        StockMovementType.SALE,
                        StockMovementType.REPLENISHMENT
                );
        assertThat(soma).isEqualTo(stockService.getByProductId(productId).quantity());
    }

    @Test
    void deveReaproveitarOPedidoQuandoAChaveDeIdempotenciaSeRepete() {
        UUID productId = criarProduto(5);
        PlaceOrderCommand command = pedido(productId, 2);

        PlacedOrder primeiro = orderService.place(command, "chave-idempotente-0001");
        PlacedOrder segundo = orderService.place(command, "chave-idempotente-0001");

        assertThat(primeiro.replayed()).isFalse();
        assertThat(segundo.replayed()).isTrue();
        assertThat(segundo.order().id()).isEqualTo(primeiro.order().id());
        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(3);
        assertThat(movimentacoes(productId)).hasSize(2);
    }

    @Test
    void deveRejeitarChaveReutilizadaComOutroConteudoSemAlterarEstoque() {
        UUID productId = criarProduto(5);

        orderService.place(pedido(productId, 2), "chave-idempotente-0002");

        assertThatThrownBy(() -> orderService.place(pedido(productId, 1), "chave-idempotente-0002"))
                .isInstanceOf(IdempotencyKeyReuseException.class);

        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(3);
    }

    @Test
    void deveLiberarAChaveQuandoOPedidoFalha() {
        UUID productId = criarProduto(5);

        assertThatThrownBy(() -> orderService.place(pedido(productId, 10), "chave-idempotente-0003"))
                .isInstanceOf(InsufficientStockException.class);

        PlacedOrder retentativa = orderService.place(pedido(productId, 1), "chave-idempotente-0003");

        assertThat(retentativa.replayed()).isFalse();
        assertThat(stockService.getByProductId(productId).quantity()).isEqualTo(4);
    }

    private List<StockMovement> movimentacoes(UUID productId) {
        return stockMovementRepository.findByProductId(productId, PageRequest.of(0, 100)).getContent();
    }

    private PlaceOrderCommand pedido(UUID productId, int quantity) {
        return new PlaceOrderCommand(List.of(new PlaceOrderItem(productId, quantity)));
    }

    private UUID criarProduto(int quantity) {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return productService.create(
                new CreateProductCommand(sku, "Produto de teste", new BigDecimal("10.00"), quantity)
        ).id();
    }
}