package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class StockMovementImmutabilityTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveBloquearAtualizacaoDeMovimentacaoNoBanco() {
        UUID productId = criarProdutoComMovimentacao();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE stock_movements SET quantity_delta = 99 WHERE product_id = ?", productId
        ))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutável");
    }

    @Test
    void deveBloquearExclusaoDeMovimentacaoNoBanco() {
        UUID productId = criarProdutoComMovimentacao();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM stock_movements WHERE product_id = ?", productId
        ))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("imutável");
    }

    private UUID criarProdutoComMovimentacao() {
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID productId = productService.create(
                new CreateProductCommand(sku, "Produto imutável", new BigDecimal("10.00"), 5)
        ).id();
        entityManager.flush();
        return productId;
    }
}