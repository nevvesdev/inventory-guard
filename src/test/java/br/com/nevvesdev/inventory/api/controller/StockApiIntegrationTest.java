package br.com.nevvesdev.inventory.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void deveReporEstoqueEAtualizarSaldo() throws Exception {
        String productId = criarProduto("10.00", 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products/{id}/stock/replenishments", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void deveRejeitarReposicaoComQuantidadeInvalida() throws Exception {
        String productId = criarProduto("10.00", 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products/{id}/stock/replenishments", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void deveRejeitarReposicaoDeProdutoInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products/{id}/stock/replenishments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound());
    }
}