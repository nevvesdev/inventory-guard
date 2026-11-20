package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.view.StockMovementView;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.StockMovement;
import br.com.nevvesdev.inventory.domain.model.StockMovementType;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    @Test
    void deveListarMovimentacoesDoProduto() {
        UUID productId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.existsById(productId)).thenReturn(true);
        when(stockMovementRepository.findByProductId(productId, pageable))
                .thenReturn(new PageImpl<>(List.of(StockMovement.initial(productId, 5))));

        Page<StockMovementView> page = stockMovementService.list(productId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).type()).isEqualTo(StockMovementType.INITIAL);
        assertThat(page.getContent().get(0).quantityDelta()).isEqualTo(5);
    }

    @Test
    void deveFalharQuandoProdutoNaoExiste() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> stockMovementService.list(productId, PageRequest.of(0, 20)))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(stockMovementRepository);
    }
}