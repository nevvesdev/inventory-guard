package br.com.nevvesdev.inventory.support;

import br.com.nevvesdev.inventory.application.port.OrderEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class OrderEventPublisherTestConfiguration {

    @Bean
    @Primary
    OrderEventPublisher testOrderEventPublisher() {
        return event -> {
        };
    }
}
