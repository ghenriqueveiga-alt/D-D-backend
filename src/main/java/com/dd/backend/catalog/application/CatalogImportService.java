package com.dd.backend.catalog.application;

import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de importação em lote do catálogo.
 *
 * <p>Roda em duas fases: primeiro valida todas as entradas (o domínio rejeita
 * as inválidas), depois persiste apenas as válidas. Assim uma linha ruim nunca
 * interrompe a importação das demais — ela é devolvida em {@code rejections}.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogImportService implements ImportProductsUseCase {

    private final ProductRepository productRepository;

    @Override
    public ImportResult execute(List<ProductEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new DomainException("Nenhum produto enviado para importação");
        }

        List<Candidate> candidates = new ArrayList<>();
        List<Rejection> rejections = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int index = 0; index < entries.size(); index++) {
            ProductEntry entry = entries.get(index);
            try {
                if (entry == null) {
                    throw new DomainException("Produto vazio na importação");
                }
                // O domínio valida os campos obrigatórios, preço e estoque.
                Product imported = Product.sync(entry.externalId(), entry.title(), entry.description(),
                        toMoney(entry.price()), entry.category(), entry.brand(), entry.imageUrl(),
                        toStock(entry.stock()));
                if (!seen.add(imported.getExternalId())) {
                    throw new DomainException("Produto repetido na importação: " + imported.getExternalId());
                }
                // Upsert: atualiza a linha existente mantendo a identidade local.
                Product resolved = productRepository.findByExternalId(imported.getExternalId())
                        .map(existing -> existing.update(
                                imported.getTitle(), imported.getDescription(), imported.getPrice(),
                                imported.getCategory(), imported.getBrand(), imported.getImageUrl(),
                                imported.getStock()))
                        .orElse(imported);
                candidates.add(new Candidate(resolved, resolved == imported));
            } catch (DomainException ex) {
                rejections.add(new Rejection(index, entry == null ? null : entry.externalId(), ex.getMessage()));
            }
        }

        int created = 0;
        int updated = 0;
        for (Candidate candidate : candidates) {
            productRepository.save(candidate.product());
            if (candidate.isNew()) {
                created++;
            } else {
                updated++;
            }
        }
        return new ImportResult(created, updated, rejections);
    }

    private Money toMoney(BigDecimal price) {
        if (price == null) {
            throw new DomainException("Preço do produto é obrigatório");
        }
        // Money.of rejeita valor negativo.
        return Money.of(price);
    }

    private int toStock(Integer stock) {
        if (stock == null) {
            throw new DomainException("Estoque do produto é obrigatório");
        }
        // O domínio rejeita estoque negativo.
        return stock;
    }

    /** Produto pronto para salvar e se já existia no catálogo. */
    private record Candidate(Product product, boolean isNew) {
    }
}
