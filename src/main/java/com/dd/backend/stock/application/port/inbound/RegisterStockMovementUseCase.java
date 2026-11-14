package com.dd.backend.stock.application.port.inbound;

import com.dd.backend.stock.domain.model.StockMovement;

/** Porta de entrada: registra uma movimentação de estoque (entrada ou saída). */
public interface RegisterStockMovementUseCase {

    /** Executa a movimentação e devolve o registro criado. */
    StockMovement execute(Command command);

    /** Dados da movimentação a registrar. */
    record Command(Long productId, StockMovement.Type type, int quantity, String note) {
    }
}
