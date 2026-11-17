package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.view.ProductView;
import br.com.nevvesdev.inventory.domain.exception.DuplicateSkuException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.model.Stock;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void deveCriarProdutoComEstoqueInicial() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsBySku("IPHONE-15")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", id);
            return product;
        });

        ProductView view = productService.create(
                new CreateProductCommand("iphone-15", "iPhone 15", new BigDecimal("5999.90"), 3)
        );

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.sku()).isEqualTo("IPHONE-15");
        assertThat(view.quantity()).isEqualTo(3);

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(id);
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void deveRejeitarSkuDuplicadoSemPersistirNada() {
        when(productRepository.existsBySku("IPHONE-15")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(
                new CreateProductCommand("iphone-15", "iPhone 15", new BigDecimal("5999.90"), 3)
        )).isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).saveAndFlush(any());
        verifyNoInteractions(stockRepository);
    }

    @Test
    void deveFalharAoBuscarProdutoInexistente() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }
}