package com.dd.backend.catalog.application.port.inbound;

import com.dd.backend.catalog.domain.model.Product;

/** Porta de entrada: busca um produto pelo id local. */
public interface GetProductUseCase {

    Product execute(Long productId);
}
