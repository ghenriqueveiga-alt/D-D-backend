package com.dd.backend.catalog.application;

import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.application.port.inbound.ListProductsUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Casos de consulta do catálogo (read side). */
@Service
@RequiredArgsConstructor
public class CatalogQueryService implements ListProductsUseCase, GetProductUseCase {

    private final ProductRepository productRepository;

    @Override
    public List<Product> execute(String category) {
        if (category == null || category.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.findByCategory(category.trim());
    }

    @Override
    public Product execute(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Produto", productId));
    }
}
