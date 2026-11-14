package com.dd.backend.stock.application.port.inbound;

import java.math.BigDecimal;

/** Porta de entrada: resumo consolidado do estoque para o painel. */
public interface GetStockSummaryUseCase {

    /** Executa a apuração do resumo. */
    StockSummary execute();

    /**
     * Retrato atual do estoque: quantidade de SKUs, unidades, valor total
     * (preço × saldo), itens em falta, itens com estoque baixo e total de
     * movimentações registradas.
     */
    record StockSummary(
            int productCount,
            long totalUnits,
            BigDecimal totalValue,
            int outOfStockCount,
            int lowStockCount,
            long movementCount) {
    }
}
