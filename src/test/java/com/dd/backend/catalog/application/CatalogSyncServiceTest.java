package com.dd.backend.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dd.backend.catalog.application.port.outbound.ProductSource;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * O caso de uso de sync roda com fakes das duas ports — os adapters
 * (DummyJSON e JPA) são testados à parte, conforme o hexágono.
 */
class CatalogSyncServiceTest {

    private FakeProductRepository repository;
    private FakeProductSource source;
    private CatalogSyncService service;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
        source = new FakeProductSource();
        service = new CatalogSyncService(source, repository);
    }

    @Test
    void deveSincronizarItensDaFonteExterna() {
        source.products = List.of(
                external("1", "Teclado Mecânico", "99.90"),
                external("2", "Mouse Gamer", "49.90"));

        int processed = service.execute();

        assertEquals(2, processed);
        assertEquals(2, repository.saved.size());
        assertEquals("Teclado Mecânico", repository.saved.get(0).getTitle());
    }

    @Test
    void segundaSincronizacaoAtualizaExistenteSemDuplicar() {
        source.products = List.of(external("1", "Teclado Mecânico", "99.90"));
        service.execute();

        source.products = List.of(external("1", "Teclado Mecânico Pro", "199.90"));
        int processed = service.execute();

        assertEquals(1, processed);
        assertEquals(1, repository.saved.size());
        Product updated = repository.saved.get(0);
        assertEquals("Teclado Mecânico Pro", updated.getTitle());
        assertEquals(new BigDecimal("199.90"), updated.getPrice().amount());
    }

    private static ProductSource.ExternalProduct external(String id, String title, String price) {
        return new ProductSource.ExternalProduct(
                id, title, "descrição", new BigDecimal(price), "teclados", "Marca", "http://img", 5);
    }

    private static final class FakeProductSource implements ProductSource {
        private List<ExternalProduct> products = List.of();

        @Override
        public List<ExternalProduct> fetchAll() {
            return products;
        }
    }

    private static final class FakeProductRepository implements ProductRepository {
        private final List<Product> saved = new ArrayList<>();

        @Override
        public Product save(Product product) {
            if (product.getId() == null) {
                product.assignId((long) (saved.size() + 1));
            }
            saved.removeIf(existing -> existing.getExternalId().equals(product.getExternalId()));
            saved.add(product);
            return product;
        }

        @Override
        public Optional<Product> findById(Long id) {
            return saved.stream().filter(product -> product.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Product> findByExternalId(String externalId) {
            return saved.stream().filter(product -> product.getExternalId().equals(externalId)).findFirst();
        }

        @Override
        public List<Product> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public List<Product> findByCategory(String category) {
            return saved.stream()
                    .filter(product -> category.equalsIgnoreCase(product.getCategory()))
                    .toList();
        }
    }
}
