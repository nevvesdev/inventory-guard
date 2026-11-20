package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.docs.ApiDocs;
import br.com.nevvesdev.inventory.api.dto.PageResponse;
import br.com.nevvesdev.inventory.api.dto.StockMovementResponse;
import br.com.nevvesdev.inventory.application.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Movimentações", description = "Histórico imutável de movimentações de estoque")
@RestController
@RequestMapping("/api/v1/products/{productId}/stock/movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @Operation(
            summary = "Lista o histórico de movimentações do estoque de um produto",
            description = "Da mais recente para a mais antiga. Vendas têm quantidade negativa e trazem o pedido de "
                    + "origem. A soma das quantidades é sempre igual ao saldo atual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de movimentações"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public PageResponse<StockMovementResponse> list(
            @PathVariable UUID productId,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página, de 1 a 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(
                stockMovementService.list(productId, pageable).map(StockMovementResponse::from)
        );
    }
}