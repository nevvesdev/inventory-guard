package br.com.nevvesdev.inventory.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocumentationTest extends ApiIntegrationTestSupport {

    @Test
    void deveExporAEspecificacaoOpenApiComOsMetadadosDoProjeto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("inventory-guard API"))
                .andExpect(jsonPath("$.info.license.name").value("MIT"));
    }

    @Test
    void deveDocumentarTodosOsEndpointsDaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/products'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{productId}/stock'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{productId}/stock/replenishments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{productId}/stock/movements'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{id}'].get").exists());
    }

    @Test
    void deveDocumentarOHeaderDeIdempotenciaEOsErrosDoPedido() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.parameters[?(@.name=='Idempotency-Key')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.responses['422']").exists());
    }
}