package com.dd.backend.catalog.infrastructure.outbound.external.dto;

import java.math.BigDecimal;
import java.util.List;

/** Item conforme retornado por https://dummyjson.com/products. */
public record DummyJsonProductDto(
        Long id,
        String title,
        BigDecimal price,
        String description,
        String category,
        String brand,
        Integer stock,
        List<String> images,
        String thumbnail) {
}
