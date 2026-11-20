package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.dto.OrderResponse;
import br.com.nevvesdev.inventory.api.dto.PlaceOrderRequest;
import br.com.nevvesdev.inventory.api.error.InvalidIdempotencyKeyException;
import br.com.nevvesdev.inventory.application.service.OrderService;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,80}$");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        PlacedOrder placed = orderService.place(request.toCommand(), validated(idempotencyKey));
        OrderResponse body = OrderResponse.from(placed.order());

        if (placed.replayed()) {
            return ResponseEntity.ok()
                    .header(IDEMPOTENCY_REPLAYED_HEADER, "true")
                    .body(body);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(placed.order().id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable UUID id) {
        return OrderResponse.from(orderService.getById(id));
    }

    private String validated(String idempotencyKey) {
        if (idempotencyKey != null && !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        return idempotencyKey;
    }
}