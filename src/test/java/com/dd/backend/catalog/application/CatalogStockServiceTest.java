package com.dd.backend.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ajuste de estoque do catálogo com fake da porta {@code ProductRepository}. */
class CatalogStockServiceTest {

    private FakeProductRepository repository;
    private CatalogStockService service;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
        repository.add(Product.restore(1L, "SKU-1", "Cafeteira", null, Money.of("299.90"),
                "kitchen-accessories", "Marca", null, 10, Instant.parse("2026-01-01T00:00:00Z")));
        service = new CatalogStockService(repository);
    }

    @Test
    void entradaSomaNoSaldo() {
        Product updated = service.execute(1L, 5);

        assertEquals(15, updated.getStock());
        assertEquals(15, repository.get(1L).getStock());
    }

    @Test
    void saidaSubtraiDoSaldo() {
        Product updated = service.execute(1L, -3);

        assertEquals(7, updated.getStock());
    }

    @Test
    void saidaAcimaDoSaldoERecusada() {
        assertThrows(DomainException.class, () -> service.execute(1L, -11));

        assertEquals(10, repository.get(1L).getStock());
    }

    @Test
    void produtoInexistenteERecusado() {
        assertThrows(ResourceNotFoundException.class, () -> service.execute(99L, 1));
    }

    @Test
    void produtoNuloERecusado() {
        assertThrows(DomainException.class, () -> service.execute(null, 1));
    }

    /** Fake em memória da porta de persistência do catálogo. */
    private static final class FakeProductRepository implements ProductRepository {

        private final Map<Long, Product> stored = new HashMap<>();

        void add(Product product) {
            stored.put(product.getId(), product);
        }

        Product get(Long id) {
            return stored.get(id);
        }

        @Override
        public Product save(Product product) {
            stored.put(product.getId(), product);
            return product;
        }

        @Override
        public Optional<Product> findById(Long id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public Optional<Product> findByExternalId(String externalId) {
            return stored.values().stream().filter(p -> p.getExternalId().equals(externalId)).findFirst();
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(stored.values());
        }

        @Override
        public List<Product> findByCategory(String category) {
            return stored.values().stream().filter(p -> category.equals(p.getCategory())).toList();
        }
    }
}
