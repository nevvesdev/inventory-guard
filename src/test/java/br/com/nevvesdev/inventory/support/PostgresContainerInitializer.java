package br.com.nevvesdev.inventory.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

public class PostgresContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("inventory_guard")
            .withUsername("inventory")
            .withPassword("inventory");

    static {
        POSTGRES.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        MapPropertySource source = new MapPropertySource("testcontainers-postgres", Map.of(
                "spring.datasource.url", POSTGRES.getJdbcUrl(),
                "spring.datasource.username", POSTGRES.getUsername(),
                "spring.datasource.password", POSTGRES.getPassword()
        ));
        context.getEnvironment().getPropertySources().addFirst(source);
    }
}