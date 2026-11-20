package br.com.nevvesdev.inventory.api.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIdempotencyApiIntegrationTest extends ApiIntegrationTestSupport {

    private static final String KEY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER = "Idempotency-Replayed";

    @Test
    void deveDevolverOMesmoPedidoQuandoAChaveSeRepete() throws Exception {
        String productId = criarProduto("100.00", 5);
        String chave = novaChave();
        String corpo = pedido(item(productId, 2));

        String primeira = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(REPLAYED_HEADER))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = JsonPath.read(primeira, "$.id");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(header().string(REPLAYED_HEADER, "true"))
                .andExpect(jsonPath("$.id").value(orderId));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void deveCriarPedidosDistintosParaChavesDistintas() throws Exception {
        String productId = criarProduto("100.00", 5);
        String corpo = pedido(item(productId, 1));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, novaChave())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, novaChave())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", productId))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void deveRejeitarChaveReutilizadaComOutroConteudo() throws Exception {
        String productId = criarProduto("100.00", 5);
        String chave = novaChave();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 2))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Chave de idempotência reutilizada"));
    }

    @Test
    void deveRejeitarChaveMalFormada() throws Exception {
        String productId = criarProduto("100.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .header(KEY_HEADER, "curta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Chave de idempotência inválida"));
    }

    @Test
    void deveContinuarAceitandoPedidosSemChave() throws Exception {
        String productId = criarProduto("100.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido(item(productId, 1))))
                .andExpect(status().isCreated());
    }

    private String novaChave() {
        return "chave-" + UUID.randomUUID();
    }
}