package br.com.nevvesdev.inventory.api.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
abstract class ApiIntegrationTestSupport {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    protected String criarProduto(String price, int quantity) throws Exception {
        String body = """
                {
                  "sku": "SKU-%s",
                  "name": "Produto de teste",
                  "price": %s,
                  "initialQuantity": %d
                }
                """.formatted(UUID.randomUUID().toString().substring(0, 8).toUpperCase(), price, quantity);

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.id");
    }

    protected String pedido(String... itens) {
        return """
                {
                  "items": [%s]
                }
                """.formatted(String.join(",", itens));
    }

    protected String item(String productId, int quantity) {
        return """
                {"productId": "%s", "quantity": %d}
                """.formatted(productId, quantity);
    }
}