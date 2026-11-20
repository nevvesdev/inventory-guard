package br.com.nevvesdev.inventory.application.service;

import br.com.nevvesdev.inventory.application.command.PlaceOrderCommand;
import br.com.nevvesdev.inventory.application.view.OrderView;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import br.com.nevvesdev.inventory.domain.exception.OrderNotFoundException;
import br.com.nevvesdev.inventory.domain.repository.OrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    public static final int MAX_ATTEMPTS = 10;

    private final OrderPlacer orderPlacer;
    private final OrderRepository orderRepository;

    public OrderService(OrderPlacer orderPlacer, OrderRepository orderRepository) {
        this.orderPlacer = orderPlacer;
        this.orderRepository = orderRepository;
    }

    public OrderView place(PlaceOrderCommand command) {
        return place(command, null).order();
    }

    public PlacedOrder place(PlaceOrderCommand command, String idempotencyKey) {
        int attempt = 1;
        while (true) {
            try {
                return orderPlacer.place(command, idempotencyKey);
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
                attempt++;
            }
        }
    }

    @Transactional(readOnly = true)
    public OrderView getById(UUID id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderView::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}