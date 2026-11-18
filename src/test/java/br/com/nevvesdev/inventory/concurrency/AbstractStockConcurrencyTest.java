package br.com.nevvesdev.inventory.concurrency;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.service.OrderService;
import br.com.nevvesdev.inventory.application.service.ProductService;
import br.com.nevvesdev.inventory.application.service.StockService;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import br.com.nevvesdev.inventory.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
abstract class AbstractStockConcurrencyTest {

    private enum Tipo {
        SUCESSO,
        ESTOQUE_INSUFICIENTE,
        ERRO_INESPERADO
    }

    private record Resultado(Tipo tipo, UUID pedidoId, Throwable erro) {
    }

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
    void apenasUmaCompraDeveVencerQuandoExisteUmaUnidadeEmEstoque() throws Exception {
        UUID productId = criarProduto(1);
        int participantes = 20;

        List<Resultado> resultados = executarSimultaneamente(participantes, indice -> pedido(productId));

        assertSemErrosInesperados(resultados);
        assertThat(contar(resultados, Tipo.SUCESSO)).isEqualTo(1);
        assertThat(contar(resultados, Tipo.ESTOQUE_INSUFICIENTE)).isEqualTo(participantes - 1);
        assertThat(stockService.getByProductId(productId).quantity()).isZero();
    }

    @Test
    void naoDeveVenderMaisUnidadesDoQueExistemEmEstoque() throws Exception {
        int estoqueInicial = 5;
        int participantes = 30;
        UUID productId = criarProduto(estoqueInicial);

        List<Resultado> resultados = executarSimultaneamente(participantes, indice -> pedido(productId));

        assertSemErrosInesperados(resultados);
        assertThat(contar(resultados, Tipo.SUCESSO)).isEqualTo(estoqueInicial);
        assertThat(contar(resultados, Tipo.ESTOQUE_INSUFICIENTE)).isEqualTo(participantes - estoqueInicial);
        assertThat(stockService.getByProductId(productId).quantity()).isZero();
    }

    @Test
    void naoDeveOcorrerDeadlockQuandoPedidosCitamOsMesmosProdutosEmOrdensOpostas() throws Exception {
        int estoqueInicial = 100;
        int participantes = 8;
        UUID a = criarProduto(estoqueInicial);
        UUID b = criarProduto(estoqueInicial);

        List<Resultado> resultados = executarSimultaneamente(
                participantes,
                indice -> indice % 2 == 0 ? pedido(a, b) : pedido(b, a)
        );

        assertSemErrosInesperados(resultados);
        assertThat(contar(resultados, Tipo.SUCESSO)).isEqualTo(participantes);
        assertThat(stockService.getByProductId(a).quantity()).isEqualTo(estoqueInicial - participantes);
        assertThat(stockService.getByProductId(b).quantity()).isEqualTo(estoqueInicial - participantes);
    }

    private List<Resultado> executarSimultaneamente(int participantes, IntFunction<PlaceOrderCommand> comando)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(participantes);
        try {
            CountDownLatch prontos = new CountDownLatch(participantes);
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Resultado>> futuros = new ArrayList<>();

            for (int indice = 0; indice < participantes; indice++) {
                PlaceOrderCommand pedido = comando.apply(indice);
                futuros.add(executor.submit(() -> {
                    prontos.countDown();
                    largada.await();
                    return tentarComprar(pedido);
                }));
            }

            assertThat(prontos.await(30, TimeUnit.SECONDS)).isTrue();
            largada.countDown();

            List<Resultado> resultados = new ArrayList<>();
            for (Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(60, TimeUnit.SECONDS));
            }

            resultados.stream()
                    .map(Resultado::pedidoId)
                    .filter(Objects::nonNull)
                    .forEach(orderIds::add);

            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private Resultado tentarComprar(PlaceOrderCommand pedido) {
        try {
            OrderView view = orderService.place(pedido);
            return new Resultado(Tipo.SUCESSO, view.id(), null);
        } catch (InsufficientStockException ex) {
            return new Resultado(Tipo.ESTOQUE_INSUFICIENTE, null, null);
        } catch (Exception ex) {
            return new Resultado(Tipo.ERRO_INESPERADO, null, ex);
        }
    }

    private void assertSemErrosInesperados(List<Resultado> resultados) {
        List<Throwable> erros = resultados.stream()
                .map(Resultado::erro)
                .filter(Objects::nonNull)
                .toList();

        assertThat(erros).as("erros inesperados: %s", erros).isEmpty();
    }

    private long contar(List<Resultado> resultados, Tipo tipo) {
        return resultados.stream().filter(resultado -> resultado.tipo() == tipo).count();
    }

    private PlaceOrderCommand pedido(UUID... produtos) {
        List<PlaceOrderItem> itens = new ArrayList<>();
        for (UUID produto : produtos) {
            itens.add(new PlaceOrderItem(produto, 1));
        }
        return new PlaceOrderCommand(itens);
    }

    private UUID criarProduto(int quantidade) {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID id = productService.create(
                new CreateProductCommand(sku, "Produto concorrente", new BigDecimal("10.00"), quantidade)
        ).id();
        productIds.add(id);
        return id;
    }
}