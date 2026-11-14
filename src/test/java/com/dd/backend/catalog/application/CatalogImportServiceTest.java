package com.dd.backend.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase;
import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase.ImportResult;
import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase.ProductEntry;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.DomainException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A importação em lote roda com um fake da porta {@code ProductRepository} —
 * o adapter JPA é testado à parte, conforme o hexágono.
 */
class CatalogImportServiceTest {

    private FakeProductRepository repository;
    private CatalogImportService service;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
        service = new CatalogImportService(repository);
    }

    @Test
    void deveImportarProdutosNovos() {
        ImportResult result = service.execute(List.of(
                entry("1", "Teclado Mecânico", "99.90"),
                entry("2", "Mouse Gamer", "49.90")));

        assertEquals(2, result.created());
        assertEquals(0, result.updated());
        assertTrue(result.rejections().isEmpty());
        assertEquals(2, repository.saved.size());
        assertEquals("Teclado Mecânico", repository.saved.get(0).getTitle());
    }

    @Test
    void deveAtualizarProdutoExistenteSemDuplicar() {
        service.execute(List.of(entry("1", "Teclado Mecânico", "99.90")));

        ImportResult result = service.execute(List.of(entry("1", "Teclado Mecânico Pro", "199.90")));

        assertEquals(0, result.created());
        assertEquals(1, result.updated());
        assertEquals(1, repository.saved.size());
        Product updated = repository.saved.get(0);
        assertEquals("Teclado Mecânico Pro", updated.getTitle());
        assertEquals(new BigDecimal("199.90"), updated.getPrice().amount());
    }

    @Test
    void deveRecusarEntradasInvalidasEPersistirAsValidas() {
        List<ProductEntry> entries = new ArrayList<>();
        entries.add(entry("1", "Teclado Mecânico", "99.90"));
        entries.add(entry("", "Sem Código", "10.00"));
        entries.add(new ProductEntry("2", "Sem Preço", "desc", null, "teclados", "Marca", "http://img", 3));
        entries.add(new ProductEntry("3", "Estoque Negativo", "desc", new BigDecimal("10.00"),
                "teclados", "Marca", "http://img", -1));
        entries.add(entry("4", "Mouse Gamer", "49.90"));

        ImportResult result = service.execute(entries);

        assertEquals(2, result.created());
        assertEquals(3, result.rejections().size());
        assertEquals(1, result.rejections().get(0).index());
        assertEquals(2, result.rejections().get(1).index());
        assertEquals(3, result.rejections().get(2).index());
        assertEquals("Preço do produto é obrigatório", result.rejections().get(1).message());
        assertEquals(2, repository.saved.size());
    }

    @Test
    void deveRecusarProdutosRepetidosNoMesmoLote() {
        ImportResult result = service.execute(List.of(
                entry("1", "Teclado Mecânico", "99.90"),
                entry("1", "Teclado Duplicado", "100.00")));

        assertEquals(1, result.created());
        assertEquals(1, result.rejections().size());
        assertEquals("Teclado Mecânico", repository.saved.get(0).getTitle());
    }

    @Test
    void deveFalharQuandoNadaForEnviado() {
        assertThrows(DomainException.class, () -> service.execute(List.of()));
        assertThrows(DomainException.class, () -> service.execute(null));
    }

    private static ProductEntry entry(String externalId, String title, String price) {
        return new ProductEntry(
                externalId, title, "descrição", new BigDecimal(price), "teclados", "Marca", "http://img", 5);
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
