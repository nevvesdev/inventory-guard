package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.command.PlaceOrderItem;
import br.com.nevvesdev.inventory.application.port.StockReservation;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.IdempotencyKeyReuseException;
import br.com.nevvesdev.inventory.domain.exception.ProductNotFoundException;
import br.com.nevvesdev.inventory.domain.model.IdempotencyKey;
import br.com.nevvesdev.inventory.domain.model.Order;
import br.com.nevvesdev.inventory.domain.model.OrderLine;
import br.com.nevvesdev.inventory.domain.model.Product;
import br.com.nevvesdev.inventory.domain.repository.IdempotencyKeyRepository;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import br.com.nevvesdev.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
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
    private final StockLedger stockLedger;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public OrderPlacer(
            ProductRepository productRepository,
            StockReservation stockReservation,
            OrderRepository orderRepository,
            StockLedger stockLedger,
            IdempotencyKeyRepository idempotencyKeyRepository
    ) {
        this.productRepository = productRepository;
        this.stockReservation = stockReservation;
        this.orderRepository = orderRepository;
        this.stockLedger = stockLedger;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public PlacedOrder place(PlaceOrderCommand command, String idempotencyKey) {
        Map<UUID, Integer> requested = consolidate(command.items());

        if (idempotencyKey == null) {
            return new PlacedOrder(createOrder(requested), false);
        }

        String fingerprint = fingerprint(requested);
        int claimed = idempotencyKeyRepository.insertIfAbsent(
                idempotencyKey,
                fingerprint,
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );

        if (claimed == 0) {
            return new PlacedOrder(replay(idempotencyKey, fingerprint), true);
        }

        OrderView created = createOrder(requested);
        idempotencyKeyRepository.attachOrder(idempotencyKey, created.id());
        return new PlacedOrder(created, false);
    }

    private OrderView createOrder(Map<UUID, Integer> requested) {
        Map<UUID, BigDecimal> prices = loadPrices(requested.keySet());

        List<OrderLine> lines = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            stockReservation.reserve(productId, quantity);

            lines.add(new OrderLine(productId, quantity, prices.get(productId)));
        }

        Order saved = orderRepository.save(Order.place(lines));
        stockLedger.recordSales(saved.getId(), lines);
        return OrderView.from(saved);
    }

    private OrderView replay(String idempotencyKey, String fingerprint) {
        IdempotencyKey stored = idempotencyKeyRepository.findById(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Chave de idempotência não encontrada: " + idempotencyKey));

        if (!stored.getRequestHash().equals(fingerprint)) {
            throw new IdempotencyKeyReuseException(idempotencyKey);
        }

        return orderRepository.findWithItemsById(stored.getOrderId())
                .map(OrderView::from)
                .orElseThrow(() -> new IllegalStateException("Pedido da chave não encontrado: " + idempotencyKey));
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

    private String fingerprint(Map<UUID, Integer> requested) {
        String canonical = requested.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(";"));

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}