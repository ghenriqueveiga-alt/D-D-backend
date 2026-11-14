package com.dd.backend.stock.infrastructure.outbound.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA — detalhe de infra, invisível para o domínio. */
public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, Long> {

    List<StockMovementJpaEntity> findAllByOrderByOccurredAtDesc();

    List<StockMovementJpaEntity> findByProductIdOrderByOccurredAtDesc(Long productId);
}
