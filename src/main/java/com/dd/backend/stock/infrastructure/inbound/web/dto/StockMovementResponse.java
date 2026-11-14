package com.dd.backend.stock.infrastructure.inbound.web.dto;

import com.dd.backend.stock.domain.model.StockMovement;
import java.time.Instant;

/** Resposta de uma movimentação de estoque. */
public record StockMovementResponse(
        Long id,
        Long productId,
        String productTitle,
        String type,
        int quantity,
        String note,
        Instant occurredAt) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProductId(),
                movement.getProductTitle(),
                movement.getType().name(),
                movement.getQuantity(),
                movement.getNote(),
                movement.getOccurredAt());
    }
}
