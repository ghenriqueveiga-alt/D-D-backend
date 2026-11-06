package com.dd.backend.catalog.application.port.outbound;

import java.math.BigDecimal;
import java.util.List;

/**
 * Porta de saída (hexagonal) para a fonte externa de itens.
 * Implementada por adapters como o DummyJSON — trocar de fonte = novo adapter.
 */
public interface ProductSource {

    /** Busca todos os itens disponíveis na fonte externa. */
    List<ExternalProduct> fetchAll();

    /** Modelo de dados conforme exposto pela fonte externa. */
    record ExternalProduct(
            String externalId,
            String title,
            String description,
            BigDecimal price,
            String category,
            String brand,
            String imageUrl,
            int stock) {
    }
}
