package com.dd.backend.catalog.infrastructure.inbound.web.dto;

import com.dd.backend.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String externalId,
        String title,
        String description,
        BigDecimal price,
        String category,
        String brand,
        String imageUrl,
        int stock,
        Instant createdAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getExternalId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice().amount(),
                product.getCategory(),
                product.getBrand(),
                product.getImageUrl(),
                product.getStock(),
                product.getCreatedAt());
    }
}
