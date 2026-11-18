package br.com.nevvesdev.inventory.api.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void deveCriarPedidoEBaixarEstoque() throws Exception {
        String productId = criarProduto("100.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 2))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(200.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void deveConsolidarItensRepetidosDoMesmoProduto() throws Exception {
        String productId = criarProduto("50.00", 10);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1), item(productId, 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(100.0));
    }

    @Test
    void deveRejeitarPedidoComEstoqueInsuficiente() throws Exception {
        String productId = criarProduto("100.00", 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Estoque insuficiente"))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.requestedQuantity").value(2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void deveEsgotarEstoqueERejeitarCompraSeguinte() throws Exception {
        String productId = criarProduto("100.00", 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isConflict());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(jsonPath("$.quantity").value(0));
    }

    @Test
    void deveRejeitarPedidoComProdutoInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(UUID.randomUUID().toString(), 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Produto não encontrado"));
    }

    @Test
    void deveRejeitarPedidoSemItens() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    void deveRejeitarItemComQuantidadeInvalida() throws Exception {
        String productId = criarProduto("100.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void deveBuscarPedidoPorId() throws Exception {
        String productId = criarProduto("100.00", 5);

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = JsonPath.read(response, "$.id");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void deveRetornarNaoEncontradoParaPedidoInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pedido não encontrado"));
    }
}