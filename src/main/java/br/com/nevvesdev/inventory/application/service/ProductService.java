package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.CreateProductCommand;
import br.com.nevvesdev.inventory.application.view.ProductView;
import br.com.nevvesdev.inventory.domain.exception.DuplicateSkuException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.model.Stock;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public ProductService(ProductRepository productRepository, StockRepository stockRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public ProductView create(CreateProductCommand command) {
        Product product = Product.create(command.sku(), command.name(), command.price());

        if (productRepository.existsBySku(product.getSku())) {
            throw new DuplicateSkuException(product.getSku());
        }

        Product saved = productRepository.saveAndFlush(product);
        stockRepository.save(Stock.create(saved.getId(), command.initialQuantity()));

        return ProductView.of(saved, command.initialQuantity());
    }

    @Transactional(readOnly = true)
    public ProductView getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        Stock stock = stockRepository.findByProductId(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductView.of(product, stock.getQuantity());
    }

    @Transactional(readOnly = true)
    public Page<ProductView> list(Pageable pageable) {
        Page<Product> page = productRepository.findAll(pageable);

        List<UUID> productIds = page.getContent().stream()
                .map(Product::getId)
                .toList();

        Map<UUID, Integer> quantities = stockRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Stock::getProductId, Stock::getQuantity, Integer::sum));

        return page.map(product -> ProductView.of(product, quantities.getOrDefault(product.getId(), 0)));
    }
}