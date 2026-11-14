package com.dd.backend.stock.application;

import com.dd.backend.catalog.application.port.inbound.ListProductsUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.stock.application.port.inbound.GetStockSummaryUseCase;
import com.dd.backend.stock.application.port.inbound.ListStockMovementsUseCase;
import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Casos de consulta do estoque (read side): histórico e resumo. */
@Service
@RequiredArgsConstructor
public class StockQueryService implements ListStockMovementsUseCase, GetStockSummaryUseCase {

    /** Abaixo ou igual a este saldo o item entra na lista de estoque baixo. */
    static final int LOW_STOCK_THRESHOLD = 5;

    private final StockMovementRepository movementRepository;
    private final ListProductsUseCase listProducts;

    @Override
    public List<StockMovement> execute(Long productId) {
        if (productId == null) {
            return movementRepository.findAll();
        }
        return movementRepository.findByProductId(productId);
    }

    @Override
    public StockSummary execute() {
        List<Product> products = listProducts.execute(null);

        long totalUnits = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        int outOfStock = 0;
        int lowStock = 0;

        for (Product product : products) {
            int stock = product.getStock();
            totalUnits += stock;
            totalValue = totalValue.add(
                    product.getPrice().amount().multiply(BigDecimal.valueOf(stock)));
            if (stock == 0) {
                outOfStock++;
            } else if (stock <= LOW_STOCK_THRESHOLD) {
                lowStock++;
            }
        }

        return new StockSummary(
                products.size(),
                totalUnits,
                totalValue,
                outOfStock,
                lowStock,
                movementRepository.count());
    }
}
