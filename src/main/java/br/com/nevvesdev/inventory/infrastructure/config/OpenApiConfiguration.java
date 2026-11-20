package br.com.nevvesdev.inventory.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI inventoryGuardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("inventory-guard API")
                .description("API de gestão de estoque com consistência garantida sob concorrência")
                .version("v1")
                .contact(new Contact().name("nevvesdev").url("https://github.com/nevvesdev"))
                .license(new License().name("MIT").url("https://opensource.org/license/mit")));
    }
}