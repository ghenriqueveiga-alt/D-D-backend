package com.dd.backend.catalog.application.port.inbound;

import com.dd.backend.catalog.domain.model.Product;
import java.util.List;

/** Porta de entrada: lista produtos do catálogo. */
public interface ListProductsUseCase {

    /** Lista todos os produtos, ou apenas os da categoria informada (opcional). */
    List<Product> execute(String category);
}
