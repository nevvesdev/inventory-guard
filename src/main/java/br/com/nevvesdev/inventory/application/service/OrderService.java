package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;
import br.com.nevvesdev.inventory.application.port.OrderEventPublisher;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    public static final int MAX_ATTEMPTS = 10;

    private final OrderPlacer orderPlacer;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            OrderPlacer orderPlacer,
            OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.orderPlacer = orderPlacer;
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderView place(PlaceOrderCommand command) {
        return place(command, null).order();
    }

    public PlacedOrder place(PlaceOrderCommand command, String idempotencyKey) {
        int attempt = 1;
        while (true) {
            try {
                PlacedOrder placedOrder = orderPlacer.place(command, idempotencyKey);
                if (!placedOrder.replayed()) {
                    publishOrderPlaced(placedOrder.order());
                }
                return placedOrder;
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
                attempt++;
            }
        }
    }

    private void publishOrderPlaced(OrderView order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                order.id(),
                order.items().stream()
                        .map(item -> new OrderPlacedEvent.Item(item.productId(), item.quantity()))
                        .toList(),
                order.totalAmount(),
                order.createdAt()
        );
        try {
            orderEventPublisher.publishOrderPlaced(event);
        } catch (RuntimeException ex) {
            // A falha de envio não desfaz o pedido já confirmado; o Outbox resolverá essa limitação.
            log.error("Falha ao publicar OrderPlaced para orderId={}", order.id(), ex);
        }
    }

    @Transactional(readOnly = true)
    public OrderView getById(UUID id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderView::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}