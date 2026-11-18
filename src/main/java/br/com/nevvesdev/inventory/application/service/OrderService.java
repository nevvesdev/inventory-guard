package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.InsufficientStockException;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import br.com.nevvesdev.inventory.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    public OrderService(
            ProductRepository productRepository,
            StockRepository stockRepository,
            OrderRepository orderRepository
    ) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderView place(PlaceOrderCommand command) {
        Map<UUID, Integer> requested = consolidate(command.items());
        Map<UUID, BigDecimal> prices = loadPrices(requested.keySet());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        List<OrderLine> lines = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            int updated = stockRepository.decrementIfAvailable(productId, quantity, now);
            if (updated == 0) {
                throw new InsufficientStockException(productId, quantity);
            }

            lines.add(new OrderLine(productId, quantity, prices.get(productId)));
        }

        Order saved = orderRepository.save(Order.place(lines));
        return OrderView.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderView getById(UUID id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderView::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Map<UUID, Integer> consolidate(List<PlaceOrderItem> items) {
        Map<UUID, Integer> merged = new TreeMap<>();
        for (PlaceOrderItem item : items) {
            merged.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return merged;
    }

    private Map<UUID, BigDecimal> loadPrices(Set<UUID> productIds) {
        Map<UUID, BigDecimal> prices = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getPrice));

        for (UUID productId : productIds) {
            if (!prices.containsKey(productId)) {
                throw new ProductNotFoundException(productId);
            }
        }

        return prices;
    }
}