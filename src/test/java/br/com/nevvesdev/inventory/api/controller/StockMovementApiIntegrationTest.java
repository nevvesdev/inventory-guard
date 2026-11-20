package br.com.nevvesdev.inventory.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockMovementApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void deveListarMovimentacoesDaMaisRecenteParaAMaisAntiga() throws Exception {
        String productId = criarProduto("10.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products/{id}/stock/replenishments", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock/movements", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].type").value("REPLENISHMENT"))
                .andExpect(jsonPath("$.content[0].quantityDelta").value(3))
                .andExpect(jsonPath("$.content[1].type").value("SALE"))
                .andExpect(jsonPath("$.content[1].quantityDelta").value(-2))
                .andExpect(jsonPath("$.content[1].orderId").isNotEmpty())
                .andExpect(jsonPath("$.content[2].type").value("INITIAL"))
                .andExpect(jsonPath("$.content[2].quantityDelta").value(5));
    }

    @Test
    void deveRetornarNaoEncontradoParaProdutoInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock/movements", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarTamanhoDePaginaInvalido() throws Exception {
        String productId = criarProduto("10.00", 1);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock/movements", productId)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}