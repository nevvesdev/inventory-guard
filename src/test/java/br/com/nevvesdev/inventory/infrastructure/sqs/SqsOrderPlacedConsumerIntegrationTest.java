package br.com.nevvesdev.inventory.infrastructure.sqs;

import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;
import br.com.nevvesdev.inventory.support.PostgresContainerInitializer;
import br.com.nevvesdev.inventory.support.SqsContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "aws.sqs.consumer.enabled=true")
@ContextConfiguration(initializers = {PostgresContainerInitializer.class, SqsContainerInitializer.class})
class SqsOrderPlacedConsumerIntegrationTest {

    @Autowired
    private SqsOrderPlacedConsumer consumer;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private SqsProperties sqsProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private String queueUrl;

    @BeforeEach
    void prepareQueue() {
        queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(sqsProperties.queueName())
                .build()).queueUrl();
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    @Test
    void processaEApagaMensagemSemProcessarDuasVezesMesmoEventId() throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new OrderPlacedEvent.Item(UUID.randomUUID(), 1)),
                BigDecimal.TEN,
                Instant.now()
        );
        String body = objectMapper.writeValueAsString(event);
        for (int i = 0; i < 2; i++) {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        }

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertThat(consumer.processedCount()).isEqualTo(1);
                    assertThat(sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                            .build()).attributes())
                            .containsEntry(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0");
                });
    }
}
