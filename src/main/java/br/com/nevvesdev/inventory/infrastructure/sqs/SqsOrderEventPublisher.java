package br.com.nevvesdev.inventory.infrastructure.sqs;

import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;
import br.com.nevvesdev.inventory.application.port.OrderEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsOrderEventPublisher implements OrderEventPublisher {

    private final SqsClient sqsClient;
    private final SqsProperties properties;
    private final ObjectMapper objectMapper;

    public SqsOrderEventPublisher(
            SqsClient sqsClient,
            SqsProperties properties,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(properties.queueName())
                .build()).queueUrl();

        String body;
        try {
            body = objectMapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Não foi possível serializar OrderPlacedEvent", ex);
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build());
    }
}
