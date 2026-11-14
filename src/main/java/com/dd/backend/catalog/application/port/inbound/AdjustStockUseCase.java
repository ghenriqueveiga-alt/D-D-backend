package com.dd.backend.catalog.application.port.inbound;

import com.dd.backend.catalog.domain.model.Product;

/**
 * Porta de entrada: ajusta o estoque de um produto.
 *
 * <p>Usada pelo módulo de estoque para registrar entradas (delta positivo) e
 * saídas (delta negativo) sem depender do adapter de persistência do catálogo.
 * A regra de saldo nunca negativo é do domínio {@link Product}.</p>
 */
public interface AdjustStockUseCase {

    /** Aplica o delta no estoque e devolve o produto atualizado. */
    Product execute(Long productId, int delta);
}
