package br.com.nevvesdev.inventory.infrastructure.sqs;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;
import br.com.nevvesdev.inventory.application.service.OrderService;
import br.com.nevvesdev.inventory.application.service.ProductService;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.support.DatabaseCleaner;
import br.com.nevvesdev.inventory.support.PostgresContainerInitializer;
import br.com.nevvesdev.inventory.support.SqsContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "aws.sqs.consumer.enabled=false")
@ContextConfiguration(initializers = {PostgresContainerInitializer.class, SqsContainerInitializer.class})
class SqsOrderEventIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private SqsProperties sqsProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String queueUrl;

    @BeforeEach
    void prepareQueue() {
        queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(sqsProperties.queueName())
                .build()).queueUrl();
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    @AfterEach
    void cleanDatabase() {
        DatabaseCleaner.clean(jdbcTemplate);
    }

    @Test
    void pedidoConfirmadoPublicaExatamenteUmaMensagemComOrderIdCorreto() throws Exception {
        UUID productId = createProduct(5);

        var order = orderService.place(order(productId, 2));
        AtomicReference<Message> received = new AtomicReference<>();

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
            List<Message> messages = receiveMessages();
            if (!messages.isEmpty()) {
                received.set(messages.getFirst());
                return true;
            }
            return false;
        });

        OrderPlacedEvent event = objectMapper.readValue(received.get().body(), OrderPlacedEvent.class);
        assertThat(event.orderId()).isEqualTo(order.id());
        assertThat(event.items()).containsExactly(new OrderPlacedEvent.Item(productId, 2));
        assertThat(receiveMessages()).isEmpty();
    }

    @Test
    void pedidoRejeitadoPorEstoqueInsuficienteNaoPublicaMensagem() {
        UUID productId = createProduct(1);

        assertThatThrownBy(() -> orderService.place(order(productId, 2)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(receiveMessages()).isEmpty();
    }

    private UUID createProduct(int initialStock) {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return productService.create(new CreateProductCommand(
                sku,
                "Produto de teste",
                new BigDecimal("10.00"),
                initialStock
        )).id();
    }

    private PlaceOrderCommand order(UUID productId, int quantity) {
        return new PlaceOrderCommand(List.of(new PlaceOrderItem(productId, quantity)));
    }

    private List<Message> receiveMessages() {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(0)
                .maxNumberOfMessages(10)
                .build()).messages();
    }
}
