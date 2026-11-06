package com.dd.backend.catalog.infrastructure.inbound.web;

import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.application.port.inbound.ListProductsUseCase;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: consulta do catálogo. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Consulta de produtos sincronizados da internet")
public class ProductController {

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;

    @GetMapping
    @Operation(summary = "Lista produtos (filtro opcional por categoria)")
    public List<ProductResponse> list(@RequestParam(name = "category", required = false) String category) {
        return listProducts.execute(category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um produto pelo id local")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(getProduct.execute(id));
    }
}
