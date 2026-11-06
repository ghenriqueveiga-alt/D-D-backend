package com.dd.backend.catalog.domain.repository;

import com.dd.backend.catalog.domain.model.Product;
import java.util.List;
import java.util.Optional;

/** Porta de saída (hexagonal) de persistência do catálogo. */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findByExternalId(String externalId);

    List<Product> findAll();

    List<Product> findByCategory(String category);
}
