package com.dd.backend.stock.domain.repository;

import com.dd.backend.stock.domain.model.StockMovement;
import java.util.List;

/** Porta de saída (hexagonal) de persistência das movimentações de estoque. */
public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    /** Todas as movimentações, mais recentes primeiro. */
    List<StockMovement> findAll();

    /** Movimentações de um produto, mais recentes primeiro. */
    List<StockMovement> findByProductId(Long productId);

    long count();
}
