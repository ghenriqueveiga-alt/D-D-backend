package com.dd.backend.stock.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.catalog.application.port.inbound.AdjustStockUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase.Command;
import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.model.StockMovement.Type;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A movimentação roda com fakes das portas {@code StockMovementRepository} e
 * {@code AdjustStockUseCase} — os adapters são testados à parte, conforme o
 * hexágono. O ajuste de saldo usa o domínio real de {@link Product}.
 */
class StockMovementServiceTest {

    private FakeMovementRepository movements;
    private FakeAdjustStock adjustStock;
    private StockMovementService service;

    @BeforeEach
    void setUp() {
        movements = new FakeMovementRepository();
        adjustStock = new FakeAdjustStock();
        adjustStock.add(Product.restore(1L, "SKU-PS5", "PlayStation 5 Slim", null,
                Money.of("4499.90"), "gaming", "Sony", null, 10,
                Instant.parse("2026-01-01T00:00:00Z")));
        service = new StockMovementService(movements, adjustStock);
    }

    @Test
    void registraEntradaESomaNoSaldo() {
        StockMovement movement = service.execute(new Command(1L, Type.IN, 5, "reposição do fornecedor"));

        assertNotNull(movement.getId());
        assertEquals(Type.IN, movement.getType());
        assertEquals(5, movement.getQuantity());
        assertEquals("PlayStation 5 Slim", movement.getProductTitle());
        assertEquals("reposição do fornecedor", movement.getNote());
        assertEquals(15, adjustStock.get(1L).getStock());
        assertEquals(1, movements.count());
    }

    @Test
    void registraSaidaESubtraiDoSaldo() {
        StockMovement movement = service.execute(new Command(1L, Type.OUT, 3, "venda na loja"));

        assertEquals(Type.OUT, movement.getType());
        assertEquals(7, adjustStock.get(1L).getStock());
        assertEquals(1, movements.count());
    }

    @Test
    void saidaAcimaDoSaldoERecusadaSemEfeito() {
        assertThrows(DomainException.class,
                () -> service.execute(new Command(1L, Type.OUT, 11, null)));

        assertEquals(10, adjustStock.get(1L).getStock(), "saldo não pode mudar quando a saída é recusada");
        assertEquals(0, movements.count(), "nada é registrado quando a saída é recusada");
    }

    @Test
    void produtoInexistenteERecusado() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new Command(99L, Type.IN, 1, null)));

        assertEquals(0, movements.count());
    }

    @Test
    void quantidadeInvalidaERecusadaAntesDeTocarNoSaldo() {
        assertThrows(DomainException.class,
                () -> service.execute(new Command(1L, Type.IN, 0, null)));
        assertThrows(DomainException.class,
                () -> service.execute(new Command(1L, Type.IN, -2, null)));

        assertEquals(10, adjustStock.get(1L).getStock());
        assertEquals(0, movements.count());
    }

    @Test
    void tipoOuProdutoAusenteSaoRecusados() {
        assertThrows(DomainException.class, () -> service.execute(new Command(1L, null, 2, null)));
        assertThrows(DomainException.class, () -> service.execute(new Command(null, Type.IN, 2, null)));
        assertThrows(DomainException.class, () -> service.execute(null));

        assertEquals(0, movements.count());
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

    /** Fake da porta de ajuste de estoque do catálogo (regras reais do domínio). */
    private static final class FakeAdjustStock implements AdjustStockUseCase {

        private final Map<Long, Product> products = new HashMap<>();

        void add(Product product) {
            products.put(product.getId(), product);
        }

        Product get(Long id) {
            return products.get(id);
        }

        @Override
        public Product execute(Long productId, int delta) {
            Product product = products.get(productId);
            if (product == null) {
                throw ResourceNotFoundException.of("Produto", productId);
            }
            product.adjustStock(delta);
            return product;
        }
    }
}
