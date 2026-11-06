package com.dd.backend.catalog.infrastructure.outbound.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    Optional<ProductJpaEntity> findByExternalId(String externalId);

    List<ProductJpaEntity> findByCategoryIgnoreCase(String category);
}
