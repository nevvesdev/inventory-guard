package br.com.nevvesdev.inventory.api.controller;

import br.com.nevvesdev.inventory.api.dto.CreateProductRequest;
import br.com.nevvesdev.inventory.api.dto.PageResponse;
import br.com.nevvesdev.inventory.api.dto.ProductResponse;
import br.com.nevvesdev.inventory.application.service.ProductService;
import br.com.nevvesdev.inventory.application.view.ProductView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductView view = productService.create(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(view.id())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(view));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return ProductResponse.from(productService.getById(id));
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(productService.list(pageable).map(ProductResponse::from));
    }
}