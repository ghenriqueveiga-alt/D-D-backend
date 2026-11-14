package com.dd.backend.stock.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.catalog.application.port.inbound.ListProductsUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.shared.domain.Money;
import com.dd.backend.stock.application.port.inbound.GetStockSummaryUseCase.StockSummary;
import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.model.StockMovement.Type;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Consultas do estoque com fakes das portas (histórico e produtos). */
class StockQueryServiceTest {

    private FakeMovementRepository movements;
    private FakeListProducts listProducts;
    private StockQueryService service;

    @BeforeEach
    void setUp() {
        movements = new FakeMovementRepository();
        movements.save(StockMovement.register(1L, "Notebook", Type.OUT, 1, "venda"));
        movements.save(StockMovement.register(2L, "Tênis", Type.IN, 4, null));

        listProducts = new FakeListProducts(List.of(
                product(1L, "Notebook", "50.00", 10),
                product(2L, "Tênis", "20.00", 3),
                product(3L, "Fone", "10.00", 0)));

        service = new StockQueryService(movements, listProducts);
    }

    @Test
    void listaTodoOHistoricoSemFiltro() {
        List<StockMovement> all = service.execute(null);

        assertEquals(2, all.size());
    }

    @Test
    void filtraHistoricoPorProduto() {
        List<StockMovement> filtered = service.execute(2L);

        assertEquals(1, filtered.size());
        assertEquals(2L, filtered.get(0).getProductId());
        assertEquals(Type.IN, filtered.get(0).getType());
    }

    @Test
    void resumoSomaUnidadesValorEFaltas() {
        StockSummary summary = service.execute();

        assertEquals(3, summary.productCount());
        assertEquals(13, summary.totalUnits());
        assertEquals(new BigDecimal("560.00"), summary.totalValue());
        assertEquals(1, summary.outOfStockCount());
        assertEquals(1, summary.lowStockCount());
        assertEquals(2, summary.movementCount());
    }

    @Test
    void resumoConsideraEstoqueBaixoAteCincoUnidades() {
        listProducts.replace(List.of(product(1L, "Item", "10.00", 5)));

        StockSummary summary = service.execute();

        assertEquals(1, summary.lowStockCount());
        assertEquals(0, summary.outOfStockCount());
        assertTrue(summary.totalValue().compareTo(new BigDecimal("50.00")) == 0);
    }

    private static Product product(Long id, String title, String price, int stock) {
        return Product.restore(id, "SKU-" + id, title, null, Money.of(price),
                "geral", "Marca", null, stock, Instant.parse("2026-01-01T00:00:00Z"));
    }

    /** Fake em memória da porta de histórico. */
    private static final class FakeMovementRepository implements StockMovementRepository {

        private final List<StockMovement> stored = new ArrayList<>();
        private long nextId = 1;

        @Override
        public StockMovement save(StockMovement movement) {
            if (movement.getId() == null) {
                movement.assignId(nextId++);
            }
            stored.add(movement);
            return movement;
        }

        @Override
        public List<StockMovement> findAll() {
            return List.copyOf(stored);
        }

        @Override
        public List<StockMovement> findByProductId(Long productId) {
            return stored.stream().filter(m -> m.getProductId().equals(productId)).toList();
        }

        @Override
        public long count() {
            return stored.size();
        }
    }

    /** Fake da porta de listagem de produtos do catálogo. */
    private static final class FakeListProducts implements ListProductsUseCase {

        private List<Product> products;

        FakeListProducts(List<Product> products) {
            this.products = products;
        }

        void replace(List<Product> products) {
            this.products = products;
        }

        @Override
        public List<Product> execute(String category) {
            return products;
        }
    }
}
