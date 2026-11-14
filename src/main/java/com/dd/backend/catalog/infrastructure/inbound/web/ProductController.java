package com.dd.backend.catalog.infrastructure.inbound.web;

import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase;
import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase.ProductEntry;
import com.dd.backend.catalog.application.port.inbound.ListProductsUseCase;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.ImportProductsRequest;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.ImportProductsRequest.ProductRequest;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.ImportProductsResponse;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: consulta do catálogo e importação em lote. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Consulta e importação de produtos do catálogo")
public class ProductController {

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;
    private final ImportProductsUseCase importProducts;

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

    @PostMapping("/import")
    @Operation(summary = "Importa vários produtos em lote (dados dos arquivos CSV/JSON convertidos pelo frontend)")
    public ImportProductsResponse importProducts(@Valid @RequestBody ImportProductsRequest request) {
        List<ProductEntry> entries = request.products().stream().map(ProductRequest::toEntry).toList();
        return ImportProductsResponse.from(this.importProducts.execute(entries));
    }
}
