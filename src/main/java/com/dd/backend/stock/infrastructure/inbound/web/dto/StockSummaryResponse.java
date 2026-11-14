package com.dd.backend.stock.infrastructure.inbound.web.dto;

import com.dd.backend.stock.application.port.inbound.GetStockSummaryUseCase.StockSummary;
import java.math.BigDecimal;

/** Resposta do resumo consolidado do estoque. */
public record StockSummaryResponse(
        int productCount,
        long totalUnits,
        BigDecimal totalValue,
        int outOfStockCount,
        int lowStockCount,
        long movementCount) {

    public static StockSummaryResponse from(StockSummary summary) {
        return new StockSummaryResponse(
                summary.productCount(),
                summary.totalUnits(),
                summary.totalValue(),
                summary.outOfStockCount(),
                summary.lowStockCount(),
                summary.movementCount());
    }
}
