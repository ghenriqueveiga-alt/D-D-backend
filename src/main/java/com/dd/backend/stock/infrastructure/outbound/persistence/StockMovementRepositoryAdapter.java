package com.dd.backend.stock.infrastructure.outbound.persistence;

import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adapter de saída: implementa a porta {@link StockMovementRepository} sobre JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final StockMovementJpaRepository jpaRepository;

    @Override
    public StockMovement save(StockMovement movement) {
        StockMovementJpaEntity saved = jpaRepository.save(toEntity(movement));
        if (movement.getId() == null) {
            movement.assignId(saved.getId());
        }
        return movement;
    }

    @Override
    public List<StockMovement> findAll() {
        return jpaRepository.findAllByOrderByOccurredAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<StockMovement> findByProductId(Long productId) {
        return jpaRepository.findByProductIdOrderByOccurredAtDesc(productId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    private StockMovement toDomain(StockMovementJpaEntity entity) {
        return StockMovement.restore(
                entity.getId(),
                entity.getProductId(),
                entity.getProductTitle(),
                entity.getType(),
                entity.getQuantity(),
                entity.getNote(),
                entity.getOccurredAt());
    }

    private StockMovementJpaEntity toEntity(StockMovement movement) {
        StockMovementJpaEntity entity = new StockMovementJpaEntity();
        entity.setId(movement.getId());
        entity.setProductId(movement.getProductId());
        entity.setProductTitle(movement.getProductTitle());
        entity.setType(movement.getType());
        entity.setQuantity(movement.getQuantity());
        entity.setNote(movement.getNote());
        entity.setOccurredAt(movement.getOccurredAt());
        return entity;
    }
}
