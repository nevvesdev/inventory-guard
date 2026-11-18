package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderPlacer {

    private final ProductRepository productRepository;
    private final StockReservation stockReservation;
    private final OrderRepository orderRepository;

    public OrderPlacer(
            ProductRepository productRepository,
            StockReservation stockReservation,
            OrderRepository orderRepository
    ) {
        this.productRepository = productRepository;
        this.stockReservation = stockReservation;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderView place(PlaceOrderCommand command) {
        Map<UUID, Integer> requested = consolidate(command.items());
        Map<UUID, BigDecimal> prices = loadPrices(requested.keySet());

        List<OrderLine> lines = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            stockReservation.reserve(productId, quantity);

            lines.add(new OrderLine(productId, quantity, prices.get(productId)));
        }

        Order saved = orderRepository.save(Order.place(lines));
        return OrderView.from(saved);
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