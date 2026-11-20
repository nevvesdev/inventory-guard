package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.docs.ApiDocs;
import br.com.nevvesdev.inventory.api.dto.ReplenishStockRequest;
import br.com.nevvesdev.inventory.api.dto.StockResponse;
import br.com.nevvesdev.inventory.application.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Estoque", description = "Consulta e reposição do saldo de estoque")
@RestController
@RequestMapping("/api/v1/products/{productId}/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @Operation(summary = "Consulta o saldo de estoque de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo atual"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public StockResponse getByProductId(@PathVariable UUID productId) {
        return StockResponse.from(stockService.getByProductId(productId));
    }

    @Operation(
            summary = "Repõe estoque de forma atômica",
            description = "Soma a quantidade ao saldo diretamente no banco, sem ler e gravar na aplicação, "
                    + "e registra a reposição no histórico de movimentações."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo atualizado"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(mediaType = ApiDocs.PROBLEM_JSON,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/replenishments")
    public StockResponse replenish(
            @PathVariable UUID productId,
            @Valid @RequestBody ReplenishStockRequest request
    ) {
        return StockResponse.from(stockService.replenish(productId, request.quantity()));
    }
}