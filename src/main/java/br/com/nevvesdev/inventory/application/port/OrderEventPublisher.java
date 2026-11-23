package br.com.nevvesdev.inventory.application.port;

import br.com.nevvesdev.inventory.application.event.OrderPlacedEvent;

public interface OrderEventPublisher {

    void publishOrderPlaced(OrderPlacedEvent event);
}
