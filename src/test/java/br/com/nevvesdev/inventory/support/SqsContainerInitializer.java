package br.com.nevvesdev.inventory.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.util.Map;

public class SqsContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String QUEUE_NAME = "order-placed";
    private static final GenericContainer<?> ELASTICMQ = new GenericContainer<>(
            DockerImageName.parse("softwaremill/elasticmq-native:1.7.1")
    ).withExposedPorts(9324);
    private static final String ENDPOINT;

    static {
        ELASTICMQ.start();
        ENDPOINT = "http://" + ELASTICMQ.getHost() + ":" + ELASTICMQ.getMappedPort(9324);
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");

        try (SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                ))
                .build()) {
            sqsClient.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build());
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testcontainers-sqs", Map.of(
                "aws.sqs.endpoint", ENDPOINT,
                "aws.sqs.region", "us-east-1",
                "aws.sqs.queue-name", QUEUE_NAME
        )));
    }
}
