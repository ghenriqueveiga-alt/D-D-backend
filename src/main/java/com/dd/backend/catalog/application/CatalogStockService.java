package com.dd.backend.catalog.application;

import com.dd.backend.catalog.application.port.inbound.AdjustStockUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Caso de uso de ajuste de estoque do catálogo (write side). */
@Service
@RequiredArgsConstructor
public class CatalogStockService implements AdjustStockUseCase {

    private final ProductRepository productRepository;

    @Override
    public Product execute(Long productId, int delta) {
        if (productId == null) {
            throw new DomainException("Produto é obrigatório para ajustar o estoque");
        }
        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> ResourceNotFoundException.of("Produto", productId));
        product.adjustStock(delta);
        return productRepository.save(product);
    }
}
