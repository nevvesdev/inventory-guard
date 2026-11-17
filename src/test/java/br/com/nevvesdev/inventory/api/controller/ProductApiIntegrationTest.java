package br.com.nevvesdev.inventory.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void deveCriarProdutoComEstoqueInicial() throws Exception {
        String sku = novoSku();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(sku, "iPhone 15", "100.00", 5)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.name").value("iPhone 15"))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void deveRejeitarPayloadInvalidoComDetalhesDosCampos() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("  ", "", "-5.00", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").isNotEmpty());
    }

    @Test
    void deveRejeitarSkuDuplicado() throws Exception {
        String sku = novoSku();
        String body = payload(sku, "iPhone 15", "100.00", 5);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("SKU duplicado"));
    }

    @Test
    void deveBuscarProdutoPorId() throws Exception {
        String id = criarProduto(novoSku(), 8);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void deveRetornarNaoEncontradoParaProdutoInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Produto não encontrado"));
    }

    @Test
    void deveRejeitarIdentificadorMalFormado() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveConsultarEstoqueDoProduto() throws Exception {
        String id = criarProduto(novoSku(), 3);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products/{id}/stock", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(id))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void deveListarProdutosComPaginacao() throws Exception {
        criarProduto(novoSku(), 1);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void deveRejeitarTamanhoDePaginaInvalido() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    private String criarProduto(String sku, int quantity) throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(sku, "Produto de teste", "10.00", quantity)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String marker = "\"id\":\"";
        int start = response.indexOf(marker) + marker.length();
        return response.substring(start, response.indexOf('"', start));
    }

    private String novoSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String payload(String sku, String name, String price, int initialQuantity) {
        return """
                {
                  "sku": "%s",
                  "name": "%s",
                  "price": %s,
                  "initialQuantity": %d
                }
                """.formatted(sku, name, price, initialQuantity);
    }
}