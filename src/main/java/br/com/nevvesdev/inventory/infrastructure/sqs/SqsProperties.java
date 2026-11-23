package br.com.nevvesdev.inventory.infrastructure.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(String endpoint, String region, String queueName) {
}
