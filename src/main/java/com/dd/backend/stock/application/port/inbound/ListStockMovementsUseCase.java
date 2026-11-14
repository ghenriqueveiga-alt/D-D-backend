package com.dd.backend.stock.application.port.inbound;

import com.dd.backend.stock.domain.model.StockMovement;
import java.util.List;

/** Porta de entrada: lista o histórico de movimentações de estoque. */
public interface ListStockMovementsUseCase {

    /** Lista todas as movimentações, ou apenas as do produto informado (opcional). */
    List<StockMovement> execute(Long productId);
}
