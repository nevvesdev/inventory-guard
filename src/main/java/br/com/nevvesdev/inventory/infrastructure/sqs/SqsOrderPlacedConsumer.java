package br.com.nevvesdev.inventory.infrastructure.sqs;

import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(prefix = "aws.sqs.consumer", name = "enabled", havingValue = "true")
public class SqsOrderPlacedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsOrderPlacedConsumer.class);

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private final ObjectMapper objectMapper;
    // Provisório: IDs não sobrevivem a reinícios nem são compartilhados entre instâncias.
    private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong processedCount = new AtomicLong();

    public SqsOrderPlacedConsumer(
            SqsClient sqsClient,
            SqsProperties properties,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    public void pollMessages() {
        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(properties.queueName())
                .build()).queueUrl();

        var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(10)
                .maxNumberOfMessages(10)
                .build());

        for (Message message : response.messages()) {
            OrderPlacedEvent event = deserialize(message.body());
            if (processedEventIds.add(event.eventId())) {
                log.info("Pedido confirmado recebido: orderId={}, eventId={}", event.orderId(), event.eventId());
                processedCount.incrementAndGet();
            } else {
                log.info("Evento OrderPlaced duplicado ignorado: eventId={}", event.eventId());
            }

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        }
    }

    public long processedCount() {
        return processedCount.get();
    }

    private OrderPlacedEvent deserialize(String body) {
        try {
            return objectMapper.readValue(body, OrderPlacedEvent.class);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Não foi possível desserializar OrderPlacedEvent", ex);
        }
    }
}
