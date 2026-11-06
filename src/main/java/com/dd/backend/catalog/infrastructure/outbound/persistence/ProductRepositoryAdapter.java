package com.dd.backend.catalog.infrastructure.outbound.persistence;

import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.Money;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adapter de saída: implementa a porta {@link ProductRepository} sobre JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        ProductJpaEntity saved = jpaRepository.save(toEntity(product));
        if (product.getId() == null) {
            product.assignId(saved.getId());
        }
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Product> findByExternalId(String externalId) {
        return jpaRepository.findByExternalId(externalId).map(this::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Product> findByCategory(String category) {
        return jpaRepository.findByCategoryIgnoreCase(category).stream().map(this::toDomain).toList();
    }

    private ProductJpaEntity toEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setExternalId(product.getExternalId());
        entity.setTitle(product.getTitle());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice().amount());
        entity.setCategory(product.getCategory());
        entity.setBrand(product.getBrand());
        entity.setImageUrl(product.getImageUrl());
        entity.setStock(product.getStock());
        entity.setCreatedAt(product.getCreatedAt());
        return entity;
    }

    private Product toDomain(ProductJpaEntity entity) {
        return Product.restore(
                entity.getId(),
                entity.getExternalId(),
                entity.getTitle(),
                entity.getDescription(),
                Money.of(entity.getPrice()),
                entity.getCategory(),
                entity.getBrand(),
                entity.getImageUrl(),
                entity.getStock() == null ? 0 : entity.getStock(),
                entity.getCreatedAt());
    }
}
