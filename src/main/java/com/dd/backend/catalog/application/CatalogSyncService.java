package com.dd.backend.catalog.application;

import com.dd.backend.catalog.application.port.inbound.SyncCatalogUseCase;
import com.dd.backend.catalog.application.port.outbound.ProductSource;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.catalog.domain.repository.ProductRepository;
import com.dd.backend.shared.domain.Money;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de sincronização do catálogo: busca itens na internet (porta de
 * saída {@link ProductSource}) e faz upsert no catálogo local.
 */
@Service
@RequiredArgsConstructor
public class CatalogSyncService implements SyncCatalogUseCase {

    private final ProductSource productSource;
    private final ProductRepository productRepository;

    @Override
    public int execute() {
        List<ProductSource.ExternalProduct> fetched = productSource.fetchAll();
        int processed = 0;
        for (ProductSource.ExternalProduct external : fetched) {
            Product product = productRepository.findByExternalId(external.externalId())
                    .map(existing -> existing.update(
                            external.title(), external.description(), toMoney(external),
                            external.category(), external.brand(), external.imageUrl(), external.stock()))
                    .orElseGet(() -> Product.sync(
                            external.externalId(), external.title(), external.description(), toMoney(external),
                            external.category(), external.brand(), external.imageUrl(), external.stock()));
            productRepository.save(product);
            processed++;
        }
        return processed;
    }

    private Money toMoney(ProductSource.ExternalProduct external) {
        return external.price() == null ? Money.zero() : Money.of(external.price());
    }
}
