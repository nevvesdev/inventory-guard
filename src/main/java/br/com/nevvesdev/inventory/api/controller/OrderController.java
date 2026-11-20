package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.docs.ApiDocs;
import br.com.nevvesdev.inventory.api.dto.OrderResponse;
import br.com.nevvesdev.inventory.api.dto.PlaceOrderRequest;
import br.com.nevvesdev.inventory.api.error.InvalidIdempotencyKeyException;
import br.com.nevvesdev.inventory.application.service.OrderService;
import br.com.nevvesdev.inventory.application.view.PlacedOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Pedidos", description = "Criação de pedidos com baixa atômica de estoque e idempotência")
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

    @Operation(
            summary = "Cria um pedido e baixa o estoque na mesma transação",
            description = "Se qualquer item não tiver estoque, nada é gravado. Itens repetidos do mesmo produto "
                    + "são consolidados. Com o header Idempotency-Key, repetir a requisição devolve o pedido "
                    + "original (200 com Idempotency-Replayed: true) em vez de criar outro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "200",
                    description = "Chave de idempotência já usada com o mesmo conteúdo: devolve o pedido original"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou chave de idempotência mal formada",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Estoque insuficiente ou conflito de concorrência",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Chave de idempotência reutilizada com outro conteúdo",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @Parameter(
                    description = "Chave opcional para tornar a criação idempotente: de 8 a 80 caracteres "
                            + "(letras, números, '.', '_', ':' ou '-')",
                    example = "pedido-2026-0001"
            )
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

    @Operation(summary = "Busca um pedido pelo identificador, com seus itens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "400", description = "Identificador mal formado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
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